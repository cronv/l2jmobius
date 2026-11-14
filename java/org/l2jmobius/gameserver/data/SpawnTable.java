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
package org.l2jmobius.gameserver.data;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.spawns.NpcSpawnTemplate;
import org.l2jmobius.gameserver.entity.spawns.Spawn;

/**
 * This class manages the collection of {@code Spawn} objects for a specific area.<br>
 * It handles the logic for determining which NPCs are allowed to appear in a given location.
 * @author Mobius
 */
public class SpawnTable
{
	private final Map<Integer, Set<Spawn>> _npcSpawns = new ConcurrentHashMap<>();
	
	/**
	 * Creates a new instance of the {@code SpawnTable} class.<br>
	 * This constructor is used to initialize the internal storage for NPC spawns.
	 */
	protected SpawnTable()
	{
	}
	
	/**
	 * Retrieves the complete collection of spawns.<br>
	 * The map uses an {@code Integer} ID as the key.<br>
	 * Each key maps to a {@code Set} of {@code Spawn} objects.
	 * @return A {@code Map} containing all registered spawn data.
	 */
	public Map<Integer, Set<Spawn>> getSpawnTable()
	{
		return _npcSpawns;
	}
	
	/**
	 * Retrieves all spawns associated with a specific NPC.<br>
	 * Returns an empty {@code Set} if no spawns are found for the given ID.
	 * @param npcId The unique identifier of the NPC to look up.
	 * @return A {@code Set} of {@code Spawn} objects.
	 */
	public Set<Spawn> getSpawns(int npcId)
	{
		return _npcSpawns.getOrDefault(npcId, Collections.emptySet());
	}
	
	/**
	 * Retrieves the total number of spawns for a specific NPC.<br>
	 * This method counts how many {@code Spawn} objects are associated with the given ID.
	 * @param npcId The unique identifier of the NPC to check.
	 * @return The total count of spawns for the provided {@code npcId}.
	 */
	public int getSpawnCount(int npcId)
	{
		return getSpawns(npcId).size();
	}
	
	/**
	 * Retrieves a single {@code Spawn} for the given NPC ID.<br>
	 * This method returns the first available spawn found in the table.<br>
	 * It returns {@code null} if no spawns exist for that ID.
	 * @param npcId The unique identifier of the NPC.
	 * @return A {@code Spawn} object or {@code null}.
	 */
	public Spawn getAnySpawn(int npcId)
	{
		return getSpawns(npcId).stream().findFirst().orElse(null);
	}
	
	/**
	 * Adds a new {@code Spawn} to the internal registry.<br>
	 * This method organizes spawns by their unique ID.<br>
	 * It ensures that multiple spawns can exist for the same NPC.
	 * @param spawn The {@code Spawn} object to be added.
	 */
	public void addSpawn(Spawn spawn)
	{
		_npcSpawns.computeIfAbsent(spawn.getId(), _ -> ConcurrentHashMap.newKeySet(1)).add(spawn);
	}
	
	/**
	 * Removes a specific {@code Spawn} from the internal registry.<br>
	 * This method updates the collection associated with the spawn's ID.<br>
	 * It also notifies other systems if the spawn is removed.
	 * @param spawn The {@code Spawn} object to be removed.
	 */
	public void removeSpawn(Spawn spawn)
	{
		final Integer spawnId = spawn.getId();
		final Set<Spawn> set = _npcSpawns.get(spawnId);
		if (set != null)
		{
			set.remove(spawn);
			if (set.isEmpty())
			{
				_npcSpawns.remove(spawnId);
			}
			
			set.forEach(this::notifyRemoved);
			return;
		}
		
		notifyRemoved(spawn);
	}
	
	/**
	 * Notifies the system that a {@code Spawn} has been removed.<br>
	 * This method updates the associated {@code NpcSpawnTemplate}.<br>
	 * It checks if the {@code Spawn}, {@code Npc}, and template are not {@code null}.
	 * @param spawn The {@code Spawn} object to notify.
	 */
	private void notifyRemoved(Spawn spawn)
	{
		if (spawn == null)
		{
			return;
		}
		
		final Npc npc = spawn.getLastSpawn();
		if (npc == null)
		{
			return;
		}
		
		final NpcSpawnTemplate template = spawn.getNpcSpawnTemplate();
		if (template == null)
		{
			return;
		}
		
		template.notifyDespawnNpc(npc);
	}
	
	/**
	 * Retrieves the singleton instance of the {@code SpawnTable}.<br>
	 * This method provides a global access point for managing NPC spawns.
	 * @return The single shared instance of {@code SpawnTable}.
	 */
	public static SpawnTable getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final SpawnTable INSTANCE = new SpawnTable();
	}
}
