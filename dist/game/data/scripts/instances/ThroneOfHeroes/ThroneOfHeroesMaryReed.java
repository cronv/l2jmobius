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
package instances.ThroneOfHeroes;

import java.util.concurrent.ScheduledFuture;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.instancezone.Instance;
import org.l2jmobius.gameserver.mechanics.script.InstanceScript;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;

/**
 * Handles the script logic for the Mary Reed encounter within the {@code ThroneOfHeroes} instance.<br>
 * This class manages specific NPC interactions and events related to this boss.
 * @author CostyKiller
 */
public class ThroneOfHeroesMaryReed extends InstanceScript
{
	// NPCs
	private static final int ROIENTAL = 34571;
	
	// Monsters
	private static final int MARY_REED = 26451;
	private static final int MARY_REED_MINION_ZAKEN = 26452;
	private static final int ZAKEN_BARREL = 19791;
	
	// Throne's Treasure Chest Mary Reed
	private static final int TREASURE_CHEST = 26456;
	
	// Template
	private static final int TEMPLATE_ID = 308;
	
	// HP Threshold Minions Spawn Settings
	private static final int BOSS_HP_THRESHOLD_1 = 60; // 60%
	private static final int BOSS_HP_THRESHOLD_2 = 30; // 30%
	private static final int ZAKEN_DESPAWN_TIME = 90; // Seconds
	private static final int ZAKEN_MIN_DIST = 150;
	private static final int ZAKEN_MAX_DIST = 200;
	private static final int ZAKEN_BARRELS_SPAWN_COUNT = 5;
	
	// NPC dialogs
	private static final int ZAKEN_MESSAGES_DELAY = 30; // Seconds
	private static final NpcStringId[] ZAKEN_MESSAGES =
	{
		NpcStringId.MARY_REED_THEY_ARE_JUST_INSECTS_HAHA_SHOW_THEM_YOUR_TRUE_POWER,
		NpcStringId.ZAKEN_IS_COMING_BACK_TO_HIS_PLACE,
		NpcStringId.ZAKEN_IS_GOING_TO_COME_BACK_TO_HIS_PLACE,
		NpcStringId.MARY_REED_REVEALS_HER_TRUE_POWER
	};
	
	// Keyed by object ID so multiple instances (if ever spawned) don't collide.
	private final java.util.Map<Integer, ScheduledFuture<?>> _countdownTasks = new java.util.concurrent.ConcurrentHashMap<>();
	private final java.util.Map<Integer, Integer> _timeRemaining = new java.util.concurrent.ConcurrentHashMap<>();
	
	public ThroneOfHeroesMaryReed()
	{
		super(TEMPLATE_ID);
		addInstanceCreatedId(TEMPLATE_ID);
		addStartNpc(ROIENTAL);
		addTalkId(ROIENTAL);
		addAttackId(MARY_REED, MARY_REED_MINION_ZAKEN);
		addKillId(MARY_REED);
		addSpawnId(MARY_REED_MINION_ZAKEN);
		addDespawnId(MARY_REED_MINION_ZAKEN);
	}
	
	@Override
	public void onInstanceCreated(Instance activeInstance, Player player)
	{
		activeInstance.setStatus(0);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "enterInstance":
			{
				enterInstance(player, npc, TEMPLATE_ID);
				startQuestTimer("ANNOUNCE_RAID_START", 10000, null, player);
				break;
			}
			case "reenterInstance":
			{
				final Instance activeInstance = getPlayerInstance(player);
				if (isInInstance(activeInstance))
				{
					enterInstance(player, npc, activeInstance.getTemplateId());
				}
				break;
			}
			case "ANNOUNCE_RAID_START":
			{
				final Instance world = player.getInstanceWorld();
				if (isInInstance(world))
				{
					showOnScreenMsg(world, NpcStringId.THE_CHALLENGE_FOR_THE_THRONE_OF_HEROES_MARY_REED_WILL_BEGIN_SHORTLY_PLEASE_GET_READY_HEROES, ExShowScreenMessage.TOP_CENTER, 5000, true);
					startQuestTimer("ANNOUNCE_RAID_PREP", 15000, null, player);
				}
				break;
			}
			case "ANNOUNCE_RAID_PREP":
			{
				final Instance world = player.getInstanceWorld();
				if (isInInstance(world))
				{
					showOnScreenMsg(world, NpcStringId.THE_BATTLE_WITH_MARY_REED_STARTS_IN_10_SEC, ExShowScreenMessage.TOP_CENTER, 5000, true);
					startQuestTimer("ANNOUNCE_5", 10000, null, player);
				}
				break;
			}
			case "ANNOUNCE_5":
			{
				final Instance world = player.getInstanceWorld();
				if (isInInstance(world))
				{
					showOnScreenMsg(world, NpcStringId.FIVE_SECONDS, ExShowScreenMessage.TOP_CENTER, 1000, true);
					startQuestTimer("ANNOUNCE_4", 1000, null, player);
				}
				break;
			}
			case "ANNOUNCE_4":
			{
				final Instance world = player.getInstanceWorld();
				if (isInInstance(world))
				{
					showOnScreenMsg(world, NpcStringId.FOUR_SECONDS, ExShowScreenMessage.TOP_CENTER, 1000, true);
					startQuestTimer("ANNOUNCE_3", 1000, null, player);
				}
				break;
			}
			case "ANNOUNCE_3":
			{
				final Instance world = player.getInstanceWorld();
				if (isInInstance(world))
				{
					showOnScreenMsg(world, NpcStringId.THREE_SECONDS_2, ExShowScreenMessage.TOP_CENTER, 1000, true);
					startQuestTimer("ANNOUNCE_2", 1000, null, player);
				}
				break;
			}
			case "ANNOUNCE_2":
			{
				final Instance world = player.getInstanceWorld();
				if (isInInstance(world))
				{
					showOnScreenMsg(world, NpcStringId.TWO_SECONDS_2, ExShowScreenMessage.TOP_CENTER, 1000, true);
					startQuestTimer("ANNOUNCE_1", 1000, null, player);
				}
				break;
			}
			case "ANNOUNCE_1":
			{
				final Instance world = player.getInstanceWorld();
				if (isInInstance(world))
				{
					showOnScreenMsg(world, NpcStringId.ONE_SECOND_2, ExShowScreenMessage.TOP_CENTER, 1000, true);
					if (world.getStatus() == 0)
					{
						startQuestTimer("SPAWN_MARY_REED", 1000, null, player);
					}
					else
					{
						startQuestTimer("MARY_REED_SPAWNS_ZAKEN", 1000, npc, null);
					}
				}
				break;
			}
			case "SPAWN_MARY_REED":
			{
				final Instance world = player.getInstanceWorld();
				if (isInInstance(world))
				{
					showOnScreenMsg(world, NpcStringId.MARY_REED_GREETS_HEROES, ExShowScreenMessage.TOP_CENTER, 5000, true);
					world.spawnGroup("MARY_REED");
				}
				break;
			}
			case "ANNOUNCE_MARY_REED_SPAWNS_ZAKEN":
			{
				final Instance world = player.getInstanceWorld();
				if (isInInstance(world))
				{
					showOnScreenMsg(world, NpcStringId.MARY_REED_SUMMONS_ZAKEN_ZAKEN_WILL_BE_HERE_VERY_SOON, ExShowScreenMessage.TOP_CENTER, 5000, true);
					startQuestTimer("ANNOUNCE_5", 5000, null, player);
				}
				break;
			}
			case "MARY_REED_SPAWNS_ZAKEN":
			{
				final Instance world = player.getInstanceWorld();
				if (isInInstance(world))
				{
					showOnScreenMsg(world, NpcStringId.ZAKEN_YOUR_TIME_HAS_COME_KILL_THEM_ALL, ExShowScreenMessage.TOP_CENTER, 5000, true);
					world.spawnGroup("MARY_REED_MINION_ZAKEN");
					startQuestTimer("ZAKEN_SPAWNS_BARRELS", 5000, npc, null);
				}
				break;
			}
			case "ZAKEN_SPAWNS_BARRELS":
			{
				final Instance world = player.getInstanceWorld();
				if (isInInstance(world))
				{
					// Zaken spawns barrels around him
					final Npc zaken = world.getNpc(MARY_REED_MINION_ZAKEN);
					spawnZakenBarrels(world, zaken, ZAKEN_BARRELS_SPAWN_COUNT);
				}
				break;
			}
			case "ZAKEN_MESSAGES":
			{
				final Instance world = npc.getInstanceWorld();
				if (isInInstance(world))
				{
					showOnScreenMsg(world, ZAKEN_MESSAGES[getRandom(ZAKEN_MESSAGES.length)], ExShowScreenMessage.TOP_CENTER, 5000, true);
				}
				break;
			}
		}
		
		return super.onEvent(event, npc, player);
	}
	
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	{
		final Instance world = npc.getInstanceWorld();
		if (isInInstance(world))
		{
			final int hpPer = npc.getCurrentHpPercent();
			if (npc.getId() == MARY_REED)
			{
				if ((hpPer <= BOSS_HP_THRESHOLD_1) && world.isStatus(0))
				{
					startQuestTimer("ANNOUNCE_MARY_REED_SPAWNS_ZAKEN", 10000, npc, null);
					world.setStatus(1);
				}
				else if ((hpPer <= BOSS_HP_THRESHOLD_2) && world.isStatus(1))
				{
					startQuestTimer("ANNOUNCE_MARY_REED_SPAWNS_ZAKEN", 10000, npc, null);
					world.setStatus(2);
				}
			}
		}
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final Instance world = npc.getInstanceWorld();
		if (isInInstance(world))
		{
			if (npc.getId() == MARY_REED)
			{
				// Despawn minions and stop timers
				cancelQuestTimer("ANNOUNCE_MARY_REED_SPAWNS_ZAKEN", npc, null);
				cancelQuestTimer("MARY_REED_SPAWNS_ZAKEN", npc, null);
				cancelQuestTimer("ZAKEN_SPAWNS_BARRELS", npc, null);
				
				world.getAliveNpcs(MARY_REED_MINION_ZAKEN).forEach(beast -> beast.doDie(null));
				world.getAliveNpcs(ZAKEN_BARREL).forEach(beast -> beast.doDie(null));
				
				// Spawn treasure chests
				boolean eightCCMembersOrMore = ((killer.getCommandChannel() != null) && (killer.getCommandChannel().getMemberCount() >= 8));
				if (killer.isGM() || eightCCMembersOrMore)
				{
					addSpawn(TREASURE_CHEST, killer.getX() + getRandom(-150, 150), killer.getY() + getRandom(-150, 150), killer.getZ() + 20, 0, false, 0, true, world.getId());
					addSpawn(TREASURE_CHEST, killer.getX() + getRandom(-150, 150), killer.getY() + getRandom(-150, 150), killer.getZ() + 20, 0, false, 0, true, world.getId());
				}
				else
				{
					addSpawn(TREASURE_CHEST, killer.getX() + getRandom(-150, 150), killer.getY() + getRandom(-150, 150), killer.getZ() + 20, 0, false, 0, true, world.getId());
				}
				
				// Finish instance
				world.finishInstance(2);
				if (!killer.isGM())
				{
					// Set clan variable
					killer.getClan().getVariables().set("TOH_MARYREED_DONE", System.currentTimeMillis());
					killer.getClan().getVariables().storeMe();
				}
			}
		}
	}
	
	@Override
	public void onSpawn(Npc npc)
	{
		if (npc.getId() != MARY_REED_MINION_ZAKEN)
		{
			return;
		}
		startCountdown(npc);
		startQuestTimer("ZAKEN_MESSAGES", ZAKEN_MESSAGES_DELAY * 1000, npc, null, true);
	}
	
	@Override
	public void onNpcDespawn(Npc npc)
	{
		if (npc.getId() == MARY_REED_MINION_ZAKEN)
		{
			stopCountdown(npc);
			cancelQuestTimer("ZAKEN_MESSAGES", npc, null);
		}
	}
	
	private void startCountdown(Npc npc)
	{
		final int objId = npc.getObjectId();
		_timeRemaining.put(objId, ZAKEN_DESPAWN_TIME);
		updateTitle(npc);
		
		final ScheduledFuture<?> task = ThreadPool.scheduleAtFixedRate(() ->
		{
			final int remaining = _timeRemaining.merge(objId, -1, Integer::sum);
			if ((remaining <= 0) || npc.isDead() || npc.isDecayed())
			{
				stopCountdown(npc);
				if (!npc.isDead())
				{
					final Instance world = npc.getInstanceWorld();
					final int worldStatus = world.getStatus();
					npc.deleteMe();
					if (isInInstance(world))
					{
						ThreadPool.schedule(() ->
						{
							final Npc maryReed = world.getNpc(MARY_REED);
							if ((maryReed != null) && !maryReed.isDead())
							{
								maryReed.setCurrentHp(maryReed.getMaxHp() * (worldStatus == 2 ? 0.6 : 0.8), true);
								if (worldStatus != 0)
								{
									world.setStatus(worldStatus - 1);
								}
							}
						}, 1000);
					}
				}
				return;
			}
			updateTitle(npc);
		}, 1000, 1000);
		
		_countdownTasks.put(objId, task);
	}
	
	private void stopCountdown(Npc npc)
	{
		final int objId = npc.getObjectId();
		final ScheduledFuture<?> task = _countdownTasks.remove(objId);
		if (task != null)
		{
			task.cancel(false);
		}
		_timeRemaining.remove(objId);
	}
	
	private void updateTitle(Npc npc)
	{
		final int remaining = _timeRemaining.getOrDefault(npc.getObjectId(), 0);
		npc.setTitle("" + remaining);
		npc.broadcastInfo();
	}
	
	private void spawnZakenBarrels(Instance world, Npc zaken, int count)
	{
		for (int i = 0; i < count; i++)
		{
			final double angle = Math.toRadians(getRandom(360));
			final int distance = getRandom(ZAKEN_MIN_DIST, ZAKEN_MAX_DIST);
			final int x = zaken.getX() + (int) (distance * Math.cos(angle));
			final int y = zaken.getY() + (int) (distance * Math.sin(angle));
			addSpawn(ZAKEN_BARREL, x, y, zaken.getZ() + 20, 0, false, 0, true, world.getId());
		}
	}
	
	public static void main(String[] args)
	{
		new ThroneOfHeroesMaryReed();
	}
}
