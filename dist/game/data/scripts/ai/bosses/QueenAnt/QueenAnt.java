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
package ai.bosses.QueenAnt;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.instance.GrandBoss;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.entity.groups.CommandChannel;
import org.l2jmobius.gameserver.entity.groups.Party;
import org.l2jmobius.gameserver.entity.spawns.Spawn;
import org.l2jmobius.gameserver.entity.zone.type.ArenaZone;
import org.l2jmobius.gameserver.managers.DatabaseSpawnManager;
import org.l2jmobius.gameserver.managers.GlobalVariablesManager;
import org.l2jmobius.gameserver.managers.GrandBossManager;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.SkillCaster;
import org.l2jmobius.gameserver.mechanics.skill.enums.SkillFinishType;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;

/**
 * This class handles the artificial intelligence for the {@code QueenAnt} grand boss.<br>
 * It manages the boss lifecycle, minion healing mechanics, raid curses, and nest constraints.
 * @author Notorion
 */
public class QueenAnt extends Script
{
	// NPCs
	private static final int QUEEN_ANT = 29381;
	private static final int INVISIBLE_NPC = 18919;
	private static final int NPC_LIFETIME = 9000;
	private static final int REQUIRED_CC_MEMBERS = 14;
	
	// Skills
	private static final SkillHolder AREA_SKILL = new SkillHolder(33918, 1);
	private static final SkillHolder COMMON_SKILL_1 = new SkillHolder(33915, 1);
	private static final SkillHolder COMMON_SKILL_2 = new SkillHolder(33916, 1);
	private static final SkillHolder INITIAL_SKILL = new SkillHolder(33917, 1);
	private static final SkillHolder LIMIT_BARRIER = new SkillHolder(29515, 1);
	
	// Barrier
	private static final int BARRIER_DURATION_MILLIS = 600000; // 10 minutes.
	private static final int HIT_COUNT = 2000; // 2000 Number of attacks needed to destroy the barrier.
	private static final int HIT_COUNT_RENEW = 500; // 500 hits in 60 seconds to continue without the barrier, not confirmed.
	private static final int RENEW_DURATION_MILLIS = 600000; // 60 seconds of vulnerability, Not confirmed.
	// Locations
	private static final Location GLUDIO_LOCATION = new Location(-14608, 123920, -3123);
	private static final Location SPAWN_LOCATION = new Location(-7848, 183389, -3624);
	
	// Zone
	private static final ArenaZone ARENA_ZONE = ZoneManager.getInstance().getZoneByName("Queen_Ants_Lair", ArenaZone.class);
	
	// Misc
	private static GrandBoss _spawnedMainBoss;
	private boolean _barrierActivated = false;
	private boolean _bossInCombat = false;
	private boolean _hp85Reached = false;
	private boolean _isDebuffImmunityActive = false;
	private boolean _vulnerablePhase = false;
	private final AtomicBoolean _canUseSkill = new AtomicBoolean(true);
	private final AtomicBoolean _isUsingAreaSkill = new AtomicBoolean(false);
	private long _lastAttackTime = 0;
	private final Map<Npc, Integer> _queenAntHits = new ConcurrentHashMap<>();
	
	/**
	 * Private constructor for the {@code QueenAnt} class.<br>
	 * This constructor initializes the boss state and handles spawning logic.<br>
	 * It checks if the boss is alive or needs to be respawned based on its status.
	 */
	private QueenAnt()
	{
		addAttackId(QUEEN_ANT);
		addSpawnId(QUEEN_ANT);
		addKillId(QUEEN_ANT);
		addAggroRangeEnterId(QUEEN_ANT);
		
		initializeRespawn();
		startQuestTimer("check_arena", 5000, null, null, true);
	}
	
	/**
	 * Sets up the initial state for the boss respawn logic.<br>
	 * It checks the current status of {@code QUEEN_ANT} via {@code GrandBossManager}.<br>
	 * If the boss is not active, it calls {@code spawnQueenAnt()}.<br>
	 * Otherwise, it schedules the next available respawn time.
	 */
	private void initializeRespawn()
	{
		try
		{
			final int status = GrandBossManager.getInstance().getStatus(QUEEN_ANT);
			if (status == 0)
			{
				spawnQueenAnt();
			}
			else if (status == 1)
			{
				scheduleNextRespawn();
			}
			else
			{
				scheduleNextRespawn();
			}
		}
		catch (Exception e)
		{
			LOGGER.severe("Queen Ant: Error during initialization: " + e.getMessage());
		}
	}
	
	/**
	 * Schedules the next respawn for the boss.<br>
	 * It calculates the time difference between now and the next spawn.<br>
	 * A task is added to the {@code ThreadPool} to trigger {@code spawnQueenAnt()} after the delay.
	 */
	private void scheduleNextRespawn()
	{
		final long currentTime = System.currentTimeMillis();
		final Calendar nextRespawn = getNextRespawnTime();
		final long delay = nextRespawn.getTimeInMillis() - currentTime;
		
		// LOGGER.info("Queen Ant: Next respawn scheduled for " + nextRespawn.getTime() + " in " + delay + "ms");
		
		ThreadPool.schedule(() ->
		{
			if (GrandBossManager.getInstance().getStatus(QUEEN_ANT) == 1)
			{
				spawnQueenAnt();
			}
		}, delay);
	}
	
	/**
	 * Handles the logic for creating the Queen Ant boss in the game world.<br>
	 * It checks if a boss is already alive before attempting to spawn a new one.<br>
	 * The method sets the initial location, heading, and status of the {@code GrandBoss}.<br>
	 * Any errors during the spawning process are caught and logged as severe errors.
	 */
	private void spawnQueenAnt()
	{
		try
		{
			if ((_spawnedMainBoss != null) && !_spawnedMainBoss.isDead())
			{
				return;
			}
			
			final NpcTemplate template = NpcData.getInstance().getTemplate(QUEEN_ANT);
			final Spawn spawn = new Spawn(template);
			spawn.setXYZ(SPAWN_LOCATION);
			spawn.setHeading(0);
			spawn.setRespawnDelay(0);
			
			final Npc boss = DatabaseSpawnManager.getInstance().addNewSpawn(spawn, false);
			_spawnedMainBoss = (GrandBoss) boss;
			GrandBossManager.getInstance().setStatus(QUEEN_ANT, 0);
			// LOGGER.info("Queen Ant: Boss spawned successfully at " + SPAWN_LOCATION);
			boss.setRandomWalking(false);
			boss.setRandomAnimation(false);
		}
		catch (Exception e)
		{
			LOGGER.severe("Queen Ant: Error spawning boss: " + e.getMessage());
		}
	}
	
	// The Queen Ant returns on Monday and Tuesday at 9 PM Hero's Tome time, with the 10/2022 Queen Ant spawning Monday at 9 PM and Shinemaker spawning Monday at 8 PM.
	/**
	 * Calculates the date and time for the next boss respawn.<br>
	 * The boss is scheduled to appear every Monday at 8:00 PM.<br>
	 * It checks if the current time has passed the target day and adjusts accordingly.
	 * @return a {@code Calendar} object representing the next spawn time.
	 */
	private Calendar getNextRespawnTime()
	{
		final Calendar nextRespawn = Calendar.getInstance();
		
		// Spawn Queen Ant Monday 8pm Night*
		nextRespawn.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
		nextRespawn.set(Calendar.HOUR_OF_DAY, 20);
		nextRespawn.set(Calendar.MINUTE, 0);
		nextRespawn.set(Calendar.SECOND, 0);
		if (nextRespawn.getTimeInMillis() < System.currentTimeMillis())
		{
			nextRespawn.add(Calendar.WEEK_OF_YEAR, 1);
		}
		
		return nextRespawn;
	}
	
	/**
	 * Handles the initial setup for an {@code Npc} when it enters the world.<br>
	 * This method configures specific behaviors based on the NPC's unique ID.<br>
	 * It sets properties like immobilization, undying status, and raid minion flags.<br>
	 * It also triggers minion spawning or timer initialization for specific boss types.
	 * @param npc The {@code Npc} instance that was just spawned.
	 */
	@Override
	public void onSpawn(Npc npc)
	{
		startQuestTimer("checkCombatStatus", 1000, npc, null, true);
	}
	
	/**
	 * Handles logic when an entity enters the {@code Npc} aggro range.<br>
	 * This method checks if a raid curse should be applied based on level differences.<br>
	 * It also handles specific logic for mages and summoned pets.
	 * @param npc The {@code Npc} that is being attacked.
	 * @param player The {@code Player} who triggered the aggro.
	 * @param isSummon A boolean indicating if the attacker is a summon.
	 */
	@Override
	public void onAggroRangeEnter(Npc npc, Player player, boolean isSummon)
	{
		_bossInCombat = true;
		checkCombatStatus(npc);
		if (_hp85Reached && (npc.getCurrentHp() < (npc.getMaxHp() * 0.85)))
		{
			activateSpecialMechanics(npc);
		}
	}
	
	/**
	 * Handles the logic when an {@code Npc} is attacked by a {@code Player}.<br>
	 * This method manages boss phases, invulnerability states, and special mechanics.<br>
	 * It also validates if the attacker meets the required level and zone requirements.
	 * @param npc The {@code Npc} being attacked.
	 * @param attacker The {@code Player} performing the attack.
	 * @param damage The amount of damage dealt to the {@code Npc}.
	 * @param isSummon Indicates if the attack comes from a summoned unit.
	 * @param skill The {@code Skill} used to perform the attack.
	 */
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon, Skill skill)
	{
		if (!_barrierActivated)
		{
			_barrierActivated = true;
			LIMIT_BARRIER.getSkill().applyEffects(npc, npc);
			npc.setInvul(true);
			startQuestTimer("remove_barrier", BARRIER_DURATION_MILLIS, npc, null);
			_queenAntHits.put(npc, 0);
		}
		
		if (_vulnerablePhase)
		{
			final int hits = _queenAntHits.getOrDefault(npc, 0) + 1;
			_queenAntHits.put(npc, hits);
			
			if (hits >= HIT_COUNT_RENEW)
			{
				cancelQuestTimer("activate_barrier", npc, null);
				startQuestTimer("activate_barrier", RENEW_DURATION_MILLIS, npc, null);
				_queenAntHits.put(npc, 0);
			}
		}
		else
		{
			final int hits = _queenAntHits.getOrDefault(npc, 0) + 1;
			_queenAntHits.put(npc, hits);
			
			if (hits >= HIT_COUNT)
			{
				npc.stopSkillEffects(LIMIT_BARRIER.getSkill());
				npc.setInvul(false);
				cancelQuestTimer("remove_barrier", npc, null);
				_vulnerablePhase = true;
				startQuestTimer("activate_barrier", RENEW_DURATION_MILLIS, npc, null);
				_queenAntHits.put(npc, 0);
			}
		}
		
		if (!ARENA_ZONE.isInsideZone(attacker))
		{
			attacker.teleToLocation(GLUDIO_LOCATION, false);
			return;
		}
		
		_lastAttackTime = System.currentTimeMillis();
		_bossInCombat = true;
		
		if (!_hp85Reached && (npc.getCurrentHp() < (npc.getMaxHp() * 0.85)))
		{
			_hp85Reached = true;
			activateSpecialMechanics(npc);
		}
		
		if (isPlayerInValidCommandChannel(attacker))
		{
			final CommandChannel cc = attacker.getParty().getCommandChannel();
			for (Player member : cc.getMembers())
			{
				if (member.getLevel() < 110)
				{
					attacker.teleToLocation(GLUDIO_LOCATION, false);
					return;
				}
			}
		}
		else if (attacker.getLevel() < 110)
		{
			attacker.teleToLocation(GLUDIO_LOCATION, false);
			return;
		}
		
		if (!isPlayerInValidCommandChannel(attacker))
		{
			attacker.teleToLocation(GLUDIO_LOCATION, false);
		}
	}
	
	/**
	 * Checks if the {@code Player} is in a valid command channel.<br>
	 * This method verifies that the player belongs to a party.<br>
	 * It also ensures the party's {@code CommandChannel} has enough members.
	 * @param player The {@code Player} to check.
	 * @return {@code true} if the requirements are met, otherwise {@code false}.
	 */
	private boolean isPlayerInValidCommandChannel(Player player)
	{
		final Party party = player.getParty();
		if (party == null)
		{
			return false;
		}
		
		final CommandChannel cc = party.getCommandChannel();
		if ((cc == null) || (cc.getMemberCount() < REQUIRED_CC_MEMBERS))
		{
			return false;
		}
		
		return true;
	}
	
	/**
	 * Enables unique boss behaviors and mechanics.<br>
	 * This method starts timers for area skills and repeating actions.
	 * @param npc The {@code Npc} instance to apply these mechanics to.
	 */
	private void activateSpecialMechanics(Npc npc)
	{
		startQuestTimer("useAreaSkill", 1000, npc, null);
		startQuestTimer("repeatSpecialMechanics", 30000, npc, null, true);
	}
	
	/**
	 * Triggers a sequence of area-of-effect skills for the boss.<br>
	 * This method handles debuff immunity, skill disabling, and spawning invisible NPCs to cast additional skills.<br>
	 * It ensures the boss remains in combat before executing these actions.
	 * @param npc The {@code Npc} instance that will perform the area skills.
	 */
	private void useAreaSkill(Npc npc)
	{
		if (!_canUseSkill.get() || (npc == null) || npc.isDead() || !_bossInCombat)
		{
			return;
		}
		
		_isUsingAreaSkill.set(true);
		
		ThreadPool.schedule(() ->
		{
			if (!npc.isDead() && _bossInCombat)
			{
				_isDebuffImmunityActive = true;
				cancelDebuffs(npc);
				
				ThreadPool.schedule(() -> _isDebuffImmunityActive = false, 7000);
			}
		}, 1000);
		
		ThreadPool.schedule(() ->
		{
			if (_bossInCombat)
			{
				npc.disableSkill(COMMON_SKILL_1.getSkill(), 7000);
				npc.disableSkill(COMMON_SKILL_2.getSkill(), 7000);
				
				ThreadPool.schedule(() ->
				{
					if (!npc.isDead() && _bossInCombat)
					{
						_isUsingAreaSkill.set(false);
					}
				}, 6000);
			}
		}, 1000);
		
		npc.enableAllSkills();
		
		final Location bossLocation = npc.getLocation();
		final List<Npc> spawnedNpcs = new ArrayList<>();
		
		ThreadPool.schedule(() ->
		{
			if (!npc.isDead() && _bossInCombat)
			{
				npc.doCast(INITIAL_SKILL.getSkill());
			}
			
			for (int i = 0; i < 10; i++)
			{
				if (!_bossInCombat)
				{
					break;
				}
				
				final int offsetX = getRandom(-1000, 1000);
				final int offsetY = getRandom(-900, 1000);
				final Location targetLocation = new Location(bossLocation.getX() + offsetX, bossLocation.getY() + offsetY, bossLocation.getZ());
				final Npc invisibleNpc = addSpawn(INVISIBLE_NPC, targetLocation, false, NPC_LIFETIME);
				if (invisibleNpc != null)
				{
					spawnedNpcs.add(invisibleNpc);
					
					ThreadPool.schedule(() ->
					{
						if (!invisibleNpc.isDead() && SkillCaster.checkUseConditions(invisibleNpc, INITIAL_SKILL.getSkill()))
						{
							SkillCaster.triggerCast(invisibleNpc, invisibleNpc, INITIAL_SKILL.getSkill());
						}
					}, 1000);
					
					ThreadPool.schedule(() ->
					{
						if (!invisibleNpc.isDead() && SkillCaster.checkUseConditions(invisibleNpc, AREA_SKILL.getSkill()))
						{
							SkillCaster.triggerCast(invisibleNpc, invisibleNpc, AREA_SKILL.getSkill());
						}
					}, 4000);
				}
			}
		}, 4000);
	}
	
	/**
	 * Removes all active debuffs from the specified {@code Npc}.<br>
	 * This method only executes if the {@code Npc} is alive and immunity is active.<br>
	 * It iterates through all effects and stops those identified as debuffs.
	 * @param npc The {@code Npc} instance to clear debuffs from.
	 */
	private void cancelDebuffs(Npc npc)
	{
		if ((npc == null) || npc.isDead() || !_isDebuffImmunityActive)
		{
			return;
		}
		
		npc.getEffectList().getEffects().stream().filter(effect -> isDebuff(effect.getSkill())).forEach(effect -> npc.getEffectList().stopSkillEffects(SkillFinishType.REMOVED, effect.getSkill()));
	}
	
	/**
	 * Checks if the provided {@code Skill} object is a debuff.<br>
	 * It returns {@code true} if the skill exists and is a debuff.<br>
	 * It returns {@code false} otherwise.
	 * @param skill The {@code Skill} to check.
	 * @return {@code true} if the skill is a debuff, {@code false} otherwise.
	 */
	private boolean isDebuff(Skill skill)
	{
		return (skill != null) && skill.isDebuff();
	}
	
	/**
	 * Handles specific events for the Queen Ant boss logic.<br>
	 * This method processes healing actions, social behaviors, and distance checks.
	 * @param event The name of the event to trigger.
	 * @param npc The {@code Npc} instance involved in the event.
	 * @param player The {@code Player} instance involved in the event.
	 * @return The result of the super class {@code onEvent} method.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if ((npc == null) || (npc.getId() != QUEEN_ANT))
		{
			return null;
		}
		
		switch (event)
		{
			case "queen_ant_barrier_start":
			{
				break;
			}
			case "activate_barrier":
			{
				_barrierActivated = true;
				LIMIT_BARRIER.getSkill().applyEffects(npc, npc);
				npc.setInvul(true);
				_vulnerablePhase = false;
				startQuestTimer("remove_barrier", BARRIER_DURATION_MILLIS, npc, null);
				_queenAntHits.put(npc, 0);
				break;
			}
			case "remove_barrier":
			{
				_barrierActivated = false;
				npc.stopSkillEffects(LIMIT_BARRIER.getSkill());
				npc.setInvul(false);
				_queenAntHits.put(npc, 0);
				break;
			}
			case "check_arena":
			{
				if ((_spawnedMainBoss != null) && !_spawnedMainBoss.isDead())
				{
					checkBossInArena(_spawnedMainBoss);
				}
				break;
			}
			case "useAreaSkill":
			{
				useAreaSkill(npc);
				break;
			}
			case "repeatSpecialMechanics":
			{
				if (!npc.isDead() && _bossInCombat)
				{
					useAreaSkill(npc);
					startQuestTimer("repeatSpecialMechanics", 30000, npc, null, true);
				}
				else
				{
					cancelQuestTimer("repeatSpecialMechanics", npc, null);
				}
				break;
			}
			case "checkCombatStatus":
			{
				checkCombatStatus(npc);
				break;
			}
		}
		
		return super.onEvent(event, npc, player);
	}
	
	/**
	 * Updates the combat state of the boss based on time since the last attack.<br>
	 * It resets the combat flag and health milestones if no attacks occur for 10000 milliseconds.<br>
	 * This method also manages specific quest timers during active combat.
	 * @param npc The {@code Npc} instance to check.
	 */
	private void checkCombatStatus(Npc npc)
	{
		if (((System.currentTimeMillis() - _lastAttackTime) > 10000))
		{
			_bossInCombat = false;
			_hp85Reached = false;
			cancelQuestTimer("repeatSpecialMechanics", npc, null);
		}
		else
		{
			_bossInCombat = true;
		}
	}
	
	/**
	 * Verifies if the {@code Npc} is currently inside the arena zone.<br>
	 * If it is outside, the method teleports it back to the spawn location.<br>
	 * It also resets the health and mana of the {@code Npc} during this teleport.
	 * @param npc The {@code Npc} instance to check.
	 */
	private void checkBossInArena(Npc npc)
	{
		if ((npc == null) || npc.isDead())
		{
			return;
		}
		
		if (!ARENA_ZONE.isInsideZone(npc))
		{
			npc.teleToLocation(SPAWN_LOCATION, false);
			npc.setCurrentHp(npc.getMaxHp());
			npc.setCurrentMp(npc.getMaxMp());
		}
	}
	
	/**
	 * Handles the logic when an {@code Npc} is killed.<br>
	 * This method manages respawn timers and cleans up active instances for specific boss entities.<br>
	 * It also handles minion death logic for royal and nurse types.
	 * @param npc The {@code Npc} that was killed.
	 * @param killer The {@code Player} who performed the kill.
	 * @param isSummon A boolean indicating if the kill was a summon.
	 */
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		if (npc == _spawnedMainBoss)
		{
			cancelQuestTimer("repeatSpecialMechanics", npc, null);
			cancelQuestTimer("checkCombatStatus", npc, null);
			cancelQuestTimers("check_arena");
			GrandBossManager.getInstance().setStatus(QUEEN_ANT, 1);
			_spawnedMainBoss = null;
			_hp85Reached = false;
			_bossInCombat = false;
			
			final long currentTime = System.currentTimeMillis();
			GlobalVariablesManager.getInstance().set("QUEEN_ANT_LAST_DEATH_TIME", currentTime);
			// LOGGER.info("Queen Ant: Boss killed. Last death time recorded: " + currentTime + " / " + new java.util.Date(currentTime));
		}
	}
	
	/**
	 * This is the entry point for the {@code QueenAnt} class.<br>
	 * It initializes a new instance of the boss AI handler.
	 * @param args The command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		new QueenAnt();
	}
}
