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
package org.l2jmobius.gameserver.mechanics.variables;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;

/**
 * This class manages persistent variables associated with a clan.<br>
 * It handles the storage and retrieval of custom data for clans in the database.
 * @author UnAfraid, Mobius
 */
public class ClanVariables extends AbstractVariables
{
	private static final Logger LOGGER = Logger.getLogger(ClanVariables.class.getName());
	
	// SQL Queries.
	private static final String SELECT_QUERY = "SELECT * FROM clan_variables WHERE clanId = ?";
	private static final String DELETE_QUERY = "DELETE FROM clan_variables WHERE clanId = ? AND var = ?";
	private static final String DELETE_ALL_QUERY = "DELETE FROM clan_variables WHERE clanId = ?";
	private static final String UPSERT_QUERY = "INSERT INTO clan_variables (clanId, var, val) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE val = VALUES(val)";
	
	// Asynchronous persistence.
	private static final long SAVE_INTERVAL = 60000; // 1 minute.
	private static final boolean ASYNC_SAVE_ENABLED = true;
	
	// Public variables.
	public static final String CLAN_DEVELOPMENT_POINTS = "CLAN_DEVELOPMENT_POINTS";
	public static final String CLAN_MASTERY = "CLAN_MASTERY_";
	public static final String CLAN_MASTERY_SKILL_TIME = "CLAN_MASTERY_SKILL_TIME_";
	
	// Private variables.
	private final AtomicBoolean _scheduledSave = new AtomicBoolean(false);
	private final int _objectId;
	
	/**
	 * Creates a new instance of {@code ClanVariables}.<br>
	 * This constructor initializes the internal ID and loads data from the database.
	 * @param objectId The unique identifier for the clan.
	 */
	public ClanVariables(int objectId)
	{
		_objectId = objectId;
		restoreMe();
	}
	
	/**
	 * Restores the variables for this object from the database.<br>
	 * It clears any current change tracking before loading data.<br>
	 * The method returns {@code true} if the restoration is successful.<br>
	 * If a {@code SQLException} occurs, it logs an error and returns {@code false}.
	 * @return {@code true} if variables were successfully restored; {@code false} otherwise.
	 */
	public boolean restoreMe()
	{
		clearChangeTracking();
		
		// Restore previous variables.
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement st = con.prepareStatement(SELECT_QUERY))
		{
			st.setInt(1, _objectId);
			try (ResultSet rset = st.executeQuery())
			{
				while (rset.next())
				{
					set(rset.getString("var"), rset.getString("val"), false);
				}
			}
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Could not restore variables for: " + _objectId, e);
			return false;
		}
		finally
		{
			compareAndSetChanges(true, false);
		}
		
		return true;
	}
	
	/**
	 * Saves the current variables to the database.<br>
	 * This method checks if any changes have occurred before saving.<br>
	 * It schedules an asynchronous save or performs a synchronous save depending on the configuration.
	 * @return {@code true} if the save was successfully scheduled or performed, and {@code false} otherwise.
	 */
	public boolean storeMe()
	{
		// No changes, nothing to store.
		if (!hasChanges())
		{
			return false;
		}
		
		// If async saving is enabled and not already scheduled, schedule a save.
		if (ASYNC_SAVE_ENABLED && !_scheduledSave.get())
		{
			_scheduledSave.set(true);
			
			ThreadPool.schedule(() ->
			{
				_scheduledSave.set(false);
				saveNow();
			}, SAVE_INTERVAL);
			return true;
		}
		
		return saveNow();
	}
	
	/**
	 * Saves the current variable data to the database.<br>
	 * This method checks if any changes have occurred before saving.<br>
	 * It performs a synchronous save operation.
	 * @return {@code true} if the save was successful, {@code false} otherwise.
	 */
	public boolean saveNow()
	{
		if (!hasChanges())
		{
			return false;
		}
		
		// FIXME: May store after server shutdown.
		// If async is enabled, offload to ThreadPool.
		// if (ASYNC_SAVE_ENABLED)
		// {
		// ThreadPool.execute(this::saveNowSync);
		// return true;
		// }
		
		return saveNowSync();
	}
	
	/**
	 * Saves all pending changes to the database immediately.<br>
	 * This method handles both deletions and updates for the current object.<br>
	 * It clears the change tracking once the operation is successful.
	 * @return {@code true} if the save was successful, or {@code false} if a {@code SQLException} occurred.
	 */
	private boolean saveNowSync()
	{
		_saveLock.lock();
		
		try
		{
			try (Connection con = DatabaseFactory.getConnection())
			{
				// Process deletions.
				if (!_deleted.isEmpty())
				{
					try (PreparedStatement st = con.prepareStatement(DELETE_QUERY))
					{
						// Sort keys to ensure deterministic locking order.
						final List<String> sortedDeleted = new ArrayList<>(_deleted);
						sortedDeleted.sort(null);
						
						for (String name : sortedDeleted)
						{
							st.setInt(1, _objectId);
							st.setString(2, name);
							st.addBatch();
						}
						
						st.executeBatch();
					}
				}
				
				// Process additions and modifications using the static UPSERT_QUERY.
				if (!_added.isEmpty() || !_modified.isEmpty())
				{
					// Merge and sort all pending changes to ensure deterministic lock order.
					final TreeSet<String> allChanges = new TreeSet<>();
					allChanges.addAll(_added);
					allChanges.addAll(_modified);
					
					try (PreparedStatement st = con.prepareStatement(UPSERT_QUERY))
					{
						for (String name : allChanges)
						{
							final Object value = getSet().get(name);
							if (value != null)
							{
								st.setInt(1, _objectId);
								st.setString(2, name);
								st.setString(3, String.valueOf(value));
								st.addBatch();
							}
						}
						
						st.executeBatch();
					}
				}
				
				// Clear tracking after successful save.
				clearChangeTracking();
				compareAndSetChanges(true, false);
				return true;
			}
			catch (SQLException e)
			{
				LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Could not update variables for: " + _objectId, e);
				return false;
			}
		}
		finally
		{
			_saveLock.unlock();
		}
	}
	
	/**
	 * Deletes all variables associated with this object from the database.<br>
	 * This method clears the internal variable set and change tracking.
	 * @return {@code true} if the deletion was successful, or {@code false} if an error occurred.
	 */
	public boolean deleteMe()
	{
		_saveLock.lock();
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement st = con.prepareStatement(DELETE_ALL_QUERY))
		{
			st.setInt(1, _objectId);
			st.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Could not delete variables for: " + _objectId, e);
			_saveLock.unlock();
			return false;
		}
		
		// Clear all variables.
		getSet().clear();
		clearChangeTracking();
		
		_saveLock.unlock();
		return true;
	}
}
