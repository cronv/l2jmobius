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
package org.l2jmobius.gameserver.mechanics.siege;

import java.util.List;

import org.l2jmobius.gameserver.entity.Location;

/**
 * This class manages the spawning logic for siege towers.<br>
 * It handles the placement and initialization of tower entities within the game world.
 * @author malyelfik
 */
public class TowerSpawn
{
	private final int _npcId;
	private final Location _location;
	private List<Integer> _zoneList = null;
	private int _upgradeLevel = 0;
	
	/**
	 * Creates a new {@code TowerSpawn} instance.<br>
	 * This constructor sets the initial NPC ID and world location.
	 * @param npcId The unique identifier for the NPC.
	 * @param location The {@code Location} where the tower will appear.
	 */
	public TowerSpawn(int npcId, Location location)
	{
		_location = location;
		_npcId = npcId;
	}
	
	/**
	 * Creates a new {@code TowerSpawn} instance.<br>
	 * This constructor initializes the tower with its specific NPC ID and location.<br>
	 * It also assigns the list of zones where this spawn is active.
	 * @param npcId The unique identifier for the NPC type.
	 * @param location The world coordinates where the tower will appear.
	 * @param zoneList A {@code List} of integers representing the allowed zones.
	 */
	public TowerSpawn(int npcId, Location location, List<Integer> zoneList)
	{
		_location = location;
		_npcId = npcId;
		_zoneList = zoneList;
	}
	
	/**
	 * Returns the unique identifier for this tower.<br>
	 * This value corresponds to the {@code npcId}.
	 * @return The integer ID of the tower.
	 */
	public int getId()
	{
		return _npcId;
	}
	
	/**
	 * Retrieves the current location of this tower.<br>
	 * This method returns the {@code Location} object stored in the instance.
	 * @return The {@code Location} of the tower.
	 */
	public Location getLocation()
	{
		return _location;
	}
	
	/**
	 * Retrieves the list of zones associated with this tower.<br>
	 * Returns a {@code List} of zone identifiers.
	 * @return A {@code List<Integer>} containing the zone IDs.
	 */
	public List<Integer> getZoneList()
	{
		return _zoneList;
	}
	
	/**
	 * Updates the current upgrade level of the tower.<br>
	 * This method sets the {@code _upgradeLevel} field to a new value.
	 * @param level The new integer level to assign.
	 */
	public void setUpgradeLevel(int level)
	{
		_upgradeLevel = level;
	}
	
	/**
	 * Retrieves the current upgrade level of the tower.<br>
	 * This value represents how many times the tower has been upgraded.
	 * @return The current {@code int} level of the tower.
	 */
	public int getUpgradeLevel()
	{
		return _upgradeLevel;
	}
}
