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
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.mechanics.variables.AbstractVariables;

/**
 * Manages global variables that are shared across the entire game server.<br>
 * This class provides a centralized way to store and retrieve persistent configuration data.<br>
 * It handles the synchronization between these variables and the database.
 */
public class GlobalVariablesManager extends AbstractVariables
{
	private static final Logger LOGGER = Logger.getLogger(GlobalVariablesManager.class.getName());
	
	// SQL Queries.
	private static final String SELECT_QUERY = "SELECT * FROM global_variables";
	private static final String DELETE_QUERY = "DELETE FROM global_variables";
	private static final String INSERT_QUERY = "REPLACE INTO global_variables (var, value) VALUES (?, ?)";
	
	// Public variable names
	public static final String DAILY_TASK_RESET = "DAILY_TASK_RESET";
	public static final String IS_EVEN_WEEK = "IS_EVEN_WEEK";
	public static final String NEXT_EVEN_WEEK_SWAP = "NEXT_EVEN_WEEK_SWAP";
	public static final String COC_TOP_MARKS = "COC_TOP_MARKS";
	public static final String COC_TOP_MEMBER = "COC_TOP_MEMBER";
	public static final String COC_TRUE_HERO = "COC_TRUE_HERO";
	public static final String COC_TRUE_HERO_REWARDED = "COC_TRUE_HERO_REWARDED";
	public static final String BATHIN_ALIVE = "BATHIN_ALIVE";
	public static final String VIRTUAL_ITEM_EVENT_START = "VIRTUAL_ITEM_EVENT_START";
	
	/**
	 * Initializes a new instance of the {@code GlobalVariablesManager}.<br>
	 * This constructor automatically calls {@code restoreMe} to load data.
	 */
	protected GlobalVariablesManager()
	{
		restoreMe();
	}
	
	/**
	 * Loads all global variables from the database.<br>
	 * This method populates the current instance with values stored in the table.<br>
	 * It returns {@code true} if the operation succeeds.<br>
	 * It returns {@code false} if a {@code SQLException} occurs.
	 * @return {@code true} if variables were successfully restored, otherwise {@code false}.
	 */
	public boolean restoreMe()
	{
		// Restore previous variables.
		try (Connection con = DatabaseFactory.getConnection();
			Statement st = con.createStatement();
			ResultSet rset = st.executeQuery(SELECT_QUERY))
		{
			while (rset.next())
			{
				set(rset.getString("var"), rset.getString("value"));
			}
		}
		catch (SQLException e)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Couldn't restore global variables.");
			return false;
		}
		
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + getSet().size() + " variables.");
		return true;
	}
	
	/**
	 * Saves all current global variables to the database.<br>
	 * This method clears existing entries before inserting new ones.<br>
	 * It returns {@code true} if the operation succeeds.<br>
	 * It returns {@code false} if a {@code SQLException} occurs.
	 * @return {@code true} if the data was saved successfully, otherwise {@code false}.
	 */
	public boolean storeMe()
	{
		try (Connection con = DatabaseFactory.getConnection();
			Statement del = con.createStatement();
			PreparedStatement st = con.prepareStatement(INSERT_QUERY))
		{
			// Clear previous entries.
			del.execute(DELETE_QUERY);
			
			// Insert all variables.
			for (Entry<String, Object> entry : getSet().entrySet())
			{
				st.setString(1, entry.getKey());
				st.setString(2, String.valueOf(entry.getValue()));
				st.addBatch();
			}
			
			st.executeBatch();
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Couldn't save global variables to database.", e);
			return false;
		}
		
		LOGGER.info(getClass().getSimpleName() + ": Stored " + getSet().size() + " variables.");
		return true;
	}
	
	/**
	 * Deletes all entries from the global variables table in the database.<br>
	 * This method clears all stored data for this manager.
	 * @return {@code true} if the deletion was successful, or {@code false} if an error occurred.
	 */
	public boolean deleteMe()
	{
		try (Connection con = DatabaseFactory.getConnection();
			Statement del = con.createStatement())
		{
			del.execute(DELETE_QUERY);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Couldn't delete global variables to database.", e);
			return false;
		}
		
		return true;
	}
	
	/**
	 * Retrieves the singleton instance of {@code GlobalVariablesManager}.<br>
	 * This method provides a global access point to manage server-wide variables.
	 * @return The active instance of {@code GlobalVariablesManager}.
	 */
	public static GlobalVariablesManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final GlobalVariablesManager INSTANCE = new GlobalVariablesManager();
	}
}
