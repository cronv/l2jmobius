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
package instances.DarkCloudMansion;

import java.util.List;

import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.instancezone.Instance;
import org.l2jmobius.gameserver.mechanics.script.InstanceScript;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.util.ArrayUtil;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the logic for the {@code DarkCloudMansion} instance.<br>
 * It manages the scripts and events specific to this dungeon area.
 * @author malyelfik, Mobius
 */
public class DarkCloudMansion extends InstanceScript
{
	// NPCs
	private static final int YIYEN = 32282;
	private static final int FAITH = 32288; // Symbol of Faith
	private static final int ADVERSITY = 32289; // Symbol of Adversity
	private static final int ADVENTURE = 32290; // Symbol of Adventure
	private static final int TRUTH = 32291; // Symbol of Truth
	private static final int MONOLITH = 32324; // Black Stone Monolith
	private static final int COLUMN = 22402; // Shadow Column
	
	// Mobs
	private static final int PARME_HEALER = 22400;
	private static final int[] BELETH_SUBORDINATE =
	{
		22272,
		22273,
		22274
	};
	private static final int[] MONOLITH_PRIVATES =
	{
		18369,
		18370
	};
	private static final int[] BELETH_SAMPLE =
	{
		18371,
		18372,
		18373,
		18374,
		18375,
		18376,
		18377
	};
	
	// Skill
	private static final SkillHolder INCARNATION = new SkillHolder(5441, 1);
	
	// Items
	private static final int CC = 9690; // Contaminated Crystal
	
	// Doors
	private static final int START_DOOR = 24230001; // Starting Room
	private static final int ROOM_A_DOOR = 24230002; // First Room
	private static final int ROOM_B_DOOR = 24230005; // Second Room
	private static final int ROOM_C_DOOR = 24230003; // Third Room
	private static final int ROOM_D_DOOR = 24230004; // Forth Room
	private static final int ROOM_E_DOOR = 24230006; // Fifth Room
	
	// Messages
	private static final NpcStringId[] SPAWN_CHAT =
	{
		NpcStringId.I_M_THE_REAL_ONE,
		NpcStringId.PICK_ME,
		NpcStringId.TRUST_ME,
		NpcStringId.NOT_THAT_DUDE_I_M_THE_REAL_ONE,
		NpcStringId.DON_T_BE_FOOLED_I_M_THE_REAL_ONE
	};
	private static final NpcStringId[] DECAY_CHAT =
	{
		NpcStringId.I_M_THE_REAL_ONE_2,
		NpcStringId.CAN_T_YOU_EVEN_FIND_OUT,
		NpcStringId.FIND_ME
	};
	private static final NpcStringId[] SUCCESS_CHAT =
	{
		NpcStringId.HUH_HOW_DID_YOU_KNOW_IT_WAS_ME,
		NpcStringId.EXCELLENT_CHOICE_TEEHEE,
		NpcStringId.YOU_VE_DONE_WELL,
		NpcStringId.OH_VERY_SENSIBLE
	};
	private static final NpcStringId[] FAIL_CHAT =
	{
		NpcStringId.YOU_VE_BEEN_FOOLED,
		NpcStringId.SORRY_BUT_I_M_THE_FAKE_ONE
	};
	
	// Misc
	private static final int TEMPLATE_ID = 9;
	
	/**
	 * Initializes the {@code DarkCloudMansion} instance script.<br>
	 * This constructor sets up the starting NPCs and initial interaction IDs.<br>
	 * It also registers specific attack and kill behaviors for various entities.
	 */
	public DarkCloudMansion()
	{
		super(TEMPLATE_ID);
		addStartNpc(YIYEN);
		addFirstTalkId(MONOLITH, TRUTH, FAITH, ADVERSITY, ADVENTURE);
		addTalkId(YIYEN, TRUTH);
		addAttackId(COLUMN, PARME_HEALER);
		addAttackId(BELETH_SAMPLE);
		addKillId(COLUMN, PARME_HEALER);
		addKillId(BELETH_SAMPLE);
		addKillId(BELETH_SUBORDINATE);
	}
	
	/**
	 * Handles specific events triggered within the instance.<br>
	 * This method processes logic for deleting NPCs and broadcasting chat messages.
	 * @param event The name of the event to trigger.
	 * @param npc The {@code Npc} object involved in the event.
	 * @param player The {@code Player} object involved in the event.
	 * @return A {@code null} value after processing the event.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		final Instance world = npc.getInstanceWorld();
		if (world != null)
		{
			switch (event)
			{
				case "DELETE_ONE":
				{
					npc.deleteMe();
					break;
				}
				case "DELETE":
				{
					world.getNpcs(BELETH_SAMPLE).stream().filter(n -> n != npc).forEach(Npc::deleteMe);
					world.setParameter("blocked", false);
					spawnRoomE(world);
					break;
				}
				case "CHAT":
				{
					world.getNpcs(BELETH_SAMPLE).stream().filter(n -> n.isScriptValue(1)).forEach(n -> n.broadcastSay(ChatType.NPC_GENERAL, getRandomEntry(DECAY_CHAT)));
					break;
				}
			}
		}
		
		return null;
	}
	
	/**
	 * Handles the logic when a {@code Player} talks to an {@code Npc}.<br>
	 * If the NPC is {@code YIYEN}, the player enters the instance.<br>
	 * Otherwise, the player is teleported out of the current world.
	 * @param npc The {@code Npc} that was interacted with.
	 * @param player The {@code Player} who initiated the talk.
	 * @return A {@code null} value.
	 */
	@Override
	public String onTalk(Npc npc, Player player)
	{
		if (npc.getId() == YIYEN)
		{
			enterInstance(player, npc, TEMPLATE_ID);
		}
		else
		{
			final Instance world = npc.getInstanceWorld();
			if (world != null)
			{
				teleportPlayerOut(player, world);
			}
		}
		
		return null;
	}
	
	/**
	 * Handles the logic for when a player talks to an NPC for the first time.<br>
	 * This method triggers specific events based on the {@code Npc} ID.<br>
	 * It can open doors, spawn mobs, or give items to the {@code Player}.
	 * @param npc The {@code Npc} that was interacted with.
	 * @param player The {@code Player} who initiated the conversation.
	 * @return A {@code null} value after processing the logic.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		final Instance world = npc.getInstanceWorld();
		if (world != null)
		{
			switch (npc.getId())
			{
				case FAITH:
				{
					world.openCloseDoor(ROOM_A_DOOR, true);
					npc.showChatWindow(player);
					break;
				}
				case MONOLITH:
				{
					if (world.isStatus(4) && !npc.isScriptValue(-1))
					{
						int current = world.getParameters().getInt("current", 0);
						if (npc.isScriptValue(current))
						{
							npc.setScriptValue(-1);
							npc.doCast(INCARNATION.getSkill());
							world.setParameter("current", ++current);
							if (current == 6)
							{
								world.setStatus(5);
								world.spawnGroup("roomBClear");
								world.spawnGroup("hall");
								world.getNpcs(MONOLITH).forEach(Npc::deleteMe);
							}
						}
						else
						{
							addSpawn(getRandomEntry(MONOLITH_PRIVATES), npc, false, 0, false, world.getId());
						}
						
						world.openCloseDoor(ROOM_B_DOOR, false);
					}
					break;
				}
				case ADVERSITY:
				{
					world.openCloseDoor(ROOM_B_DOOR, true);
					npc.showChatWindow(player);
					break;
				}
				case ADVENTURE:
				{
					if (world.isStatus(7))
					{
						world.spawnGroup("roomC").forEach(n -> n.setScriptValue(8));
						world.setStatus(8);
						world.openCloseDoor(ROOM_D_DOOR, true);
					}
					
					npc.showChatWindow(player);
					break;
				}
				case TRUTH:
				{
					if (!hasQuestItems(player, CC))
					{
						giveItems(player, CC, 1);
					}
					
					npc.showChatWindow(player);
					break;
				}
			}
		}
		
		return null;
	}
	
	/**
	 * Handles logic when an {@code Npc} is attacked by a {@code Player}.<br>
	 * This method checks the specific ID of the {@code Npc} to trigger world events.<br>
	 * It can open doors or spawn new entities based on current instance statuses.
	 * @param npc The {@code Npc} being attacked.
	 * @param attacker The {@code Player} performing the attack.
	 * @param damage The amount of damage dealt.
	 * @param isSummon Whether the attack was performed by a summon.
	 */
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	{
		final Instance world = npc.getInstanceWorld();
		if (world != null)
		{
			switch (npc.getId())
			{
				case PARME_HEALER:
				{
					if (world.isStatus(2) && (attacker.getY() < 179986))
					{
						world.openCloseDoor(ROOM_A_DOOR, false);
					}
					break;
				}
				case COLUMN:
				{
					if (world.isStatus(7))
					{
						world.openCloseDoor(ROOM_D_DOOR, false);
						if (npc.isHpBlocked() && (getRandom(100) < 12) && !npc.getVariables().getBoolean("DCM_GUARD_SPAWNED", false))
						{
							npc.getVariables().set("DCM_GUARD_SPAWNED", true);
							addSpawn(getRandomEntry(BELETH_SUBORDINATE), npc, false, 0, false, world.getId());
						}
					}
					break;
				}
				default:
				{
					if (world.isStatus(9))
					{
						handleRoomE(world, npc);
					}
				}
			}
		}
	}
	
	/**
	 * This method is called when an {@code Npc} is killed by a {@code Player}.<br>
	 * It checks the current instance status and triggers progression events.<br>
	 * Progression includes spawning new groups, opening doors, or changing room statuses.
	 * @param npc The {@code Npc} that was killed.
	 * @param player The {@code Player} who performed the kill.
	 * @param isSummon Indicates if the kill was performed by a summon.
	 */
	@Override
	public void onKill(Npc npc, Player player, boolean isSummon)
	{
		final Instance world = npc.getInstanceWorld();
		if (world != null)
		{
			switch (world.getStatus())
			{
				case 0: // Start room
				{
					if (world.getAliveNpcCount() == 0)
					{
						world.setStatus(1);
						world.spawnGroup("hall");
						world.openCloseDoor(START_DOOR, true);
					}
					break;
				}
				case 1: // Hall
				{
					if (world.getAliveNpcCount() == 0)
					{
						world.setStatus(2);
						world.spawnGroup("roomA");
						world.openCloseDoor(ROOM_A_DOOR, true);
					}
					break;
				}
				case 2: // Room A - cleared
				{
					if (world.getAliveNpcCount() == 0)
					{
						world.setStatus(3);
						world.spawnGroup("roomAClear");
						world.spawnGroup("hall");
					}
					break;
				}
				case 3: // Hall
				{
					if (world.getAliveNpcCount(BELETH_SUBORDINATE) == 0)
					{
						world.setStatus(4);
						world.openCloseDoor(ROOM_B_DOOR, true);
						int current = 0;
						final List<Npc> desks = world.spawnGroup("roomB");
						while (!desks.isEmpty())
						{
							final Npc desk = desks.remove(getRandom(desks.size()));
							desk.setScriptValue(current++);
						}
					}
					break;
				}
				case 5: // Hall
				{
					if (world.getAliveNpcCount(BELETH_SUBORDINATE) == 0)
					{
						world.setStatus(6);
						world.spawnGroup("roomC");
						world.openCloseDoor(ROOM_C_DOOR, true);
					}
					break;
				}
				case 6: // Room C
				{
					if (world.getAliveNpcCount(BELETH_SUBORDINATE) == 0)
					{
						world.setStatus(7);
						world.spawnGroup("roomD");
						world.openCloseDoor(ROOM_D_DOOR, true);
						for (int i = 1; i <= 7; i++)
						{
							final List<Npc> row = world.spawnGroup("roomD" + i);
							for (int x = 0; x < 2; x++)
							{
								final Npc col = row.remove(getRandom(row.size()));
								col.setInvul(true);
							}
						}
					}
					break;
				}
				case 8: // Room C
				{
					final long roomClear = world.getAliveNpcs(BELETH_SUBORDINATE).stream().filter(n -> n.isScriptValue(8)).count();
					if (roomClear == 0)
					{
						world.setStatus(9);
						world.openCloseDoor(ROOM_E_DOOR, true);
						spawnRoomE(world);
					}
					break;
				}
				case 9:
				{
					handleRoomE(world, npc);
					break;
				}
			}
		}
	}
	
	/**
	 * Spawns the NPCs for room E in the instance.<br>
	 * This method initializes a group of NPCs and assigns them random chat messages.<br>
	 * It also selects three random NPCs to receive a specific script value.
	 * @param world The {@code Instance} where the NPCs will be spawned.
	 */
	private void spawnRoomE(Instance world)
	{
		final List<Npc> npcs = world.spawnGroup("roomE");
		for (Npc n : npcs)
		{
			n.broadcastSay(ChatType.NPC_GENERAL, getRandomEntry(SPAWN_CHAT));
		}
		
		for (int i = 0; i < 3; i++)
		{
			final Npc n = npcs.remove(getRandom(npcs.size()));
			n.setScriptValue(1);
		}
	}
	
	/**
	 * Handles the interaction logic for specific NPCs in Room E.<br>
	 * It checks if the NPC is part of the {@code BELETH_SAMPLE} group.<br>
	 * This method manages quest progress, success messages, and room transitions.
	 * @param world The {@code Instance} where the action occurs.
	 * @param npc The {@code Npc} interacting with the player.
	 */
	private void handleRoomE(Instance world, Npc npc)
	{
		if (ArrayUtil.contains(BELETH_SAMPLE, npc.getId()))
		{
			final StatSet params = world.getParameters();
			if (!params.getBoolean("blocked", false))
			{
				if (npc.isScriptValue(1))
				{
					final int found = params.getInt("found", 0) + 1;
					world.setParameter("found", found);
					npc.setScriptValue(-1);
					npc.broadcastSay(ChatType.NPC_GENERAL, getRandomEntry(SUCCESS_CHAT));
					if (found != 3)
					{
						startQuestTimer("DELETE_ONE", 1500, npc, null);
					}
					else
					{
						world.setStatus(10);
						world.getNpcs().forEach(Npc::deleteMe);
						world.spawnGroup("roomEClear");
					}
				}
				else if (!npc.isScriptValue(-1))
				{
					world.setParameter("blocked", true);
					world.setParameter("found", 0);
					npc.broadcastSay(ChatType.NPC_GENERAL, getRandomEntry(FAIL_CHAT));
					npc.setScriptValue(-1);
					startQuestTimer("CHAT", 4000, npc, null);
					startQuestTimer("DELETE", 4500, npc, null);
				}
				
				world.openCloseDoor(ROOM_E_DOOR, false);
			}
		}
	}
	
	/**
	 * This is the main entry point for the {@code DarkCloudMansion} class.<br>
	 * It initializes a new instance of the script.
	 * @param args Command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		new DarkCloudMansion();
	}
}
