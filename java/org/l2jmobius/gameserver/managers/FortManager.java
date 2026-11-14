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
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.mechanics.siege.Fort;

/**
 * Manages the lifecycle and state of {@code Fort} objects within the game world.<br>
 * This class handles database interactions and provides access to active fort data.
 */
public class FortManager
{
	private static final Logger LOGGER = Logger.getLogger(FortManager.class.getName());
	
	private static final Map<Integer, Fort> _forts = new ConcurrentSkipListMap<>();
	
	/**
	 * Finds the closest {@code Fort} to a specific object.<br>
	 * This method searches all available forts in the world.<br>
	 * It returns the one with the shortest distance to the provided location.
	 * @param obj The {@code WorldObject} used as the starting point for the search.
	 * @return The nearest {@code Fort} found, or {@code null} if none exist.
	 */
	public Fort findNearestFort(WorldObject obj)
	{
		return findNearestFort(obj, Long.MAX_VALUE);
	}
	
	/**
	 * Finds the closest {@code Fort} to a specific {@code WorldObject}.<br>
	 * It searches within a maximum distance of {@code maxDistanceValue}.<br>
	 * If no fort is found within that range, it returns {@code null}.
	 * @param obj The {@code WorldObject} used as the starting point for the search.
	 * @param maxDistanceValue The maximum distance to search for a {@code Fort}.
	 * @return The nearest {@code Fort} object or {@code null} if none are found.
	 */
	public Fort findNearestFort(WorldObject obj, long maxDistanceValue)
	{
		Fort nearestFort = getFort(obj);
		if (nearestFort == null)
		{
			long maxDistance = maxDistanceValue;
			for (Fort fort : _forts.values())
			{
				final double distance = fort.getDistance(obj);
				if (maxDistance > distance)
				{
					maxDistance = (long) distance;
					nearestFort = fort;
				}
			}
		}
		
		return nearestFort;
	}
	
	/**
	 * Retrieves a {@code Fort} object based on its unique identifier.<br>
	 * This method searches through all loaded forts to find a match.<br>
	 * It returns {@code null} if no fort is found with the given ID.
	 * @param fortId The unique identification number of the fort.
	 * @return The matching {@code Fort} object or {@code null}.
	 */
	public Fort getFortById(int fortId)
	{
		for (Fort fort : _forts.values())
		{
			if (fort.getResidenceId() == fortId)
			{
				return fort;
			}
		}
		
		return null;
	}
	
	/**
	 * Retrieves the {@code Fort} owned by a specific {@code Clan}.<br>
	 * This method searches through all active forts.<br>
	 * It returns {@code null} if no fort is found or if the input is {@code null}.
	 * @param clan The {@code Clan} that owns the fort.
	 * @return The {@code Fort} object owned by the provided {@code Clan}, or {@code null}.
	 */
	public Fort getFortByOwner(Clan clan)
	{
		if (clan == null)
		{
			return null;
		}
		
		for (Fort fort : _forts.values())
		{
			if (fort.getOwnerClan() == clan)
			{
				return fort;
			}
		}
		
		return null;
	}
	
	/**
	 * Retrieves a {@code Fort} object based on its name.<br>
	 * The search is case-insensitive and ignores leading or trailing spaces.<br>
	 * Returns {@code null} if no matching fort is found.
	 * @param name The name of the fort to search for.
	 * @return The matching {@code Fort} object, or {@code null}.
	 */
	public Fort getFort(String name)
	{
		for (Fort fort : _forts.values())
		{
			if (fort.getName().equalsIgnoreCase(name.trim()))
			{
				return fort;
			}
		}
		
		return null;
	}
	
	/**
	 * Retrieves a {@code Fort} based on its coordinates.<br>
	 * This method checks if any existing fort contains the specified location.
	 * @param x The x-coordinate of the position.
	 * @param y The y-coordinate of the position.
	 * @param z The z-coordinate of the position.
	 * @return The {@code Fort} at the location, or {@code null} if no fort is found.
	 */
	public Fort getFort(int x, int y, int z)
	{
		for (Fort fort : _forts.values())
		{
			if (fort.checkIfInZone(x, y, z))
			{
				return fort;
			}
		}
		
		return null;
	}
	
	/**
	 * Retrieves the {@code Fort} located at the current position of a given object.<br>
	 * This method uses the coordinates of the {@code activeObject} to perform the lookup.
	 * @param activeObject The {@code WorldObject} used to determine the search location.
	 * @return The {@code Fort} found at those coordinates, or {@code null} if none exists.
	 */
	public Fort getFort(WorldObject activeObject)
	{
		return getFort(activeObject.getX(), activeObject.getY(), activeObject.getZ());
	}
	
	/**
	 * Retrieves all currently loaded forts.<br>
	 * This method returns a collection of {@code Fort} objects.
	 * @return A {@code Collection} containing all {@code Fort} instances.
	 */
	public Collection<Fort> getForts()
	{
		return _forts.values();
	}
	
	/**
	 * Loads all fortress data from the database.<br>
	 * Populates the internal map with {@code Fort} objects.<br>
	 * Initializes siege guards for every loaded fort.
	 */
	public void loadInstances()
	{
		try (Connection con = DatabaseFactory.getConnection();
			Statement s = con.createStatement();
			ResultSet rs = s.executeQuery("SELECT id FROM fort ORDER BY id"))
		{
			while (rs.next())
			{
				final int fortId = rs.getInt("id");
				_forts.put(fortId, new Fort(fortId));
			}
			
			LOGGER.info(getClass().getSimpleName() + ": Loaded " + _forts.values().size() + " fortress.");
			for (Fort fort : _forts.values())
			{
				fort.getSiege().loadSiegeGuard();
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Exception: loadFortData(): " + e.getMessage(), e);
		}
	}
	
	/**
	 * This method activates all loaded fort instances.<br>
	 * It iterates through the internal collection of {@code Fort} objects.<br>
	 * Each instance calls its own {@code activateInstance()} method.
	 */
	public void activateInstances()
	{
		for (Fort fort : _forts.values())
		{
			fort.activateInstance();
		}
	}
	
	/**
	 * Provides access to the singleton instance of {@code FortManager}.<br>
	 * Use this method to get the global manager for fort-related operations.
	 * @return The single instance of {@code FortManager}.
	 */
	public static FortManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final FortManager INSTANCE = new FortManager();
	}
}
