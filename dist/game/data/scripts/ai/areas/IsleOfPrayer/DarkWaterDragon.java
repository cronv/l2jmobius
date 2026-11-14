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
package ai.areas.IsleOfPrayer;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.entity.actor.Attackable;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.script.Script;

/**
 * This class handles the artificial intelligence for the {@code DarkWaterDragon} NPC.<br>
 * It defines the behaviors and actions performed by this creature within its area.
 */
public class DarkWaterDragon extends Script
{
	private static final int DRAGON = 22267;
	private static final int SHADE1 = 22268;
	private static final int SHADE2 = 22269;
	private static final int FAFURION = 18482;
	private static final int DETRACTOR1 = 22270;
	private static final int DETRACTOR2 = 22271;
	private static Set<Integer> SECOND_SPAWN = ConcurrentHashMap.newKeySet(); // Used to track if second Shades were already spawned
	private static Set<Integer> MY_TRACKING_SET = ConcurrentHashMap.newKeySet(); // Used to track instances of npcs
	private static Map<Integer, Player> ID_MAP = new ConcurrentHashMap<>(); // Used to track instances of npcs
	
	/**
	 * Initializes the {@code DarkWaterDragon} script.<br>
	 * Registers all required NPC IDs for events.<br>
	 * Clears the internal tracking sets for new instances.
	 */
	private DarkWaterDragon()
	{
		final int[] mobs =
		{
			DRAGON,
			SHADE1,
			SHADE2,
			FAFURION,
			DETRACTOR1,
			DETRACTOR2
		};
		addKillId(mobs);
		addAttackId(mobs);
		addSpawnId(mobs);
		MY_TRACKING_SET.clear();
		SECOND_SPAWN.clear();
	}
	
	/**
	 * Handles specific events for the Dark Water Dragon script.<br>
	 * This method manages spawn timers and special logic like poison effects.<br>
	 * It also handles the despawning of Fafurion and item drops.
	 * @param event The name of the event to trigger.
	 * @param npc The {@code Npc} instance involved in the event.
	 * @param player The {@code Player} instance involved in the event.
	 * @return The result of the super class {@code onEvent} method.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (npc != null)
		{
			if (event.equalsIgnoreCase("first_spawn")) // timer to start timer "1"
			{
				startQuestTimer("1", 40000, npc, null, true); // spawns detractor every 40 seconds
			}
			else if (event.equalsIgnoreCase("second_spawn")) // timer to start timer "2"
			{
				startQuestTimer("2", 40000, npc, null, true); // spawns detractor every 40 seconds
			}
			else if (event.equalsIgnoreCase("third_spawn")) // timer to start timer "3"
			{
				startQuestTimer("3", 40000, npc, null, true); // spawns detractor every 40 seconds
			}
			else if (event.equalsIgnoreCase("fourth_spawn")) // timer to start timer "4"
			{
				startQuestTimer("4", 40000, npc, null, true); // spawns detractor every 40 seconds
			}
			else if (event.equalsIgnoreCase("1")) // spawns a detractor
			{
				addSpawn(DETRACTOR1, (npc.getX() + 100), (npc.getY() + 100), npc.getZ(), 0, false, 40000);
			}
			else if (event.equalsIgnoreCase("2")) // spawns a detractor
			{
				addSpawn(DETRACTOR2, (npc.getX() + 100), (npc.getY() - 100), npc.getZ(), 0, false, 40000);
			}
			else if (event.equalsIgnoreCase("3")) // spawns a detractor
			{
				addSpawn(DETRACTOR1, (npc.getX() - 100), (npc.getY() + 100), npc.getZ(), 0, false, 40000);
			}
			else if (event.equalsIgnoreCase("4")) // spawns a detractor
			{
				addSpawn(DETRACTOR2, (npc.getX() - 100), (npc.getY() - 100), npc.getZ(), 0, false, 40000);
			}
			else if (event.equalsIgnoreCase("fafurion_despawn")) // Fafurion Kindred disappears and drops reward
			{
				cancelQuestTimer("fafurion_poison", npc, null);
				cancelQuestTimer("1", npc, null);
				cancelQuestTimer("2", npc, null);
				cancelQuestTimer("3", npc, null);
				cancelQuestTimer("4", npc, null);
				
				final int objectId = npc.getObjectId();
				MY_TRACKING_SET.remove(objectId);
				final Player removed = ID_MAP.remove(objectId);
				if (removed != null)
				{
					npc.asAttackable().doItemDrop(NpcData.getInstance().getTemplate(18485), removed);
				}
				
				npc.deleteMe();
			}
			else if (event.equalsIgnoreCase("fafurion_poison")) // Reduces Fafurions hp like it is poisoned
			{
				if (npc.getCurrentHp() <= 500)
				{
					cancelQuestTimer("fafurion_despawn", npc, null);
					cancelQuestTimer("first_spawn", npc, null);
					cancelQuestTimer("second_spawn", npc, null);
					cancelQuestTimer("third_spawn", npc, null);
					cancelQuestTimer("fourth_spawn", npc, null);
					cancelQuestTimer("1", npc, null);
					cancelQuestTimer("2", npc, null);
					cancelQuestTimer("3", npc, null);
					cancelQuestTimer("4", npc, null);
					
					final int objectId = npc.getObjectId();
					MY_TRACKING_SET.remove(objectId);
					ID_MAP.remove(objectId);
				}
				
				npc.reduceCurrentHp(500, npc, null); // poison kills Fafurion if he is not healed
			}
		}
		
		return super.onEvent(event, npc, player);
	}
	
	/**
	 * Handles the logic when an {@code Npc} is attacked.<br>
	 * This method triggers the spawning of shades based on health thresholds.
	 * @param npc The {@code Npc} being attacked.
	 * @param attacker The {@code Player} who initiated the attack.
	 * @param damage The amount of damage dealt to the {@code Npc}.
	 * @param isSummon True if the attack was performed by a summon, false otherwise.
	 */
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	{
		final int npcId = npc.getId();
		final int npcObjId = npc.getObjectId();
		if (npcId == DRAGON)
		{
			if (MY_TRACKING_SET.add(npcObjId)) // this allows to handle multiple instances of npc
			{
				// Spawn first 5 shades on first attack on Dark Water Dragon
				final Creature originalAttacker = isSummon ? attacker.getServitors().values().stream().findFirst().orElse(attacker.getPet()) : attacker;
				final int nx = npc.getX();
				final int ny = npc.getY();
				final int nz = npc.getZ();
				spawnShade(originalAttacker, SHADE1, nx + 100, ny + 100, nz);
				spawnShade(originalAttacker, SHADE2, nx + 100, ny - 100, nz);
				spawnShade(originalAttacker, SHADE1, nx - 100, ny + 100, nz);
				spawnShade(originalAttacker, SHADE2, nx - 100, ny - 100, nz);
				spawnShade(originalAttacker, SHADE1, nx - 150, ny + 150, nz);
			}
			else if ((npc.getCurrentHp() < (npc.getMaxHp() / 2.0)) && SECOND_SPAWN.add(npcObjId))
			{
				// Spawn second 5 shades on half hp of on Dark Water Dragon
				final Creature originalAttacker = isSummon ? attacker.getServitors().values().stream().findFirst().orElse(attacker.getPet()) : attacker;
				final int nx = npc.getX();
				final int ny = npc.getY();
				final int nz = npc.getZ();
				spawnShade(originalAttacker, SHADE2, nx + 100, ny + 100, nz);
				spawnShade(originalAttacker, SHADE1, nx + 100, ny - 100, nz);
				spawnShade(originalAttacker, SHADE2, nx - 100, ny + 100, nz);
				spawnShade(originalAttacker, SHADE1, nx - 100, ny - 100, nz);
				spawnShade(originalAttacker, SHADE2, nx - 150, ny + 150, nz);
			}
		}
	}
	
	/**
	 * Handles logic that occurs when an {@code Npc} is killed.<br>
	 * This method manages specific spawns and quest timers based on the NPC type.
	 * @param npc The {@code Npc} instance that was killed.
	 * @param killer The {@code Player} who dealt the killing blow.
	 * @param isSummon Indicates if the kill was performed by a summon.
	 */
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final int npcId = npc.getId();
		final int npcObjId = npc.getObjectId();
		if (npcId == DRAGON)
		{
			MY_TRACKING_SET.remove(npcObjId);
			SECOND_SPAWN.remove(npcObjId);
			final Attackable faf = addSpawn(FAFURION, npc.getX(), npc.getY(), npc.getZ(), 0, false, 0).asAttackable(); // spawns Fafurion Kindred when Dard Water Dragon is dead
			ID_MAP.put(faf.getObjectId(), killer);
		}
		else if (npcId == FAFURION)
		{
			cancelQuestTimer("fafurion_poison", npc, null);
			cancelQuestTimer("fafurion_despawn", npc, null);
			cancelQuestTimer("first_spawn", npc, null);
			cancelQuestTimer("second_spawn", npc, null);
			cancelQuestTimer("third_spawn", npc, null);
			cancelQuestTimer("fourth_spawn", npc, null);
			cancelQuestTimer("1", npc, null);
			cancelQuestTimer("2", npc, null);
			cancelQuestTimer("3", npc, null);
			cancelQuestTimer("4", npc, null);
			MY_TRACKING_SET.remove(npcObjId);
			ID_MAP.remove(npcObjId);
		}
	}
	
	/**
	 * This method is called when an {@code Npc} spawns.<br>
	 * It handles special logic for the {@code FAFURION} npc.<br>
	 * It triggers the spawning of multiple Detractors and manages quest timers.
	 * @param npc The {@code Npc} instance that has just spawned.
	 */
	@Override
	public void onSpawn(Npc npc)
	{
		final int npcId = npc.getId();
		final int npcObjId = npc.getObjectId();
		if ((npcId == FAFURION) && MY_TRACKING_SET.add(npcObjId))
		{
			// Spawn 4 Detractors on spawn of Fafurion
			final int x = npc.getX();
			final int y = npc.getY();
			final int z = npc.getZ();
			addSpawn(DETRACTOR2, x + 100, y + 100, z, 0, false, 40000);
			addSpawn(DETRACTOR1, x + 100, y - 100, z, 0, false, 40000);
			addSpawn(DETRACTOR2, x - 100, y + 100, z, 0, false, 40000);
			addSpawn(DETRACTOR1, x - 100, y - 100, z, 0, false, 40000);
			cancelQuestTimer("first_spawn", npc, null);
			cancelQuestTimer("second_spawn", npc, null);
			cancelQuestTimer("third_spawn", npc, null);
			cancelQuestTimer("fourth_spawn", npc, null);
			cancelQuestTimer("fafurion_poison", npc, null);
			cancelQuestTimer("fafurion_despawn", npc, null);
			startQuestTimer("first_spawn", 2000, npc, null); // timer to delay timer "1"
			startQuestTimer("second_spawn", 4000, npc, null); // timer to delay timer "2"
			startQuestTimer("third_spawn", 8000, npc, null); // timer to delay timer "3"
			startQuestTimer("fourth_spawn", 10000, npc, null); // timer to delay timer "4"
			startQuestTimer("fafurion_poison", 3000, npc, null, true); // Every three seconds reduces Fafurions hp like it is poisoned
			startQuestTimer("fafurion_despawn", 120000, npc, null); // Fafurion Kindred disappears after two minutes
		}
	}
	
	/**
	 * Spawns a Shade NPC at the specified coordinates.<br>
	 * The new NPC will target the provided {@code attacker}.<br>
	 * It is initialized with immediate aggression toward the source.
	 * @param attacker The {@code Creature} that triggered the spawn.
	 * @param npcId The unique ID of the Shade to spawn.
	 * @param x The X coordinate for spawning.
	 * @param y The Y coordinate for spawning.
	 * @param z The Z coordinate for spawning.
	 */
	public void spawnShade(Creature attacker, int npcId, int x, int y, int z)
	{
		final Npc shade = addSpawn(npcId, x, y, z, 0, false, 0);
		shade.setRunning();
		shade.asAttackable().addDamageHate(attacker, 0, 999);
		shade.getAI().setIntentionAttack(attacker);
	}
	
	/**
	 * This is the entry point for the {@code DarkWaterDragon} class.<br>
	 * It initializes a new instance of the script.
	 * @param args Command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		new DarkWaterDragon();
	}
}
