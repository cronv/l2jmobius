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
package org.l2jmobius.gameserver.data.holders;

import java.util.HashMap;
import java.util.Map;

import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.spawns.Spawn;
import org.l2jmobius.gameserver.interfaces.ILocational;

/**
 * This class manages the relationship between {@code Npc} spawn points and their associated routes.<br>
 * It ensures that NPCs are correctly linked to specific paths based on their location.
 * @author GKR
 */
public class NpcRoutesHolder
{
	private final Map<String, String> _correspondences;
	
	/**
	 * Creates a new instance of {@code NpcRoutesHolder}.<br>
	 * Initializes the internal map for NPC route correspondences.
	 */
	public NpcRoutesHolder()
	{
		_correspondences = new HashMap<>();
	}
	
	/**
	 * Adds a new route to the internal mapping.<br>
	 * This method links a {@code Location} to a specific {@code routeName}.
	 * @param routeName The name of the route to assign.
	 * @param loc The {@code Location} where the route is registered.
	 */
	public void addRoute(String routeName, Location loc)
	{
		_correspondences.put(getUniqueKey(loc), routeName);
	}
	
	/**
	 * Retrieves the route name associated with a specific {@code Npc}.<br>
	 * This method checks if the {@code Npc} has a valid spawn point.<br>
	 * It returns an empty string if no route is found or if the spawn is null.
	 * @param npc The {@code Npc} entity to check for a route name.
	 * @return The name of the route as a {@code String}, or an empty string if none exists.
	 */
	public String getRouteName(Npc npc)
	{
		final Spawn spawn = npc.getSpawn();
		if (spawn != null)
		{
			final String key = getUniqueKey(spawn.getLocation());
			return _correspondences.getOrDefault(key, "");
		}
		
		return "";
	}
	
	/**
	 * Generates a unique identifier for a specific location.<br>
	 * This key is created by combining the X, Y, and Z coordinates.<br>
	 * It helps identify distinct points in the game world.
	 * @param loc The {@code ILocational} object containing the coordinates.
	 * @return A {@code String} representing the unique coordinate key.
	 */
	private String getUniqueKey(ILocational loc)
	{
		return (loc.getX() + "-" + loc.getY() + "-" + loc.getZ());
	}
}
