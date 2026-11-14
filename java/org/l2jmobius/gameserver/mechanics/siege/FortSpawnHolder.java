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

import org.l2jmobius.gameserver.entity.Location;

/**
 * Represents a specific location for spawning entities during a fort siege.<br>
 * This class extends {@code Location} to provide specialized data for siege mechanics.
 * @author xban1x
 */
public class FortSpawnHolder extends Location
{
	private final int _npcId;
	private final int _fortId;
	private final int _id;
	
	/**
	 * Creates a new {@code FortSpawnHolder} instance.<br>
	 * This constructor initializes the spawn location and its associated IDs.
	 * @param fortId The unique identifier for the fort.
	 * @param x The X coordinate of the spawn point.
	 * @param y The Y coordinate of the spawn point.
	 * @param z The Z coordinate of the spawn point.
	 * @param heading The rotation angle of the spawn point.
	 * @param npcId The ID of the NPC to be spawned.
	 * @param id The unique identifier for this specific spawn holder.
	 */
	public FortSpawnHolder(int fortId, int x, int y, int z, int heading, int npcId, int id)
	{
		super(x, y, z, heading);
		_fortId = fortId;
		_npcId = npcId;
		_id = id;
	}
	
	/**
	 * Retrieves the unique identifier for the fort.<br>
	 * This value is used to identify which specific fort this spawn belongs to.
	 * @return The {@code int} ID of the fort.
	 */
	public int getFortId()
	{
		return _fortId;
	}
	
	/**
	 * Retrieves the unique identifier for the NPC.<br>
	 * This value is used to identify which NPC spawns at this location.
	 * @return The {@code int} ID of the NPC.
	 */
	public int getId()
	{
		return _npcId;
	}
	
	/**
	 * Retrieves the unique identifier for this message.<br>
	 * This value is used to identify specific messages within the system.
	 * @return The {@code int} ID of the message.
	 */
	public int getMessageId()
	{
		return _id;
	}
}
