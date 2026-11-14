/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2jmobius.gameserver.mechanics.punishment;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.handler.IPunishmentHandler;
import org.l2jmobius.gameserver.handler.PunishmentHandler;
import org.l2jmobius.gameserver.managers.PunishmentManager;
import org.l2jmobius.gameserver.mechanics.skill.AbnormalVisualEffect;

/**
 * This class handles the execution of specific punishment actions for players.<br>
 * It implements {@code Runnable} to be processed by a background thread pool.<br>
 * It interacts with {@link org.l2jmobius.gameserver.managers.PunishmentManager} to apply penalties.
 * @author UnAfraid
 */
public class PunishmentTask implements Runnable
{
	protected static final Logger LOGGER = Logger.getLogger(PunishmentTask.class.getName());
	
	private static final String INSERT_QUERY = "INSERT INTO punishments (`key`, `affect`, `type`, `expiration`, `reason`, `punishedBy`) VALUES (?, ?, ?, ?, ?, ?)";
	private static final String UPDATE_QUERY = "UPDATE punishments SET expiration = ? WHERE id = ?";
	
	private int _id;
	private final String _key;
	private final PunishmentAffect _affect;
	private final PunishmentType _type;
	private final long _expirationTime;
	private final String _reason;
	private final String _punishedBy;
	private boolean _isStored;
	private ScheduledFuture<?> _task = null;
	
	/**
	 * Creates a new {@code PunishmentTask} instance.<br>
	 * This constructor initializes a task that has not been stored in the database yet.
	 * @param key The unique identifier for the punishment.
	 * @param affect The type of effect applied by the punishment.
	 * @param type The category of the punishment.
	 * @param expirationTime The timestamp when the punishment expires.
	 * @param reason The description of why the punishment was issued.
	 * @param punishedBy The name or ID of the entity who issued the punishment.
	 */
	public PunishmentTask(Object key, PunishmentAffect affect, PunishmentType type, long expirationTime, String reason, String punishedBy)
	{
		this(0, key, affect, type, expirationTime, reason, punishedBy, false);
	}
	
	/**
	 * Creates a new {@code PunishmentTask} with the specified details.<br>
	 * This constructor initializes all fields and starts the punishment immediately.
	 * @param id The unique identifier for this task.
	 * @param key The object used as the primary key for identifying the target.
	 * @param affect The type of effect applied by the punishment.
	 * @param type The category of the punishment.
	 * @param expirationTime The timestamp when the punishment should end.
	 * @param reason A description of why the punishment was issued.
	 * @param punishedBy The name or ID of the entity that issued the punishment.
	 * @param isStored Whether this task should be saved to the database.
	 */
	public PunishmentTask(int id, Object key, PunishmentAffect affect, PunishmentType type, long expirationTime, String reason, String punishedBy, boolean isStored)
	{
		_id = id;
		_key = String.valueOf(key);
		_affect = affect;
		_type = type;
		_expirationTime = expirationTime;
		_reason = reason;
		_punishedBy = punishedBy;
		_isStored = isStored;
		startPunishment();
	}
	
	/**
	 * Retrieves the unique identifier for this punishment task.<br>
	 * This value was provided during the construction of the {@code PunishmentTask}.
	 * @return The {@code Object} key associated with this task.
	 */
	public Object getKey()
	{
		return _key;
	}
	
	/**
	 * Retrieves the {@code PunishmentAffect} associated with this task.<br>
	 * This represents the specific effect of the punishment.
	 * @return The {@code PunishmentAffect} object.
	 */
	public PunishmentAffect getAffect()
	{
		return _affect;
	}
	
	/**
	 * Retrieves the type of punishment associated with this task.
	 * @return the {@code PunishmentType} of the punishment.
	 */
	public PunishmentType getType()
	{
		return _type;
	}
	
	/**
	 * Retrieves the expiration time of the punishment.<br>
	 * This value represents when the punishment will end.
	 * @return The expiration time as a {@code long}.
	 */
	public long getExpirationTime()
	{
		return _expirationTime;
	}
	
	/**
	 * Retrieves the reason for the punishment.<br>
	 * This value is stored as a {@code String}.
	 * @return The reason string.
	 */
	public String getReason()
	{
		return _reason;
	}
	
	/**
	 * Retrieves the name of the entity that issued the punishment.<br>
	 * This value is stored as a {@code String}.
	 * @return The name of the punisher.
	 */
	public String getPunishedBy()
	{
		return _punishedBy;
	}
	
	/**
	 * Checks if the punishment has been saved to the database.
	 * @return {@code true} if the task is stored, {@code false} otherwise.
	 */
	public boolean isStored()
	{
		return _isStored;
	}
	
	/**
	 * Checks if the punishment has expired.<br>
	 * It compares the current system time against the expiration time.
	 * @return {@code true} if the current time is greater than the expiration time and that time is positive, otherwise {@code false}.
	 */
	public boolean isExpired()
	{
		return (_expirationTime > 0) && (System.currentTimeMillis() > _expirationTime);
	}
	
	/**
	 * Initializes the punishment process.<br>
	 * It checks if the punishment has expired before starting.<br>
	 * If it is still active, it calls {@code onStart()} and schedules the task.
	 */
	private void startPunishment()
	{
		if (isExpired())
		{
			return;
		}
		
		onStart();
		if (_expirationTime > 0) // Has expiration?
		{
			_task = ThreadPool.schedule(this, (_expirationTime - System.currentTimeMillis()));
		}
	}
	
	/**
	 * Stops the current punishment task.<br>
	 * This method cancels any active scheduled tasks.<br>
	 * It also triggers the {@code onEnd()} logic.
	 */
	public void stopPunishment()
	{
		abortTask();
		onEnd();
	}
	
	/**
	 * Cancels the scheduled punishment task if it is currently running.<br>
	 * This method sets the {@code _task} reference to {@code null}.<br>
	 * It checks if the task is not already cancelled or finished before calling {@code cancel(false)}.
	 */
	private void abortTask()
	{
		if (_task != null)
		{
			if (!_task.isCancelled() && !_task.isDone())
			{
				_task.cancel(false);
			}
			
			_task = null;
		}
	}
	
	/**
	 * Initializes the punishment task by saving it to the database if necessary.<br>
	 * It then triggers the start logic for the specific punishment handler.
	 */
	private void onStart()
	{
		if (!_isStored)
		{
			try (Connection con = DatabaseFactory.getConnection();
				PreparedStatement st = con.prepareStatement(INSERT_QUERY, Statement.RETURN_GENERATED_KEYS))
			{
				st.setString(1, _key);
				st.setString(2, _affect.name());
				st.setString(3, _type.name());
				st.setLong(4, _expirationTime);
				st.setString(5, _reason);
				st.setString(6, _punishedBy);
				st.execute();
				try (ResultSet rset = st.getGeneratedKeys())
				{
					if (rset.next())
					{
						_id = rset.getInt(1);
					}
				}
				
				_isStored = true;
			}
			catch (SQLException e)
			{
				LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Couldn't store punishment task for: " + _affect + " " + _key, e);
			}
		}
		
		final IPunishmentHandler handler = PunishmentHandler.getInstance().getHandler(_type);
		if (handler != null)
		{
			handler.onStart(this);
		}
	}
	
	/**
	 * Handles the logic for when a punishment task finishes.<br>
	 * Updates the database if the task was stored.<br>
	 * Removes specific visual effects for chat bans.<br>
	 * Notifies the appropriate {@code IPunishmentHandler}.
	 */
	private void onEnd()
	{
		if (_isStored)
		{
			try (Connection con = DatabaseFactory.getConnection();
				PreparedStatement st = con.prepareStatement(UPDATE_QUERY))
			{
				st.setLong(1, System.currentTimeMillis());
				st.setLong(2, _id);
				st.execute();
			}
			catch (SQLException e)
			{
				LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Couldn't update punishment task for: " + _affect + " " + _key + " id: " + _id, e);
			}
		}
		
		if ((_type == PunishmentType.CHAT_BAN) && (_affect == PunishmentAffect.CHARACTER))
		{
			final Player player = World.getPlayer(Integer.parseInt(_key));
			if (player != null)
			{
				player.getEffectList().stopAbnormalVisualEffect(AbnormalVisualEffect.NO_CHAT);
			}
		}
		
		final IPunishmentHandler handler = PunishmentHandler.getInstance().getHandler(_type);
		if (handler != null)
		{
			handler.onEnd(this);
		}
	}
	
	@Override
	public void run()
	{
		PunishmentManager.getInstance().stopPunishment(_key, _affect, _type);
	}
}
