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

import org.l2jmobius.gameserver.entity.Location;

/**
 * This class represents a specific spawn point within the game world.<br>
 * It extends {@code Location} to store coordinates for entity spawning.
 * @author St3eT
 */
public class SpawnHolder extends Location
{
	private final int _npcId;
	private final int _respawnDelay;
	private final boolean _spawnAnimation;
	
	/**
	 * Creates a new {@code SpawnHolder} instance.<br>
	 * This constructor sets the initial location and NPC data.<br>
	 * The respawn delay is set to {@code 0} by default.
	 * @param npcId The unique identifier for the NPC.
	 * @param x The X coordinate of the spawn point.
	 * @param y The Y coordinate of the spawn point.
	 * @param z The Z coordinate of the spawn point.
	 * @param heading The rotation angle of the spawn point.
	 * @param spawnAnimation Whether to play an animation when spawning.
	 */
	public SpawnHolder(int npcId, int x, int y, int z, int heading, boolean spawnAnimation)
	{
		super(x, y, z, heading);
		_npcId = npcId;
		_respawnDelay = 0;
		_spawnAnimation = spawnAnimation;
	}
	
	/**
	 * Creates a new {@code SpawnHolder} instance.<br>
	 * This constructor initializes the NPC data and its location coordinates.
	 * @param npcId The unique identifier for the NPC.
	 * @param x The X coordinate of the spawn point.
	 * @param y The Y coordinate of the spawn point.
	 * @param z The Z coordinate of the spawn point.
	 * @param heading The direction the NPC faces at the spawn point.
	 * @param respawn The delay before the NPC can respawn again.
	 * @param spawnAnimation Whether to play an animation when the NPC spawns.
	 */
	public SpawnHolder(int npcId, int x, int y, int z, int heading, int respawn, boolean spawnAnimation)
	{
		super(x, y, z, heading);
		_npcId = npcId;
		_respawnDelay = respawn;
		_spawnAnimation = spawnAnimation;
	}
	
	/**
	 * Creates a new {@code SpawnHolder} instance.<br>
	 * This constructor sets the NPC ID and location coordinates.<br>
	 * It also defines whether a spawn animation should play.
	 * @param npcId The unique identifier for the NPC.
	 * @param loc The {@code Location} where the NPC will appear.
	 * @param spawnAnimation Set to {@code true} to enable an animation, or {@code false} to disable it.
	 */
	public SpawnHolder(int npcId, Location loc, boolean spawnAnimation)
	{
		super(loc.getX(), loc.getY(), loc.getZ(), loc.getHeading());
		_npcId = npcId;
		_respawnDelay = 0;
		_spawnAnimation = spawnAnimation;
	}
	
	/**
	 * Creates a new {@code SpawnHolder} instance.<br>
	 * This constructor initializes the NPC data and its location.
	 * @param npcId The unique identifier for the NPC.
	 * @param loc The {@code Location} where the NPC will appear.
	 * @param respawn The delay in seconds before the NPC can reappear.
	 * @param spawnAnimation Whether to play an animation when the NPC spawns.
	 */
	public SpawnHolder(int npcId, Location loc, int respawn, boolean spawnAnimation)
	{
		super(loc.getX(), loc.getY(), loc.getZ(), loc.getHeading());
		_npcId = npcId;
		_respawnDelay = respawn;
		_spawnAnimation = spawnAnimation;
	}
	
	/**
	 * Retrieves the unique identifier for the NPC.<br>
	 * This ID identifies which creature spawns at this location.
	 * @return The {@code int} value of the NPC ID.
	 */
	public int getNpcId()
	{
		return _npcId;
	}
	
	/**
	 * Checks if the spawn animation is enabled.<br>
	 * Returns {@code true} if an animation should play when the NPC appears.<br>
	 * Returns {@code false} otherwise.
	 * @return The status of the spawn animation.
	 */
	public boolean isSpawnAnimation()
	{
		return _spawnAnimation;
	}
	
	/**
	 * Retrieves the time it takes for an NPC to reappear.<br>
	 * This value is measured in seconds.
	 * @return The respawn delay as an {@code int}.
	 */
	public int getRespawnDelay()
	{
		return _respawnDelay;
	}
}
