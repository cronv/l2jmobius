/*
 * Copyright (c) 2013 L2jMobius
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.l2jmobius.gameserver.managers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.LoginServerThread;
import org.l2jmobius.gameserver.data.holders.PunishmentHolder;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.IllegalActionPunishmentType;
import org.l2jmobius.gameserver.entity.actor.tasks.player.IllegalPlayerActionTask;
import org.l2jmobius.gameserver.mechanics.punishment.PunishmentAffect;
import org.l2jmobius.gameserver.mechanics.punishment.PunishmentTask;
import org.l2jmobius.gameserver.mechanics.punishment.PunishmentType;

/**
 * Manages the application of various punishments to players.<br>
 * It handles the execution of {@code PunishmentTask} objects and tracks active penalties.<br>
 * This class ensures that illegal actions result in appropriate consequences for characters.
 * @author UnAfraid, Skache
 */
public class PunishmentManager
{
	private static final Logger LOGGER = Logger.getLogger(PunishmentManager.class.getName());
	
	private final Map<PunishmentAffect, PunishmentHolder> _tasks = new ConcurrentHashMap<>();
	
	/**
	 * Initializes a new instance of the {@code PunishmentManager}.<br>
	 * This constructor calls the {@code load()} method to prepare data.
	 */
	protected PunishmentManager()
	{
		load();
	}
	
	/**
	 * Initializes the punishment system.<br>
	 * This method populates the {@code _tasks} map with new {@code PunishmentHolder} instances.<br>
	 * It then retrieves all records from the database to load active punishments into memory.
	 */
	private void load()
	{
		// Initiate task holders.
		for (PunishmentAffect affect : PunishmentAffect.values())
		{
			_tasks.put(affect, new PunishmentHolder());
		}
		
		int initiated = 0;
		int expired = 0;
		
		// Load punishments.
		try (Connection con = DatabaseFactory.getConnection();
			Statement st = con.createStatement();
			ResultSet rset = st.executeQuery("SELECT * FROM punishments"))
		{
			while (rset.next())
			{
				final int id = rset.getInt("id");
				final String key = rset.getString("key");
				final PunishmentAffect affect = PunishmentAffect.getByName(rset.getString("affect"));
				final PunishmentType type = PunishmentType.getByName(rset.getString("type"));
				final long expirationTime = rset.getLong("expiration");
				if ((type != null) && (affect != null))
				{
					if ((expirationTime > 0) && (System.currentTimeMillis() > expirationTime)) // expired task.
					{
						expired++;
					}
					else
					{
						initiated++;
						_tasks.get(affect).addPunishment(new PunishmentTask(id, key, affect, type, expirationTime, rset.getString("reason"), rset.getString("punishedBy"), true));
					}
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Error while loading punishments: " + e.getMessage());
		}
		
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + initiated + " active and " + expired + " expired punishments.");
	}
	
	/**
	 * Starts a new punishment task for a player or account.<br>
	 * This method applies the specific restrictions defined in the {@code PunishmentTask}.<br>
	 * It handles immediate actions like banning access levels based on the punishment type.
	 * @param task The {@code PunishmentTask} to be executed.
	 */
	public void startPunishment(PunishmentTask task)
	{
		final PunishmentAffect affect = task.getAffect();
		if (task.getType() == PunishmentType.BAN)
		{
			switch (affect)
			{
				case ACCOUNT:
				{
					final String accountName = (String) task.getKey();
					LoginServerThread.getInstance().sendAccessLevel(accountName, -1);
					break;
				}
				case CHARACTER:
				{
					final String charId = String.valueOf(task.getKey());
					changeCharAccessLevel(charId, -1);
					break;
				}
			}
		}
		
		_tasks.get(affect).addPunishment(task);
	}
	
	/**
	 * Stops an active punishment for a specific entity.<br>
	 * This method removes the punishment and restores access levels if applicable.
	 * @param key The unique identifier for the punished account or character.
	 * @param affect The scope of the punishment, such as {@code ACCOUNT} or {@code CHARACTER}.
	 * @param type The category of the punishment, such as {@code BAN}.
	 */
	public void stopPunishment(Object key, PunishmentAffect affect, PunishmentType type)
	{
		final PunishmentTask task = getPunishment(key, affect, type);
		if (task == null)
		{
			return;
		}
		
		if (type == PunishmentType.BAN)
		{
			switch (affect)
			{
				case ACCOUNT:
				{
					final String accountName = (String) task.getKey();
					LoginServerThread.getInstance().sendAccessLevel(accountName, 0);
					break;
				}
				case CHARACTER:
				{
					final String charId = String.valueOf(task.getKey());
					changeCharAccessLevel(charId, 0);
					break;
				}
			}
		}
		
		_tasks.get(affect).stopPunishment(task);
	}
	
	/**
	 * Updates the access level of a specific character in the database.<br>
	 * This method executes an {@code UPDATE} query on the characters table.<br>
	 * It logs a warning if a {@code SQLException} occurs during the process.
	 * @param charId The unique identifier of the character to update.
	 * @param lvl The new access level value to assign.
	 */
	private void changeCharAccessLevel(String charId, int lvl)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("UPDATE characters SET accessLevel=? WHERE charId=?"))
		{
			ps.setInt(1, lvl);
			ps.setString(2, charId);
			ps.executeUpdate();
		}
		catch (SQLException e)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Error updating access level for character: " + charId + ". " + e.getMessage());
		}
	}
	
	/**
	 * Checks if a specific punishment is currently active.<br>
	 * This method verifies the existence of a punishment based on the provided criteria.
	 * @param key The unique identifier for the entity being checked.
	 * @param affect The type of effect the punishment has.
	 * @param type The specific category of the punishment.
	 * @return {@code true} if the punishment exists, otherwise {@code false}.
	 */
	public boolean hasPunishment(Object key, PunishmentAffect affect, PunishmentType type)
	{
		final PunishmentHolder holder = _tasks.get(affect);
		return holder.hasPunishment(String.valueOf(key), type);
	}
	
	/**
	 * Retrieves the expiration time for a specific punishment.<br>
	 * It checks if a punishment exists for the given parameters.<br>
	 * If no punishment is found, it returns {@code 0}.
	 * @param key The unique identifier for the punished entity.
	 * @param affect The type of effect the punishment has on the entity.
	 * @param type The specific category of the punishment.
	 * @return The expiration timestamp as a {@code long}, or {@code 0} if none exists.
	 */
	public long getPunishmentExpiration(Object key, PunishmentAffect affect, PunishmentType type)
	{
		final PunishmentTask p = getPunishment(key, affect, type);
		return p != null ? p.getExpirationTime() : 0;
	}
	
	/**
	 * Retrieves a specific {@code PunishmentTask} based on the provided criteria.<br>
	 * This method looks up the task using the unique key, affect, and type.
	 * @param key The unique identifier for the punishment.
	 * @param affect The category of the punishment effect.
	 * @param type The specific type of punishment to retrieve.
	 * @return The {@code PunishmentTask} associated with the given parameters or {@code null}.
	 */
	public PunishmentTask getPunishment(Object key, PunishmentAffect affect, PunishmentType type)
	{
		return _tasks.get(affect).getPunishment(String.valueOf(key), type);
	}
	
	/**
	 * This method handles actions taken by a player that violate game rules.<br>
	 * It schedules a task to apply the specified punishment after a delay.
	 * @param actor The {@code Player} who performed the illegal action.
	 * @param message The text message to display regarding the violation.
	 * @param punishment The type of {@code IllegalActionPunishmentType} to apply.
	 */
	public static void handleIllegalPlayerAction(Player actor, String message, IllegalActionPunishmentType punishment)
	{
		ThreadPool.schedule(new IllegalPlayerActionTask(actor, message, punishment), 5000);
	}
	
	/**
	 * Retrieves the singleton instance of the {@code PunishmentManager}.<br>
	 * Use this method to access the global punishment system.
	 * @return The active {@code PunishmentManager} instance.
	 */
	public static PunishmentManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final PunishmentManager INSTANCE = new PunishmentManager();
	}
}
