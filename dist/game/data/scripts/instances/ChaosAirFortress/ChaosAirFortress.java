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
package instances.ChaosAirFortress;

import java.util.List;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.instance.Door;
import org.l2jmobius.gameserver.entity.groups.Party;
import org.l2jmobius.gameserver.entity.instancezone.Instance;
import org.l2jmobius.gameserver.entity.item.holders.ItemHolder;
import org.l2jmobius.gameserver.mechanics.script.InstanceScript;
import org.l2jmobius.gameserver.mechanics.variables.NpcVariables;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.network.serverpackets.OnEventTrigger;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Handles the logic and progression for the {@code ChaosAirFortress} instance.<br>
 * The party picks a difficulty from Benusta, clears the zones of the Upper Deck while breaking its three generators, repeats it on the Lower Deck with two more, then fights the golem the last generator releases.
 * @author CostyKiller
 */
public class ChaosAirFortress extends InstanceScript
{
	// NPCs
	private static final int BENUSTA = 34542;
	private static final int INNER_BENUSTA = 31462;
	
	// Bosses
	private static final int CHAOS_FORTRESS_GOLEM_1_110 = 26624;
	private static final int CHAOS_FORTRESS_GOLEM_2_120 = 26629;
	private static final int CHAOS_FORTRESS_GOLEM_3_130 = 26634;
	
	// Gunners guard the generators and call their guards in on the first hit they take.
	private static final int GUNNER_110 = 26620;
	private static final int GUNNER_120 = 26625;
	private static final int GUNNER_130 = 26630;
	
	// Called in by a Gunner, never spawned with the zone.
	private static final int GUARDIAN_110 = 26621;
	private static final int DEFENDER_110 = 26622;
	private static final int GUARDIAN_120 = 26626;
	private static final int DEFENDER_120 = 26627;
	private static final int GUARDIAN_130 = 26631;
	private static final int DEFENDER_130 = 26632;
	
	// How many of each a generator calls in when its guard is first hit, indexed by zone, kept per level so each difficulty tunes alone.
	private static final int[] GUARDIANS_PER_ZONE_110 =
	{
		4,
		6,
		8
	};
	private static final int[] DEFENDERS_PER_ZONE_110 =
	{
		0,
		4,
		8
	};
	private static final int[] GUARDIANS_PER_ZONE_120 =
	{
		6,
		8,
		10
	};
	private static final int[] DEFENDERS_PER_ZONE_120 =
	{
		0,
		6,
		10
	};
	private static final int[] GUARDIANS_PER_ZONE_130 =
	{
		8,
		10,
		12
	};
	private static final int[] DEFENDERS_PER_ZONE_130 =
	{
		0,
		8,
		10
	};
	
	// A Gunner only calls minions when it stands this close to a generator.
	private static final int GUARD_RADIUS = 500;
	// Guards stand on a ring this far from the generator, spaced evenly so none of them overlap.
	private static final int GUARD_SPAWN_RADIUS = 250;
	
	// Generators
	private static final int UPPER_GENERATOR_110 = 18044;
	private static final int LOWER_GENERATOR_110 = 18045;
	private static final int UPPER_GENERATOR_120 = 18046;
	private static final int LOWER_GENERATOR_120 = 18047;
	private static final int UPPER_GENERATOR_130 = 18048;
	private static final int LOWER_GENERATOR_130 = 18049;
	private static final int[] UPPER_GENERATORS =
	{
		UPPER_GENERATOR_110,
		UPPER_GENERATOR_120,
		UPPER_GENERATOR_130
	};
	private static final int[] LOWER_GENERATORS =
	{
		LOWER_GENERATOR_110,
		LOWER_GENERATOR_120,
		LOWER_GENERATOR_130
	};
	private static final int UPPER_GENERATOR_COUNT = 3;
	private static final int LOWER_GENERATOR_COUNT = 2;
	
	// The Upper Deck is one open area ending at a single door on top of the ramp down.
	// DoorData holds three more at x 176000, the mirrored half of the map this instance never reaches.
	private static final int[] BARRIER_LOWER_DECK =
	{
		25200106
	};
	private static final int[] BARRIER_CHAOS_CORE =
	{
		25200104,
		25200102
	};
	
	// The lightning wall is a client effect rather than the door itself, two per gate, one per face.
	// 25200212 is not a gate face, it is the arc across the Chaos Core approach.
	private static final int[] EFFECT_LOWER_DECK =
	{
		25200208,
		25200210
	};
	private static final int[] EFFECT_CHAOS_CORE =
	{
		25200204,
		25200206,
		25200212,
		25200200,
		25200202
	};
	
	// Items
	private static final ItemHolder BENUSTAS_REWARD_PACK_LV1_110 = new ItemHolder(82453, 1);
	private static final ItemHolder BENUSTAS_REWARD_PACK_LV3_120 = new ItemHolder(82455, 1);
	private static final ItemHolder BENUSTAS_REWARD_PACK_LV5_130 = new ItemHolder(83729, 1);
	private static final ItemHolder NOVICE_WANDERERS_TALES = new ItemHolder(83575, 2);
	
	// Misc
	private static final int TEMPLATE_ID = 362;
	private static final int ZONE_COUNT = 3; // Upper Deck before and after the bridge, then the Lower Deck.
	private static final int TAUNT_COUNT = 4;
	private static final int TAUNT_DELAY = 15000; // Guessed, the video only shows that the four taunts loop.
	private static final int REPLY_DELAY = 2000;
	
	public ChaosAirFortress()
	{
		super(TEMPLATE_ID);
		addStartNpc(BENUSTA);
		addFirstTalkId(INNER_BENUSTA);
		addAttackId(GUNNER_110, GUNNER_120, GUNNER_130);
		addKillId(UPPER_GENERATOR_110, LOWER_GENERATOR_110, UPPER_GENERATOR_120, LOWER_GENERATOR_120, UPPER_GENERATOR_130, LOWER_GENERATOR_130);
		addKillId(CHAOS_FORTRESS_GOLEM_1_110, CHAOS_FORTRESS_GOLEM_2_120, CHAOS_FORTRESS_GOLEM_3_130);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "31462-1.html":
			case "31462-2.html":
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
				if (!isInInstance(world) || (world.getAliveNpcCount(INNER_BENUSTA) == 0))
				{
					return null;
				}
				
				final StatSet parameters = world.getParameters();
				final Npc benusta = world.getNpc(INNER_BENUSTA);
				final int benustaText = parameters.getInt("BENUSTA_TEXT", 0);
				if (benustaText == 0)
				{
					showOnScreenMsg(world, NpcStringId.THE_PARTY_LEADER_SHOULD_TALK_TO_ME_AND_SELECT_THE_DIFFICULTY_LEVEL, ExShowScreenMessage.TOP_CENTER, 5000, true);
					benusta.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.THE_PARTY_LEADER_SHOULD_TALK_TO_ME_AND_SELECT_THE_DIFFICULTY_LEVEL);
					startQuestTimer("benusta_texts", 4000, null, player);
					parameters.set("BENUSTA_TEXT", 1);
				}
				else if (benustaText == 1)
				{
					showOnScreenMsg(world, NpcStringId.TAKE_INTO_ACCOUNT_THE_ABILITIES_OF_YOUR_PARTY_MEMBERS_WHEN_SELECTING_THE_DIFFICULTY, ExShowScreenMessage.TOP_CENTER, 5000, true);
					benusta.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.TAKE_INTO_ACCOUNT_THE_ABILITIES_OF_YOUR_PARTY_MEMBERS_WHEN_SELECTING_THE_DIFFICULTY);
					startQuestTimer("benusta_texts", 4000, null, player);
					parameters.set("BENUSTA_TEXT", 2);
				}
				else
				{
					showOnScreenMsg(world, NpcStringId.THE_SELECTED_DIFFICULTY_CANNOT_BE_CHANGED_SO_CHOOSE_WISELY, ExShowScreenMessage.TOP_CENTER, 5000, true);
					benusta.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.THE_SELECTED_DIFFICULTY_CANNOT_BE_CHANGED_SO_CHOOSE_WISELY);
					parameters.set("BENUSTA_TEXT", 0);
				}
				break;
			}
			case "easyInstance":
			{
				startRun(player, "110", NpcStringId.LV_110_IS_SELECTED_PREPARE_FOR_BATTLE_2);
				break;
			}
			case "mediumInstance":
			{
				startRun(player, "120", NpcStringId.LV_120_IS_SELECTED_PREPARE_FOR_BATTLE_2);
				break;
			}
			case "hardInstance":
			{
				startRun(player, "130", NpcStringId.LV_130_IS_SELECTED_PREPARE_FOR_BATTLE_2);
				break;
			}
			case "fortress_taunts":
			{
				final Instance world = player.getInstanceWorld();
				if (!isInInstance(world))
				{
					return null;
				}
				
				final StatSet parameters = world.getParameters();
				
				// The taunts run until the party breaks the first generator.
				if (parameters.getInt("UPPER_KILLED", 0) > 0)
				{
					return null;
				}
				
				final int taunt = parameters.getInt("TAUNT", 0);
				switch (taunt)
				{
					case 0:
					{
						showOnScreenMsg(world, NpcStringId.DESTROY_THE_GENERATORS_THAT_POWER_THE_AIR_FORTRESS_AND_FALL_BACK, ExShowScreenMessage.TOP_CENTER, 5000, true);
						break;
					}
					case 1:
					{
						showOnScreenMsg(world, NpcStringId.DON_T_LOSE_FOCUS_THE_FORTRESS_MUST_BE_FINISHED_AS_SOON_AS_POSSIBLE, ExShowScreenMessage.TOP_CENTER, 5000, true);
						break;
					}
					case 2:
					{
						showOnScreenMsg(world, NpcStringId.CHAOS_FORTRESS_IS_SO_BEAUTIFUL, ExShowScreenMessage.TOP_CENTER, 5000, true);
						break;
					}
					default:
					{
						showOnScreenMsg(world, NpcStringId.MOVE_YOU_SLACKERS_OR_YOU_LL_MISS_THE_DEADLINE_ONCE_AGAIN, ExShowScreenMessage.TOP_CENTER, 5000, true);
						break;
					}
				}
				
				parameters.set("TAUNT", (taunt + 1) % TAUNT_COUNT);
				startQuestTimer("fortress_taunts", TAUNT_DELAY, null, player);
				break;
			}
			case "spawn_raid":
			{
				final Instance world = player.getInstanceWorld();
				if (!isInInstance(world))
				{
					return null;
				}
				
				world.spawnGroup("RAID_" + world.getParameters().getString("INSTANCE_LEVEL", "110"));
				world.setStatus(3);
				break;
			}
			default:
			{
				break;
			}
		}
		
		return null;
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		if (isInInstance(player.getInstanceWorld()))
		{
			if (player.isInParty())
			{
				if (!player.getParty().isLeader(player))
				{
					return "31462-1.html";
				}
				
				return "31462.html";
			}
			else if (player.isGM())
			{
				return "31462.html";
			}
		}
		
		return null;
	}
	
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	{
		final Instance world = npc.getInstanceWorld();
		if (!isInInstance(world))
		{
			return;
		}
		
		switch (npc.getId())
		{
			case GUNNER_110:
			case GUNNER_120:
			case GUNNER_130:
			{
				break;
			}
			default:
			{
				return;
			}
		}
		
		// The guards belong to the generator, so they come once however many of its Gunners get hit.
		final Npc generator = findGuardedGenerator(world, npc);
		if (generator == null)
		{
			return;
		}
		
		final NpcVariables variables = generator.getVariables();
		if (variables.getBoolean("GUARDS_CALLED", false))
		{
			return;
		}
		
		variables.set("GUARDS_CALLED", true);
		
		// Plain spawns rather than minions, because killing the Gunner does not clear them in retail.
		// They ring the generator evenly spaced and go straight for whoever hit the Gunner.
		final String level = world.getParameters().getString("INSTANCE_LEVEL", "110");
		final int zone = getZone(world, npc, level) - 1;
		final int guardians = getGuardiansPerZone(level)[zone];
		final int total = guardians + getDefendersPerZone(level)[zone];
		for (int i = 0; i < total; i++)
		{
			final double angle = (2 * Math.PI * i) / total;
			final int x = generator.getX() + (int) (GUARD_SPAWN_RADIUS * Math.cos(angle));
			final int y = generator.getY() + (int) (GUARD_SPAWN_RADIUS * Math.sin(angle));
			addAttackPlayerDesire(addSpawn(i < guardians ? getGuardianId(level) : getDefenderId(level), x, y, generator.getZ(), generator.getHeading(), false, 0, false, world.getId()), attacker);
		}
	}
	
	/**
	 * Finds the generator this Gunner stands guard over.
	 * @param world The instance world.
	 * @param npc The Gunner being attacked.
	 * @return The generator within {@code GUARD_RADIUS}, or {@code null} when it guards none.
	 */
	private Npc findGuardedGenerator(Instance world, Npc npc)
	{
		for (Npc generator : world.getNpcs(UPPER_GENERATORS))
		{
			if (npc.isInsideRadius3D(generator, GUARD_RADIUS))
			{
				return generator;
			}
		}
		
		for (Npc generator : world.getNpcs(LOWER_GENERATORS))
		{
			if (npc.isInsideRadius3D(generator, GUARD_RADIUS))
			{
				return generator;
			}
		}
		
		return null;
	}
	
	/**
	 * Finds which zone group spawned this npc.
	 * @param world The instance world.
	 * @param npc The npc to place.
	 * @param level The chosen difficulty.
	 * @return The zone number, 1 to {@code ZONE_COUNT}, defaulting to 1.
	 */
	private int getZone(Instance world, Npc npc, String level)
	{
		for (int zone = 1; zone <= ZONE_COUNT; zone++)
		{
			if (world.getNpcsOfGroup("ZONE_" + zone + "_" + level).contains(npc))
			{
				return zone;
			}
		}
		
		return 1;
	}
	
	private static int[] getGuardiansPerZone(String level)
	{
		if (level.equals("120"))
		{
			return GUARDIANS_PER_ZONE_120;
		}
		
		if (level.equals("130"))
		{
			return GUARDIANS_PER_ZONE_130;
		}
		
		return GUARDIANS_PER_ZONE_110;
	}
	
	private static int[] getDefendersPerZone(String level)
	{
		if (level.equals("120"))
		{
			return DEFENDERS_PER_ZONE_120;
		}
		
		if (level.equals("130"))
		{
			return DEFENDERS_PER_ZONE_130;
		}
		
		return DEFENDERS_PER_ZONE_110;
	}
	
	private static int getGuardianId(String level)
	{
		if (level.equals("120"))
		{
			return GUARDIAN_120;
		}
		
		if (level.equals("130"))
		{
			return GUARDIAN_130;
		}
		
		return GUARDIAN_110;
	}
	
	private static int getDefenderId(String level)
	{
		if (level.equals("120"))
		{
			return DEFENDER_120;
		}
		
		if (level.equals("130"))
		{
			return DEFENDER_130;
		}
		
		return DEFENDER_110;
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isPet)
	{
		final Instance world = npc.getInstanceWorld();
		if (!isInInstance(world))
		{
			return;
		}
		
		final int npcId = npc.getId();
		switch (npcId)
		{
			case UPPER_GENERATOR_110:
			case UPPER_GENERATOR_120:
			case UPPER_GENERATOR_130:
			{
				onUpperGeneratorKill(world);
				break;
			}
			case LOWER_GENERATOR_110:
			case LOWER_GENERATOR_120:
			case LOWER_GENERATOR_130:
			{
				onLowerGeneratorKill(world, killer);
				break;
			}
			case CHAOS_FORTRESS_GOLEM_1_110:
			case CHAOS_FORTRESS_GOLEM_2_120:
			case CHAOS_FORTRESS_GOLEM_3_130:
			{
				onGolemKill(world, npcId);
				break;
			}
		}
	}
	
	/**
	 * Advances the Upper Deck, announcing the damage and opening the barrier that generator held.
	 * @param world The instance world.
	 */
	private void onUpperGeneratorKill(Instance world)
	{
		final StatSet parameters = world.getParameters();
		final int killed = parameters.getInt("UPPER_KILLED", 0) + 1;
		parameters.set("UPPER_KILLED", killed);
		switch (killed)
		{
			case 1:
			{
				announce(world, NpcStringId.ONE_OF_THE_UPPER_DECK_GENERATORS_IS_BROKEN_URGENT_MAINTENANCE_AND_REPAIRS_ARE_REQUIRED, NpcStringId.THE_UPPER_DECK_GENERATOR_HAS_FAILED_REPAIR_IT_AS_SOON_AS_POSSIBLE);
				break;
			}
			case 2:
			{
				announce(world, NpcStringId.TWO_OF_THE_UPPER_DECK_GENERATORS_ARE_HEAVILY_BROKEN_THE_FAILURE_CAUSE_MUST_BE_ELIMINATED, NpcStringId.WHO_S_BREAKING_OUR_GENERATORS_WHY_HAVEN_T_YOU_STOPPED_THEM_DO_IT_NOW);
				break;
			}
			case UPPER_GENERATOR_COUNT:
			{
				announce(world, NpcStringId.ALL_OF_THE_UPPER_DECK_GENERATORS_HAVE_FAILED_THE_LOWER_DECK_BARRIER_IS_DEACTIVATED, NpcStringId.ALL_OF_THE_UPPER_DECK_GENERATORS_ARE_DESTROYED_WE_LL_NEVER_FINISH_THE_AIR_FORTRESS_ON_TIME);
				openBarrier(world, BARRIER_LOWER_DECK, EFFECT_LOWER_DECK);
				break;
			}
			default:
			{
				break;
			}
		}
	}
	
	/**
	 * Advances the Lower Deck and, on the last generator, releases the golem.
	 * @param world The instance world.
	 * @param killer The player who landed the killing blow, used to time the golem spawn.
	 */
	private void onLowerGeneratorKill(Instance world, Player killer)
	{
		final StatSet parameters = world.getParameters();
		final int killed = parameters.getInt("LOWER_KILLED", 0) + 1;
		parameters.set("LOWER_KILLED", killed);
		if (killed == 1)
		{
			announce(world, NpcStringId.ONE_OF_THE_LOWER_DECK_GENERATORS_HAS_FAILED_THE_GENERATORS_POWER_HAS_BEEN_REDUCED_FOR_50, NpcStringId.THE_LOWER_DECK_GENERATORS_ARE_UNDER_ATTACK_THE_ENEMIES_MUST_BE_STOPPED_AT_ANY_COST);
		}
		else if (killed == LOWER_GENERATOR_COUNT)
		{
			announce(world, NpcStringId.ALL_OF_THE_LOWER_DECK_GENERATORS_HAVE_FAILED_THE_BARRIER_THAT_PROTECTS_THE_GENERATOR_AND_THE_LOWER_DECK_VENTILATOR_IS_DEACTIVATED, NpcStringId.ALL_OF_THE_LOWER_DECK_GENERATORS_ARE_DESTROYED_TIME_TO_ACTIVATE_MY_STRONGEST_GOLEMS);
			openBarrier(world, BARRIER_CHAOS_CORE, EFFECT_CHAOS_CORE);
			startQuestTimer("spawn_raid", 5000, null, killer);
		}
	}
	
	/**
	 * Pays out the victory reward to every party member inside and closes the instance.
	 * @param world The instance world.
	 * @param golemId The id of the defeated golem, which decides the reward pack.
	 */
	private void onGolemKill(Instance world, int golemId)
	{
		final ItemHolder rewardPack;
		if (golemId == CHAOS_FORTRESS_GOLEM_2_120)
		{
			rewardPack = BENUSTAS_REWARD_PACK_LV3_120;
		}
		else if (golemId == CHAOS_FORTRESS_GOLEM_3_130)
		{
			rewardPack = BENUSTAS_REWARD_PACK_LV5_130;
		}
		else
		{
			rewardPack = BENUSTAS_REWARD_PACK_LV1_110;
		}
		
		// Every member inside is paid, even one who dealt no damage.
		for (Player member : world.getPlayers())
		{
			if ((member.getParty() != null) || member.isGM())
			{
				giveItems(member, rewardPack);
				giveItems(member, NOVICE_WANDERERS_TALES);
			}
		}
		
		announce(world, NpcStringId.THE_ENEMY_HAS_DESTROYED_THE_CHAOS_CORE_AND_ESCAPED_I_LL_NEED_A_YEAR_TO_REPAIR_THE_FORTRESS, NpcStringId.THE_GOLEMS_ARE_DEFEATED_AND_YOU_CAN_ESCAPE_NOW_LEAVE_THE_AIR_FORTRESS);
		world.setStatus(4);
		world.finishInstance();
	}
	
	/**
	 * Locks the difficulty in, clears Benusta away and spawns the whole fortress.
	 * @param player The party leader who picked the difficulty.
	 * @param level The chosen level, one of {@code 110}, {@code 120} or {@code 130}.
	 * @param message The on screen confirmation of the choice.
	 */
	private void startRun(Player player, String level, NpcStringId message)
	{
		final Instance world = player.getInstanceWorld();
		if (!isInInstance(world))
		{
			return;
		}
		
		world.getParameters().set("INSTANCE_LEVEL", level);
		showOnScreenMsg(world, message, ExShowScreenMessage.TOP_CENTER, 8000, true);
		
		final Npc benusta = world.getNpc(INNER_BENUSTA);
		if (benusta != null)
		{
			benusta.deleteMe();
		}
		
		for (int zone = 1; zone <= ZONE_COUNT; zone++)
		{
			world.spawnGroup("ZONE_" + zone + "_" + level);
		}
		
		// A generator is a machine bolted to the deck, it soaks hits but never swings back, and no knockback shifts it.
		for (Npc generator : world.spawnGroup("UPPER_GENERATOR_" + level))
		{
			generator.disableCoreAI(true);
			generator.setImmobilized(true);
		}
		
		for (Npc generator : world.spawnGroup("LOWER_GENERATOR_" + level))
		{
			generator.disableCoreAI(true);
			generator.setImmobilized(true);
		}
		
		// Raise both lightning walls, each one falls when its gate opens.
		showWall(world, EFFECT_LOWER_DECK, true);
		showWall(world, EFFECT_CHAOS_CORE, true);
		world.setStatus(1);
		world.setReenterTime();
		startQuestTimer("fortress_taunts", 2000, null, player);
	}
	
	/**
	 * Shows the red warning at once and the white taunt two seconds behind it.
	 * @param world The instance world.
	 * @param warning The red message.
	 * @param taunt The white message that follows.
	 */
	private void announce(Instance world, NpcStringId warning, NpcStringId taunt)
	{
		world.broadcastPacket(new ExShowScreenMessage(warning, ExShowScreenMessage.TOP_CENTER, 5000, ExShowScreenMessage.COLOR_RED));
		ThreadPool.schedule(() -> world.broadcastPacket(new ExShowScreenMessage(taunt, ExShowScreenMessage.TOP_CENTER, 5000, ExShowScreenMessage.COLOR_WHITE)), REPLY_DELAY);
	}
	
	/**
	 * Drops a barrier, killing both faces of its lightning wall before opening the door behind it.
	 * @param world The instance world.
	 * @param doorIds The ids of the doors making up the gate.
	 * @param effectIds The ids of the client effects drawing the wall.
	 */
	private void openBarrier(Instance world, int[] doorIds, int[] effectIds)
	{
		showWall(world, effectIds, false);
		
		for (int doorId : doorIds)
		{
			final Door door = world.getDoor(doorId);
			if (door != null)
			{
				door.openMe();
			}
		}
	}
	
	/**
	 * Switches both faces of one lightning wall on or off.
	 * @param world The instance world.
	 * @param effectIds The ids of the client effects drawing the wall.
	 * @param visible {@code true} to raise the wall.
	 */
	private void showWall(Instance world, int[] effectIds, boolean visible)
	{
		for (int effectId : effectIds)
		{
			world.broadcastPacket(new OnEventTrigger(effectId, visible));
		}
	}
	
	public static void main(String[] args)
	{
		new ChaosAirFortress();
	}
}
