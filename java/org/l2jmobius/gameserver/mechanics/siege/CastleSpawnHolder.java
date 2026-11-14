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
 * This class represents a specific location where entities can spawn during siege events.<br>
 * It extends {@code Location} to provide specialized handling for castle-related spawning.
 * @author St3eT
 */
public class CastleSpawnHolder extends Location
{
	private final int _npcId;
	private final CastleSide _side;
	
	/**
	 * Creates a new {@code CastleSpawnHolder} at a specific location.<br>
	 * This constructor initializes the spawn point with an NPC ID and a castle side.
	 * @param npcId The unique identifier for the NPC.
	 * @param side The side of the castle where the spawn occurs.
	 * @param x The X coordinate of the location.
	 * @param y The Y coordinate of the location.
	 * @param z The Z coordinate of the location.
	 * @param heading The rotation angle of the spawn point.
	 */
	public CastleSpawnHolder(int npcId, CastleSide side, int x, int y, int z, int heading)
	{
		super(x, y, z, heading);
		_npcId = npcId;
		_side = side;
	}
	
	/**
	 * Retrieves the unique identifier for the NPC.<br>
	 * This ID is used to identify which NPC spawns at this location.
	 * @return The {@code int} value of the NPC ID.
	 */
	public int getNpcId()
	{
		return _npcId;
	}
	
	/**
	 * Retrieves the side associated with this spawn holder.<br>
	 * This identifies which castle side the location belongs to.
	 * @return the {@code CastleSide} of the current holder.
	 */
	public CastleSide getSide()
	{
		return _side;
	}
}
