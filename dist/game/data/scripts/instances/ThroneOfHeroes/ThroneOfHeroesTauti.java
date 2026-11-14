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

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.instancezone.Instance;
import org.l2jmobius.gameserver.mechanics.script.InstanceScript;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;

/**
 * This class handles the script logic for the {@code Tauti} instance within the Throne of Heroes content.<br>
 * It manages specific events and interactions for players entering this instance zone.
 * @author CostyKiller
 */
public class ThroneOfHeroesTauti extends InstanceScript
{
	// NPCs
	private static final int ROIENTAL = 34571;
	
	// Monsters
	private static final int TAUTI = 26453;
	private static final int MINION_ZAHAK = 26454;
	private static final int PETRA_INJECTION_DEVICE = 19790;
	
	// Throne's Treasure Chest Tauti
	private static final int TREASURE_CHEST = 26457;
	
	// Template
	private static final int TEMPLATE_ID = 309;
	
	// HP Threshold Minions Spawn Settings
	private static final int BOSS_HP_THRESHOLD_1 = 80; // Spawn Petra Injection Device and minions at 80%
	private static final int BOSS_HP_THRESHOLD_2 = 60; // Spawn Petra Injection Device and minions at 60%
	private static final int BOSS_HP_THRESHOLD_3 = 40; // Spawn Petra Injection Device and minions at 40%
	
	// NPC dialogs
	private static final int TAUTI_MESSAGES_DELAY = 60; // Seconds
	private static final NpcStringId[] TAUTI_MESSAGES =
	{
		NpcStringId.TAUTI_INTENDS_TO_SHOW_OFF_HIS_POWER_TO_HIS_FOLLOWERS,
		NpcStringId.TAUTI_IS_GOING_TO_CRUSH_DOWN_THOSE_WHO_ARE_RUNNING_AWAY,
		NpcStringId.YOU_RAT_LIKE_CREATURES_TASTE_MY_ATTACK,
		NpcStringId.DO_YOU_REALLY_THINK_YOU_RE_SAFE_IF_YOU_GO_JUST_FAR_ENOUGH_THEN_GET_A_TASTE_OF_MY_POWER,
	};
	
	public ThroneOfHeroesTauti()
	{
		super(TEMPLATE_ID);
		addInstanceCreatedId(TEMPLATE_ID);
		addStartNpc(ROIENTAL);
		addTalkId(ROIENTAL);
		addAttackId(TAUTI);
		addKillId(TAUTI, PETRA_INJECTION_DEVICE);
		addSpawnId(TAUTI, PETRA_INJECTION_DEVICE);
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
					showOnScreenMsg(world, NpcStringId.THE_CHALLENGE_FOR_THE_THRONE_OF_HEROES_TAUTI_WILL_BEGIN_SHORTLY_PLEASE_GET_READY_HEROES, ExShowScreenMessage.TOP_CENTER, 5000, true);
					startQuestTimer("ANNOUNCE_RAID_PREP", 15000, null, player);
				}
				break;
			}
			case "ANNOUNCE_RAID_PREP":
			{
				final Instance world = player.getInstanceWorld();
				if (isInInstance(world))
				{
					showOnScreenMsg(world, NpcStringId.THE_BATTLE_WITH_TAUTI_STARTS_IN_10_SEC, ExShowScreenMessage.TOP_CENTER, 5000, true);
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
					startQuestTimer("SPAWN_TAUTI", 1000, null, player);
				}
				break;
			}
			case "SPAWN_TAUTI":
			{
				final Instance world = player.getInstanceWorld();
				if (isInInstance(world))
				{
					showOnScreenMsg(world, NpcStringId.TAUTI_IS_READY_TO_FIGHT, ExShowScreenMessage.TOP_CENTER, 5000, true);
					world.spawnGroup("TAUTI");
				}
				break;
			}
			case "TAUTI_MESSAGES":
			{
				final Instance world = npc.getInstanceWorld();
				if (isInInstance(world))
				{
					showOnScreenMsg(world, TAUTI_MESSAGES[getRandom(TAUTI_MESSAGES.length)], ExShowScreenMessage.TOP_CENTER, 5000, true);
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
				world.setStatus(1);
				world.spawnGroup("PETRA_INJECTION_DEVICE");
				world.spawnGroup("TAUTI_MINIONS_STAGE1");
				showOnScreenMsg(world, NpcStringId.THE_PETRA_S_INJECTION_DEVICE_IS_SUMMONED_TO_HELP_TAUTI, ExShowScreenMessage.TOP_CENTER, 5000, true);
			}
			else if ((hpPer <= BOSS_HP_THRESHOLD_2) && world.isStatus(1))
			{
				world.setStatus(2);
				world.spawnGroup("PETRA_INJECTION_DEVICE");
				world.spawnGroup("TAUTI_MINIONS_STAGE2");
				showOnScreenMsg(world, NpcStringId.THE_PETRA_S_INJECTION_DEVICE_IS_SUMMONED_TO_HELP_TAUTI, ExShowScreenMessage.TOP_CENTER, 5000, true);
			}
			else if ((hpPer <= BOSS_HP_THRESHOLD_3) && world.isStatus(2))
			{
				world.setStatus(3);
				world.spawnGroup("PETRA_INJECTION_DEVICE");
				world.spawnGroup("TAUTI_MINIONS_STAGE3");
				showOnScreenMsg(world, NpcStringId.THE_PETRA_S_INJECTION_DEVICE_IS_SUMMONED_TO_HELP_TAUTI, ExShowScreenMessage.TOP_CENTER, 5000, true);
			}
		}
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final Instance world = npc.getInstanceWorld();
		if (isInInstance(world))
		{
			if (npc.getId() == TAUTI)
			{
				world.getAliveNpcs(MINION_ZAHAK).forEach(beast -> beast.doDie(null));
				world.getAliveNpcs(PETRA_INJECTION_DEVICE).forEach(device -> device.deleteMe());
				
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
					killer.getClan().getVariables().set("TOH_TAUTI_DONE", System.currentTimeMillis());
					killer.getClan().getVariables().storeMe();
				}
			}
			else if (npc.getId() == PETRA_INJECTION_DEVICE)
			{
				showOnScreenMsg(world, NpcStringId.THE_PETRA_S_INJECTION_DEVICE_IS_DESTROYED, ExShowScreenMessage.TOP_CENTER, 5000, true);
			}
		}
	}
	
	@Override
	public void onSpawn(Npc npc)
	{
		if (npc.getId() == TAUTI)
		{
			startQuestTimer("TAUTI_MESSAGES", TAUTI_MESSAGES_DELAY * 1000, npc, null, true);
		}
		else if (npc.getId() == PETRA_INJECTION_DEVICE)
		{
			final Instance world = npc.getInstanceWorld();
			if (isInInstance(world))
			{
				ThreadPool.schedule(() ->
				{
					final Npc tauti = world.getNpc(TAUTI);
					if ((tauti != null) && !tauti.isDead())
					{
						// XXX buff tauti?
						showOnScreenMsg(world, NpcStringId.THE_PETRA_S_INJECTION_DEVICE_IMBUES_TAUTI_WITH_POWER_AND_THEN_DISAPPEARS_A_SHIELD_EFFECT_IS_APPLIED, ExShowScreenMessage.TOP_CENTER, 5000, true);
					}
				}, 1000);
			}
		}
	}
	
	public static void main(String[] args)
	{
		new ThroneOfHeroesTauti();
	}
}