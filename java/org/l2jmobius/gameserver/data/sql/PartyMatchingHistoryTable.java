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
package org.l2jmobius.gameserver.data.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.entity.groups.matching.MatchingRoomHistory;

/**
 * This class handles database operations for the party matching history.<br>
 * It allows for retrieving and managing {@code MatchingRoomHistory} records from the SQL table.
 * @author Mobius
 */
public class PartyMatchingHistoryTable
{
	private static final Logger LOGGER = Logger.getLogger(PartyMatchingHistoryTable.class.getName());
	
	private static final String RESTORE_PARTY_HISTORY = "SELECT title, leader FROM party_matching_history ORDER BY id DESC LIMIT 100"; // Maximum size according to retail is 100.
	private static final String DELETE_PARTY_HISTORY = "DELETE FROM party_matching_history";
	private static final String INSERT_PARTY_HISTORY = "INSERT INTO party_matching_history (title,leader) values (?,?)";
	
	private static final Deque<MatchingRoomHistory> HISTORY = new ArrayDeque<>(100);
	
	/**
	 * Initializes the {@code PartyMatchingHistoryTable}.<br>
	 * This constructor loads historical data from the database.<br>
	 * It populates the internal history collection with up to 100 records.
	 */
	protected PartyMatchingHistoryTable()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(RESTORE_PARTY_HISTORY);
			ResultSet rset = statement.executeQuery())
		{
			while (rset.next())
			{
				addRoomHistory(rset.getString("title"), rset.getString("leader"));
			}
		}
		catch (Exception e)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Could not load data: " + e.getMessage());
		}
		
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + HISTORY.size() + " party matching history data.");
	}
	
	/**
	 * Adds a new entry to the room history list.<br>
	 * This method creates a {@code MatchingRoomHistory} object.<br>
	 * It ensures the history size does not exceed 100 entries.
	 * @param title The name of the room.
	 * @param leaderName The name of the room leader.
	 */
	public void addRoomHistory(String title, String leaderName)
	{
		synchronized (HISTORY)
		{
			HISTORY.add(new MatchingRoomHistory(title, leaderName));
			if (HISTORY.size() > 100) // Maximum size according to retail is 100.
			{
				HISTORY.removeFirst();
			}
		}
	}
	
	/**
	 * Retrieves the list of previous matching room records.<br>
	 * This method returns all entries stored in the {@code HISTORY} collection.
	 * @return a {@code Collection} of {@code MatchingRoomHistory} objects.
	 */
	public Collection<MatchingRoomHistory> getHistory()
	{
		return HISTORY;
	}
	
	/**
	 * Saves the current matching history to the database.<br>
	 * This method clears existing records and inserts new ones from the {@code HISTORY} collection.<br>
	 * It handles any errors by logging a warning message.
	 */
	public void storeMe()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps1 = con.prepareStatement(DELETE_PARTY_HISTORY);
			PreparedStatement ps2 = con.prepareStatement(INSERT_PARTY_HISTORY))
		{
			ps1.execute();
			
			for (MatchingRoomHistory history : HISTORY)
			{
				ps2.setString(1, history.getTitle());
				ps2.setString(2, history.getLeader());
				ps2.addBatch();
			}
			
			ps2.executeBatch();
		}
		catch (Exception e)
		{
			LOGGER.warning(getClass().getSimpleName() + ": PartyMatchingHistoryTable: Problem inserting room history!");
		}
	}
	
	/**
	 * Provides the global instance of the {@code PartyMatchingHistoryTable}.<br>
	 * This method follows the singleton pattern.<br>
	 * Use this to access the history table data from anywhere in the code.
	 * @return The single instance of {@code PartyMatchingHistoryTable}.
	 */
	public static PartyMatchingHistoryTable getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final PartyMatchingHistoryTable INSTANCE = new PartyMatchingHistoryTable();
	}
}
