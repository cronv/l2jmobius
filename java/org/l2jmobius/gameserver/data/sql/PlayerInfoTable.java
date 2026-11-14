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
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Calendar;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.entity.actor.Player;

/**
 * This class handles the database operations for player information.<br>
 * It provides methods to save and retrieve data related to {@code Player} entities from the SQL database.
 */
public class PlayerInfoTable
{
	private static final Logger LOGGER = Logger.getLogger(PlayerInfoTable.class.getName());
	
	private final Map<Integer, String> _names = new ConcurrentHashMap<>();
	private final Map<String, Integer> _idsByLowerName = new ConcurrentHashMap<>();
	private final Map<Integer, Integer> _accessLevels = new ConcurrentHashMap<>();
	private final Map<Integer, Integer> _levels = new ConcurrentHashMap<>();
	private final Map<Integer, Integer> _classes = new ConcurrentHashMap<>();
	private final Map<Integer, Integer> _clans = new ConcurrentHashMap<>();
	private final Map<Integer, Map<Integer, String>> _memos = new ConcurrentHashMap<>();
	private final Map<Integer, Calendar> _creationDates = new ConcurrentHashMap<>();
	private final Map<Integer, Long> _lastAccess = new ConcurrentHashMap<>();
	
	/**
	 * Initializes the {@code PlayerInfoTable} by loading data from the database.<br>
	 * It retrieves character IDs, names, and access levels into internal maps.<br>
	 * If a {@code SQLException} occurs, it logs a warning message.
	 */
	protected PlayerInfoTable()
	{
		try (Connection con = DatabaseFactory.getConnection();
			Statement s = con.createStatement();
			ResultSet rs = s.executeQuery("SELECT charId, char_name, accesslevel FROM characters"))
		{
			while (rs.next())
			{
				final int id = rs.getInt("charId");
				final String charName = rs.getString("char_name");
				_names.put(id, charName);
				_idsByLowerName.put(charName.toLowerCase(), id);
				_accessLevels.put(id, rs.getInt("accesslevel"));
			}
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Couldn't retrieve all char id/name/access: " + e.getMessage(), e);
		}
		
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _names.size() + " char names.");
	}
	
	/**
	 * Registers a {@code Player} in the internal name and access level maps.<br>
	 * This method updates the data for the provided {@code Player} object.<br>
	 * It checks if the {@code Player} is not {@code null} before processing.
	 * @param player The {@code Player} object to add to the table.
	 */
	public void addName(Player player)
	{
		if (player != null)
		{
			final Integer playerObjectId = player.getObjectId();
			addName(playerObjectId, player.getName());
			_accessLevels.put(playerObjectId, player.getAccessLevel().getLevel());
		}
	}
	
	/**
	 * Updates the name associated with a specific object ID.<br>
	 * This method handles internal mapping for both names and lowercase versions.<br>
	 * It ignores the update if the provided name is {@code null}.
	 * @param objectId The unique identifier of the object.
	 * @param name The new name to assign to the object.
	 */
	private void addName(int objectId, String name)
	{
		if (name == null)
		{
			return;
		}
		
		final String previous = _names.get(objectId);
		if (name.equals(previous))
		{
			return;
		}
		
		_names.put(objectId, name);
		if (previous != null)
		{
			_idsByLowerName.remove(previous.toLowerCase());
		}
		_idsByLowerName.put(name.toLowerCase(), objectId);
	}
	
	/**
	 * Removes all data associated with a specific object ID.<br>
	 * This method clears the name, level, and other attributes from the internal maps.<br>
	 * It also removes the entry from the lower-case name index if it exists.
	 * @param objId The unique identifier of the object to remove.
	 */
	public void removeName(int objId)
	{
		final String name = _names.remove(objId);
		_accessLevels.remove(objId);
		_levels.remove(objId);
		_classes.remove(objId);
		_clans.remove(objId);
		_memos.remove(objId);
		_creationDates.remove(objId);
		_lastAccess.remove(objId);
		if (name != null)
		{
			_idsByLowerName.remove(name.toLowerCase(), objId);
		}
	}
	
	/**
	 * Retrieves the unique identifier for a character based on their name.<br>
	 * This method checks the internal cache before querying the database.<br>
	 * It returns -1 if the name is null, empty, or not found.
	 * @param name The name of the character to search for.
	 * @return The integer ID of the character, or -1 if no match exists.
	 */
	public int getIdByName(String name)
	{
		if ((name == null) || name.isEmpty())
		{
			return -1;
		}
		
		final String lowerName = name.toLowerCase();
		final Integer cachedId = _idsByLowerName.get(lowerName);
		if (cachedId != null)
		{
			return cachedId;
		}
		
		int id = -1;
		int accessLevel = 0;
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT charId,accesslevel FROM characters WHERE char_name=?"))
		{
			ps.setString(1, name);
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					id = rs.getInt("charId");
					accessLevel = rs.getInt("accesslevel");
				}
			}
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Could not check existing char name: " + e.getMessage(), e);
		}
		
		if (id > 0)
		{
			_names.put(id, name);
			_idsByLowerName.put(lowerName, id);
			_accessLevels.put(id, accessLevel);
			return id;
		}
		
		return -1; // Not found.
	}
	
	/**
	 * Retrieves the character name associated with a specific ID.<br>
	 * This method checks the internal cache before querying the database.<br>
	 * It returns {@code null} if the ID is less than or equal to 0 or not found.
	 * @param id The unique identifier of the character.
	 * @return The character name as a {@code String}, or {@code null} if no name exists.
	 */
	public String getNameById(int id)
	{
		if (id <= 0)
		{
			return null;
		}
		
		String name = _names.get(id);
		if (name != null)
		{
			return name;
		}
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT char_name,accesslevel FROM characters WHERE charId=?"))
		{
			ps.setInt(1, id);
			try (ResultSet rset = ps.executeQuery())
			{
				if (rset.next())
				{
					name = rset.getString("char_name");
					_names.put(id, name);
					_idsByLowerName.put(name.toLowerCase(), id);
					_accessLevels.put(id, rset.getInt("accesslevel"));
					return name;
				}
			}
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Could not check existing char id: " + e.getMessage(), e);
		}
		
		return null; // Not found.
	}
	
	/**
	 * Retrieves the access level for a specific character.<br>
	 * It checks if the name exists before looking up the value.<br>
	 * Returns {@code 0} if the character is not found.
	 * @param objectId The unique identifier of the character.
	 * @return The integer access level associated with the ID.
	 */
	public int getAccessLevelById(int objectId)
	{
		return getNameById(objectId) != null ? _accessLevels.get(objectId) : 0;
	}
	
	/**
	 * Checks if a character name already exists in the database.<br>
	 * This method queries the characters table to see if any record matches the provided name.
	 * @param name The character name to check.
	 * @return {@code true} if the name is taken, {@code false} otherwise.
	 */
	public boolean doesCharNameExist(String name)
	{
		boolean result = false;
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) as count FROM characters WHERE char_name=?"))
		{
			ps.setString(1, name);
			try (ResultSet rs = ps.executeQuery())
			{
				if (rs.next())
				{
					result = rs.getInt("count") > 0;
				}
			}
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Could not check existing charname: " + e.getMessage(), e);
		}
		
		return result;
	}
	
	/**
	 * Calculates the total number of characters associated with a specific account.<br>
	 * This method queries the database to count entries matching the provided name.
	 * @param account The name of the account to check.
	 * @return The total count of characters, or 0 if an error occurs or no characters are found.
	 */
	public int getAccountCharacterCount(String account)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT COUNT(char_name) as count FROM characters WHERE account_name=?"))
		{
			ps.setString(1, account);
			try (ResultSet rset = ps.executeQuery())
			{
				if (rset.next())
				{
					return rset.getInt("count");
				}
			}
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, "Couldn't retrieve account for id: " + e.getMessage(), e);
		}
		
		return 0;
	}
	
	/**
	 * Updates the character level for a specific object.<br>
	 * This method stores the new value in the internal cache.
	 * @param objectId The unique identifier of the character.
	 * @param level The new level to assign to the character.
	 */
	public void setLevel(int objectId, int level)
	{
		_levels.put(objectId, level);
	}
	
	/**
	 * Retrieves the level of a character based on its unique ID.<br>
	 * This method checks the internal cache before querying the database.<br>
	 * If the level is not found in either location, it returns {@code 0}.
	 * @param objectId The unique identifier of the character.
	 * @return The level of the character as an {@code int}.
	 */
	public int getLevelById(int objectId)
	{
		final Integer level = _levels.get(objectId);
		if (level != null)
		{
			return level;
		}
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT level FROM characters WHERE charId = ?"))
		{
			ps.setInt(1, objectId);
			try (ResultSet rset = ps.executeQuery())
			{
				if (rset.next())
				{
					final int dbLevel = rset.getInt("level");
					_levels.put(objectId, dbLevel);
					return dbLevel;
				}
			}
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Could not check existing char count: " + e.getMessage(), e);
		}
		
		return 0;
	}
	
	/**
	 * Updates the character class identifier for a specific player.<br>
	 * This method stores the new {@code classId} in the internal map using the provided {@code objectId}.
	 * @param objectId The unique identifier of the player character.
	 * @param classId The new class identifier to assign to the character.
	 */
	public void setClassId(int objectId, int classId)
	{
		_classes.put(objectId, classId);
	}
	
	/**
	 * Retrieves the class ID associated with a specific character object ID.<br>
	 * It first checks the internal cache before querying the database.<br>
	 * If the data is not found or an error occurs, it returns {@code 0}.
	 * @param objectId The unique identifier of the character.
	 * @return The class ID of the character or {@code 0} if not found.
	 */
	public int getClassIdById(int objectId)
	{
		final Integer classId = _classes.get(objectId);
		if (classId != null)
		{
			return classId;
		}
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT classid FROM characters WHERE charId = ?"))
		{
			ps.setInt(1, objectId);
			try (ResultSet rset = ps.executeQuery())
			{
				if (rset.next())
				{
					final int dbClassId = rset.getInt("classid");
					_classes.put(objectId, dbClassId);
					return dbClassId;
				}
			}
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Couldn't retrieve class for id: " + e.getMessage(), e);
		}
		
		return 0;
	}
	
	/**
	 * Updates the clan identifier for a specific character.<br>
	 * This method stores the new {@code clanId} in the internal map using the {@code objectId} as the key.
	 * @param objectId The unique identifier of the character.
	 * @param clanId The new clan identifier to assign.
	 */
	public void setClanId(int objectId, int clanId)
	{
		_clans.put(objectId, clanId);
	}
	
	/**
	 * Removes the clan association for a specific character.<br>
	 * This method deletes the entry from the internal map using the provided ID.
	 * @param objectId The unique identifier of the character to update.
	 */
	public void removeClanId(int objectId)
	{
		_clans.remove(objectId);
	}
	
	/**
	 * Retrieves the clan ID associated with a specific character.<br>
	 * This method checks the internal cache before querying the database.<br>
	 * If the ID is not found in the database, it returns {@code 0}.
	 * @param objectId The unique identifier of the character.
	 * @return The clan ID of the character or {@code 0} if none exists.
	 */
	public int getClanIdById(int objectId)
	{
		final Integer clanId = _clans.get(objectId);
		if (clanId != null)
		{
			return clanId;
		}
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT clanId FROM characters WHERE charId = ?"))
		{
			ps.setInt(1, objectId);
			try (ResultSet rset = ps.executeQuery())
			{
				while (rset.next())
				{
					final int dbClanId = rset.getInt("clanId");
					_clans.put(objectId, dbClanId);
					return dbClanId;
				}
			}
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Could not check existing char count: " + e.getMessage(), e);
		}
		
		// Prevent searching again.
		_clans.put(objectId, 0);
		return 0;
	}
	
	/**
	 * Saves a personal note for a specific friend.<br>
	 * This method cleans the input by removing HTML-like tags.<br>
	 * It also prevents saving notes containing certain restricted keywords.
	 * @param charId The unique identifier of the character owning the memo.
	 * @param friendId The unique identifier of the friend associated with the memo.
	 * @param memo The text content to be saved as a note.
	 */
	public void setFriendMemo(int charId, int friendId, String memo)
	{
		final Map<Integer, String> memos = _memos.computeIfAbsent(charId, k -> new ConcurrentHashMap<>());
		if (memo == null)
		{
			memos.put(friendId, "");
			return;
		}
		
		// Bypass exploit check.
		final String text = memo.toLowerCase();
		if (text.contains("action") && text.contains("bypass"))
		{
			memos.put(friendId, "");
			return;
		}
		
		// Add text without tags.
		memos.put(friendId, memo.replaceAll("<.*?>", ""));
	}
	
	/**
	 * Removes a specific friend memo from a character.<br>
	 * This method deletes the entry associated with a {@code friendId}.<br>
	 * It does nothing if no memos exist for the given {@code charId}.
	 * @param charId The unique identifier of the character.
	 * @param friendId The unique identifier of the friend whose memo is being removed.
	 */
	public void removeFriendMemo(int charId, int friendId)
	{
		final Map<Integer, String> memos = _memos.get(charId);
		if (memos == null)
		{
			return;
		}
		
		memos.remove(friendId);
	}
	
	/**
	 * Retrieves the personal note for a specific friend.<br>
	 * This method checks the cache before querying the database.<br>
	 * If no memo exists in the database, it returns {@code null}.
	 * @param charId The unique identifier of the character owning the memo.
	 * @param friendId The unique identifier of the friend associated with the memo.
	 * @return The memo string if found, or {@code null} if it does not exist.
	 */
	public String getFriendMemo(int charId, int friendId)
	{
		final Map<Integer, String> memos = _memos.computeIfAbsent(charId, k -> new ConcurrentHashMap<>());
		final String cached = memos.get(friendId);
		if (cached != null)
		{
			return cached;
		}
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT memo FROM character_friends WHERE charId=? AND friendId=?"))
		{
			statement.setInt(1, charId);
			statement.setInt(2, friendId);
			
			try (ResultSet rset = statement.executeQuery())
			{
				if (rset.next())
				{
					final String dbMemo = rset.getString("memo");
					final String cachedMemo = dbMemo == null ? "" : dbMemo;
					memos.put(friendId, cachedMemo);
					return cachedMemo;
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Error occurred while retrieving memo: " + e.getMessage(), e);
		}
		
		// Prevent searching again.
		memos.put(friendId, "");
		return null;
	}
	
	/**
	 * Retrieves the creation date of a specific character.<br>
	 * This method checks the internal cache before querying the database.<br>
	 * If the date is found in the database, it updates the cache.
	 * @param objectId The unique identifier of the character.
	 * @return A {@code Calendar} object containing the creation date, or {@code null} if not found.
	 */
	public Calendar getCharacterCreationDate(int objectId)
	{
		final Calendar calendar = _creationDates.get(objectId);
		if (calendar != null)
		{
			return (Calendar) calendar.clone();
		}
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT createDate FROM characters WHERE charId = ?"))
		{
			ps.setInt(1, objectId);
			try (ResultSet rset = ps.executeQuery())
			{
				if (rset.next())
				{
					final Date createDate = rset.getDate("createDate");
					final Calendar newCalendar = Calendar.getInstance();
					newCalendar.setTime(createDate);
					_creationDates.put(objectId, newCalendar);
					return (Calendar) newCalendar.clone();
				}
			}
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Could not retrieve character creation date: " + e.getMessage(), e);
		}
		
		return null;
	}
	
	/**
	 * Updates the last access time for a specific character.<br>
	 * This method stores the timestamp in the internal cache.
	 * @param objectId The unique identifier of the character.
	 * @param lastAccess The timestamp representing when the character was last accessed.
	 */
	public void setLastAccess(int objectId, long lastAccess)
	{
		_lastAccess.put(objectId, lastAccess);
	}
	
	/**
	 * Calculates the time elapsed since the character was last accessed.<br>
	 * This value is returned in seconds.<br>
	 * It checks the internal cache before querying the database.
	 * @param objectId The unique identifier of the character.
	 * @return The delay in seconds, or 0 if no data is found.
	 */
	public int getLastAccessDelay(int objectId)
	{
		final Long lastAccess = _lastAccess.get(objectId);
		if (lastAccess != null)
		{
			final long currentTime = System.currentTimeMillis();
			final long timeDifferenceInMillis = currentTime - lastAccess;
			return (int) (timeDifferenceInMillis / 1000);
		}
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT lastAccess FROM characters WHERE charId = ?"))
		{
			ps.setInt(1, objectId);
			try (ResultSet rset = ps.executeQuery())
			{
				if (rset.next())
				{
					final long dbLastAccess = rset.getLong("lastAccess");
					_lastAccess.put(objectId, dbLastAccess);
					
					final long currentTime = System.currentTimeMillis();
					final long timeDifferenceInMillis = currentTime - dbLastAccess;
					return (int) (timeDifferenceInMillis / 1000);
				}
			}
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Could not retrieve lastAccess timestamp: " + e.getMessage(), e);
		}
		
		return 0;
	}
	
	/**
	 * Provides the singleton instance of the {@code PlayerInfoTable}.<br>
	 * Use this method to access the shared data table for player information.
	 * @return The global instance of {@code PlayerInfoTable}.
	 */
	public static PlayerInfoTable getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final PlayerInfoTable INSTANCE = new PlayerInfoTable();
	}
}
