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
package org.l2jmobius.gameserver.entity.residences;

import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.network.NpcStringId;

/**
 * This class represents a specific {@code Location} used for teleporting to a clan hall.<br>
 * It stores the necessary coordinates and data required to handle these teleports.
 * @author St3eT
 */
public class ClanHallTeleportHolder extends Location
{
	private final NpcStringId _npcStringId;
	private final int _minFunctionLevel;
	private final int _cost;
	
	/**
	 * Creates a new {@code ClanHallTeleportHolder} at a specific location.<br>
	 * This constructor initializes the teleport destination and its requirements.
	 * @param npcStringId The unique identifier for the NPC associated with this teleport.
	 * @param x The X coordinate of the teleport destination.
	 * @param y The Y coordinate of the teleport destination.
	 * @param z The Z coordinate of the teleport destination.
	 * @param minFunctionLevel The minimum level required to use this teleport.
	 * @param cost The amount of currency required to use this teleport.
	 */
	public ClanHallTeleportHolder(int npcStringId, int x, int y, int z, int minFunctionLevel, int cost)
	{
		super(x, y, z);
		_npcStringId = NpcStringId.getNpcStringId(npcStringId);
		_minFunctionLevel = minFunctionLevel;
		_cost = cost;
	}
	
	/**
	 * Retrieves the unique identifier for the NPC associated with this teleport.<br>
	 * This ID is used to identify the specific entity in the game world.
	 * @return the {@code NpcStringId} of the NPC.
	 */
	public NpcStringId getNpcStringId()
	{
		return _npcStringId;
	}
	
	/**
	 * Retrieves the minimum function level required for this teleport.<br>
	 * This value is used to check if a player meets the necessary requirements.
	 * @return The minimum function level as an {@code int}.
	 */
	public int getMinFunctionLevel()
	{
		return _minFunctionLevel;
	}
	
	/**
	 * Retrieves the teleportation cost.<br>
	 * This value represents the amount required to use this specific teleport.
	 * @return The {@code int} value of the cost.
	 */
	public int getCost()
	{
		return _cost;
	}
}
