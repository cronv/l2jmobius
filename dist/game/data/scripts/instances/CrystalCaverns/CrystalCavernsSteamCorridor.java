/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package instances.CrystalCaverns;

import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.instancezone.Instance;
import org.l2jmobius.gameserver.entity.zone.ZoneType;
import org.l2jmobius.gameserver.entity.zone.type.TeleportZone;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.mechanics.script.InstanceScript;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.ExSendUIEvent;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Defines the logic and behavior for the Steam Corridor area within the {@code CrystalCaverns} instance.<br>
 * This class handles specific scripts, events, and zone mechanics for this location.
 * @author St3eT
 */
public class CrystalCavernsSteamCorridor extends InstanceScript
{
	// NPCs
	private static final int CAVERNS_ENTRACE = 33522;
	private static final int KECHI_NORMAL = 25797;
	private static final int KECHI_WISE = 26113;
	private static final int KECHI_WEALTHY = 26112;
	private static final int KECHI_ARMED = 26111;
	private static final int VICIOUS_DUELER = 23014;
	private static final int VICIOUS_WARRIOR = 23016;
	private static final int VICIOUS_SWORDSMAN = 23015;
	private static final int SPIRIT_PROTECTOR = 23013;
	private static final int FIRE_REGION = 19161;
	private static final int PLAYER_DETECTOR = 19075;
	private static final int TRAP_1 = 19011;
	private static final int TRAP_2 = 19012;
	
	// Skills
	private static final SkillHolder FIRE_SKILL_1 = new SkillHolder(14373, 1);
	private static final SkillHolder FIRE_SKILL_2 = new SkillHolder(14373, 2);
	private static final SkillHolder FIRE_SKILL_3 = new SkillHolder(14197, 1);
	private static final SkillHolder TRAP_SKILL_1 = new SkillHolder(14180, 1);
	private static final SkillHolder TRAP_SKILL_2 = new SkillHolder(14181, 1);
	private static final SkillHolder TRAP_SKILL_3 = new SkillHolder(14372, 1);
	
	// Location
	private static final Location BOSS_LOC = new Location(154078, 215125, -12140);
	
	// Misc
	private static final int TEMPLATE_ID = 164;
	
	/**
	 * Initializes the {@code CrystalCavernsSteamCorridor} instance script.<br>
	 * This constructor sets up all required NPCs, skills, and events for the zone.<br>
	 * It registers starting points and defines spawn behaviors for various creatures.
	 */
	public CrystalCavernsSteamCorridor()
	{
		super(TEMPLATE_ID);
		addStartNpc(CAVERNS_ENTRACE);
		addTalkId(CAVERNS_ENTRACE);
		addAttackId(TRAP_1, TRAP_2);
		addKillId(VICIOUS_DUELER, VICIOUS_WARRIOR, VICIOUS_SWORDSMAN, KECHI_NORMAL, KECHI_WISE, KECHI_WEALTHY, KECHI_ARMED);
		addSpawnId(SPIRIT_PROTECTOR, VICIOUS_DUELER, VICIOUS_WARRIOR, VICIOUS_SWORDSMAN, FIRE_REGION);
		addEventReceivedId(SPIRIT_PROTECTOR, VICIOUS_DUELER, VICIOUS_WARRIOR, VICIOUS_SWORDSMAN);
		addInstanceCreatedId(TEMPLATE_ID);
		addInstanceEnterId(TEMPLATE_ID);
		addInstanceLeaveId(TEMPLATE_ID);
		addCreatureSeeId(PLAYER_DETECTOR);
	}
	
	/**
	 * Handles specific timed events for NPCs within the instance.<br>
	 * This method manages fire region skills and trap reactions.
	 * @param event The name of the timer event to process.
	 * @param params A set of statistics used as parameters for the event.
	 * @param npc The {@code Npc} entity that triggered the timer.
	 * @param player The {@code Player} associated with the event.
	 */
	@Override
	public void onTimerEvent(String event, StatSet params, Npc npc, Player player)
	{
		final Instance instance = npc.getInstanceWorld();
		if (isInInstance(instance))
		{
			final StatSet npcVars = npc.getVariables();
			
			switch (event)
			{
				case "FIRE_REGION_TIMER_1":
				{
					addSkillCastDesire(npc, npc, FIRE_SKILL_1, 23);
					getTimers().addTimer("FIRE_REGION_TIMER_2", 10000, npc, null);
					break;
				}
				case "FIRE_REGION_TIMER_2":
				{
					addSkillCastDesire(npc, npc, FIRE_SKILL_2, 23);
					getTimers().addTimer("FIRE_REGION_TIMER_3", 10000, npc, null);
					break;
				}
				case "FIRE_REGION_TIMER_3":
				{
					addSkillCastDesire(npc, npc, FIRE_SKILL_3, 23);
					getTimers().addTimer("FIRE_REGION_TIMER_3", 1000, npc, null);
					break;
				}
				case "TRAP_REACT_TIMER":
				{
					final int timer = npcVars.increaseInt("TIMER_VAL", -1);
					if (timer > 0)
					{
						npc.broadcastSay(ChatType.NPC_GENERAL, " " + timer);
					}
					else
					{
						final int npcId = npc.getId();
						if (npcId == TRAP_1)
						{
							addSkillCastDesire(npc, npc, (getRandom(10) < 8 ? TRAP_SKILL_1 : TRAP_SKILL_3), 23);
						}
						else if (npcId == TRAP_2)
						{
							addSkillCastDesire(npc, npc, (getRandom(10) < 8 ? TRAP_SKILL_2 : TRAP_SKILL_3), 23);
						}
					}
					
					getTimers().addTimer("TRAP_REACT_TIMER", 1000, npc, null);
					break;
				}
			}
		}
	}
	
	/**
	 * Handles specific events triggered within the instance.<br>
	 * This method checks for the {@code enterInstance} event to trigger teleportation.<br>
	 * It then calls the parent class implementation.
	 * @param event The name of the event being triggered.
	 * @param npc The {@code Npc} object associated with the event.
	 * @param player The {@code Player} object involved in the event.
	 * @return The result of the superclass method call.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.equals("enterInstance"))
		{
			enterInstance(player, npc, TEMPLATE_ID);
		}
		
		return super.onEvent(event, npc, player);
	}
	
	/**
	 * This method is called when a new instance is created.<br>
	 * It disables specific teleport zones for the current instance.<br>
	 * It then calls the superclass implementation.
	 * @param instance The {@code Instance} object being created.
	 * @param player The {@code Player} who entered the instance.
	 */
	@Override
	public void onInstanceCreated(Instance instance, Player player)
	{
		instance.setStatus(1);
		for (int i = 0; i < 6; i++)
		{
			final ZoneType zone = ZoneManager.getInstance().getZoneByName("24_24_fire_telezone_0" + i, TeleportZone.class);
			if (zone != null)
			{
				zone.setEnabled(false, instance.getId());
			}
		}
		
		super.onInstanceCreated(instance, player);
	}
	
	/**
	 * This method is called when a {@code Player} enters an {@code Instance}.<br>
	 * It sends a UI event to the player showing the elapsed and remaining time.
	 * @param player The {@code Player} who entered the instance.
	 * @param instance The {@code Instance} being entered.
	 */
	@Override
	public void onInstanceEnter(Player player, Instance instance)
	{
		final int startTime = (int) (instance.getElapsedTime() / 1000);
		final int endTime = (int) (instance.getRemainingTime() / 1000);
		player.sendPacket(new ExSendUIEvent(player, false, true, startTime, endTime, NpcStringId.ELAPSED_TIME));
	}
	
	/**
	 * This method is called when a {@code Player} leaves an {@code Instance}.<br>
	 * It sends a UI event packet to the player.
	 * @param player The {@code Player} who is leaving the instance.
	 * @param instance The {@code Instance} that was just exited.
	 */
	@Override
	public void onInstanceLeave(Player player, Instance instance)
	{
		player.sendPacket(new ExSendUIEvent(player, true, true, 0, 0, NpcStringId.ELAPSED_TIME));
	}
	
	/**
	 * This method is called when an {@code Npc} spawns.<br>
	 * It configures specific behaviors for different NPC types within the instance.<br>
	 * Some NPCs are made invisible or untargetable based on their ID.
	 * @param npc The {@code Npc} that was just spawned.
	 */
	@Override
	public void onSpawn(Npc npc)
	{
		final Instance instance = npc.getInstanceWorld();
		if (isInInstance(instance))
		{
			final StatSet npcParams = npc.getParameters();
			
			switch (npc.getId())
			{
				case SPIRIT_PROTECTOR:
				case VICIOUS_DUELER:
				case VICIOUS_WARRIOR:
				case VICIOUS_SWORDSMAN:
				{
					npc.setTargetable(false);
					npc.disableCoreAI(true);
					npc.setInvisible(true);
					break;
				}
				case FIRE_REGION:
				{
					final int timeLimit = npcParams.getInt("Limit_Time", 0);
					if (timeLimit > 0)
					{
						getTimers().addTimer("FIRE_REGION_TIMER_1", ((timeLimit * 30) * 100), npc, null);
					}
					
					npc.setTargetable(false);
					npc.setInvul(true);
					npc.setRandomAnimation(false);
					npc.setRandomWalking(false);
					npc.disableCoreAI(true);
					break;
				}
			}
		}
	}
	
	/**
	 * Handles events received by an {@code Npc}.<br>
	 * This method checks if the event matches a specific ID to enable the NPC.<br>
	 * It makes the NPC targetable and visible within an instance.
	 * @param eventName The name or ID of the event being triggered.
	 * @param sender The {@code Npc} that sent the event.
	 * @param receiver The {@code Npc} receiving the event.
	 * @param reference The {@code WorldObject} associated with the event.
	 * @return The result of the super class method call.
	 */
	@Override
	public String onEventReceived(String eventName, Npc sender, Npc receiver, WorldObject reference)
	{
		final Instance instance = receiver.getInstanceWorld();
		if (isInInstance(instance))
		{
			final StatSet npcParams = receiver.getParameters();
			if (eventName.equals(String.valueOf(24220005 + npcParams.getInt("Terri_ID", 0))))
			{
				receiver.setTargetable(true);
				receiver.disableCoreAI(false);
				receiver.setInvisible(false);
			}
		}
		
		return super.onEventReceived(eventName, sender, receiver, reference);
	}
	
	/**
	 * Handles logic when an {@code Npc} is attacked by a {@code Player}.<br>
	 * This method checks if the target is a specific trap and triggers reactions.
	 * @param npc The {@code Npc} being attacked.
	 * @param attacker The {@code Player} performing the attack.
	 * @param damage The amount of damage dealt to the {@code Npc}.
	 * @param isSummon Whether the attack was performed by a summon.
	 */
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	{
		final Instance instance = npc.getInstanceWorld();
		if (isInInstance(instance))
		{
			switch (npc.getId())
			{
				case TRAP_1:
				case TRAP_2:
				{
					if (npc.isScriptValue(0))
					{
						getTimers().addTimer("TRAP_REACT_TIMER", 1000, npc, null);
						npc.setScriptValue(1);
					}
					break;
				}
			}
		}
	}
	
	/**
	 * This method is called when an {@code Npc} is killed by a {@code Player}.<br>
	 * It handles progress for specific enemy types and manages instance progression.<br>
	 * If the target is a boss, it finishes the instance immediately.
	 * @param npc The {@code Npc} that was defeated.
	 * @param killer The {@code Player} who dealt the killing blow.
	 * @param isSummon Indicates if the kill was performed by a summon.
	 */
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final Instance instance = npc.getInstanceWorld();
		if (isInInstance(instance))
		{
			final StatSet npcParams = npc.getParameters();
			final int killTarget = instance.getParameters().getInt("KILL_TARGET", 5);
			int currentKillCount = instance.getParameters().getInt("KILL_COUNT", 0);
			
			switch (npc.getId())
			{
				case VICIOUS_DUELER:
				case VICIOUS_WARRIOR:
				case VICIOUS_SWORDSMAN:
				{
					if (npcParams.getInt("last_checker", 0) == 1)
					{
						currentKillCount = instance.getParameters().increaseInt("KILL_COUNT", 0, 1);
						if (currentKillCount >= killTarget)
						{
							final ZoneType zone = ZoneManager.getInstance().getZoneByName(npc.getParameters().getString("AreaTeleName"), TeleportZone.class);
							if (zone != null)
							{
								final int instanceId = instance.getId();
								zone.setEnabled(true, instanceId);
								showOnScreenMsg(instance, NpcStringId.THE_PORTAL_TO_THE_NEXT_ROOM_IS_NOW_OPEN, ExShowScreenMessage.MIDDLE_CENTER, 4000);
								instance.spawnGroup("innadril23_mb2422_pt" + instance.getStatus() + "m1");
								instance.getParameters().set("KILL_COUNT", 0);
								
								switch (instance.getStatus())
								{
									case 1:
									{
										instance.getParameters().set("KILL_TARGET", 12);
										instance.setStatus(2);
										break;
									}
									case 2:
									{
										instance.getParameters().set("KILL_TARGET", 3);
										instance.setStatus(3);
										break;
									}
									case 3:
									{
										instance.getParameters().set("KILL_TARGET", 18);
										instance.setStatus(4);
										break;
									}
									case 4:
									{
										instance.getParameters().set("KILL_TARGET", 5);
										instance.setStatus(5);
										break;
									}
									case 5:
									{
										instance.getParameters().set("KILL_TARGET", 20);
										instance.setStatus(6);
										break;
									}
									case 6:
									{
										final int random = getRandom(100);
										int bossId = -1;
										if (random < 55)
										{
											bossId = KECHI_NORMAL;
										}
										else if (random < 80)
										{
											bossId = KECHI_WISE;
										}
										else if (random < 95)
										{
											bossId = KECHI_WEALTHY;
										}
										else
										{
											bossId = KECHI_ARMED;
										}
										
										addSpawn(bossId, BOSS_LOC, false, 0, false, instanceId);
										break;
									}
								}
							}
							else
							{
								LOGGER.warning("Cannot find teleport zone for Crystal Cavern: Steam Corridor instance!!!");
							}
						}
					}
					break;
				}
				case KECHI_NORMAL:
				case KECHI_WISE:
				case KECHI_WEALTHY:
				case KECHI_ARMED:
				{
					instance.finishInstance();
					break;
				}
			}
		}
	}
	
	/**
	 * Handles the logic when an {@code Npc} sees a {@code Creature}.<br>
	 * This method checks if the creature is a {@code Player} within an instance.<br>
	 * It triggers specific events and spawns traps before deleting the {@code Npc}.
	 * @param npc The {@code Npc} that detects the creature.
	 * @param creature The {@code Creature} that was seen by the {@code Npc}.
	 */
	@Override
	public void onCreatureSee(Npc npc, Creature creature)
	{
		final Instance instance = npc.getInstanceWorld();
		if (isInInstance(instance) && creature.isPlayer())
		{
			if (npc.isScriptValue(0))
			{
				npc.setScriptValue(1);
				final StatSet npcParams = npc.getParameters();
				npc.broadcastEvent(String.valueOf(24220005 + npcParams.getInt("Terri_ID", 0)), 2000, null);
				for (int i = 0; i < getRandom(5); i++)
				{
					final Npc trap = addSpawn(((npcParams.getInt("MobType", 0) == 0) ? TRAP_1 : TRAP_2), npc, true, 0, false, instance.getId());
					trap.getVariables().set("TIMER_VAL", 4);
				}
				
				npc.deleteMe();
			}
		}
	}
	
	/**
	 * This is the main entry point for the {@code CrystalCavernsSteamCorridor} class.<br>
	 * It initializes a new instance of the script.
	 * @param args The command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		new CrystalCavernsSteamCorridor();
	}
}
