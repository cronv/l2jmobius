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
 * This class manages custom variables associated with specific items.<br>
 * It allows for the storage and retrieval of unique data points for each item in the game world.
 * @author UnAfraid, Mobius
 */
public class ItemVariables extends AbstractVariables
{
	private static final Logger LOGGER = Logger.getLogger(ItemVariables.class.getName());
	
	// SQL Queries.
	private static final String SELECT_QUERY = "SELECT * FROM item_variables WHERE id = ?";
	private static final String SELECT_COUNT = "SELECT COUNT(*) FROM item_variables WHERE id = ?";
	private static final String DELETE_QUERY = "DELETE FROM item_variables WHERE id = ? AND var = ?";
	private static final String DELETE_ALL_QUERY = "DELETE FROM item_variables WHERE id = ?";
	private static final String UPSERT_QUERY = "INSERT INTO item_variables (id, var, val) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE val = VALUES(val)";
	
	// Asynchronous persistence.
	private static final long SAVE_INTERVAL = 60000; // 1 minute.
	private static final boolean ASYNC_SAVE_ENABLED = true;
	
	// Public variables.
	public static final String VISUAL_ID = "visualId";
	public static final String VISUAL_APPEARANCE_STONE_ID = "visualAppearanceStoneId";
	public static final String VISUAL_APPEARANCE_LIFE_TIME = "visualAppearanceLifetime";
	public static final String TRANSMOG_ID = "transmogId";
	public static final String VIRTUAL = "virtual";
	public static final String DAMAGED = "damaged";
	
	// Private variables.
	private final AtomicBoolean _scheduledSave = new AtomicBoolean(false);
	private final int _objectId;
	
	/**
	 * Creates a new {@code ItemVariables} instance for a specific object.<br>
	 * This constructor initializes the internal ID and loads existing data.
	 * @param objectId The unique identifier of the item.
	 */
	public ItemVariables(int objectId)
	{
		_objectId = objectId;
		restoreMe();
	}
	
	/**
	 * Checks if any variables exist for a specific object.<br>
	 * It queries the database to see if there are records associated with the given ID.<br>
	 * If an error occurs during the query, it returns {@code false}.
	 * @param objectId The unique identifier of the object to check.
	 * @return {@code true} if variables exist or no record is found, and {@code false} if a database error occurs.
	 */
	public static boolean hasVariables(int objectId)
	{
		// Restore previous variables.
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement st = con.prepareStatement(SELECT_COUNT))
		{
			st.setInt(1, objectId);
			try (ResultSet rset = st.executeQuery())
			{
				if (rset.next())
				{
					return rset.getInt(1) > 0;
				}
			}
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, ItemVariables.class.getSimpleName() + ": Could not select variables count for: " + objectId, e);
			return false;
		}
		
		return true;
	}
	
	/**
	 * This method restores the variables for this object from the database.<br>
	 * It clears any pending changes before loading the data.<br>
	 * The process uses a {@code SELECT} query to fetch all values.
	 * @return {@code true} if the restoration was successful, or {@code false} if an error occurred.
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
	 * Saves the current variable data to the database.<br>
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
	 * Saves the current variable data to the database immediately.<br>
	 * This method checks if any changes have occurred before saving.
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
	 * Saves the current variable changes to the database immediately.<br>
	 * This method handles both deletions and updates in a single sync operation.<br>
	 * It clears the change tracking if the save is successful.
	 * @return {@code true} if the data was saved successfully, {@code false} otherwise.
	 */
	private boolean saveNowSync()
	{
		_saveLock.lock();
		
		try
		{
			try (Connection con = DatabaseFactory.getConnection())
			{
				if (!_deleted.isEmpty())
				{
					try (PreparedStatement st = con.prepareStatement(DELETE_QUERY))
					{
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
				
				if (!_added.isEmpty() || !_modified.isEmpty())
				{
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
