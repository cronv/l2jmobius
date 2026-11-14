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
package instances.CanonCathedral;

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.groups.Party;
import org.l2jmobius.gameserver.entity.instancezone.Instance;
import org.l2jmobius.gameserver.entity.item.holders.ItemHolder;
import org.l2jmobius.gameserver.entity.zone.ZoneType;
import org.l2jmobius.gameserver.mechanics.script.InstanceScript;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;

/**
 * This class handles the logic and events for the {@code CanonCathedral} instance.<br>
 * Party leader selects a difficulty (110 / 120 / 130) at Benusta. The Cathedral has 4 sealed rooms (Red/Blood, Blue/Gnosis, Yellow/Radiance, Green/Restoration); one statue is spawned at a time in a randomly chosen unvisited room. Attacking the statue drives it through a series of HP-percentage thresholds that spawn minion waves and show flavor messages, and around the halfway/quarter points the statue briefly changes its displayed form. Once all 4 statues are destroyed, the tier's raid boss spawns in a random room other than the last one cleared.
 * @author CostyKiller
 */
public class CanonCathedral extends InstanceScript
{
	// NPCs
	private static final int BENUSTA = 34542;
	private static final int INNER_BENUSTA = 31464;
	
	// Room keys.
	private static final String ROOM_RED = "RED"; // Blood seal
	private static final String ROOM_BLUE = "BLUE"; // Gnosis seal
	private static final String ROOM_YELLOW = "YELLOW"; // Radiance seal
	private static final String ROOM_GREEN = "GREEN"; // Restoration seal
	
	// Zone ids for each room, used to detect when the party actually walks into the room that
	// currently holds the active statue (rather than firing intro messages on a timer right after
	// difficulty select).
	// XXX: Added custom zone ids for each room.
	private static final int ZONE_RED = 366001;
	private static final int ZONE_BLUE = 366002;
	private static final int ZONE_YELLOW = 366003;
	private static final int ZONE_GREEN = 366004;
	
	// Statue ids per tier, in RED/BLUE/YELLOW/GREEN order (Blood/Gnosis/Radiance/Restoration).
	private static final int[] STATUES_110 =
	{
		26644,
		26645,
		26646,
		26647
	};
	private static final int[] STATUES_120 =
	{
		26656,
		26657,
		26658,
		26659
	};
	private static final int[] STATUES_130 =
	{
		26668,
		26669,
		26670,
		26671
	};
	
	// Minion ids per tier {GRAND_TEMPLE_GUARDIAN, TEMPLE_EXECUTOR, PRIEST_OF_BLOOD}.
	private static final int[] MINIONS_110 =
	{
		26649,
		26650,
		26651
	};
	private static final int[] MINIONS_120 =
	{
		26661,
		26662,
		26663
	};
	private static final int[] MINIONS_130 =
	{
		26673,
		26674,
		26675
	};
	
	// Final raid id per tier.
	private static final int RAID_VELKIR = 26655; // 110
	private static final int RAID_RADIGES = 26667; // 120
	private static final int RAID_AQUILON = 26679; // 130
	
	// Statue display states
	private static final int DISPLAY_EINHASAD = 0;
	private static final int DISPLAY_DEMONIC = 1;
	private static final int DISPLAY_BROKEN = 2;
	
	// HP% thresholds the statue fight reacts to, checked in descending order.
	private static final int[] THRESHOLDS =
	{
		90,
		85,
		80,
		75,
		70,
		65,
		60,
		55,
		50,
		45,
		40,
		35,
		30,
		25,
		20,
		15,
		5
	};
	
	// Statues messages
	private static final NpcStringId MSG_STRIKE_THE_STATUE = NpcStringId.STRIKE_AT_THE_STATUE_OF_EINHASAD_AND_THE_HIDDEN_TRUTH_WILL_BE_REVEALED_TO_YOU;
	private static final NpcStringId MSG_OMINOUS_ENERGY_ALL_AROUND = NpcStringId.YOU_FEEL_OMINOUS_ENERGY_ALL_AROUND_YOU_THE_FALLEN_SOUL_THAT_RULES_HERE_NOTICED_YOUR_PARTY_S_PROGRESS;
	private static final NpcStringId MSG_DEMONIC_STATUE_REVEALED = NpcStringId.THE_DEMONIC_STATUE_HAS_REVEALED_ITS_TRUE_APPEARANCE;
	private static final NpcStringId MSG_ENERGY_GETTING_STRONGER = NpcStringId.OMINOUS_ENERGY_IS_GETTING_STRONGER_THE_FALLEN_SOUL_THAT_RULES_HERE_IS_READY_TO_REVEAL_ITSELF;
	private static final NpcStringId MSG_ENERGY_READY_TO_REVEAL = NpcStringId.OMINOUS_ENERGY_IS_GETTING_STRONGER_THE_FALLEN_SOUL_THAT_RULES_HERE_IS_READY_TO_REVEAL_ITSELF;
	private static final NpcStringId MSG_ENERGY_RINGING_TENSION = NpcStringId.OMINOUS_ENERGY_IS_RINGING_WITH_TENSION_THE_FALLEN_SOUL_THAT_RULES_HERE_IS_ABOUT_TO_SHOW_UP_ON_ONE_OF_THE_SEALS;
	// Per-seal variant messages
	// Indexed RED/BLUE/YELLOW/GREEN (Blood/Gnosis/Radiance/Restoration).
	private static final NpcStringId[] MSG_SERVITORS_APPEARED_AGAIN =
	{
		NpcStringId.SERVITORS_AND_GUARDIANS_PROTECTING_BLOOD_SEAL_HAVE_APPEARED_AGAIN,
		NpcStringId.SERVITORS_AND_GUARDIANS_PROTECTING_GNOSIS_SEAL_HAVE_APPEARED_AGAIN,
		NpcStringId.SERVITORS_AND_GUARDIANS_PROTECTING_RADIANCE_SEAL_HAVE_APPEARED_AGAIN,
		NpcStringId.SERVITORS_AND_GUARDIANS_PROTECTING_RESTORATION_SEAL_HAVE_APPEARED_AGAIN
	};
	private static final NpcStringId[] MSG_SERVITORS_APPEARED_AGAIN_MORE =
	{
		NpcStringId.SERVITORS_AND_GUARDIANS_PROTECTING_BLOOD_SEAL_HAVE_APPEARED_AGAIN_NOW_THERE_ARE_EVEN_MORE_OF_THEM,
		NpcStringId.SERVITORS_AND_GUARDIANS_PROTECTING_GNOSIS_SEAL_HAVE_APPEARED_AGAIN_NOW_THERE_ARE_EVEN_MORE_OF_THEM,
		NpcStringId.SERVITORS_AND_GUARDIANS_PROTECTING_THE_RADIANCE_SEAL_HAVE_APPEARED_AGAIN_NOW_THERE_ARE_EVEN_MORE_OF_THEM,
		NpcStringId.SERVITORS_AND_GUARDIANS_PROTECTING_RESTORATION_SEAL_HAVE_APPEARED_AGAIN_NOW_THERE_ARE_EVEN_MORE_OF_THEM
	};
	private static final NpcStringId MSG_FALLEN_SOUL_ANOTHER_SEAL = NpcStringId.THE_FALLEN_SOUL_APPEARED_ON_ANOTHER_SEAL;
	private static final NpcStringId MSG_ENERGY_DISPERSED = NpcStringId.OMINOUS_ENERGY_IS_DISPERSED_THE_FALLEN_SOUL_THAT_RULES_HERE_DISAPPEARS;
	private static final NpcStringId MSG_STATUES_DESTROYED_1 = NpcStringId.DEMON_S_STATUES_DESTROYED_1_4;
	private static final NpcStringId MSG_STATUES_DESTROYED_2 = NpcStringId.DEMON_S_STATUES_DESTROYED_2_4;
	private static final NpcStringId MSG_STATUES_DESTROYED_3 = NpcStringId.DEMON_S_STATUES_DESTROYED_3_4;
	private static final NpcStringId MSG_STATUES_DESTROYED_4 = NpcStringId.ALL_DEMON_S_STATUES_ARE_DESTROYED_4_4;
	private static final NpcStringId MSG_RAID_TAUNT = NpcStringId.NO_IT_CANNOT_BE;
	private static final NpcStringId MSG_VELKIR_DESTROYED = NpcStringId.THE_CORRUPTED_HIGH_PRIEST_VELKIR_IS_DESTROYED;
	private static final NpcStringId MSG_RADIGES_DESTROYED = NpcStringId.THE_CORRUPTED_HIGH_PRIEST_RADIGES_IS_DESTROYED;
	private static final NpcStringId MSG_AQUILON_DESTROYED = NpcStringId.THE_CORRUPTED_HIGH_PRIEST_AQUILON_IS_DESTROYED;
	
	// Reward items
	private static final ItemHolder REWARD_BOX_110 = new ItemHolder(82453, 1); // Benusta's Reward Pack Lv. 1
	private static final ItemHolder REWARD_BOX_120 = new ItemHolder(82455, 1); // Benusta's Reward Pack Lv. 3
	private static final ItemHolder REWARD_BOX_130 = new ItemHolder(83729, 1); // Benusta's Reward Pack Lv. 5
	private static final ItemHolder WANDERER_TALES = new ItemHolder(83575, 2); // Novice Wanderer's Tales
	
	// Misc
	private static final int TEMPLATE_ID = 366;
	private static final int STATUE_INTRO_DELAY = 3000;
	private static final int RAID_TAUNT_DELAY = 2000;
	private static final int RAID_FINISH_DELAY = 2000;
	private static final int NEXT_ROOM_DELAY = 8000;
	private static final int MINION_SPAWN_RADIUS_MIN = 150;
	private static final int MINION_SPAWN_RADIUS_MAX = 300;
	
	/**
	 * Initializes the {@code CanonCathedral} instance script.<br>
	 * Sets up the required template ID, registers the leave ID, and registers attack/kill listeners for every statue (across all 3 tiers) and every raid boss.
	 */
	public CanonCathedral()
	{
		super(TEMPLATE_ID);
		addStartNpc(BENUSTA);
		addFirstTalkId(INNER_BENUSTA);
		addInstanceLeaveId(TEMPLATE_ID);
		addAttackId(STATUES_110[0], STATUES_110[1], STATUES_110[2], STATUES_110[3], STATUES_120[0], STATUES_120[1], STATUES_120[2], STATUES_120[3], STATUES_130[0], STATUES_130[1], STATUES_130[2], STATUES_130[3]);
		addKillId(STATUES_110[0], STATUES_110[1], STATUES_110[2], STATUES_110[3], STATUES_120[0], STATUES_120[1], STATUES_120[2], STATUES_120[3], STATUES_130[0], STATUES_130[1], STATUES_130[2], STATUES_130[3], RAID_VELKIR, RAID_RADIGES, RAID_AQUILON);
		addEnterZoneId(ZONE_RED, ZONE_BLUE, ZONE_YELLOW, ZONE_GREEN);
	}
	
	private static int[] getStatues(String instanceLv)
	{
		switch (instanceLv)
		{
			case "120":
			{
				return STATUES_120;
			}
			case "130":
			{
				return STATUES_130;
			}
			default:
			{
				return STATUES_110;
			}
		}
	}
	
	private static int[] getMinions(String instanceLv)
	{
		switch (instanceLv)
		{
			case "120":
			{
				return MINIONS_120;
			}
			case "130":
			{
				return MINIONS_130;
			}
			default:
			{
				return MINIONS_110;
			}
		}
	}
	
	private static int getRaidId(String instanceLv)
	{
		switch (instanceLv)
		{
			case "120":
			{
				return RAID_RADIGES;
			}
			case "130":
			{
				return RAID_AQUILON;
			}
			default:
			{
				return RAID_VELKIR;
			}
		}
	}
	
	private static NpcStringId getRaidDestroyedMsg(String instanceLv)
	{
		switch (instanceLv)
		{
			case "120":
			{
				return MSG_RADIGES_DESTROYED;
			}
			case "130":
			{
				return MSG_AQUILON_DESTROYED;
			}
			default:
			{
				return MSG_VELKIR_DESTROYED;
			}
		}
	}
	
	private static int getRoomIndex(String roomKey)
	{
		switch (roomKey)
		{
			case ROOM_RED:
			{
				return 0;
			}
			case ROOM_BLUE:
			{
				return 1;
			}
			case ROOM_YELLOW:
			{
				return 2;
			}
			default:
			{
				return 3; // GREEN
			}
		}
	}
	
	private static NpcStringId getServitorsMsg(String roomKey)
	{
		return MSG_SERVITORS_APPEARED_AGAIN[getRoomIndex(roomKey)];
	}
	
	private static NpcStringId getServitorsMoreMsg(String roomKey)
	{
		return MSG_SERVITORS_APPEARED_AGAIN_MORE[getRoomIndex(roomKey)];
	}
	
	private static NpcStringId getStatuesDestroyedMsg(int killedCount)
	{
		switch (killedCount)
		{
			case 1:
			{
				return MSG_STATUES_DESTROYED_1;
			}
			case 2:
			{
				return MSG_STATUES_DESTROYED_2;
			}
			case 3:
			{
				return MSG_STATUES_DESTROYED_3;
			}
			default:
			{
				return MSG_STATUES_DESTROYED_4;
			}
		}
	}
	
	private static int getStatueId(String instanceLv, String roomKey)
	{
		final int[] statues = getStatues(instanceLv);
		switch (roomKey)
		{
			case ROOM_RED:
			{
				return statues[0];
			}
			case ROOM_BLUE:
			{
				return statues[1];
			}
			case ROOM_YELLOW:
			{
				return statues[2];
			}
			default:
			{
				return statues[3]; // GREEN
			}
		}
	}
	
	private static String getRoomKeyForZone(int zoneId)
	{
		if (zoneId == ZONE_RED)
		{
			return ROOM_RED;
		}
		else if (zoneId == ZONE_BLUE)
		{
			return ROOM_BLUE;
		}
		else if (zoneId == ZONE_YELLOW)
		{
			return ROOM_YELLOW;
		}
		else if (zoneId == ZONE_GREEN)
		{
			return ROOM_GREEN;
		}
		return null;
	}
	
	private static List<String> parseRoomList(String csv)
	{
		final List<String> rooms = new ArrayList<>();
		if ((csv != null) && !csv.isEmpty())
		{
			for (String room : csv.split(","))
			{
				rooms.add(room);
			}
		}
		return rooms;
	}
	
	private static String joinRoomList(List<String> rooms)
	{
		return String.join(",", rooms);
	}
	
	/**
	 * Picks and spawns a random not-yet-cleared room's statue, resets its threshold progress, and immobilizes the statue. Intro messages fire separately, on room entry (see onEnterZone).
	 * @param world
	 * @param player
	 */
	private void startNextStatuePhase(Instance world, Player player)
	{
		final List<String> remaining = parseRoomList(world.getParameters().getString("REMAINING_ROOMS", ROOM_RED + "," + ROOM_BLUE + "," + ROOM_YELLOW + "," + ROOM_GREEN));
		if (remaining.isEmpty())
		{
			return;
		}
		
		final String roomKey = remaining.remove(Rnd.get(remaining.size()));
		world.getParameters().set("REMAINING_ROOMS", joinRoomList(remaining));
		world.getParameters().set("CURRENT_ROOM", roomKey);
		world.getParameters().set("STATUE_THRESHOLD_INDEX", 0);
		world.getParameters().set("STATUE_DISPLAY_STATE", DISPLAY_EINHASAD);
		
		final String instanceLv = world.getParameters().getString("INSTANCE_LEVEL", "110");
		world.spawnGroup("STATUE_" + instanceLv + "_" + roomKey);
		
		// TODO: verify if statues still move around
		final Npc statue = world.getNpc(getStatueId(instanceLv, roomKey));
		if (statue != null)
		{
			statue.setImmobilized(true);
			statue.setRandomWalking(false);
			statue.stopMove(null);
		}
	}
	
	private void spawnAroundStatue(Instance world, Npc statue, int npcId, int count)
	{
		for (int i = 0; i < count; i++)
		{
			final double angle = Math.toRadians(Rnd.get(360));
			final int radius = Rnd.get(MINION_SPAWN_RADIUS_MIN, MINION_SPAWN_RADIUS_MAX);
			final int x = statue.getX() + (int) (Math.cos(angle) * radius);
			final int y = statue.getY() + (int) (Math.sin(angle) * radius);
			// TODO: Check minions spawn
			final Npc minion = addSpawn(npcId, x, y, statue.getZ(), 0, false, 0, false, world.getId());
			aggroNearestPlayer(world, minion);
		}
	}
	
	/**
	 * Makes a freshly spawned minion immediately hostile toward the nearest player in the instance, instead of waiting passively until someone attacks it first. TODO: verify these calls (addDamageHate / setIntention / CtrlIntention import) against your Attackable/AI API - the goal is: minion goes straight into combat on spawn.
	 * @param world
	 * @param minion
	 */
	private void aggroNearestPlayer(Instance world, Npc minion)
	{
		if ((minion == null) || !minion.isAttackable())
		{
			return;
		}
		
		Player nearest = null;
		double nearestDistance = Double.MAX_VALUE;
		for (Player member : world.getPlayers())
		{
			final double distance = minion.calculateDistance3D(member);
			if (distance < nearestDistance)
			{
				nearestDistance = distance;
				nearest = member;
			}
		}
		
		if (nearest != null)
		{
			minion.setRunning();
			minion.asAttackable().addDamageHate(nearest, 0, 500);
			minion.getAI().setIntentionAttack(nearest);
		}
	}
	
	private void spawnMinionWave(Instance world, Npc statue, int guardianCount, int executorCount, int priestCount)
	{
		final int[] minions = getMinions(world.getParameters().getString("INSTANCE_LEVEL", "110"));
		spawnAroundStatue(world, statue, minions[0], guardianCount);
		spawnAroundStatue(world, statue, minions[1], executorCount);
		spawnAroundStatue(world, statue, minions[2], priestCount);
	}
	
	/**
	 * Fires the scripted reaction for a single HP% threshold: shows the message(s) and, for thresholds that call for it, spawns a minion wave around the statue.
	 * @param world
	 * @param statue
	 * @param threshold
	 * @param player
	 */
	private void triggerStatueThreshold(Instance world, Npc statue, int threshold, Player player)
	{
		final String roomKey = world.getParameters().getString("CURRENT_ROOM", ROOM_RED);
		
		switch (threshold)
		{
			case 90:
			{
				// Guard: only ever move display state forward (0 -> 1 -> 2), never back.
				final int currentDisplay = world.getParameters().getInt("STATUE_DISPLAY_STATE", DISPLAY_EINHASAD);
				if (currentDisplay < DISPLAY_DEMONIC)
				{
					world.getParameters().set("STATUE_DISPLAY_STATE", DISPLAY_DEMONIC);
					statue.setDisplayEffect(DISPLAY_DEMONIC);
					world.broadcastPacket(new ExShowScreenMessage(MSG_DEMONIC_STATUE_REVEALED, ExShowScreenMessage.TOP_CENTER, 5000, true));
				}
				break;
			}
			case 85:
			{
				spawnMinionWave(world, statue, 2, 2, 2);
				world.broadcastPacket(new ExShowScreenMessage(MSG_ENERGY_GETTING_STRONGER, ExShowScreenMessage.TOP_CENTER, 5000, true));
				break;
			}
			case 80:
			{
				world.broadcastPacket(new ExShowScreenMessage(MSG_ENERGY_RINGING_TENSION, ExShowScreenMessage.TOP_CENTER, 5000, true));
				break;
			}
			case 75:
			case 70:
			case 65:
			case 55:
			case 45:
			{
				spawnMinionWave(world, statue, 2, 2, 2);
				world.broadcastPacket(new ExShowScreenMessage(getServitorsMsg(roomKey), ExShowScreenMessage.TOP_CENTER, 5000, true));
				break;
			}
			case 60:
			{
				world.broadcastPacket(new ExShowScreenMessage(MSG_ENERGY_READY_TO_REVEAL, ExShowScreenMessage.TOP_CENTER, 5000, true));
				break;
			}
			case 50:
			{
				world.broadcastPacket(new ExShowScreenMessage(MSG_ENERGY_RINGING_TENSION, ExShowScreenMessage.TOP_CENTER, 5000, true));
				startQuestTimer("statue_msg_fallen_soul", 4000, null, player);
				break;
			}
			case 40:
			case 5:
			{
				world.broadcastPacket(new ExShowScreenMessage(MSG_ENERGY_DISPERSED, ExShowScreenMessage.TOP_CENTER, 5000, true));
				break;
			}
			case 35:
			{
				spawnMinionWave(world, statue, 4, 4, 4);
				world.broadcastPacket(new ExShowScreenMessage(getServitorsMoreMsg(roomKey), ExShowScreenMessage.TOP_CENTER, 5000, true));
				break;
			}
			case 30:
			{
				spawnMinionWave(world, statue, 4, 4, 4);
				world.broadcastPacket(new ExShowScreenMessage(getServitorsMsg(roomKey), ExShowScreenMessage.TOP_CENTER, 5000, true));
				break;
			}
			case 25:
			{
				world.broadcastPacket(new ExShowScreenMessage(MSG_ENERGY_READY_TO_REVEAL, ExShowScreenMessage.TOP_CENTER, 5000, true));
				startQuestTimer("statue_msg_energy_tension", 4000, null, player);
				break;
			}
			case 20:
			{
				world.broadcastPacket(new ExShowScreenMessage(MSG_FALLEN_SOUL_ANOTHER_SEAL, ExShowScreenMessage.TOP_CENTER, 5000, true));
				break;
			}
			case 15:
			{
				spawnMinionWave(world, statue, 0, 0, 8);
				world.broadcastPacket(new ExShowScreenMessage(getServitorsMsg(roomKey), ExShowScreenMessage.TOP_CENTER, 5000, true));
				break;
			}
		}
	}
	
	@Override
	public void onEnterZone(Creature creature, ZoneType zone)
	{
		if (creature.isPlayer())
		{
			final Player player = creature.asPlayer();
			final Instance world = player.getInstanceWorld();
			if (isInInstance(world))
			{
				final String roomKey = getRoomKeyForZone(zone.getId());
				final String currentRoom = world.getParameters().getString("CURRENT_ROOM", "");
				if ((roomKey != null) && roomKey.equals(currentRoom) && (world.getParameters().getInt("ROOM_INTRO_SHOWN_" + roomKey, 0) == 0))
				{
					world.getParameters().set("ROOM_INTRO_SHOWN_" + roomKey, 1);
					startQuestTimer("statue_intro_1", 500, null, player);
					startQuestTimer("statue_intro_2", STATUE_INTRO_DELAY, null, player);
				}
			}
		}
	}
	
	/**
	 * Handles specific events triggered within the Canon Cathedral instance: entry, difficulty selection, the statue intro lines, delayed follow-up messages for the 50%/25% thresholds, advancing to the next room, and spawning the final raid.
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
			case "31464-1.html":
			case "31464-2.html":
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
				
				final int benustaText = world.getParameters().getInt("BENUSTA_TEXT", 0);
				if (world.getAliveNpcCount(INNER_BENUSTA) > 0)
				{
					switch (benustaText)
					{
						case 0:
						{
							showOnScreenMsg(world, NpcStringId.THE_PARTY_LEADER_SHOULD_TALK_TO_ME_AND_SELECT_THE_DIFFICULTY_LEVEL, ExShowScreenMessage.TOP_CENTER, 5000, true);
							world.getNpc(INNER_BENUSTA).broadcastSay(ChatType.NPC_GENERAL, NpcStringId.THE_PARTY_LEADER_SHOULD_TALK_TO_ME_AND_SELECT_THE_DIFFICULTY_LEVEL);
							startQuestTimer("benusta_texts", 4000, null, player);
							world.getParameters().set("BENUSTA_TEXT", 1);
							break;
						}
						case 1:
						{
							showOnScreenMsg(world, NpcStringId.TAKE_INTO_ACCOUNT_THE_ABILITIES_OF_YOUR_PARTY_MEMBERS_WHEN_SELECTING_THE_DIFFICULTY, ExShowScreenMessage.TOP_CENTER, 5000, true);
							world.getNpc(INNER_BENUSTA).broadcastSay(ChatType.NPC_GENERAL, NpcStringId.TAKE_INTO_ACCOUNT_THE_ABILITIES_OF_YOUR_PARTY_MEMBERS_WHEN_SELECTING_THE_DIFFICULTY);
							startQuestTimer("benusta_texts", 4000, null, player);
							world.getParameters().set("BENUSTA_TEXT", 2);
							break;
						}
						case 2:
						{
							showOnScreenMsg(world, NpcStringId.THE_SELECTED_DIFFICULTY_CANNOT_BE_CHANGED_SO_CHOOSE_WISELY, ExShowScreenMessage.TOP_CENTER, 5000, true);
							world.getNpc(INNER_BENUSTA).broadcastSay(ChatType.NPC_GENERAL, NpcStringId.THE_SELECTED_DIFFICULTY_CANNOT_BE_CHANGED_SO_CHOOSE_WISELY);
							world.getParameters().set("BENUSTA_TEXT", 0);
							break;
						}
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
				world.getParameters().set("STATUES_KILLED", 0);
				world.getNpc(INNER_BENUSTA).deleteMe();
				world.setReenterTime();
				startNextStatuePhase(world, player);
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
				world.getParameters().set("STATUES_KILLED", 0);
				world.getNpc(INNER_BENUSTA).deleteMe();
				world.setReenterTime();
				startNextStatuePhase(world, player);
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
				world.getParameters().set("STATUES_KILLED", 0);
				world.getNpc(INNER_BENUSTA).deleteMe();
				world.setReenterTime();
				startNextStatuePhase(world, player);
				break;
			}
			case "statue_intro_1":
			{
				final Instance world = player.getInstanceWorld();
				if (!isInInstance(world))
				{
					return null;
				}
				
				world.broadcastPacket(new ExShowScreenMessage(MSG_STRIKE_THE_STATUE, ExShowScreenMessage.TOP_CENTER, 5000, true));
				break;
			}
			case "statue_intro_2":
			{
				final Instance world = player.getInstanceWorld();
				if (!isInInstance(world))
				{
					return null;
				}
				
				world.broadcastPacket(new ExShowScreenMessage(MSG_OMINOUS_ENERGY_ALL_AROUND, ExShowScreenMessage.TOP_CENTER, 5000, true));
				break;
			}
			case "statue_msg_fallen_soul":
			{
				final Instance world = player.getInstanceWorld();
				if (!isInInstance(world))
				{
					return null;
				}
				
				world.broadcastPacket(new ExShowScreenMessage(MSG_FALLEN_SOUL_ANOTHER_SEAL, ExShowScreenMessage.TOP_CENTER, 5000, true));
				break;
			}
			case "statue_msg_energy_tension":
			{
				final Instance world = player.getInstanceWorld();
				if (!isInInstance(world))
				{
					return null;
				}
				
				world.broadcastPacket(new ExShowScreenMessage(MSG_ENERGY_RINGING_TENSION, ExShowScreenMessage.TOP_CENTER, 5000, true));
				break;
			}
			case "next_statue_phase":
			{
				final Instance world = player.getInstanceWorld();
				if (!isInInstance(world))
				{
					return null;
				}
				
				startNextStatuePhase(world, player);
				break;
			}
			case "spawn_final_raid":
			{
				final Instance world = player.getInstanceWorld();
				if (!isInInstance(world))
				{
					return null;
				}
				
				// Random room other than the last one a statue died in.
				final List<String> allRooms = new ArrayList<>(List.of(ROOM_RED, ROOM_BLUE, ROOM_YELLOW, ROOM_GREEN));
				allRooms.remove(world.getParameters().getString("LAST_CLEARED_ROOM", ""));
				final String raidRoom = allRooms.get(Rnd.get(allRooms.size()));
				world.getParameters().set("RAID_ROOM", raidRoom);
				
				final String instanceLv = world.getParameters().getString("INSTANCE_LEVEL", "110");
				world.spawnGroup("RAID_" + instanceLv + "_" + raidRoom);
				startQuestTimer("raid_taunt", RAID_TAUNT_DELAY, null, player);
				break;
			}
			case "raid_taunt":
			{
				final Instance world = player.getInstanceWorld();
				if (!isInInstance(world))
				{
					return null;
				}
				
				final int raidId = getRaidId(world.getParameters().getString("INSTANCE_LEVEL", "110"));
				final Npc raid = world.getNpc(raidId);
				world.broadcastPacket(new ExShowScreenMessage(MSG_RAID_TAUNT, ExShowScreenMessage.TOP_CENTER, 5000, true, ExShowScreenMessage.COLOR_RED));
				if (raid != null)
				{
					raid.broadcastSay(ChatType.NPC_GENERAL, MSG_RAID_TAUNT);
				}
				break;
			}
			case "raid_finished":
			{
				final Instance world = player.getInstanceWorld();
				if (!isInInstance(world))
				{
					return null;
				}
				
				final NpcStringId destroyedMsg = getRaidDestroyedMsg(world.getParameters().getString("INSTANCE_LEVEL", "110"));
				world.broadcastPacket(new ExShowScreenMessage(destroyedMsg, ExShowScreenMessage.TOP_CENTER, 5000, true));
				// XXX REWARDS: reward distribution
				final int raidId = getRaidId(world.getParameters().getString("INSTANCE_LEVEL", "110"));
				grantBossRewards(world, raidId);
				break;
			}
		}
		
		return null;
	}
	
	/**
	 * Tracks statue HP% as it takes damage and fires every threshold that has been crossed since the last check, in order, so a single big hit that skips several thresholds at once still plays every message/spawn instead of only the lowest one reached.
	 * @param npc The statue being attacked.
	 * @param attacker The attacking {@code Player}.
	 * @param damage Damage dealt.
	 * @param isSummon Whether the attacker's summon landed the hit.
	 */
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	{
		final Instance world = npc.getInstanceWorld();
		if (!isInInstance(world))
		{
			return;
		}
		
		final int hpPercent = (int) ((npc.getCurrentHp() / npc.getMaxHp()) * 100);
		int index = world.getParameters().getInt("STATUE_THRESHOLD_INDEX", 0);
		while ((index < THRESHOLDS.length) && (hpPercent <= THRESHOLDS[index]))
		{
			triggerStatueThreshold(world, npc, THRESHOLDS[index], attacker);
			index++;
		}
		world.getParameters().set("STATUE_THRESHOLD_INDEX", index);
		
		return;
	}
	
	/**
	 * Handles statue and raid boss deaths. On a statue death: shows the "Demon statues destroyed X/4" counter, records the room as cleared, and either opens the next room or (on the 4th statue) queues the final raid. On the raid boss death: queues the "is destroyed" message.
	 * @param npc The {@code Npc} that died.
	 * @param killer The {@code Player} credited with the kill.
	 * @param isSummon Whether a summon landed the killing blow.
	 */
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final Instance world = npc.getInstanceWorld();
		if (!isInInstance(world))
		{
			return;
		}
		
		final String instanceLv = world.getParameters().getString("INSTANCE_LEVEL", "110");
		final int raidId = getRaidId(instanceLv);
		if (npc.getId() == raidId)
		{
			startQuestTimer("raid_finished", RAID_FINISH_DELAY, null, killer);
			return;
		}
		
		// Remove minions when statue is destroyed
		final int[] minions = getMinions(world.getParameters().getString("INSTANCE_LEVEL", "110"));
		world.getAliveNpcs(minions).forEach(minon -> minon.doDie(null));
		
		final String currentRoom = world.getParameters().getString("CURRENT_ROOM", "");
		if (npc.getId() != getStatueId(instanceLv, currentRoom))
		{
			// Not the currently-active statue (stale/duplicate event) - ignore.
			return;
		}
		
		// Guard against this same statue death being processed twice.
		final String deadFlag = "STATUE_DEAD_" + currentRoom;
		if (world.getParameters().getInt(deadFlag, 0) == 1)
		{
			return;
		}
		world.getParameters().set(deadFlag, 1);
		
		final int killedCount = world.getParameters().getInt("STATUES_KILLED", 0) + 1;
		world.getParameters().set("STATUES_KILLED", killedCount);
		world.getParameters().set("LAST_CLEARED_ROOM", currentRoom);
		
		world.broadcastPacket(new ExShowScreenMessage(getStatuesDestroyedMsg(killedCount), ExShowScreenMessage.TOP_CENTER, 5000, true));
		npc.setDisplayEffect(DISPLAY_BROKEN);
		
		if (killedCount >= 4)
		{
			startQuestTimer("spawn_final_raid", NEXT_ROOM_DELAY, null, killer);
		}
		else
		{
			startQuestTimer("next_statue_phase", NEXT_ROOM_DELAY, null, killer);
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
		final Instance world = player.getInstanceWorld();
		if (isInInstance(world))
		{
			if (npc.getId() == INNER_BENUSTA)
			{
				if (player.isInParty())
				{
					final Party party = player.getParty();
					if (!party.isLeader(player))
					{
						return "31464-1.html";
					}
					
					return "31464.html";
				}
				else if (player.isGM())
				{
					return "31464.html";
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
			case RAID_RADIGES:
				rewardBox = REWARD_BOX_120;
				break;
			case RAID_AQUILON:
				rewardBox = REWARD_BOX_130;
				break;
			default: // Raid 110
				rewardBox = REWARD_BOX_110;
				break;
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
	 * This is the main entry point for the {@code CanonCathedral} class.<br>
	 * It initializes a new instance of the script.
	 * @param args Command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		new CanonCathedral();
	}
}