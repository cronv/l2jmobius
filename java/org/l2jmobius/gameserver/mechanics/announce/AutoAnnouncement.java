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
package org.l2jmobius.gameserver.mechanics.announce;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.entity.World;

/**
 * Schedules and broadcasts an announcement to all online players at a fixed interval.
 * @author UnAfraid
 */
public class AutoAnnouncement extends Announcement implements Runnable
{
	private static final String INSERT_QUERY = "INSERT INTO announcements (`type`, `content`, `author`, `initial`, `delay`, `repeat`) VALUES (?, ?, ?, ?, ?, ?)";
	private static final String UPDATE_QUERY = "UPDATE announcements SET `type` = ?, `content` = ?, `author` = ?, `initial` = ?, `delay` = ?, `repeat` = ? WHERE id = ?";
	
	private long _initial;
	private long _delay;
	private int _repeat = -1;
	private int _currentState;
	private ScheduledFuture<?> _task;
	
	/**
	 * Creates an {@code AutoAnnouncement} with the given timing and starts its task.
	 * @param type The announcement type.
	 * @param content The message text.
	 * @param author The author name.
	 * @param initial The delay before the first broadcast, in milliseconds.
	 * @param delay The delay between repeats, in milliseconds.
	 * @param repeat The repeat count, or {@code -1} for unlimited.
	 */
	public AutoAnnouncement(AnnouncementType type, String content, String author, long initial, long delay, int repeat)
	{
		super(type, content, author);
		_initial = initial;
		_delay = delay;
		_repeat = repeat;
		restartMe();
	}
	
	/**
	 * Creates an {@code AutoAnnouncement} from a database row and starts its task.
	 * @param rset The {@code ResultSet} positioned on the announcement row.
	 * @throws SQLException If the row cannot be read.
	 */
	public AutoAnnouncement(ResultSet rset) throws SQLException
	{
		super(rset);
		_initial = rset.getLong("initial");
		_delay = rset.getLong("delay");
		_repeat = rset.getInt("repeat");
		restartMe();
	}
	
	/**
	 * Retrieves the delay before the first broadcast.
	 * @return The initial delay in milliseconds as a {@code long}.
	 */
	public long getInitial()
	{
		return _initial;
	}
	
	/**
	 * Sets the delay before the first broadcast.
	 * @param initial The initial delay in milliseconds.
	 */
	public void setInitial(long initial)
	{
		_initial = initial;
	}
	
	/**
	 * Retrieves the delay between repeats.
	 * @return The repeat delay in milliseconds as a {@code long}.
	 */
	public long getDelay()
	{
		return _delay;
	}
	
	/**
	 * Sets the delay between repeats.
	 * @param delay The repeat delay in milliseconds.
	 */
	public void setDelay(long delay)
	{
		_delay = delay;
	}
	
	/**
	 * Retrieves the repeat count.
	 * @return The repeat count as an {@code int}, or {@code -1} for unlimited.
	 */
	public int getRepeat()
	{
		return _repeat;
	}
	
	/**
	 * Sets the repeat count.
	 * @param repeat The repeat count, or {@code -1} for unlimited.
	 */
	public void setRepeat(int repeat)
	{
		_repeat = repeat;
	}
	
	/**
	 * Inserts this announcement into the database.
	 * @return {@code true} on success, {@code false} on error.
	 */
	@Override
	public boolean storeMe()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement st = con.prepareStatement(INSERT_QUERY, Statement.RETURN_GENERATED_KEYS))
		{
			st.setInt(1, getType().ordinal());
			st.setString(2, getContent());
			st.setString(3, getAuthor());
			st.setLong(4, _initial);
			st.setLong(5, _delay);
			st.setInt(6, _repeat);
			st.execute();
			try (ResultSet rset = st.getGeneratedKeys())
			{
				if (rset.next())
				{
					_id = rset.getInt(1);
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Couldn't store announcement: ", e);
			return false;
		}
		
		return true;
	}
	
	/**
	 * Updates this announcement in the database.
	 * @return {@code true} on success, {@code false} on error.
	 */
	@Override
	public boolean updateMe()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement st = con.prepareStatement(UPDATE_QUERY))
		{
			st.setInt(1, getType().ordinal());
			st.setString(2, getContent());
			st.setString(3, getAuthor());
			st.setLong(4, _initial);
			st.setLong(5, _delay);
			st.setLong(6, _repeat);
			st.setLong(7, getId());
			st.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Couldn't update announcement: ", e);
			return false;
		}
		
		return true;
	}
	
	/**
	 * Cancels the scheduled task and deletes this announcement from the database.
	 * @return {@code true} on success, {@code false} otherwise.
	 */
	@Override
	public boolean deleteMe()
	{
		if ((_task != null) && !_task.isCancelled())
		{
			_task.cancel(false);
		}
		
		return super.deleteMe();
	}
	
	/**
	 * Cancels any running task and reschedules this announcement after its initial delay.
	 */
	public void restartMe()
	{
		if ((_task != null) && !_task.isCancelled())
		{
			_task.cancel(false);
		}
		
		_currentState = _repeat;
		_task = ThreadPool.schedule(this, _initial);
	}
	
	@Override
	public void run()
	{
		if ((_currentState == -1) || (_currentState > 0))
		{
			for (String content : getContent().split(System.lineSeparator()))
			{
				World.broadcastToAllOnlinePlayers(content, (getType() == AnnouncementType.AUTO_CRITICAL));
			}
			
			if (_currentState != -1)
			{
				_currentState--;
			}
			
			_task = ThreadPool.schedule(this, _delay);
		}
	}
}
