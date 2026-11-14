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

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.instancezone.Instance;
import org.l2jmobius.gameserver.mechanics.script.InstanceScript;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;

/**
 * @author CostyKiller
 */
public class ThroneOfHeroesToros extends InstanceScript
{
	// NPCs
	private static final int ROIENTAL = 34571;
	
	// Monsters
	private static final int TOROS = 29451;
	private static final int TOROS_CLONE1 = 29452;
	private static final int TOROS_CLONE2 = 29453;
	private static final int TOROS_CLONE3 = 29454;
	private static final int[] TOROS_CLONES =
	{
		TOROS_CLONE1,
		TOROS_CLONE2,
		TOROS_CLONE3
	};
	
	// Throne's Treasure Chest Toros
	private static final int TREASURE_CHEST = 29456;
	
	// Template
	private static final int TEMPLATE_ID = 365;
	
	// HP Threshold Minions Spawn Settings
	private static final int BOSS_HP_THRESHOLD_1 = 60;
	private static final int BOSS_HP_THRESHOLD_2 = 30;
	
	// NPC dialogs
	private static final int TOROS_MESSAGES_DELAY = 60; // Seconds
	private static final NpcStringId[] TOROS_MESSAGES =
	{
		NpcStringId.HA_HA_HA_THE_SMELL_OF_FRESHLY_SHED_BLOOD_IS_EVERYWHERE,
		NpcStringId.HA_HA_HA_HA_SCREAM_SCREAM_LOUDER,
		NpcStringId.HAHA_AT_LAST_THE_BLOOD_IS_SPILLED_ON_THE_BATTLEFIELD,
		NpcStringId.S1_THE_SMELL_OF_YOUR_BLOOD_IS_INCREDIBLY_DELICIOUS,
		NpcStringId.HA_HA_HA_LET_RIVERS_FLOW_WITH_BLOOD
	};
	
	public ThroneOfHeroesToros()
	{
		super(TEMPLATE_ID);
		addInstanceCreatedId(TEMPLATE_ID);
		addStartNpc(ROIENTAL);
		addTalkId(ROIENTAL);
		addAttackId(TOROS);
		addKillId(TOROS);
		addSpawnId(TOROS);
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
					showOnScreenMsg(world, NpcStringId.THE_CHALLENGE_FOR_THE_THRONE_OF_HEROES_TOROS_WILL_BEGIN_SHORTLY_PLEASE_GET_READY_HEROES, ExShowScreenMessage.TOP_CENTER, 5000, true);
					startQuestTimer("ANNOUNCE_RAID_PREP", 15000, null, player);
				}
				break;
			}
			case "ANNOUNCE_RAID_PREP":
			{
				final Instance world = player.getInstanceWorld();
				if (isInInstance(world))
				{
					showOnScreenMsg(world, NpcStringId.BATTLE_WITH_TOROS_STARTS_IN_10_SEC, ExShowScreenMessage.TOP_CENTER, 5000, true);
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
					startQuestTimer("SPAWN_TOROS", 1000, null, player);
				}
				break;
			}
			case "SPAWN_TOROS":
			{
				final Instance world = player.getInstanceWorld();
				if (isInInstance(world))
				{
					showOnScreenMsg(world, NpcStringId.HA_HA_HA_THE_SMELL_OF_FRESHLY_SHED_BLOOD_IS_EVERYWHERE, ExShowScreenMessage.TOP_CENTER, 5000, true);
					world.spawnGroup("TOROS");
				}
				break;
			}
			case "ANNOUNCE_TOROS_SPAWNS_CLONES":
			{
				final Instance world = npc.getInstanceWorld();
				if (isInInstance(world))
				{
					showOnScreenMsg(world, NpcStringId.RUN_AWAY_IF_YOU_CAN_OTHERWISE_YOUR_FLESH_WILL_BE_TORN_INTO_PIECES, ExShowScreenMessage.TOP_CENTER, 5000, true);
					startQuestTimer("TOROS_SPAWNS_CLONES", 10000, npc, null);
				}
				break;
			}
			case "TOROS_SPAWNS_CLONES":
			{
				final Instance world = npc.getInstanceWorld();
				if (isInInstance(world))
				{
					showOnScreenMsg(world, NpcStringId.MY_CHILDREN_MY_BLOOD_RAISE_AND_TEAR_THEM_TO_PIECES, ExShowScreenMessage.TOP_CENTER, 5000, true);
					world.spawnGroup("TOROS_CLONES");
				}
				break;
			}
			case "TOROS_MESSAGES":
			{
				final Instance world = npc.getInstanceWorld();
				if (isInInstance(world))
				{
					final int randomMessage = getRandom(TOROS_MESSAGES.length);
					final List<Player> players = new ArrayList<>(world.getPlayers());
					final Player randomPlayer = players.isEmpty() ? null : players.get(getRandom(players.size()));
					
					if ((randomMessage == 3) && (randomPlayer != null))
					{
						showOnScreenMsg(world, TOROS_MESSAGES[randomMessage], ExShowScreenMessage.TOP_CENTER, 5000, true, randomPlayer.getName());
					}
					else
					{
						showOnScreenMsg(world, TOROS_MESSAGES[randomMessage], ExShowScreenMessage.TOP_CENTER, 5000, true);
					}
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
			if ((hpPer <= BOSS_HP_THRESHOLD_1) && world.isStatus(0))
			{
				startQuestTimer("ANNOUNCE_TOROS_SPAWNS_CLONES", 10000, npc, null);
				world.setStatus(1);
			}
			else if ((hpPer <= BOSS_HP_THRESHOLD_2) && world.isStatus(1))
			{
				startQuestTimer("ANNOUNCE_TOROS_SPAWNS_CLONES", 10000, npc, null);
				world.setStatus(2);
			}
		}
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final Instance world = npc.getInstanceWorld();
		if (isInInstance(world))
		{
			// Final message
			showOnScreenMsg(world, NpcStringId.MY_BLOOD_IS_NOT_YET_DRIED, ExShowScreenMessage.TOP_CENTER, 5000, true);
			
			// Despawn minions and stop timer
			cancelQuestTimer("ANNOUNCE_TOROS_SPAWNS_CLONES", npc, null);
			cancelQuestTimer("TOROS_SPAWNS_CLONES", npc, null);
			world.getAliveNpcs(TOROS_CLONES).forEach(beast -> beast.doDie(null));
			
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
				killer.getClan().getVariables().set("TOH_TOROS_DONE", System.currentTimeMillis());
				killer.getClan().getVariables().storeMe();
			}
		}
	}
	
	@Override
	public void onSpawn(Npc npc)
	{
		final Instance world = npc.getInstanceWorld();
		if (isInInstance(world))
		{
			startQuestTimer("TOROS_MESSAGES", TOROS_MESSAGES_DELAY * 1000, npc, null, true);
		}
	}
	
	public static void main(String[] args)
	{
		new ThroneOfHeroesToros();
	}
}