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
import java.sql.Statement;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.clan.ClanMember;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.siege.Castle;

/**
 * Manages the lifecycle and state of {@code Castle} objects within the game world.<br>
 * This class handles the loading, saving, and general maintenance of castle data.
 */
public class CastleManager
{
	private static final Logger LOGGER = Logger.getLogger(CastleManager.class.getName());
	
	private static final int LORDS_CROWN = 6841;
	
	private final Map<Integer, Castle> _castles = new ConcurrentSkipListMap<>();
	private final Map<Integer, Long> _castleSiegeDate = new ConcurrentHashMap<>();
	
	private static final int[] _castleCirclets =
	{
		0,
		6838,
		6835,
		6839,
		6837,
		6840,
		6834,
		6836,
		8182,
		8183
	};
	
	/**
	 * Finds the closest {@code Castle} to a specific world object.<br>
	 * This method searches all available castles and returns the one with the shortest distance.<br>
	 * If no castle is found, it may return {@code null}.
	 * @param obj The {@code WorldObject} used as the starting point for the search.
	 * @return The nearest {@code Castle} object.
	 */
	public Castle findNearestCastle(WorldObject obj)
	{
		return findNearestCastle(obj, Long.MAX_VALUE);
	}
	
	/**
	 * Finds the closest {@code Castle} to a specific {@code WorldObject}.<br>
	 * It searches within a maximum distance defined by {@code maxDistanceValue}.<br>
	 * If no castle is found nearby, it returns {@code null}.
	 * @param obj The {@code WorldObject} used as the starting point for the search.
	 * @param maxDistanceValue The maximum distance to search for a {@code Castle}.
	 * @return The nearest {@code Castle} object or {@code null} if none are found.
	 */
	public Castle findNearestCastle(WorldObject obj, long maxDistanceValue)
	{
		Castle nearestCastle = getCastle(obj);
		if (nearestCastle == null)
		{
			double distance;
			long maxDistance = maxDistanceValue;
			for (Castle castle : _castles.values())
			{
				distance = castle.getDistance(obj);
				if (maxDistance > distance)
				{
					maxDistance = (long) distance;
					nearestCastle = castle;
				}
			}
		}
		
		return nearestCastle;
	}
	
	/**
	 * Retrieves a {@code Castle} object based on its unique identifier.<br>
	 * This method looks up the castle in the internal map.<br>
	 * It returns {@code null} if no castle is found with the given ID.
	 * @param castleId The unique integer ID of the castle to find.
	 * @return The corresponding {@code Castle} object or {@code null}.
	 */
	public Castle getCastleById(int castleId)
	{
		return _castles.get(castleId);
	}
	
	/**
	 * Retrieves the {@code Castle} owned by a specific {@code Clan}.<br>
	 * This method searches through all active castles to find a match.<br>
	 * It returns {@code null} if no castle is found or if the input is {@code null}.
	 * @param clan The {@code Clan} that owns the castle.
	 * @return The {@code Castle} object owned by the clan, or {@code null}.
	 */
	public Castle getCastleByOwner(Clan clan)
	{
		if (clan == null)
		{
			return null;
		}
		
		for (Castle temp : _castles.values())
		{
			if (temp.getOwnerId() == clan.getId())
			{
				return temp;
			}
		}
		
		return null;
	}
	
	/**
	 * Retrieves a {@code Castle} object based on its name.<br>
	 * The search is case-insensitive and ignores surrounding whitespace.
	 * @param name The name of the castle to find.
	 * @return The matching {@code Castle} object, or {@code null} if no match is found.
	 */
	public Castle getCastle(String name)
	{
		for (Castle temp : _castles.values())
		{
			if (temp.getName().equalsIgnoreCase(name.trim()))
			{
				return temp;
			}
		}
		
		return null;
	}
	
	/**
	 * Retrieves a {@code Castle} based on its world coordinates.<br>
	 * This method checks if any castle contains the specified location.
	 * @param x The x-coordinate of the position.
	 * @param y The y-coordinate of the position.
	 * @param z The z-coordinate of the position.
	 * @return The {@code Castle} at the location, or {@code null} if none is found.
	 */
	public Castle getCastle(int x, int y, int z)
	{
		for (Castle temp : _castles.values())
		{
			if (temp.checkIfInZone(x, y, z))
			{
				return temp;
			}
		}
		
		return null;
	}
	
	/**
	 * Retrieves the {@code Castle} located at the position of the provided object.<br>
	 * This method uses the coordinates from the {@code WorldObject}.
	 * @param activeObject The {@code WorldObject} used to determine the location.
	 * @return The {@code Castle} at that location, or {@code null} if none exists.
	 */
	public Castle getCastle(WorldObject activeObject)
	{
		return getCastle(activeObject.getX(), activeObject.getY(), activeObject.getZ());
	}
	
	/**
	 * Retrieves all castles currently managed by the system.<br>
	 * This method returns a collection of {@code Castle} objects.
	 * @return A {@code Collection} containing all {@code Castle} instances.
	 */
	public Collection<Castle> getCastles()
	{
		return _castles.values();
	}
	
	/**
	 * Checks if any castle is currently owned by a clan.<br>
	 * It iterates through all castles to see if any have an owner ID greater than {@code 0}.
	 * @return {@code true} if at least one castle has an owner, otherwise {@code false}.
	 */
	public boolean hasOwnedCastle()
	{
		boolean hasOwnedCastle = false;
		for (Castle castle : _castles.values())
		{
			if (castle.getOwnerId() > 0)
			{
				hasOwnedCastle = true;
				break;
			}
		}
		
		return hasOwnedCastle;
	}
	
	/**
	 * Retrieves the circlet item ID associated with a specific castle.<br>
	 * This method checks if the {@code castleId} is between 1 and 9.<br>
	 * If the ID is out of range, it returns 0.
	 * @param castleId The unique identifier of the castle.
	 * @return The circlet item ID or 0 if not found.
	 */
	public int getCircletByCastleId(int castleId)
	{
		if ((castleId > 0) && (castleId < 10))
		{
			return _castleCirclets[castleId];
		}
		
		return 0;
	}
	
	// Remove this castle's circlets from the clan.
	/**
	 * Removes the circlet from all members of a specific clan.<br>
	 * This method iterates through every {@code ClanMember} in the provided {@code Clan}.<br>
	 * It calls the overloaded {@code removeCirclet} method for each member using the given {@code castleId}.
	 * @param clan The {@code Clan} object containing the members to update.
	 * @param castleId The unique identifier of the castle associated with the circlet.
	 */
	public void removeCirclet(Clan clan, int castleId)
	{
		for (ClanMember member : clan.getMembers())
		{
			removeCirclet(member, castleId);
		}
		
		// The lord loses the crown together with the castle.
		removeItemFromMember(clan.getLeader(), LORDS_CROWN);
	}
	
	/**
	 * Removes the castle circlet from a specific clan member.<br>
	 * This method handles both online and offline players.<br>
	 * If the player is online, it unequips and destroys the item.<br>
	 * If the player is offline, it removes the item from the database.
	 * @param member The {@code ClanMember} to remove the circlet from.
	 * @param castleId The unique identifier for the castle.
	 */
	public void removeCirclet(ClanMember member, int castleId)
	{
		removeItemFromMember(member, getCircletByCastleId(castleId));
	}
	
	/**
	 * Takes one item away from a clan member, whether that member is online or not.
	 * @param member The {@code ClanMember} losing the item.
	 * @param itemId The item to remove, ignored when it is 0.
	 */
	private void removeItemFromMember(ClanMember member, int itemId)
	{
		if (member == null)
		{
			return;
		}
		
		final Player player = member.getPlayer();
		if (itemId != 0)
		{
			// Online-player circlet removal.
			if (player != null)
			{
				try
				{
					final Item circlet = player.getInventory().getItemByItemId(itemId);
					if (circlet != null)
					{
						if (circlet.isEquipped())
						{
							player.getInventory().unEquipItemInSlot(circlet.getLocationSlot());
						}
						
						player.destroyItemByItemId(ItemProcessType.DESTROY, itemId, 1, player, true);
					}
					return;
				}
				catch (NullPointerException e)
				{
					// Continue removing offline.
				}
			}
			
			// Else offline-player circlet removal.
			try (Connection con = DatabaseFactory.getConnection();
				PreparedStatement ps = con.prepareStatement("DELETE FROM items WHERE owner_id = ? and item_id = ?"))
			{
				ps.setInt(1, member.getObjectId());
				ps.setInt(2, itemId);
				ps.execute();
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Failed to remove castle circlets offline for player " + member.getName() + ": ", e);
			}
		}
	}
	
	/**
	 * Loads all castle data from the database.<br>
	 * This method populates the internal map of {@code Castle} objects.<br>
	 * It logs a message once the loading process is complete.
	 */
	public void loadInstances()
	{
		try (Connection con = DatabaseFactory.getConnection();
			Statement s = con.createStatement();
			ResultSet rs = s.executeQuery("SELECT id FROM castle ORDER BY id"))
		{
			while (rs.next())
			{
				final int castleId = rs.getInt("id");
				_castles.put(castleId, new Castle(castleId));
			}
			
			LOGGER.info(getClass().getSimpleName() + ": Loaded " + _castles.values().size() + " castles.");
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Exception: loadCastleData():", e);
		}
	}
	
	/**
	 * This method activates all loaded castle instances.<br>
	 * It iterates through every {@code Castle} in the internal map.<br>
	 * Each instance calls its own {@code activateInstance()} method.
	 */
	public void activateInstances()
	{
		for (Castle castle : _castles.values())
		{
			castle.activateInstance();
		}
	}
	
	/**
	 * Registers a specific siege date for a castle.<br>
	 * This method updates the internal map with the provided ID and timestamp.
	 * @param castleId The unique identifier of the castle.
	 * @param siegeDate The timestamp representing when the siege occurs.
	 */
	public void registerSiegeDate(int castleId, long siegeDate)
	{
		_castleSiegeDate.put(castleId, siegeDate);
	}
	
	/**
	 * Counts the number of castles with a siege date near the provided value.<br>
	 * It checks for dates within a range of 1000 units from {@code siegeDate}.
	 * @param siegeDate The timestamp to check against.
	 * @return The total count of matching siege dates.
	 */
	public int getSiegeDates(long siegeDate)
	{
		int count = 0;
		for (long date : _castleSiegeDate.values())
		{
			if (Math.abs(date - siegeDate) < 1000)
			{
				count++;
			}
		}
		
		return count;
	}
	
	/**
	 * Retrieves the singleton instance of the {@code CastleManager}.<br>
	 * Use this method to access the global manager for castle operations.
	 * @return The active {@code CastleManager} instance.
	 */
	public static CastleManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final CastleManager INSTANCE = new CastleManager();
	}
}
