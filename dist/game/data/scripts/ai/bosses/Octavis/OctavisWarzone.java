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
package ai.bosses.Octavis;

import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Attackable;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.instancezone.Instance;
import org.l2jmobius.gameserver.entity.zone.ZoneType;
import org.l2jmobius.gameserver.entity.zone.type.ScriptZone;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.mechanics.script.InstanceScript;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.network.enums.Movie;
import org.l2jmobius.gameserver.network.serverpackets.ExShowUsm;
import org.l2jmobius.gameserver.taskmanagers.WalkingTaskManager;
import org.l2jmobius.gameserver.util.ArrayUtil;
import org.l2jmobius.gameserver.util.LocationUtil;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Defines the logic and behavior for the {@code Octavis} warzone instance.<br>
 * This class handles the specific mechanics of the zone as an {@code InstanceScript}.
 * @author St3eT, Mobius
 */
public class OctavisWarzone extends InstanceScript
{
	// NPCs
	private static final int[] OCTAVIS_STAGE_1 =
	{
		29191, // Common
		29209, // Extreme
	};
	private static final int[] OCTAVIS_STAGE_2 =
	{
		29193, // Common
		29211, // Extreme
	};
	private static final int[] OCTAVIS_STAGE_3 =
	{
		29194, // Common
		29212, // Extreme
	};
	private static final int[] BEASTS =
	{
		29192, // Common
		29210, // Extreme
	};
	private static final int[] BEASTS_MINIONS =
	{
		22929, // Common
		23087, // Extreme
	};
	private static final int[] GLADIATORS =
	{
		22928, // Common
		23086, // Extreme
	};
	private static final int LYDIA = 32892;
	private static final int DOOR_MANAGER = 18984;
	
	// Skills
	private static final SkillHolder STAGE_2_SKILL_1 = new SkillHolder(14026, 1);
	private static final SkillHolder STAGE_2_SKILL_2 = new SkillHolder(14027, 1);
	private static final SkillHolder STAGE_2_SKILL_3 = new SkillHolder(14575, 1);
	
	// Locations
	private static final Location BATTLE_LOC = new Location(208720, 120576, -10000);
	private static final Location OCTAVIS_SPAWN_LOC = new Location(207069, 120580, -9987);
	private static final Location BEASTS_RANDOM_POINT = new Location(207244, 120579, -10008);
	private static final Location[] BEASTS_MINIONS_LOC =
	{
		new Location(206681, 119327, -9987),
		new Location(207724, 119303, -9987),
		new Location(208472, 120047, -9987),
		new Location(208484, 121110, -9987),
		new Location(207730, 121859, -9987),
		new Location(206654, 121865, -9987),
	};
	
	// Zones
	private static final ScriptZone TELEPORT_ZONE = ZoneManager.getInstance().getZoneById(12042, ScriptZone.class);
	
	// Misc
	private static final int TEMPLATE_ID = 180;
	private static final int EXTREME_TEMPLATE_ID = 181;
	private static final int MAIN_DOOR_1 = 26210002;
	private static final int MAIN_DOOR_2 = 26210001;
	
	/**
	 * Initializes the {@code OctavisWarzone} instance script.<br>
	 * This constructor sets up all required NPCs and event IDs.<br>
	 * It configures the starting conditions for both common and extreme modes.
	 */
	public OctavisWarzone()
	{
		super(TEMPLATE_ID, EXTREME_TEMPLATE_ID);
		addStartNpc(LYDIA);
		addTalkId(LYDIA);
		addSpawnId(GLADIATORS);
		addAttackId(OCTAVIS_STAGE_1);
		addAttackId(OCTAVIS_STAGE_2);
		addAttackId(BEASTS);
		addKillId(OCTAVIS_STAGE_1);
		addKillId(OCTAVIS_STAGE_2);
		addKillId(OCTAVIS_STAGE_3);
		addMoveFinishedId(GLADIATORS);
		addSpellFinishedId(OCTAVIS_STAGE_2);
		addEnterZoneId(TELEPORT_ZONE.getId());
		addCreatureSeeId(DOOR_MANAGER);
		addInstanceCreatedId(TEMPLATE_ID, EXTREME_TEMPLATE_ID);
	}
	
	/**
	 * Handles specific events triggered within the instance zone.<br>
	 * This method processes logic for entering or re-entering instances based on the event name.
	 * @param event The name of the event to be processed.
	 * @param npc The {@code Npc} associated with the event.
	 * @param player The {@code Player} involved in the event.
	 * @return A string representing the result or a HTML file path, or {@code null}.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "enterEasyInstance":
			{
				enterInstance(player, npc, TEMPLATE_ID);
				break;
			}
			case "enterExtremeInstance":
			{
				enterInstance(player, npc, EXTREME_TEMPLATE_ID);
				break;
			}
			case "reenterInstance":
			{
				final Instance activeInstance = getPlayerInstance(player);
				if (isInInstance(activeInstance))
				{
					enterInstance(player, npc, activeInstance.getTemplateId());
					return "PartyMemberReenter.html";
				}
				break;
			}
		}
		
		return super.onEvent(event, npc, player);
	}
	
	/**
	 * Handles logic for specific timed events within the Octavis Warzone instance.<br>
	 * This method executes different actions based on the provided {@code event} string.<br>
	 * It manages stage transitions, NPC behaviors, and cinematic triggers.
	 * @param event The name of the timer event to process.
	 * @param params A set of parameters used for calculations or state checks.
	 * @param npc The {@code Npc} entity associated with the timer.
	 * @param player The {@code Player} involved in the current instance context.
	 */
	@Override
	public void onTimerEvent(String event, StatSet params, Npc npc, Player player)
	{
		switch (event)
		{
			case "SECOND_DOOR_OPEN":
			{
				final Instance world = npc.getInstanceWorld();
				if (!isInInstance(world))
				{
					return;
				}
				
				world.openCloseDoor(MAIN_DOOR_2, true);
				break;
			}
			case "CLOSE_DOORS":
			{
				final Instance world = npc.getInstanceWorld();
				if (!isInInstance(world))
				{
					return;
				}
				
				world.openCloseDoor(MAIN_DOOR_2, false);
				world.openCloseDoor(MAIN_DOOR_1, false);
				world.setParameter("TELEPORT_ACTIVE", true);
				npc.teleToLocation(BATTLE_LOC);
				playMovie(world, Movie.SC_OCTABIS_OPENING);
				getTimers().addTimer("START_STAGE_1", 26500, npc, null);
				break;
			}
			case "START_STAGE_1":
			{
				final Instance world = npc.getInstanceWorld();
				if (!isInInstance(world))
				{
					return;
				}
				
				world.spawnGroup("STAGE_1");
				world.getAliveNpcs(BEASTS).forEach(beasts ->
				{
					beasts.disableCoreAI(true);
					beasts.setUndying(true);
					beasts.asAttackable().setCanReturnToSpawnPoint(false);
					final Npc octavis = addSpawn((!isExtremeMode(world) ? OCTAVIS_STAGE_1[0] : OCTAVIS_STAGE_1[1]), OCTAVIS_SPAWN_LOC, false, 0, false, world.getId());
					octavis.disableCoreAI(true);
					octavis.setRunning();
					octavis.sendChannelingEffect(beasts, 1);
					octavis.setTargetable(false);
					octavis.asAttackable().setCanReturnToSpawnPoint(false);
					getTimers().addRepeatingTimer("FOLLOW_BEASTS", 500, octavis, null);
					getTimers().addRepeatingTimer("BEASTS_CHECK_HP", 5000, beasts, null);
					WalkingTaskManager.getInstance().startMoving(beasts, "octabis_superpoint");
				});
				break;
			}
			case "FOLLOW_BEASTS":
			{
				final Instance world = npc.getInstanceWorld();
				if (!isInInstance(world))
				{
					return;
				}
				
				world.getAliveNpcs(BEASTS).forEach(beasts ->
				{
					addMoveToDesire(npc, beasts.getLocation(), 23);
					npc.sendChannelingEffect(beasts, 1);
				});
				break;
			}
			case "BEASTS_CHECK_HP":
			{
				final Instance world = npc.getInstanceWorld();
				if (!isInInstance(world))
				{
					return;
				}
				
				final int hpPer = npc.getCurrentHpPercent();
				if ((hpPer < 50) && npc.isScriptValue(0))
				{
					npc.getStat().addFixedValue(Stat.REGENERATE_HP_RATE, 100d); // On original script 95000d was added, making Octavis targetable for two seconds.
					npc.setScriptValue(1);
				}
				else if ((hpPer > 90) && npc.isScriptValue(1))
				{
					npc.getStat().addFixedValue(Stat.REGENERATE_HP_RATE, 0d);
					npc.setScriptValue(0);
				}
				
				final Npc octavis = world.getAliveNpcs(OCTAVIS_STAGE_1).stream().findAny().orElse(null);
				if (octavis != null)
				{
					octavis.setTargetable(hpPer < 50);
				}
				break;
			}
			case "END_STAGE_1":
			{
				final Instance world = player.getInstanceWorld();
				if (!isInInstance(world))
				{
					return;
				}
				
				playMovie(world, Movie.SC_OCTABIS_PHASECH_A);
				getTimers().addTimer("START_STAGE_2", 12000, null, player);
				break;
			}
			case "START_STAGE_2":
			{
				final Instance world = player.getInstanceWorld();
				if (!isInInstance(world))
				{
					return;
				}
				
				world.spawnGroup("STAGE_2").forEach(octavis -> octavis.asAttackable().setCanReturnToSpawnPoint(false));
				break;
			}
			case "END_STAGE_2":
			{
				final Instance world = player.getInstanceWorld();
				if (!isInInstance(world))
				{
					return;
				}
				
				playMovie(world, Movie.SC_OCTABIS_PHASECH_B);
				getTimers().addTimer("START_STAGE_3", 15000, null, player);
				break;
			}
			case "START_STAGE_3":
			{
				final Instance world = player.getInstanceWorld();
				if (!isInInstance(world))
				{
					return;
				}
				
				world.spawnGroup("STAGE_3").forEach(octavis -> octavis.asAttackable().setCanReturnToSpawnPoint(false));
				break;
			}
			case "END_STAGE_3":
			{
				final Instance world = player.getInstanceWorld();
				if (!isInInstance(world))
				{
					return;
				}
				
				playMovie(world, Movie.SC_OCTABIS_ENDING);
				getTimers().addTimer("USM_SCENE_TIMER", 40000, null, player);
				break;
			}
			case "USM_SCENE_TIMER":
			{
				final Instance world = player.getInstanceWorld();
				if (!isInInstance(world))
				{
					return;
				}
				
				world.broadcastPacket(ExShowUsm.OCTAVIS_INSTANCE_END);
				break;
			}
			case "GLADIATOR_START_SPAWN":
			{
				final Instance world = npc.getInstanceWorld();
				if (!isInInstance(world))
				{
					return;
				}
				
				final StatSet npcVars = npc.getVariables();
				final int spawnIndex = npcVars.getInt("SPAWN_INDEX", 1);
				if (spawnIndex < 7)
				{
					if (isExtremeMode(world))
					{
						world.spawnGroup("magmeld4_2621_gro" + spawnIndex + "m1");
					}
					else
					{
						world.spawnGroup("magmeld4_2621_gmo" + spawnIndex + "m1");
					}
					
					npcVars.set("SPAWN_INDEX", spawnIndex + 1);
					getTimers().addTimer("GLADIATOR_START_SPAWN", 3000, npc, null);
				}
				break;
			}
			case "GLADIATOR_MOVING":
			{
				final Instance world = npc.getInstanceWorld();
				if (!isInInstance(world))
				{
					return;
				}
				
				final StatSet npcParams = npc.getParameters();
				final int moveX = npcParams.getInt("Move_to_X", 0);
				final int moveY = npcParams.getInt("Move_to_Y", 0);
				if ((moveX != 0) && (moveY != 0))
				{
					npc.setRunning();
					addMoveToDesire(npc, new Location(moveX, moveY, -10008), 23);
				}
				break;
			}
			case "BEASTS_MINIONS_SPAWN":
			{
				final Instance world = npc.getInstanceWorld();
				if (!isInInstance(world))
				{
					return;
				}
				
				for (int i = 0; i < getRandom(10); i++)
				{
					final Npc beast = addSpawn((!isExtremeMode(world) ? BEASTS_MINIONS[0] : BEASTS_MINIONS[1]), getRandomEntry(BEASTS_MINIONS_LOC), false, 0, false, world.getId());
					beast.setRunning();
					beast.asAttackable().setCanReturnToSpawnPoint(false);
					addMoveToDesire(beast, LocationUtil.getRandomLocation(BEASTS_RANDOM_POINT, 500, 500), 23);
				}
				
				getTimers().addTimer("BEASTS_MINIONS_SPAWN", 30000 + (getRandom(10) * 1000), npc, null);
				break;
			}
			case "MINION_CALL":
			{
				final Creature mostHated = npc.asAttackable().getMostHated();
				if ((mostHated != null) && mostHated.isPlayer() && (npc.calculateDistance3D(npc) < 5000))
				{
					World.forEachVisibleObjectInRange(npc, Attackable.class, 4000, minion ->
					{
						if (ArrayUtil.contains(BEASTS_MINIONS, minion.getId()) || ArrayUtil.contains(GLADIATORS, minion.getId()))
						{
							addAttackPlayerDesire(minion, mostHated.asPlayer(), 23);
						}
					});
				}
				
				getTimers().addTimer("MINION_CALL", 5000 + (getRandom(5) * 1000), npc, null);
				break;
			}
			case "ATTACK_TIMER":
			{
				final Creature mostHated = npc.asAttackable().getMostHated();
				if ((mostHated != null) && mostHated.isPlayable() && (npc.calculateDistance2D(mostHated) < 1000))
				{
					final int random = getRandom(5);
					if (random < 3)
					{
						addSkillCastDesire(npc, mostHated, STAGE_2_SKILL_1, 23);
					}
					else if (random < 5)
					{
						addSkillCastDesire(npc, mostHated, STAGE_2_SKILL_2, 23);
					}
				}
				
				getTimers().addTimer("ATTACK_TIMER", getRandom(7, 9) * 1000, npc, null);
				break;
			}
			case "MEDUSA_SKILL_TIMER":
			{
				addSkillCastDesire(npc, npc, STAGE_2_SKILL_3, 23);
				break;
			}
		}
	}
	
	/**
	 * Handles logic when a player attacks an {@code Npc}.<br>
	 * This method updates visual effects and timers based on the NPC's health.<br>
	 * It also manages specific behaviors for different stages of the instance.
	 * @param npc The {@code Npc} being attacked.
	 * @param attacker The {@code Player} performing the attack.
	 * @param damage The amount of damage dealt.
	 * @param isSummon Whether the attack was performed by a summon.
	 */
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	{
		final Instance world = npc.getInstanceWorld();
		if (isInInstance(world))
		{
			final int hpPer = npc.getCurrentHpPercent();
			final int npcId = npc.getId();
			switch (npcId)
			{
				case 29191: // Common
				case 29209: // Extreme
				{
					if (hpPer >= 90)
					{
						npc.setDisplayEffect(0);
					}
					else if (hpPer >= 80)
					{
						npc.setDisplayEffect(1);
					}
					else if (hpPer >= 70)
					{
						npc.setDisplayEffect(2);
					}
					else if (hpPer >= 60)
					{
						npc.setDisplayEffect(3);
					}
					else if (hpPer >= 50)
					{
						npc.setDisplayEffect(4);
					}
					else
					{
						npc.setDisplayEffect(5);
					}
					break;
				}
				case 29193: // Common
				case 29211: // Extreme
				{
					final StatSet npcVars = npc.getVariables();
					if (npcVars.getBoolean("START_TIMERS", true))
					{
						npcVars.set("START_TIMERS", false);
						getTimers().addTimer("GLADIATOR_START_SPAWN", 6000, npc, null);
						getTimers().addTimer("ATTACK_TIMER", 15000, npc, null);
						getTimers().addTimer("MINION_CALL", 30000, npc, null);
						
						// myself->AddTimerEx(Royal_Timer, 30 * 1000);
						// myself->AddTimerEx(Scan_Timer, 1000);
						getTimers().addTimer("BEASTS_MINIONS_SPAWN", 1000, npc, null);
						// myself->AddTimerEx(Gladiator_Fishnet_Timer, 15 * 1000);
					}
					
					final int hpState = npcVars.getInt("HP_STATE", 0);
					final long maxHp = npc.getMaxHp();
					if ((maxHp - npc.getCurrentHp()) > (maxHp * 0.01 * hpState))
					{
						final int state = hpState % 5;
						if (state == 0)
						{
							npc.setDisplayEffect(5);
							getTimers().addTimer("MEDUSA_SKILL_TIMER", 15000, npc, null);
						}
						else
						{
							npc.setDisplayEffect(state);
						}
						
						npcVars.set("HP_STATE", hpState + 1);
					}
					break;
				}
			}
		}
	}
	
	/**
	 * Handles logic when an {@code Npc} is killed by a {@code Player}.<br>
	 * This method checks the ID of the killed NPC to trigger specific stage transitions.<br>
	 * It manages timer cancellations and despawns other entities based on the current instance state.
	 * @param npc The {@code Npc} that was defeated.
	 * @param killer The {@code Player} who dealt the killing blow.
	 * @param isSummon Indicates if the kill was performed by a summon.
	 */
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final Instance world = npc.getInstanceWorld();
		if (isInInstance(world))
		{
			final int npcId = npc.getId();
			switch (npcId)
			{
				case 29191: // Common
				case 29209: // Extreme
				{
					getTimers().cancelTimer("FOLLOW_BEASTS", npc, null);
					world.getAliveNpcs(BEASTS).forEach(beast ->
					{
						getTimers().cancelTimer("BEASTS_CHECK_HP", beast, null);
						beast.teleToLocation(new Location(-126920, -234182, -15563)); // Don't even ask, it's pure hack. - St3eT 2k16
						beast.deleteMe();
					});
					getTimers().addTimer("END_STAGE_1", 1000, null, killer);
					break;
				}
				case 29193: // Common
				case 29211: // Extreme
				{
					// Cancel timers
					getTimers().cancelTimer("BEASTS_MINIONS_SPAWN", npc, null);
					getTimers().cancelTimer("MINION_CALL", npc, null);
					getTimers().cancelTimer("ATTACK_TIMER", npc, null);
					getTimers().cancelTimer("MEDUSA_SKILL_TIMER", npc, null);
					
					// Despawn beasts
					world.getAliveNpcs(BEASTS_MINIONS).forEach(beast -> beast.doDie(null));
					
					// Despawn gladiators
					for (int i = 1; i < 7; i++)
					{
						world.despawnGroup(isExtremeMode(world) ? ("magmeld4_2621_gro" + i + "m1") : ("magmeld4_2621_gmo" + i + "m1"));
					}
					
					getTimers().addTimer("END_STAGE_2", 3000, null, killer);
					break;
				}
				case 29194: // Common
				case 29212: // Extreme
				{
					world.finishInstance();
					getTimers().addTimer("END_STAGE_3", 2000, null, killer);
					break;
				}
			}
		}
	}
	
	/**
	 * This method is called when a new instance is created.<br>
	 * It displays an HTML file to the player based on the instance type.<br>
	 * The logic checks if the {@code player} is not {@code null}.
	 * @param instance The {@code Instance} that was just created.
	 * @param player The {@code Player} who triggered the creation.
	 */
	@Override
	public void onInstanceCreated(Instance instance, Player player)
	{
		if ((player != null) && isInInstance(instance))
		{
			showHtmlFile(player, (instance.getTemplateId() == TEMPLATE_ID) ? "PartyEnterCommon.html" : "PartyEnterExtreme.html");
		}
	}
	
	/**
	 * This method is called when an {@code Npc} spawns.<br>
	 * It checks if the NPC is inside an instance.<br>
	 * If it is, it disables random walking and opens a specific door.<br>
	 * It also starts a new timer for the NPC.
	 * @param npc The {@code Npc} that has just spawned.
	 */
	@Override
	public void onSpawn(Npc npc)
	{
		final Instance world = npc.getInstanceWorld();
		if (isInInstance(world))
		{
			npc.setRandomWalking(false);
			world.openCloseDoor(npc.getParameters().getInt("My_DoorName", -1), true);
			getTimers().addTimer("GLADIATOR_MOVING", 3000, npc, null);
		}
	}
	
	/**
	 * This method is called when an {@code Npc} finishes moving.<br>
	 * It checks if the NPC is inside an instance zone.<br>
	 * If it is, it opens a specific door based on the NPC parameters.
	 * @param npc The {@code Npc} that finished its movement.
	 */
	@Override
	public void onMoveFinished(Npc npc)
	{
		final Instance world = npc.getInstanceWorld();
		if (isInInstance(world))
		{
			world.openCloseDoor(npc.getParameters().getInt("My_DoorName", -1), false);
		}
	}
	
	/**
	 * This method is called when an {@code Npc} finishes casting a skill on a {@code Player}.<br>
	 * It checks if the specific skill matches a predefined ID.<br>
	 * If it matches, it triggers a display effect on the {@code Npc}.
	 * @param npc The {@code Npc} that performed the action.
	 * @param player The {@code Player} who was targeted by the skill.
	 * @param skill The {@code Skill} object representing the cast spell.
	 */
	@Override
	public void onSpellFinished(Npc npc, Player player, Skill skill)
	{
		if (skill.getId() == STAGE_2_SKILL_3.getSkillId())
		{
			npc.setDisplayEffect(6);
		}
	}
	
	/**
	 * Handles the logic when an {@code Npc} detects a {@code Creature}.<br>
	 * This method checks if the creature is a {@code Player} and triggers door events.
	 * @param npc The {@code Npc} that sees the target.
	 * @param creature The {@code Creature} being seen by the NPC.
	 */
	@Override
	public void onCreatureSee(Npc npc, Creature creature)
	{
		final Instance world = npc.getInstanceWorld();
		if (isInInstance(world) && creature.isPlayer() && npc.isScriptValue(0))
		{
			npc.setScriptValue(1);
			world.openCloseDoor(MAIN_DOOR_1, true);
			getTimers().addTimer("SECOND_DOOR_OPEN", 3000, npc, null);
			getTimers().addTimer("CLOSE_DOORS", 60000, npc, null);
		}
	}
	
	/**
	 * Handles logic when a {@code Creature} enters a specific {@code ZoneType}.<br>
	 * This method checks if the creature is a player and teleports them to the battle location.<br>
	 * Teleportation only occurs if the instance has the {@code TELEPORT_ACTIVE} parameter set to {@code true}.
	 * @param creature The {@code Creature} entering the zone.
	 * @param zone The type of the {@code ZoneType} entered.
	 */
	@Override
	public void onEnterZone(Creature creature, ZoneType zone)
	{
		final Instance world = creature.getInstanceWorld();
		if (creature.isPlayer() && isInInstance(world) && world.getParameters().getBoolean("TELEPORT_ACTIVE", false))
		{
			creature.teleToLocation(BATTLE_LOC);
		}
	}
	
	/**
	 * Checks if the current instance is set to extreme mode.<br>
	 * It compares the template ID of the {@code Instance} with the extreme constant.
	 * @param instance The {@code Instance} object to check.
	 * @return {@code true} if it is an extreme mode instance, {@code false} otherwise.
	 */
	private boolean isExtremeMode(Instance instance)
	{
		return instance.getTemplateId() == EXTREME_TEMPLATE_ID;
	}
	
	/**
	 * This is the entry point for the {@code OctavisWarzone} class.<br>
	 * It initializes a new instance of the script.
	 * @param args Command line arguments.
	 */
	public static void main(String[] args)
	{
		new OctavisWarzone();
	}
}
