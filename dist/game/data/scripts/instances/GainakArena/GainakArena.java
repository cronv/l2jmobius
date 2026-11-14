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
package instances.GainakArena;

import java.util.List;

import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.groups.Party;
import org.l2jmobius.gameserver.entity.instancezone.Instance;
import org.l2jmobius.gameserver.entity.item.holders.ItemHolder;
import org.l2jmobius.gameserver.mechanics.script.InstanceScript;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the logic and events for the {@code GainakArena} instance.<br>
 * Party leader selects a difficulty (110 / 120 / 130) at Benusta, then faces three raid bosses in sequence, one at a time. Each boss taunts on spawn, is untargetable and passive during a short countdown, then becomes active. On death it speaks a kill line, and after a short delay either the next boss spawns or (on the third kill) the instance's final message plays.
 * @author CostyKiller
 */
public class GainakArena extends InstanceScript
{
	// NPCs
	private static final int BENUSTA = 34542;
	private static final int INNER_BENUSTA = 31463;
	
	// Lvl 110 bosses (1st, 2nd, 3rd)
	private static final int KADZAN = 26635;
	private static final int GRABOL = 26636;
	private static final int VEXUS = 26637;
	// Lvl 120 bosses (1st, 2nd, 3rd)
	private static final int LIDAKIN = 26638;
	private static final int TALGOR = 26639;
	private static final int SINDRIC = 26640;
	// Lvl 130 bosses (1st, 2nd, 3rd)
	private static final int BELFUS = 26641;
	private static final int GALATAN = 26642;
	private static final int GODIAK = 26643;
	
	private static final int[] BOSSES_110 =
	{
		KADZAN,
		GRABOL,
		VEXUS
	};
	private static final int[] BOSSES_120 =
	{
		LIDAKIN,
		TALGOR,
		SINDRIC
	};
	private static final int[] BOSSES_130 =
	{
		BELFUS,
		GALATAN,
		GODIAK
	};
	
	private static final String[] GROUPS_110 =
	{
		"FIRST_RAID_110",
		"SECOND_RAID_110",
		"THIRD_RAID_110"
	};
	private static final String[] GROUPS_120 =
	{
		"FIRST_RAID_120",
		"SECOND_RAID_120",
		"THIRD_RAID_120"
	};
	private static final String[] GROUPS_130 =
	{
		"FIRST_RAID_130",
		"SECOND_RAID_130",
		"THIRD_RAID_130"
	};
	
	// Taunt line said by each boss (index 0 = 1st boss, 1 = 2nd boss, 2 = 3rd boss), same across all three difficulties.
	// NOTE: verify these NpcStringId constants exist in your client string tables / NpcStringId.java; rename if your data uses different keys.
	private static final NpcStringId[] TAUNT_MSG =
	{
		NpcStringId.ONLY_LUCK_CAN_SAVE_YOU_FROM_DEATH_SO_SHALL_WE_BEGIN,
		NpcStringId.SOONER_OR_LATER_YOU_LL_HAVE_TO_MAKE_YOUR_CHOICE,
		NpcStringId.DEATH_IS_AN_INTEGRAL_PART_OF_THE_STRUGGLE_BUT_NOT_ITS_GOAL
	};
	
	// Line said on death of each boss.
	private static final NpcStringId[] KILL_MSG =
	{
		NpcStringId.YOU_WERE_JUST_LUCKY,
		NpcStringId.NEXT_TIME_ALL_MAY_END_DIFFERENTLY,
		NpcStringId.CAN_IT_BE_THE_END
	};
	
	// Countdown lines shown/said 10-13s after spawn, then battle start at 14s.
	private static final NpcStringId COUNTDOWN_3 = NpcStringId.THE_FINAL_MATCH_IS_ABOUT_TO_BEGIN_3;
	private static final NpcStringId COUNTDOWN_2 = NpcStringId.THE_FINAL_MATCH_IS_ABOUT_TO_BEGIN_2;
	private static final NpcStringId COUNTDOWN_1 = NpcStringId.THE_FINAL_MATCH_IS_ABOUT_TO_BEGIN_1;
	private static final NpcStringId BATTLE_BEGINS = NpcStringId.THE_BATTLE_BEGINS;
	
	// Post-kill messages.
	private static final NpcStringId NEXT_BATTLE_SOON = NpcStringId.THE_ENEMY_IS_DEFEATED_A_NEW_BATTLE_WILL_START_SOON_GET_READY;
	private static final NpcStringId ALL_ENEMIES_DEFEATED = NpcStringId.THE_BATTLE_IS_OVER_ALL_ENEMIES_ARE_DEFEATED;
	
	// Reward items
	private static final ItemHolder REWARD_BOX_110 = new ItemHolder(82453, 1); // Benusta's Reward Pack Lv. 1
	private static final ItemHolder REWARD_BOX_120 = new ItemHolder(82455, 1); // Benusta's Reward Pack Lv. 3
	private static final ItemHolder REWARD_BOX_130 = new ItemHolder(83729, 1); // Benusta's Reward Pack Lv. 5
	private static final ItemHolder WANDERER_TALES = new ItemHolder(83575, 2); // Novice Wanderer's Tales
	
	// Misc
	private static final int TEMPLATE_ID = 363;
	private static final int RAID_TAUNT_DELAY = 2000;
	private static final int RAID_COUNTDOWN_START = 10000;
	private static final int RAID_COUNTDOWN_STEP = 1000;
	private static final int RAID_BATTLE_START_DELAY = 13000;
	private static final int POST_KILL_DELAY = 5000;
	
	/**
	 * Initializes the {@code GainakArena} instance script.<br>
	 * Sets up the required template IDs for this zone.<br>
	 * Registers the leave ID for the instance and kill listeners for every boss.
	 */
	public GainakArena()
	{
		super(TEMPLATE_ID);
		addStartNpc(BENUSTA);
		addFirstTalkId(INNER_BENUSTA);
		addInstanceLeaveId(TEMPLATE_ID);
		addKillId(KADZAN, GRABOL, VEXUS, LIDAKIN, TALGOR, SINDRIC, BELFUS, GALATAN, GODIAK);
	}
	
	private static int[] getBosses(String instanceLv)
	{
		switch (instanceLv)
		{
			case "120":
			{
				return BOSSES_120;
			}
			case "130":
			{
				return BOSSES_130;
			}
			default:
			{
				return BOSSES_110;
			}
		}
	}
	
	private static String[] getGroups(String instanceLv)
	{
		switch (instanceLv)
		{
			case "120":
			{
				return GROUPS_120;
			}
			case "130":
			{
				return GROUPS_130;
			}
			default:
			{
				return GROUPS_110;
			}
		}
	}
	
	private static int getRaidId(String instanceLv)
	{
		switch (instanceLv)
		{
			case "120":
			{
				return SINDRIC;
			}
			case "130":
			{
				return GODIAK;
			}
			default:
			{
				return VEXUS;
			}
		}
	}
	
	/**
	 * Spawns the raid boss for the given index (0, 1 or 2), makes it passive/untargetable and queues the taunt, countdown and battle-start timers.
	 * @param world
	 * @param player
	 * @param index
	 */
	private void startRaidPhase(Instance world, Player player, int index)
	{
		// Guard: if this phase was already started (e.g. a duplicate onKill/timer fired twice),
		// do nothing instead of spawning the same boss group a second time.
		final StatSet parameters = world.getParameters();
		final String startedFlag = "RAID_STARTED_" + index;
		if (parameters.getInt(startedFlag, 0) == 1)
		{
			return;
		}
		
		parameters.set(startedFlag, 1);
		
		final String instanceLv = parameters.getString("INSTANCE_LEVEL", "110");
		final int[] bosses = getBosses(instanceLv);
		final String[] groups = getGroups(instanceLv);
		
		parameters.set("RAID_INDEX", index);
		world.spawnGroup(groups[index]);
		
		final Npc boss = world.getNpc(bosses[index]);
		if (boss != null)
		{
			boss.setInvul(true);
			boss.setTargetable(false);
			boss.disableCoreAI(true);
			boss.setTalkable(false);
			boss.setRandomWalking(false);
			
			boss.broadcastInfo();
		}
		
		startQuestTimer("raid_taunt", RAID_TAUNT_DELAY, null, player);
		startQuestTimer("raid_countdown_3", RAID_COUNTDOWN_START, null, player);
		startQuestTimer("raid_countdown_2", RAID_COUNTDOWN_START + RAID_COUNTDOWN_STEP, null, player);
		startQuestTimer("raid_countdown_1", RAID_COUNTDOWN_START + (RAID_COUNTDOWN_STEP * 2), null, player);
		startQuestTimer("raid_battle_start", RAID_BATTLE_START_DELAY, null, player);
	}
	
	/**
	 * Handles specific events triggered within the Gainak Arena instance.<br>
	 * This method checks requirements for entering the instance based on party status and location.<br>
	 * It validates if the player is a leader, meets distance requirements, and is not already in another zone, then drives the difficulty selection and the three-boss raid sequence.
	 * @param event The name of the event being triggered.
	 * @param npc The {@code Npc} associated with the event.
	 * @param player The {@code Player} who triggered the event.
	 * @return A {@code String} result or {@code null} if no action is taken.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "31463-1.html":
			case "31463-2.html":
			{
				return event;
			}
			case "enterInstance":
			{
				if (player.isInParty())
				{
					final Party party = player.getParty();
					if (!party.isLeader(player))
					{
						player.sendPacket(SystemMessageId.ONLY_A_PARTY_LEADER_CAN_MAKE_THE_REQUEST_TO_ENTER);
						return null;
					}
					
					if (player.isInCommandChannel())
					{
						player.sendPacket(SystemMessageId.YOU_CANNOT_ENTER_AS_YOU_DON_T_MEET_THE_REQUIREMENTS);
						return null;
					}
					
					final List<Player> members = party.getMembers();
					for (Player member : members)
					{
						if (!member.isInsideRadius3D(npc, 1000))
						{
							player.sendMessage("Player " + member.getName() + " must come closer.");
							return null;
						}
					}
					
					for (Player member : members)
					{
						enterInstance(member, npc, TEMPLATE_ID);
					}
				}
				else if (player.isGM())
				{
					enterInstance(player, npc, TEMPLATE_ID);
				}
				else
				{
					player.sendPacket(SystemMessageId.YOU_ARE_NOT_IN_A_PARTY_SO_YOU_CANNOT_ENTER);
				}
				
				final Instance enteredWorld = player.getInstanceWorld();
				if (isInInstance(enteredWorld) && (enteredWorld.getAliveNpcCount(INNER_BENUSTA) == 0))
				{
					enteredWorld.spawnGroup("BENUSTA");
					startQuestTimer("benusta_texts", 5000, null, player);
					startQuestTimer("check_status", 5000, null, player);
				}
				break;
			}
			case "benusta_texts":
			{
				final Instance world = player.getInstanceWorld();
				if (!isInInstance(world))
				{
					return null;
				}
				
				if (world.getAliveNpcCount(INNER_BENUSTA) == 0)
				{
					return null;
				}
				
				final StatSet parameters = world.getParameters();
				final Npc benusta = world.getNpc(INNER_BENUSTA);
				final int benustaText = parameters.getInt("BENUSTA_TEXT", 0);
				switch (benustaText)
				{
					case 0:
					{
						showOnScreenMsg(world, NpcStringId.THE_PARTY_LEADER_SHOULD_TALK_TO_ME_AND_SELECT_THE_DIFFICULTY_LEVEL, ExShowScreenMessage.TOP_CENTER, 5000, true);
						benusta.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.THE_PARTY_LEADER_SHOULD_TALK_TO_ME_AND_SELECT_THE_DIFFICULTY_LEVEL);
						startQuestTimer("benusta_texts", 4000, null, player);
						parameters.set("BENUSTA_TEXT", 1);
						break;
					}
					case 1:
					{
						showOnScreenMsg(world, NpcStringId.TAKE_INTO_ACCOUNT_THE_ABILITIES_OF_YOUR_PARTY_MEMBERS_WHEN_SELECTING_THE_DIFFICULTY, ExShowScreenMessage.TOP_CENTER, 5000, true);
						benusta.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.TAKE_INTO_ACCOUNT_THE_ABILITIES_OF_YOUR_PARTY_MEMBERS_WHEN_SELECTING_THE_DIFFICULTY);
						startQuestTimer("benusta_texts", 4000, null, player);
						parameters.set("BENUSTA_TEXT", 2);
						break;
					}
					default:
					{
						showOnScreenMsg(world, NpcStringId.THE_SELECTED_DIFFICULTY_CANNOT_BE_CHANGED_SO_CHOOSE_WISELY, ExShowScreenMessage.TOP_CENTER, 5000, true);
						benusta.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.THE_SELECTED_DIFFICULTY_CANNOT_BE_CHANGED_SO_CHOOSE_WISELY);
						parameters.set("BENUSTA_TEXT", 0);
						break;
					}
				}
				break;
			}
			case "easyInstance":
			{
				final Instance world = player.getInstanceWorld();
				if (!isInInstance(world))
				{
					return null;
				}
				
				world.getParameters().set("INSTANCE_LEVEL", "110");
				world.broadcastPacket(new ExShowScreenMessage(NpcStringId.LV_110_IS_SELECTED_PREPARE_FOR_BATTLE_2, ExShowScreenMessage.TOP_CENTER, 8000, true));
				world.getNpc(INNER_BENUSTA).deleteMe();
				world.setStatus(1);
				break;
			}
			case "mediumInstance":
			{
				final Instance world = player.getInstanceWorld();
				if (!isInInstance(world))
				{
					return null;
				}
				
				world.getParameters().set("INSTANCE_LEVEL", "120");
				world.broadcastPacket(new ExShowScreenMessage(NpcStringId.LV_120_IS_SELECTED_PREPARE_FOR_BATTLE_2, ExShowScreenMessage.TOP_CENTER, 8000, true));
				world.getNpc(INNER_BENUSTA).deleteMe();
				world.setStatus(1);
				break;
			}
			case "hardInstance":
			{
				final Instance world = player.getInstanceWorld();
				if (!isInInstance(world))
				{
					return null;
				}
				
				world.getParameters().set("INSTANCE_LEVEL", "130");
				world.broadcastPacket(new ExShowScreenMessage(NpcStringId.LV_130_IS_SELECTED_PREPARE_FOR_BATTLE_2, ExShowScreenMessage.TOP_CENTER, 8000, true));
				world.getNpc(INNER_BENUSTA).deleteMe();
				world.setStatus(1);
				break;
			}
			case "check_status":
			{
				final Instance world = player.getInstanceWorld();
				if (!isInInstance(world))
				{
					return null;
				}
				
				// Status 2 onward is driven by the raid timers and onKill, so nothing polls here.
				switch (world.getStatus())
				{
					case 0:
					{
						// Benusta and her lines are started once on entry, this only waits for the choice.
						startQuestTimer("check_status", 5000, null, player);
						break;
					}
					case 1:
					{
						world.setStatus(2);
						world.setReenterTime();
						startRaidPhase(world, player, 0);
						break;
					}
					default:
					{
						break;
					}
				}
				break;
			}
			case "raid_taunt":
			{
				final Instance world = player.getInstanceWorld();
				if (!isInInstance(world))
				{
					return null;
				}
				
				final StatSet parameters = world.getParameters();
				final int index = parameters.getInt("RAID_INDEX", 0);
				final Npc boss = world.getNpc(getBosses(parameters.getString("INSTANCE_LEVEL", "110"))[index]);
				if (boss != null)
				{
					world.broadcastPacket(new ExShowScreenMessage(TAUNT_MSG[index], ExShowScreenMessage.TOP_CENTER, 5000, false, ExShowScreenMessage.COLOR_RED));
					boss.broadcastSay(ChatType.NPC_GENERAL, TAUNT_MSG[index]);
				}
				break;
			}
			case "raid_countdown_3":
			case "raid_countdown_2":
			case "raid_countdown_1":
			{
				final Instance world = player.getInstanceWorld();
				if (!isInInstance(world))
				{
					return null;
				}
				
				world.broadcastPacket(new ExShowScreenMessage(event.equals("raid_countdown_3") ? COUNTDOWN_3 : event.equals("raid_countdown_2") ? COUNTDOWN_2 : COUNTDOWN_1, ExShowScreenMessage.TOP_CENTER, 1000, true));
				break;
			}
			case "raid_battle_start":
			{
				final Instance world = player.getInstanceWorld();
				if (!isInInstance(world))
				{
					return null;
				}
				
				final StatSet parameters = world.getParameters();
				final Npc boss = world.getNpc(getBosses(parameters.getString("INSTANCE_LEVEL", "110"))[parameters.getInt("RAID_INDEX", 0)]);
				if (boss != null)
				{
					boss.setInvul(false);
					boss.setTargetable(true);
					boss.disableCoreAI(false);
					boss.broadcastInfo();
				}
				
				world.broadcastPacket(new ExShowScreenMessage(BATTLE_BEGINS, ExShowScreenMessage.TOP_CENTER, 3000, true));
				break;
			}
			case "raid_next":
			{
				final Instance world = player.getInstanceWorld();
				if (!isInInstance(world))
				{
					return null;
				}
				
				world.broadcastPacket(new ExShowScreenMessage(NEXT_BATTLE_SOON, ExShowScreenMessage.TOP_CENTER, 5000, true));
				startRaidPhase(world, player, world.getParameters().getInt("RAID_INDEX", 0));
				break;
			}
			case "raid_finished":
			{
				final Instance world = player.getInstanceWorld();
				if (!isInInstance(world))
				{
					return null;
				}
				
				world.broadcastPacket(new ExShowScreenMessage(ALL_ENEMIES_DEFEATED, ExShowScreenMessage.TOP_CENTER, 5000, true));
				world.setStatus(3);
				// XXX REWARDS: reward distribution
				grantBossRewards(world, getRaidId(world.getParameters().getString("INSTANCE_LEVEL", "110")));
				world.finishInstance(2);
				break;
			}
			default:
			{
				break;
			}
		}
		
		return null;
	}
	
	/**
	 * Handles boss deaths for the raid sequence: says the boss's kill line, then either queues the next boss (indices 0 and 1) or the final "all enemies defeated" message (index 2).
	 * @param npc The boss {@code Npc} that died.
	 * @param killer The {@code Player} credited with the kill.
	 */
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final Instance world = npc.getInstanceWorld();
		if (!isInInstance(world))
		{
			return;
		}
		
		final StatSet parameters = world.getParameters();
		final int index = parameters.getInt("RAID_INDEX", 0);
		
		// Ignore this death when it is not the boss the instance currently expects.
		// That happens on a duplicate or late onKill for a phase which has already advanced.
		if (npc.getId() != getBosses(parameters.getString("INSTANCE_LEVEL", "110"))[index])
		{
			return;
		}
		
		// Guard: ignore a second onKill call for the same boss death.
		final String deadFlag = "RAID_DEAD_" + index;
		if (parameters.getInt(deadFlag, 0) == 1)
		{
			return;
		}
		
		parameters.set(deadFlag, 1);
		
		world.broadcastPacket(new ExShowScreenMessage(KILL_MSG[index], ExShowScreenMessage.TOP_CENTER, 5000, false, ExShowScreenMessage.COLOR_RED));
		npc.broadcastSay(ChatType.NPC_GENERAL, KILL_MSG[index]);
		
		if (index >= 2)
		{
			startQuestTimer("raid_finished", POST_KILL_DELAY, null, killer);
		}
		else
		{
			world.getParameters().set("RAID_INDEX", index + 1);
			startQuestTimer("raid_next", POST_KILL_DELAY, null, killer);
		}
	}
	
	/**
	 * Handles the logic when a {@code Player} talks to an {@code Npc} for the first time.<br>
	 * This method determines which dialogue file to display based on the NPC ID and player status.<br>
	 * It checks if the player is in a party or has GM privileges.
	 * @param npc The {@code Npc} being interacted with.
	 * @param player The {@code Player} initiating the conversation.
	 * @return The name of the HTML dialogue file to display, or {@code null} if no action is required.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		if (isInInstance(player.getInstanceWorld()))
		{
			if (npc.getId() == INNER_BENUSTA)
			{
				if (player.isInParty())
				{
					if (!player.getParty().isLeader(player))
					{
						return "31463-1.html";
					}
					
					return "31463.html";
				}
				else if (player.isGM())
				{
					return "31463.html";
				}
			}
		}
		
		return null;
	}
	
	private void grantBossRewards(Instance world, int generalId)
	{
		final ItemHolder rewardBox;
		switch (generalId)
		{
			case SINDRIC:
			{
				rewardBox = REWARD_BOX_120;
				break;
			}
			case GODIAK:
			{
				rewardBox = REWARD_BOX_130;
				break;
			}
			default:
			{
				rewardBox = REWARD_BOX_110;
				break;
			}
		}
		
		// Per-member reward.
		for (Player member : world.getPlayers())
		{
			if ((member.getParty() != null) || member.isGM())
			{
				giveItems(member, rewardBox);
				giveItems(member, WANDERER_TALES);
			}
		}
	}
	
	/**
	 * This is the main entry point for the {@code GainakArena} class.<br>
	 * It initializes a new instance of the script.
	 * @param args Command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		new GainakArena();
	}
}