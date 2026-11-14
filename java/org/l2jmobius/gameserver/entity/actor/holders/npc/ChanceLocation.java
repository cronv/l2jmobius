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
package org.l2jmobius.gameserver.entity.actor.holders.npc;

import org.l2jmobius.gameserver.entity.Location;

/**
 * Represents a location for an NPC that is determined by a random chance.<br>
 * It allows the game server to spawn entities at various points based on weighted probabilities.
 * @author UnAfraid
 */
public class ChanceLocation extends Location
{
	private final double _chance;
	
	/**
	 * Creates a new {@code ChanceLocation} at the specified coordinates.<br>
	 * This class defines a location with an associated spawn probability.
	 * @param x The X coordinate of the location.
	 * @param y The Y coordinate of the location.
	 * @param z The Z coordinate of the location.
	 * @param heading The direction the entity faces at this location.
	 * @param chance The probability value for spawning at this location.
	 */
	public ChanceLocation(int x, int y, int z, int heading, double chance)
	{
		super(x, y, z, heading);
		_chance = chance;
	}
	
	/**
	 * Retrieves the probability value for this location.<br>
	 * This value is used to determine if an NPC spawns here.
	 * @return The chance as a {@code double}.
	 */
	public double getChance()
	{
		return _chance;
	}
}
