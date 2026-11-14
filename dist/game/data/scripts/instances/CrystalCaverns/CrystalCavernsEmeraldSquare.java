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
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.instance.Monster;
import org.l2jmobius.gameserver.entity.instancezone.Instance;
import org.l2jmobius.gameserver.mechanics.script.InstanceScript;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.serverpackets.ExSendUIEvent;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Defines the logic for the {@code Emerald Square} instance zone within the {@code Crystal Caverns}.<br>
 * This class handles the specific scripts and behaviors for this area.
 * @author St3eT
 */
public class CrystalCavernsEmeraldSquare extends InstanceScript
{
	// NPCs
	private static final int CAVERNS_ENTRACE = 33522;
	private static final int VERIDAN_NORMAL = 25796;
	private static final int VERIDAN_WISE = 26107;
	private static final int VERIDAN_WEALTHY = 26106;
	private static final int VERIDAN_ARMED = 26105;
	private static final int WATER_CANNON = 19008;
	private static final int WATER_CANNON_SKILL = 19009;
	private static final int STRONGHOLD_PROTECTOR = 23012;
	private static final int SQUARE_INTRUDER = 23010;
	private static final int SQUARE_ATTACKER = 23011;
	
	// Skills
	private static final SkillHolder DESTROY_SKILL = new SkillHolder(12003, 1);
	private static final SkillHolder WATER_CANNON_SKILL_ATTACK = new SkillHolder(14179, 1);
	
	// Locations
	private static final Location[] BOSS_SPAWNS =
	{
		new Location(152745, 145957, -12584, 16446),
		new Location(152816, 145968, -12633, 16446),
	};
	
	// Misc
	private static final int TEMPLATE_ID = 163;
	private static final int RAID_DOOR_1 = 24220005;
	private static final int RAID_DOOR_2 = 24220006;
	
	/**
	 * Initializes the {@code CrystalCavernsEmeraldSquare} instance script.<br>
	 * This constructor sets up the starting NPCs and spawn locations.<br>
	 * It also configures kill, attack, and spell events for the zone.
	 */
	public CrystalCavernsEmeraldSquare()
	{
		super(TEMPLATE_ID);
		addStartNpc(CAVERNS_ENTRACE);
		addTalkId(CAVERNS_ENTRACE);
		addSpawnId(WATER_CANNON);
		addKillId(WATER_CANNON, VERIDAN_NORMAL, VERIDAN_WISE, VERIDAN_WEALTHY, VERIDAN_ARMED);
		addAttackId(WATER_CANNON, VERIDAN_NORMAL, VERIDAN_WISE, VERIDAN_WEALTHY, VERIDAN_ARMED);
		addSpellFinishedId(WATER_CANNON_SKILL);
		addInstanceEnterId(TEMPLATE_ID);
		addInstanceLeaveId(TEMPLATE_ID);
		addCreatureSeeId(WATER_CANNON);
	}
	
	/**
	 * Handles specific events triggered within the instance.<br>
	 * This method checks for the {@code enterInstance} event to initiate entry logic.
	 * @param event The name of the event being triggered.
	 * @param npc The {@code Npc} associated with the event.
	 * @param player The {@code Player} involved in the event.
	 * @return The result of the super class {@code onEvent} method.
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
	 * Handles specific timed events for NPCs within the instance.<br>
	 * This method processes logic for HP regeneration, support spawning, cannon attacks, and suicide timers.
	 * @param event The name of the timer event to process.
	 * @param params A {@code StatSet} containing configuration parameters for the NPC.
	 * @param npc The {@code Npc} instance that triggered the timer.
	 * @param player The {@code Player} associated with the event, if applicable.
	 */
	@Override
	public void onTimerEvent(String event, StatSet params, Npc npc, Player player)
	{
		final Instance instance = npc.getInstanceWorld();
		if (isInInstance(instance))
		{
			final StatSet npcParams = npc.getParameters();
			final StatSet npcVars = npc.getVariables();
			final int baseId = npcParams.getInt("base_id", -1);
			
			switch (event)
			{
				case "HP_REGEN_TIMER":
				{
					final int value = ((baseId == 5) || (baseId == 6)) ? 5 : baseId;
					npc.getStat().addFixedValue(Stat.REGENERATE_HP_RATE, Double.valueOf(value * 1000));
					break;
				}
				case "SUPPORT_SPAWN_TIMER":
				{
					final int supportVal = npcVars.getInt("SUPPORT_VALUE", 0);
					if (supportVal > 3)
					{
						return;
					}
					
					if ((supportVal == 0) || (supportVal == 1) || (supportVal == 2))
					{
						final String spawnName = npcParams.getString("SupportMaker" + (supportVal + 1), null);
						if (spawnName != null)
						{
							instance.spawnGroup(spawnName);
						}
						
						npcVars.increaseInt("SUPPORT_VALUE", 1);
					}
					
					if (!npcVars.getBoolean("PREVIOUS_BASE_DESTROYED", false))
					{
						getTimers().addTimer("SUPPORT_SPAWN_TIMER", 60000, npc, null);
					}
					break;
				}
				case "CANNON_LOOP_ATTACK":
				{
					if ((npc.getCurrentHpPercent() > 30) && (npcVars.getBoolean("IS_DESTROY_ACTIVATED", false) || (getRandom(10) < 2)))
					{
						final Npc cannonSkill = addSpawn(WATER_CANNON_SKILL, npc, true, 0, false, instance.getId());
						addSkillCastDesire(cannonSkill, cannonSkill, WATER_CANNON_SKILL_ATTACK, 23);
					}
					break;
				}
				case "SUICIDE_TIMER":
				{
					npc.doDie(null);
					break;
				}
			}
		}
	}
	
	/**
	 * Handles logic when an {@code Npc} is attacked by a {@code Player}.<br>
	 * This method triggers specific events like opening doors or activating effects.
	 * @param npc The {@code Npc} being attacked.
	 * @param attacker The {@code Player} performing the attack.
	 * @param damage The amount of damage dealt to the {@code Npc}.
	 * @param isSummon Indicates if the attack was performed by a summon.
	 * @param skill The {@code Skill} used for the attack.
	 */
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon, Skill skill)
	{
		final Instance instance = npc.getInstanceWorld();
		if (isInInstance(instance))
		{
			final StatSet npcVars = npc.getVariables();
			
			switch (npc.getId())
			{
				case VERIDAN_WISE:
				case VERIDAN_WEALTHY:
				case VERIDAN_ARMED:
				{
					if (!npcVars.getBoolean("CLOSED_DOORS", false))
					{
						npcVars.set("CLOSED_DOORS", true);
						instance.openCloseDoor(RAID_DOOR_2, false);
					}
					break; // fallthrou?
				}
				case WATER_CANNON:
				{
					if ((skill != null) && (skill.getId() == DESTROY_SKILL.getSkillId()) && !npcVars.getBoolean("IS_DESTROY_ACTIVATED", false))
					{
						npcVars.set("IS_DESTROY_ACTIVATED", true);
						npc.setDisplayEffect(2);
						getTimers().addTimer("SUICIDE_TIMER", 60000, npc, null);
					}
					
					if (npc.getCurrentHpPercent() < 30)
					{
						if (!npcVars.getBoolean("IS_DESTROY_ACTIVATED", false))
						{
							npc.setDisplayEffect(3);
						}
					}
					else if (!npcVars.getBoolean("IS_DESTROY_ACTIVATED", false))
					{
						npc.setDisplayEffect(1);
					}
					break;
				}
			}
		}
	}
	
	/**
	 * This method handles logic when an {@code Npc} is killed.<br>
	 * It checks the ID of the dead NPC to trigger specific instance events.<br>
	 * For example, it can finish the instance or spawn new bosses.
	 * @param npc The {@code Npc} that was killed.
	 * @param killer The {@code Player} who killed the NPC.
	 * @param isSummon Whether the kill was performed by a summon.
	 */
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final Instance instance = npc.getInstanceWorld();
		if (isInInstance(instance))
		{
			final StatSet npcParams = npc.getParameters();
			final int baseId = npcParams.getInt("base_id", -1);
			
			switch (npc.getId())
			{
				case VERIDAN_NORMAL:
				case VERIDAN_WISE:
				case VERIDAN_WEALTHY:
				case VERIDAN_ARMED:
				{
					if (instance.getAliveNpcCount(VERIDAN_NORMAL, VERIDAN_WISE, VERIDAN_WEALTHY, VERIDAN_ARMED) == 0)
					{
						instance.finishInstance();
					}
					else
					{
						instance.setReenterTime();
					}
					break;
				}
				case WATER_CANNON:
				{
					npc.setDisplayEffect(4);
					showOnScreenMsg(instance, NpcStringId.SUCCESSFUL_DESTRUCTION_OF_STRONGHOLD_S1, ExShowScreenMessage.MIDDLE_CENTER, 4000, String.valueOf(npcParams.getInt("base_id", -1)));
					World.forEachVisibleObjectInRange(npc, Monster.class, 400, monster ->
					{
						final int monsterId = monster.getId();
						if ((monsterId == STRONGHOLD_PROTECTOR) || (monsterId == SQUARE_INTRUDER) || (monsterId == SQUARE_ATTACKER))
						{
							monster.doDie(null);
						}
					});
					
					instance.getAliveNpcs(WATER_CANNON).forEach(cannon ->
					{
						final int cannonBaseId = cannon.getParameters().getInt("base_id", -1);
						
						switch (baseId)
						{
							case 1:
							{
								if (cannonBaseId == 2)
								{
									cannon.getVariables().set("PREVIOUS_BASE_DESTROYED", true);
									cannon.setTargetable(true);
								}
								break;
							}
							case 2:
							{
								if (cannonBaseId == 3)
								{
									cannon.getVariables().set("PREVIOUS_BASE_DESTROYED", true);
									cannon.setTargetable(true);
								}
								break;
							}
							case 3:
							{
								if (cannonBaseId == 4)
								{
									cannon.getVariables().set("PREVIOUS_BASE_DESTROYED", true);
									cannon.setTargetable(true);
								}
								break;
							}
							case 4:
							{
								if ((cannonBaseId == 5) || (cannonBaseId == 6))
								{
									cannon.getVariables().set("PREVIOUS_BASE_DESTROYED", true);
									cannon.setTargetable(true);
								}
								break;
							}
							case 5:
							case 6:
							{
								if (cannonBaseId == 7)
								{
									cannon.getVariables().set("PREVIOUS_BASE_DESTROYED", true);
									cannon.setTargetable(true);
								}
								break;
							}
							case 7:
							{
								if ((cannonBaseId == 8) || (cannonBaseId == 9))
								{
									cannon.getVariables().set("PREVIOUS_BASE_DESTROYED", true);
									cannon.setTargetable(true);
								}
								break;
							}
						}
					});
					
					if ((baseId == 8) || (baseId == 9))
					{
						instance.getParameters().increaseInt("MAIN_TARGETS_KILLED", 0, 1);
						if (instance.getParameters().getInt("MAIN_TARGETS_KILLED", 0) == 2)
						{
							showOnScreenMsg(instance, NpcStringId.SUCCESSFUL_DESTRUCTION_OF_STRONGHOLD_ENTRY_ACCESSED, ExShowScreenMessage.MIDDLE_CENTER, 4000);
							instance.openCloseDoor(RAID_DOOR_1, true);
							instance.openCloseDoor(RAID_DOOR_2, true);
							
							final int random = getRandom(100);
							int bossId = -1;
							if (random < 55)
							{
								bossId = VERIDAN_NORMAL;
							}
							else if (random < 80)
							{
								bossId = VERIDAN_WISE;
							}
							else if (random < 95)
							{
								bossId = VERIDAN_WEALTHY;
							}
							else
							{
								bossId = VERIDAN_ARMED;
							}
							
							for (Location loc : BOSS_SPAWNS)
							{
								addSpawn(bossId, loc, false, 0, false, instance.getId());
							}
						}
					}
				}
			}
		}
	}
	
	/**
	 * Handles logic after a spell has finished.<br>
	 * This method checks if the {@code Npc} is a specific water cannon.<br>
	 * It deletes the {@code Npc} if it used the correct skill within an instance.
	 * @param npc The {@code Npc} that performed the action.
	 * @param player The {@code Player} who cast the spell.
	 * @param skill The {@code Skill} that was just completed.
	 */
	@Override
	public void onSpellFinished(Npc npc, Player player, Skill skill)
	{
		final Instance instance = npc.getInstanceWorld();
		if (isInInstance(instance) && (npc.getId() == WATER_CANNON_SKILL) && (skill.getId() == WATER_CANNON_SKILL_ATTACK.getSkillId()))
		{
			npc.deleteMe();
		}
	}
	
	/**
	 * This method is called when an {@code Npc} spawns.<br>
	 * It checks if the {@code Npc} is inside an instance zone.<br>
	 * It initializes specific timers and targetable status for the {@code WATER_CANNON} NPC.
	 * @param npc The {@code Npc} that has just spawned.
	 */
	@Override
	public void onSpawn(Npc npc)
	{
		final Instance instance = npc.getInstanceWorld();
		if (isInInstance(instance))
		{
			switch (npc.getId())
			{
				case WATER_CANNON:
				{
					final StatSet npcParams = npc.getParameters();
					final int baseId = npcParams.getInt("base_id", -1);
					if (baseId != 1)
					{
						npc.setTargetable(false);
					}
					
					getTimers().addTimer("HP_REGEN_TIMER", 10000, npc, null);
					if (baseId > 0)
					{
						getTimers().addTimer("SUPPORT_SPAWN_TIMER", (baseId * 60) * 1000, npc, null);
					}
					break;
				}
			}
		}
	}
	
	/**
	 * Handles the logic when an {@code Npc} sees a {@code Creature}.<br>
	 * This method checks if the creature is a {@code Player} and triggers specific behaviors.<br>
	 * It sets script values, displays effects, and starts a repeating timer for the NPC.
	 * @param npc The {@code Npc} that detects the creature.
	 * @param creature The {@code Creature} that was seen by the NPC.
	 */
	@Override
	public void onCreatureSee(Npc npc, Creature creature)
	{
		final Instance world = npc.getInstanceWorld();
		if ((world != null) && creature.isPlayer() && npc.isScriptValue(0))
		{
			npc.setScriptValue(1);
			npc.setDisplayEffect(1);
			getTimers().addRepeatingTimer("CANNON_LOOP_ATTACK", 1000, npc, null);
		}
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
	 * This is the main entry point for the {@code CrystalCavernsEmeraldSquare} class.<br>
	 * It initializes a new instance of the script.
	 * @param args Command line arguments passed to the program.
	 */
	public static void main(String[] args)
	{
		new CrystalCavernsEmeraldSquare();
	}
}
