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
package ai.bosses.Core;

import java.awt.Polygon;
import java.util.logging.Logger;

import org.l2jmobius.commons.time.TimeUtil;
import org.l2jmobius.gameserver.config.GrandBossConfig;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.instance.GrandBoss;
import org.l2jmobius.gameserver.entity.actor.instance.Monster;
import org.l2jmobius.gameserver.managers.GlobalVariablesManager;
import org.l2jmobius.gameserver.managers.GrandBossManager;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.NpcSay;
import org.l2jmobius.gameserver.network.serverpackets.PlaySound;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class serves as the primary AI handler for the {@code Core} grand boss.<br>
 * It manages the boss lifecycle, including state restoration from the database; the minions are the Privates of the NPC template.<br>
 * It also handles specific mechanics such as voice lines and teleport cubes upon death.
 * @author BazookaRpm
 */
public class Core extends Script
{
	// Logging.
	private static final Logger LOGGER = Logger.getLogger(Core.class.getName());
	
	// NPC identifiers.
	private static final int CORE_NPC_ID = 29006;
	private static final int TELEPORT_CUBE_NPC_ID = 899099;
	
	// Boss status flags.
	private static final byte STATUS_ALIVE = 0;
	private static final byte STATUS_DEAD = 1;
	
	// Event identifiers.
	private static final String EVENT_CORE_UNLOCK = "core_unlock";
	private static final String EVENT_TELEPORT_PREFIX = "TELEPORT";
	private static final String EVENT_MINION_CHECK = "MINION_CHECK";
	
	// Timing constants.
	private static final long MILLIS_PER_HOUR = 3600000L;
	private static final int TELEPORT_CUBE_DESPAWN_MS = 900000;
	private static final int MINION_CHECK_INTERVAL_MS = 10000;
	
	// Teleport destinations.
	private static final Location LOCATION_CRUMA_ENTRANCE = new Location(17253, 114232, -3440); // Cruma Tower entrance.
	private static final Location LOCATION_CRUMA_CORE_FLOOR3 = new Location(17719, 115590, -6584); // Cruma Core (3rd floor).
	
	// Core spawn position.
	private static final Location CORE_DEFAULT_SPAWN = new Location(17726, 108915, -6480);
	
	// Retail core_cube territory that holds Core and its minions.
	private static final Polygon CORE_ROOM = new Polygon(new int[]
	{
		17066,
		18357,
		19223,
		19232,
		16203,
		16203
	}, new int[]
	{
		108039,
		108039,
		108832,
		110389,
		110401,
		108861
	}, 6);
	private static final int CORE_ROOM_MIN_Z = -6688;
	private static final int CORE_ROOM_MAX_Z = -6188;
	
	// Attack state.
	private static boolean _firstAttacked;
	
	/**
	 * Private constructor for the {@code Core} class.<br>
	 * Initializes boss IDs and handles initial spawning logic.<br>
	 * It checks if the boss is dead to manage respawn timers or restores its state from data.
	 */
	private Core()
	{
		addKillId(CORE_NPC_ID);
		addAttackId(CORE_NPC_ID);
		addSpawnId(CORE_NPC_ID);
		addStartNpc(TELEPORT_CUBE_NPC_ID);
		addFirstTalkId(TELEPORT_CUBE_NPC_ID);
		addTalkId(TELEPORT_CUBE_NPC_ID);
		
		_firstAttacked = false;
		
		final GrandBossManager bossManager = GrandBossManager.getInstance();
		final StatSet info = bossManager.getStatSet(CORE_NPC_ID);
		final int status = bossManager.getStatus(CORE_NPC_ID);
		if (status == STATUS_DEAD)
		{
			final long remaining = info.getLong("respawn_time") - System.currentTimeMillis();
			if (remaining > 0)
			{
				startQuestTimer(EVENT_CORE_UNLOCK, remaining, null, null);
			}
			else
			{
				final GrandBoss core = (GrandBoss) addSpawn(CORE_NPC_ID, CORE_DEFAULT_SPAWN, false, 0);
				bossManager.setStatus(CORE_NPC_ID, STATUS_ALIVE);
				spawnBoss(core);
			}
		}
		else
		{
			final boolean attacked = GlobalVariablesManager.getInstance().getBoolean("Core_Attacked", false);
			if (attacked)
			{
				_firstAttacked = true;
			}
			
			final int locX = info.getInt("loc_x");
			final int locY = info.getInt("loc_y");
			final int locZ = info.getInt("loc_z");
			final int heading = info.getInt("heading");
			final double hp = info.getDouble("currentHP");
			final double mp = info.getDouble("currentMP");
			
			final GrandBoss core = (GrandBoss) addSpawn(CORE_NPC_ID, locX, locY, locZ, heading, false, 0);
			core.setCurrentHpMp(hp, mp);
			spawnBoss(core);
		}
	}
	
	/**
	 * Saves the current state of the boss to the global variables.<br>
	 * This method updates the {@code Core_Attacked} key in {@code GlobalVariablesManager}.
	 */
	@Override
	public void onSave()
	{
		GlobalVariablesManager.getInstance().set("Core_Attacked", _firstAttacked);
	}
	
	/**
	 * Registers a newly spawned boss with the manager and plays its spawn sound.<br>
	 * Its minions come with it from the Privates of the NPC template.
	 * @param npc The {@code GrandBoss} instance to spawn.
	 */
	public void spawnBoss(GrandBoss npc)
	{
		if (npc == null)
		{
			return;
		}
		
		GrandBossManager.getInstance().addBoss(npc);
		npc.broadcastPacket(new PlaySound(1, "BS01_A", 1, npc.getObjectId(), npc.getX(), npc.getY(), npc.getZ()));
		startQuestTimer(EVENT_MINION_CHECK, MINION_CHECK_INTERVAL_MS, npc, null, true);
	}
	
	/**
	 * Handles specific game events for the boss logic.<br>
	 * This method processes boss unlocking and teleportation requests.
	 * @param event The name or identifier of the event to process.
	 * @param npc The {@code Npc} entity associated with the event.
	 * @param player The {@code Player} entity involved in the event.
	 * @return The result of the superclass {@code onEvent} method.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event == null)
		{
			return super.onEvent(null, npc, player);
		}
		
		if (EVENT_CORE_UNLOCK.equals(event))
		{
			final GrandBoss core = (GrandBoss) addSpawn(CORE_NPC_ID, CORE_DEFAULT_SPAWN, false, 0);
			GrandBossManager.getInstance().setStatus(CORE_NPC_ID, STATUS_ALIVE);
			spawnBoss(core);
		}
		else if (event.startsWith(EVENT_TELEPORT_PREFIX))
		{
			if (player != null)
			{
				final String[] split = event.split(" ");
				if (split.length >= 2)
				{
					final String destination = split[1];
					switch (destination)
					{
						case "floor3":
						{
							player.teleToLocation(LOCATION_CRUMA_CORE_FLOOR3);
							break;
						}
						case "ground":
						{
							player.teleToLocation(LOCATION_CRUMA_ENTRANCE);
							break;
						}
						default:
						{
							LOGGER.warning(Core.class.getSimpleName() + ": Unknown teleport destination '" + destination + "' in event '" + event + "'.");
							break;
						}
					}
				}
				else
				{
					LOGGER.warning(Core.class.getSimpleName() + ": TELEPORT event missing destination token: '" + event + "'.");
				}
			}
		}
		else if (EVENT_MINION_CHECK.equals(event) && (npc != null))
		{
			returnMinions(npc);
		}
		
		return super.onEvent(event, npc, player);
	}
	
	/**
	 * Brings back Core's minions that left its room and makes them forget their targets.
	 * @param core The Core instance.
	 */
	private void returnMinions(Npc core)
	{
		if (core.isDead())
		{
			cancelQuestTimer(EVENT_MINION_CHECK, core, null);
			return;
		}
		
		for (Monster minion : core.asMonster().getMinionList().getSpawnedMinions())
		{
			final int z = minion.getZ();
			if (minion.isDead() || (CORE_ROOM.contains(minion.getX(), minion.getY()) && (z >= CORE_ROOM_MIN_Z) && (z <= CORE_ROOM_MAX_Z)))
			{
				continue;
			}
			
			minion.clearAggroList();
			minion.teleToLocation(core.getX() + getRandom(-100, 100), core.getY() + getRandom(-100, 100), core.getZ());
		}
	}
	
	/**
	 * Handles the logic when an {@code Npc} is attacked by a {@code Player}.<br>
	 * It triggers specific dialogue based on whether it is the first time the boss is hit.
	 * @param npc The {@code Npc} being attacked.
	 * @param attacker The {@code Player} performing the attack.
	 * @param damage The amount of damage dealt to the {@code Npc}.
	 * @param isSummon Whether the attack was performed by a summon.
	 */
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	{
		if (npc == null)
		{
			return;
		}
		
		if (_firstAttacked)
		{
			if (getRandom(100) == 0)
			{
				npc.broadcastPacket(new NpcSay(npc.getObjectId(), ChatType.NPC_GENERAL, npc.getId(), NpcStringId.REMOVING_INTRUDERS));
			}
		}
		else
		{
			_firstAttacked = true;
			final int objectId = npc.getObjectId();
			final int npcId = npc.getId();
			npc.broadcastPacket(new NpcSay(objectId, ChatType.NPC_GENERAL, npcId, NpcStringId.A_NON_PERMITTED_TARGET_HAS_BEEN_DISCOVERED));
			npc.broadcastPacket(new NpcSay(objectId, ChatType.NPC_GENERAL, npcId, NpcStringId.INTRUDER_REMOVAL_SYSTEM_INITIATED));
		}
	}
	
	/**
	 * Handles the logic when an {@code Npc} is killed.<br>
	 * It triggers specific actions for the core boss; its template minions are removed with it by the minion list.
	 * @param npc The {@code Npc} that was killed.
	 * @param killer The {@code Player} who dealt the killing blow.
	 * @param isSummon Indicates if the kill was performed by a summon.
	 */
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		if (npc == null)
		{
			return;
		}
		
		final int npcId = npc.getId();
		if (npcId == CORE_NPC_ID)
		{
			final int objectId = npc.getObjectId();
			npc.broadcastPacket(new PlaySound(1, "BS02_D", 1, objectId, npc.getX(), npc.getY(), npc.getZ()));
			npc.broadcastPacket(new NpcSay(objectId, ChatType.NPC_GENERAL, npcId, NpcStringId.A_FATAL_ERROR_HAS_OCCURRED));
			npc.broadcastPacket(new NpcSay(objectId, ChatType.NPC_GENERAL, npcId, NpcStringId.SYSTEM_IS_BEING_SHUT_DOWN));
			npc.broadcastPacket(new NpcSay(objectId, ChatType.NPC_GENERAL, npcId, NpcStringId.EMPTY));
			
			_firstAttacked = false;
			
			addSpawn(TELEPORT_CUBE_NPC_ID, 16502, 110165, -6394, 0, false, TELEPORT_CUBE_DESPAWN_MS);
			addSpawn(TELEPORT_CUBE_NPC_ID, 18948, 110166, -6397, 0, false, TELEPORT_CUBE_DESPAWN_MS);
			
			final GrandBossManager bossManager = GrandBossManager.getInstance();
			bossManager.setStatus(CORE_NPC_ID, STATUS_DEAD);
			
			final long baseIntervalMillis = GrandBossConfig.CORE_SPAWN_INTERVAL * MILLIS_PER_HOUR;
			final long randomRangeMillis = GrandBossConfig.CORE_SPAWN_RANDOM * MILLIS_PER_HOUR;
			final long respawnTime = baseIntervalMillis + getRandom(-randomRangeMillis, randomRangeMillis);
			
			final long nextRespawnTime = System.currentTimeMillis() + respawnTime;
			LOGGER.info("Core will respawn at: " + TimeUtil.getDateTimeString(nextRespawnTime));
			
			startQuestTimer(EVENT_CORE_UNLOCK, respawnTime, null, null);
			
			final StatSet info = bossManager.getStatSet(CORE_NPC_ID);
			info.set("respawn_time", nextRespawnTime);
			bossManager.setStatSet(CORE_NPC_ID, info);
		}
	}
	
	/**
	 * This method is called when an {@code Npc} spawns.<br>
	 * It sets the {@code immobilized} state of the {@code npc} to {@code true}.
	 * @param npc The {@code Npc} instance that was spawned.
	 */
	@Override
	public void onSpawn(Npc npc)
	{
		npc.setImmobilized(true);
	}
	
	/**
	 * Determines the dialogue file to display when a {@code Player} talks to an {@code Npc} for the first time.<br>
	 * This method constructs a filename based on the unique identifier of the {@code Npc}.
	 * @param npc The {@code Npc} entity being interacted with.
	 * @param player The {@code Player} initiating the conversation.
	 * @return A {@code String} representing the path to the HTML dialogue file.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return npc.getId() + ".html";
	}
	
	/**
	 * This is the entry point for the {@code Core} class.<br>
	 * It initializes a new instance of {@code Core}.
	 * @param args The command line arguments.
	 */
	public static void main(String[] args)
	{
		new Core();
	}
}
