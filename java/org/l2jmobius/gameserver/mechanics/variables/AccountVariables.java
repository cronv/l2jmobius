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
 * This class manages persistent variables associated with a specific user account.<br>
 * It handles the storage and retrieval of custom data from the database for {@code Account} objects.
 * @author UnAfraid, Mobius
 */
public class AccountVariables extends AbstractVariables
{
	private static final Logger LOGGER = Logger.getLogger(AccountVariables.class.getName());
	
	// SQL Queries.
	private static final String SELECT_QUERY = "SELECT * FROM account_gsdata WHERE account_name = ?";
	private static final String DELETE_QUERY = "DELETE FROM account_gsdata WHERE account_name = ? AND var = ?";
	private static final String DELETE_ALL_QUERY = "DELETE FROM account_gsdata WHERE account_name = ?";
	private static final String UPSERT_QUERY = "INSERT INTO account_gsdata (account_name, var, value) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE value = VALUES(value)";
	
	// Asynchronous persistence.
	private static final long SAVE_INTERVAL = 60000; // 1 minute.
	private static final boolean ASYNC_SAVE_ENABLED = true;
	
	// Public variables.
	public static final String HWID = "HWID";
	public static final String HWIDSLIT_VAR = "	";
	public static final String PRIME_SHOP_PRODUCT_COUNT = "PSPCount";
	public static final String PRIME_SHOP_PRODUCT_DAILY_COUNT = "PSPDailyCount";
	public static final String LCOIN_SHOP_PRODUCT_COUNT = "LCSCount";
	public static final String LCOIN_SHOP_PRODUCT_DAILY_COUNT = "LCSDailyCount";
	public static final String LCOIN_SHOP_PRODUCT_WEEKLY_COUNT = "LCSWeeklyCount";
	public static final String LCOIN_SHOP_PRODUCT_MONTHLY_COUNT = "LCSMonthlyCount";
	public static final String A_GRADE_RELIC_ATEMPTS = "A_GRADE_RELIC_ATEMPTS";
	public static final String B_GRADE_RELIC_ATEMPTS = "B_GRADE_RELIC_ATEMPTS";
	public static final String UNCONFIRMED_RELICS_COUNT = "UNCONFIRMED_RELICS_COUNT";
	public static final String DYE_LEVEL_FOR_SLOT_ = "DYE_LEVEL_FOR_SLOT_";
	public static final String DYE_CHALLENGE_COUNT_FOR_SLOT_ = "DYE_CHALLENGE_COUNT_FOR_SLOT_";
	public static final String DYE_HIDDEN_SKILL_LEVEL_FOR_SLOT_ = "DYE_HIDDEN_SKILL_LEVEL_FOR_SLOT_";
	
	// Private variables.
	private final AtomicBoolean _scheduledSave = new AtomicBoolean(false);
	private final String _accountName;
	
	/**
	 * Creates a new instance of {@code AccountVariables}.<br>
	 * It initializes the object with the provided account name.<br>
	 * This constructor also triggers the restoration of data from the database.
	 * @param accountName The unique name of the account to load.
	 */
	public AccountVariables(String accountName)
	{
		_accountName = accountName;
		restoreMe();
	}
	
	/**
	 * Restores the variables for this account from the database.<br>
	 * This method clears any pending changes and loads data using {@code SELECT_QUERY}.<br>
	 * It returns {@code true} if the restoration is successful.<br>
	 * It returns {@code false} if a {@code SQLException} occurs during the process.
	 * @return {@code true} if variables were restored successfully, otherwise {@code false}.
	 */
	public boolean restoreMe()
	{
		clearChangeTracking();
		
		// Restore previous variables.
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement st = con.prepareStatement(SELECT_QUERY))
		{
			st.setString(1, _accountName);
			try (ResultSet rset = st.executeQuery())
			{
				while (rset.next())
				{
					set(rset.getString("var"), rset.getString("value"), false);
				}
			}
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Could not restore variables for: " + _accountName, e);
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
	 * It handles both scheduled asynchronous saves and immediate synchronous saves.
	 * @return {@code true} if a save was successfully initiated or completed, otherwise {@code false}.
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
	 * Saves the current variables to the database immediately.<br>
	 * This method checks if any changes have been made before saving.
	 * @return {@code true} if the data was saved successfully, or {@code false} otherwise.
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
	 * Saves the account variables to the database immediately.<br>
	 * This method performs a synchronous update of all added, modified, and deleted changes.<br>
	 * It clears the change tracking if the operation succeeds.
	 * @return {@code true} if the save was successful, or {@code false} if an {@code SQLException} occurred.
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
							st.setString(1, _accountName);
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
								st.setString(1, _accountName);
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
				LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Could not update variables for: " + _accountName, e);
				return false;
			}
		}
		finally
		{
			_saveLock.unlock();
		}
	}
	
	/**
	 * Deletes all variables associated with this account from the database.<br>
	 * This method also clears the local variable set and change tracking.
	 * @return {@code true} if the deletion was successful, or {@code false} if an error occurred.
	 */
	public boolean deleteMe()
	{
		_saveLock.lock();
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement st = con.prepareStatement(DELETE_ALL_QUERY))
		{
			st.setString(1, _accountName);
			st.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Could not delete variables for: " + _accountName, e);
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
