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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.clan.Crest;
import org.l2jmobius.gameserver.entity.clan.enums.CrestType;

/**
 * This class handles the loading and saving of {@code Crest} data to and from the database.<br>
 * It provides a structured way to manage clan crest information within the SQL tables.
 * @author NosBit, Mobius
 */
public class CrestTable
{
	private static final Logger LOGGER = Logger.getLogger(CrestTable.class.getName());
	
	private final Map<Integer, Crest> _crests = new ConcurrentHashMap<>();
	private final AtomicInteger _nextId = new AtomicInteger(1);
	
	/**
	 * Initializes a new instance of the {@code CrestTable} class.<br>
	 * This constructor automatically calls the {@code load} method to populate data.
	 */
	protected CrestTable()
	{
		load();
	}
	
	/**
	 * Loads crest data from the database into memory.<br>
	 * This method clears existing data and populates the internal map.<br>
	 * It also removes unused crests from the database to keep it clean.<br>
	 * Finally, it validates that all clans have valid associated crest IDs.
	 */
	public synchronized void load()
	{
		_crests.clear();
		final Collection<Clan> clans = ClanTable.getInstance().getClans();
		final Set<Integer> crestsInUse = new HashSet<>(Math.max(16, clans.size() * 4));
		for (Clan clan : clans)
		{
			final int crestId = clan.getCrestId();
			if (crestId != 0)
			{
				crestsInUse.add(crestId);
			}
			
			final int crestLargeId = clan.getCrestLargeId();
			if (crestLargeId != 0)
			{
				crestsInUse.add(crestLargeId);
			}
			
			final int allyCrestId = clan.getAllyCrestId();
			if (allyCrestId != 0)
			{
				crestsInUse.add(allyCrestId);
			}
		}
		
		final List<Integer> toDelete = new ArrayList<>();
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT `crest_id`, `data`, `type` FROM `crests` ORDER BY `crest_id` DESC");
			ResultSet rs = statement.executeQuery())
		{
			while (rs.next())
			{
				final int id = rs.getInt("crest_id");
				int nextId = _nextId.get();
				if (nextId <= id)
				{
					nextId = id + 1;
					_nextId.set(nextId);
				}
				
				// Delete all unused crests except the last one we do not want to reuse a crest id because client will display wrong crest if it is reused.
				if (!crestsInUse.contains(id) && (id != (nextId - 1)))
				{
					toDelete.add(id);
					continue;
				}
				
				final int typeId = rs.getInt("type");
				final CrestType crestType = CrestType.getById(typeId);
				if (crestType != null)
				{
					_crests.put(id, new Crest(id, rs.getBytes("data"), crestType));
				}
				else
				{
					LOGGER.warning("Unknown crest type found in database. Type:" + typeId);
				}
			}
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, "There was an error while loading crests from database:", e);
		}
		
		if (!toDelete.isEmpty())
		{
			try (Connection con = DatabaseFactory.getConnection();
				PreparedStatement ps = con.prepareStatement("DELETE FROM `crests` WHERE `crest_id`=?"))
			{
				for (int crestId : toDelete)
				{
					ps.setInt(1, crestId);
					ps.addBatch();
				}
				ps.executeBatch();
			}
			catch (SQLException e)
			{
				LOGGER.log(Level.WARNING, "There was an error while deleting unused crests from database:", e);
			}
		}
		
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _crests.size() + " Crests.");
		
		for (Clan clan : clans)
		{
			final int crestId = clan.getCrestId();
			if ((crestId != 0) && (getCrest(crestId) == null))
			{
				LOGGER.info("Removing non-existent crest for clan " + clan.getName() + " [" + clan.getId() + "], crestId:" + crestId);
				clan.setCrestId(0);
				clan.changeClanCrest(0);
			}
			
			final int crestLargeId = clan.getCrestLargeId();
			if ((crestLargeId != 0) && (getCrest(crestLargeId) == null))
			{
				LOGGER.info("Removing non-existent large crest for clan " + clan.getName() + " [" + clan.getId() + "], crestLargeId:" + crestLargeId);
				clan.setCrestLargeId(0);
				clan.changeLargeCrest(0);
			}
			
			final int allyCrestId = clan.getAllyCrestId();
			if ((allyCrestId != 0) && (getCrest(allyCrestId) == null))
			{
				LOGGER.info("Removing non-existent ally crest for clan " + clan.getName() + " [" + clan.getId() + "], allyCrestId:" + allyCrestId);
				clan.setAllyCrestId(0);
				clan.changeAllyCrest(0, true);
			}
		}
	}
	
	/**
	 * Retrieves a {@code Crest} object from the internal cache.<br>
	 * This method uses the provided unique identifier to find the data.
	 * @param crestId The unique ID of the crest to retrieve.
	 * @return The {@code Crest} associated with the given ID, or {@code null} if not found.
	 */
	public Crest getCrest(int crestId)
	{
		return _crests.get(crestId);
	}
	
	/**
	 * Creates a new {@code Crest} and saves it to the database.<br>
	 * The method generates a unique ID for the new entry.<br>
	 * It adds the created object to the internal cache.
	 * @param data The raw byte array representing the crest information.
	 * @param crestType The type of the crest being created.
	 * @return The newly created {@code Crest} object, or {@code null} if an error occurs.
	 */
	public Crest createCrest(byte[] data, CrestType crestType)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement("INSERT INTO `crests`(`crest_id`, `data`, `type`) VALUES(?, ?, ?)"))
		{
			final Crest crest = new Crest(_nextId.getAndIncrement(), data, crestType);
			final int crestId = crest.getId();
			statement.setInt(1, crestId);
			statement.setBytes(2, crest.getData());
			statement.setInt(3, crest.getType().getId());
			statement.executeUpdate();
			_crests.put(crestId, crest);
			return crest;
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, "There was an error while saving crest in database:", e);
		}
		
		return null;
	}
	
	/**
	 * Removes a specific crest from the internal cache.<br>
	 * This method also deletes the record from the database.<br>
	 * It prevents removing the last ID to maintain correct client indexing.
	 * @param crestId The unique identifier of the crest to remove.
	 */
	public void removeCrest(int crestId)
	{
		_crests.remove(crestId);
		
		// Avoid removing last crest id we do not want to lose index because client will display wrong crest if it is reused.
		if (crestId == (_nextId.get() - 1))
		{
			return;
		}
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement("DELETE FROM `crests` WHERE `crest_id`=?"))
		{
			statement.setInt(1, crestId);
			statement.executeUpdate();
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, "There was an error while deleting crest from database:", e);
		}
	}
	
	/**
	 * Retrieves the next available unique identifier for a {@code Crest}.<br>
	 * This method increments the internal counter automatically.
	 * @return The next integer ID to be assigned.
	 */
	public int getNextId()
	{
		return _nextId.getAndIncrement();
	}
	
	/**
	 * Provides the global instance of the {@code CrestTable} class.<br>
	 * This method follows the singleton pattern to ensure only one instance exists.<br>
	 * Use this method to access database operations for crests.
	 * @return The single instance of {@code CrestTable}.
	 */
	public static CrestTable getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final CrestTable INSTANCE = new CrestTable();
	}
}
