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
package org.l2jmobius.gameserver.entity.zone;

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.entity.Location;

/**
 * This class represents an abstract zone that contains specific spawn locations.<br>
 * It serves as a base for defining areas where entities can appear in the game world.
 * @author DS, Nyaran
 */
public abstract class ZoneRespawn extends ZoneType
{
	private List<Location> _spawnLocs = null;
	private List<Location> _otherSpawnLocs = null;
	private List<Location> _chaoticSpawnLocs = null;
	private List<Location> _banishSpawnLocs = null;
	
	/**
	 * Creates a new instance of a {@code ZoneRespawn}.<br>
	 * This constructor initializes the zone with a specific unique identifier.
	 * @param id The unique identification number for this zone.
	 */
	protected ZoneRespawn(int id)
	{
		super(id);
	}
	
	/**
	 * Parses and adds a new spawn location to the zone.<br>
	 * It determines which list to add the coordinates to based on the provided {@code type}.<br>
	 * If {@code type} is {@code null} or empty, it defaults to a standard spawn.
	 * @param x The X coordinate of the location.
	 * @param y The Y coordinate of the location.
	 * @param z The Z coordinate of the location.
	 * @param type The category of the spawn, such as "other", "chaotic", or "banish".
	 */
	public void parseLoc(int x, int y, int z, String type)
	{
		if ((type == null) || type.isEmpty())
		{
			addSpawn(x, y, z);
		}
		else
		{
			switch (type)
			{
				case "other":
				{
					addOtherSpawn(x, y, z);
					break;
				}
				case "chaotic":
				{
					addChaoticSpawn(x, y, z);
					break;
				}
				case "banish":
				{
					addBanishSpawn(x, y, z);
					break;
				}
				default:
				{
					LOGGER.warning(getClass().getSimpleName() + ": Unknown location type: " + type);
					break;
				}
			}
		}
	}
	
	/**
	 * Adds a new spawn location to the zone.<br>
	 * This method creates a {@code Location} object using the provided coordinates.<br>
	 * It stores the location in the internal list of spawns.
	 * @param x The X coordinate of the spawn point.
	 * @param y The Y coordinate of the spawn point.
	 * @param z The Z coordinate of the spawn point.
	 */
	public void addSpawn(int x, int y, int z)
	{
		if (_spawnLocs == null)
		{
			_spawnLocs = new ArrayList<>();
		}
		
		_spawnLocs.add(new Location(x, y, z));
	}
	
	/**
	 * Adds a new location to the other spawn list.<br>
	 * This method creates a {@code Location} object using the provided coordinates.
	 * @param x The x coordinate of the spawn point.
	 * @param y The y coordinate of the spawn point.
	 * @param z The z coordinate of the spawn point.
	 */
	public void addOtherSpawn(int x, int y, int z)
	{
		if (_otherSpawnLocs == null)
		{
			_otherSpawnLocs = new ArrayList<>();
		}
		
		_otherSpawnLocs.add(new Location(x, y, z));
	}
	
	/**
	 * Adds a new location to the chaotic spawn list.<br>
	 * This method creates a {@code Location} object using the provided coordinates.<br>
	 * It ensures the internal list is initialized before adding the point.
	 * @param x The X coordinate of the spawn.
	 * @param y The Y coordinate of the spawn.
	 * @param z The Z coordinate of the spawn.
	 */
	public void addChaoticSpawn(int x, int y, int z)
	{
		if (_chaoticSpawnLocs == null)
		{
			_chaoticSpawnLocs = new ArrayList<>();
		}
		
		_chaoticSpawnLocs.add(new Location(x, y, z));
	}
	
	/**
	 * Adds a new banish spawn location to the zone.<br>
	 * This method creates a {@code Location} object using the provided coordinates.<br>
	 * The new location is stored in the internal list of banish spawns.
	 * @param x The X coordinate of the spawn point.
	 * @param y The Y coordinate of the spawn point.
	 * @param z The Z coordinate of the spawn point.
	 */
	public void addBanishSpawn(int x, int y, int z)
	{
		if (_banishSpawnLocs == null)
		{
			_banishSpawnLocs = new ArrayList<>();
		}
		
		_banishSpawnLocs.add(new Location(x, y, z));
	}
	
	/**
	 * Retrieves the list of standard spawn locations for this zone.
	 * @return a {@code List} of {@code Location} objects.
	 */
	public List<Location> getSpawns()
	{
		return _spawnLocs;
	}
	
	/**
	 * Retrieves a spawn location for the zone.<br>
	 * It returns a random location if {@code PlayerConfig.RANDOM_RESPAWN_IN_TOWN_ENABLED} is true.<br>
	 * Otherwise, it returns the first location in the list.
	 * @return the selected {@code Location} object.
	 */
	public Location getSpawnLoc()
	{
		if (PlayerConfig.RANDOM_RESPAWN_IN_TOWN_ENABLED)
		{
			return _spawnLocs.get(Rnd.get(_spawnLocs.size()));
		}
		
		return _spawnLocs.get(0);
	}
	
	/**
	 * Retrieves a random or default spawn location from the other spawns list.<br>
	 * If {@code PlayerConfig.RANDOM_RESPAWN_IN_TOWN_ENABLED} is true, it returns a random {@code Location}.<br>
	 * Otherwise, it returns the first {@code Location} in the list.<br>
	 * If no other spawns exist, it returns the result of {@code getSpawnLoc}.
	 * @return The selected {@code Location} object.
	 */
	public Location getOtherSpawnLoc()
	{
		if (_otherSpawnLocs != null)
		{
			if (PlayerConfig.RANDOM_RESPAWN_IN_TOWN_ENABLED)
			{
				return _otherSpawnLocs.get(Rnd.get(_otherSpawnLocs.size()));
			}
			
			return _otherSpawnLocs.get(0);
		}
		
		return getSpawnLoc();
	}
	
	/**
	 * Retrieves a random chaotic spawn location.<br>
	 * It checks if {@code PlayerConfig.RANDOM_RESPAWN_IN_TOWN_ENABLED} is true to decide the selection logic.<br>
	 * If no chaotic locations exist, it returns the default spawn location.
	 * @return The selected {@code Location} object or the default spawn location.
	 */
	public Location getChaoticSpawnLoc()
	{
		if (_chaoticSpawnLocs != null)
		{
			if (PlayerConfig.RANDOM_RESPAWN_IN_TOWN_ENABLED)
			{
				return _chaoticSpawnLocs.get(Rnd.get(_chaoticSpawnLocs.size()));
			}
			
			return _chaoticSpawnLocs.get(0);
		}
		
		return getSpawnLoc();
	}
	
	/**
	 * Retrieves the spawn location for a banished player.<br>
	 * It checks if {@code PlayerConfig.RANDOM_RESPAWN_IN_TOWN_ENABLED} is true to decide between a random or fixed location.<br>
	 * If no specific banish locations exist, it returns the default spawn location.
	 * @return the {@code Location} of the banish spawn point.
	 */
	public Location getBanishSpawnLoc()
	{
		if (_banishSpawnLocs != null)
		{
			if (PlayerConfig.RANDOM_RESPAWN_IN_TOWN_ENABLED)
			{
				return _banishSpawnLocs.get(Rnd.get(_banishSpawnLocs.size()));
			}
			
			return _banishSpawnLocs.get(0);
		}
		
		return getSpawnLoc();
	}
}
