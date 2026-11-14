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
package ai.bosses.Lilith;

import java.util.Calendar;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.gameserver.data.xml.MapRegionData;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.TeleportWhereType;
import org.l2jmobius.gameserver.entity.actor.instance.GrandBoss;
import org.l2jmobius.gameserver.entity.spawns.Spawn;
import org.l2jmobius.gameserver.entity.zone.ZoneType;
import org.l2jmobius.gameserver.managers.GrandBossManager;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.mechanics.skill.AbnormalType;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.util.ArrayUtil;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the artificial intelligence for the {@code Lilith} boss.<br>
 * It manages combat behaviors, skill usage, and special mechanics during encounters.
 * @author Notorion
 */
public class Lilith extends Script
{
	// Status
	private static final int ALIVE = 0;
	private static final int FIGHTING = 1;
	private static final int DEAD = 2;
	
	// NPCs
	private static final int LILITH = 29336;
	private static final int EXIST_CUBIC = 31124;
	private static final int LILITH_CUBIC = 31118;
	
	// General Configs
	private static final int MIN_LEVEL_TO_ATTACK = 110;
	
	// Minions
	// @formatter:off
	private static final int[] LILITH_MINIONS = {29337, 29338, 29339};
	// @formatter:on
	private static final int[] ALL_MOBS =
	{
		LILITH,
		LILITH_MINIONS[0],
		LILITH_MINIONS[1],
		LILITH_MINIONS[2]
	};
	
	// Locations
	private static final Location ENTER_LILITH_LOC = new Location(-6687, 21271, -5488);
	private static final Location SPAWN_LOC = new Location(-6682, 22181, -5488, 49151);
	private static final Location KICK_LOC = new Location(-14051, 22195, -3616);
	
	private static final ZoneType BOSS_ZONE = ZoneManager.getInstance().getZoneById(12005);
	
	// Barrier Skills
	private static final SkillHolder BARRIER_INITIAL = new SkillHolder(29518, 1);
	private static final SkillHolder BARRIER_TIMED = new SkillHolder(29515, 1);
	
	// Barrier Configuration
	private static final int HITS_TO_BREAK_INITIAL = 2000; // Hits to break immunity
	private static final int HITS_TO_BREAK_TIMED = 2000; // Hits to break timed barrier
	private static final long TIME_VULNERABLE = 3 * 60000; // Vulnerable duration (3 min)
	private static final long TIME_BARRIER_LIMIT = 10 * 60000; // Max barrier duration (10 min)
	private static final long TIME_RESET_INACTIVITY = 10 * 60000; // Reset if inactive
	
	// Variables
	private static long _lastAction;
	private static Npc _lilithBoss;
	
	// Barrier Controls
	private boolean _isInitialBarrier = false;
	private boolean _isTimedBarrier = false;
	private final Map<Npc, Integer> _hitCounter = new ConcurrentHashMap<>();
	private long _lastHitTime = 0;
	
	/**
	 * Initializes the {@code Lilith} boss logic.<br>
	 * Registers all necessary talk IDs and NPC interactions.<br>
	 * Sets up event listeners for attacks, kills, and skills.<br>
	 * Checks the current status of the boss to handle respawning or unlocking.
	 */
	public Lilith()
	{
		addTalkId(EXIST_CUBIC, LILITH_CUBIC);
		addStartNpc(EXIST_CUBIC, LILITH_CUBIC);
		addFirstTalkId(EXIST_CUBIC, LILITH_CUBIC);
		addAttackId(ALL_MOBS);
		addKillId(ALL_MOBS);
		addSkillSeeId(ALL_MOBS);
		
		final StatSet info = GrandBossManager.getInstance().getStatSet(LILITH);
		final int status = GrandBossManager.getInstance().getStatus(LILITH);
		if (status == DEAD)
		{
			long respawnTime = info.getLong("respawn_time");
			if (respawnTime == 0)
			{
				respawnTime = getNextRespawnTime();
				info.set("respawn_time", respawnTime);
				GrandBossManager.getInstance().setStatSet(LILITH, info);
			}
			
			final long delay = respawnTime - System.currentTimeMillis();
			if (delay > 0)
			{
				startQuestTimer("unlock_lilith", delay, null, null);
				// System.out.println("GrandBossManager: Lilith spawn scheduled in " + (delay / 60000) + " minutes.");
			}
			else
			{
				spawnLilith();
			}
		}
		else
		{
			spawnLilith();
		}
	}
	
	/**
	 * Spawns the Lilith boss into the game world.<br>
	 * This method initializes the boss state and adds it to the {@code GrandBossManager}.<br>
	 * It also applies the initial barrier and starts the activity timer.
	 */
	private void spawnLilith()
	{
		GrandBossManager.getInstance().setStatus(LILITH, ALIVE);
		
		if ((_lilithBoss != null) && !_lilithBoss.isDead())
		{
			return;
		}
		
		_lilithBoss = addSpawn(LILITH, SPAWN_LOC, false, 0);
		GrandBossManager.getInstance().addBoss((GrandBoss) _lilithBoss);
		
		// Immobilize Boss
		_lilithBoss.setRandomWalking(false);
		_lilithBoss.setRandomAnimation(false);
		
		applyInitialBarrier(_lilithBoss);
		
		_lastAction = System.currentTimeMillis();
		startQuestTimer("check_activity_task", 60000, null, null);
	}
	
	/**
	 * Handles specific game events for the Lilith boss.<br>
	 * This method processes logic such as unlocking the boss, spawning remnants, and managing timers.
	 * @param event The name of the event to trigger.
	 * @param npc The {@code Npc} instance involved in the event.
	 * @param player The {@code Player} instance involved in the event.
	 * @return The result of the super class {@code onEvent} method.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "unlock_lilith":
			{
				spawnLilith();
				break;
			}
			case "check_activity_task":
			{
				// Reset logic
				if ((_lastAction + TIME_RESET_INACTIVITY) < System.currentTimeMillis())
				{
					if ((_lilithBoss != null) && !_lilithBoss.isDead())
					{
						final boolean isFighting = GrandBossManager.getInstance().getStatus(LILITH) == FIGHTING;
						final boolean isDamaged = _lilithBoss.getCurrentHp() < _lilithBoss.getMaxHp();
						if (isFighting || isDamaged)
						{
							resetLilith(_lilithBoss);
						}
					}
				}
				else
				{
					// Minion Logic
					final boolean isBossAlive = ((_lilithBoss != null) && !_lilithBoss.isDead());
					final boolean isNotFighting = GrandBossManager.getInstance().getStatus(LILITH) != FIGHTING;
					final boolean hasPlayers = ((BOSS_ZONE != null) && !BOSS_ZONE.getPlayersInside().isEmpty());
					if (isBossAlive && isNotFighting && hasPlayers)
					{
						manageMinions(_lilithBoss);
					}
					
					startQuestTimer("check_activity_task", 60000, null, null);
				}
				break;
			}
			case "END_VULNERABILITY":
			{
				if ((npc != null) && !npc.isDead())
				{
					applyTimedBarrier(npc);
				}
				break;
			}
			case "END_TIMED_BARRIER":
			{
				if ((npc != null) && !npc.isDead() && _isTimedBarrier)
				{
					applyTimedBarrier(npc);
				}
				break;
			}
			case "cancel_timers":
			{
				cancelQuestTimer("check_activity_task", null, null);
				cancelQuestTimers("END_VULNERABILITY");
				cancelQuestTimers("END_TIMED_BARRIER");
				break;
			}
			case "exist":
			{
				player.teleToLocation(TeleportWhereType.TOWN);
				break;
			}
		}
		return super.onEvent(event, npc, player);
	}
	
	/**
	 * Handles the movement of minions within the boss zone.<br>
	 * It checks for all living NPCs in the area.<br>
	 * If an NPC is a minion, it teleports them to a random location near the boss.
	 * @param boss The {@code Npc} instance representing the boss.
	 */
	private void manageMinions(Npc boss)
	{
		if (BOSS_ZONE == null)
		{
			return;
		}
		
		for (Creature c : BOSS_ZONE.getCharactersInside())
		{
			if ((c != null) && c.isNpc() && !c.isDead())
			{
				if (ArrayUtil.contains(LILITH_MINIONS, c.getId()))
				{
					// Blink Effect
					int x = boss.getX() + getRandom(-150, 150);
					int y = boss.getY() + getRandom(-150, 150);
					c.teleToLocation(x, y, boss.getZ());
				}
			}
		}
	}
	
	/**
	 * Resets the {@code Lilith} boss to its initial state.<br>
	 * This method restores health and mana for the {@code Npc}.<br>
	 * It teleports the boss back to its spawn location and clears all active effects.<br>
	 * All players in the boss zone are teleported away during this process.
	 * @param npc The {@code Npc} instance of the boss to reset.
	 */
	private void resetLilith(Npc npc)
	{
		if (GrandBossManager.getInstance().getStatus(LILITH) != ALIVE)
		{
			GrandBossManager.getInstance().setStatus(LILITH, ALIVE);
		}
		
		npc.setCurrentHp(npc.getMaxHp());
		npc.setCurrentMp(npc.getMaxMp());
		npc.teleToLocation(SPAWN_LOC, true);
		npc.stopAllEffects();
		
		if (npc.isAttackable())
		{
			npc.asAttackable().clearAggroList();
		}
		
		if (BOSS_ZONE != null)
		{
			for (Creature c : BOSS_ZONE.getCharactersInside())
			{
				if ((c != null) && c.isPlayer())
				{
					c.teleToLocation(MapRegionData.getInstance().getTeleToLocation(c, TeleportWhereType.TOWN));
				}
			}
		}
		
		applyInitialBarrier(npc);
		_lastAction = System.currentTimeMillis();
		startQuestTimer("check_activity_task", 60000, null, null);
	}
	
	/**
	 * Handles the interaction logic when a {@code Player} talks to an {@code Npc}.<br>
	 * It checks for party requirements and level constraints before teleporting players.<br>
	 * This method also manages the spawning of the boss based on current status.
	 * @param npc The {@code Npc} entity being interacted with.
	 * @param player The {@code Player} initiating the conversation.
	 * @return A {@code String} representing the HTML message to display, or {@code null}.
	 */
	@Override
	public String onTalk(Npc npc, Player player)
	{
		if (npc.getId() == LILITH_CUBIC)
		{
			player.teleToLocation(ENTER_LILITH_LOC, true);
			return null;
		}
		
		return super.onTalk(npc, player);
	}
	
	/**
	 * Handles the logic for when a player talks to an {@code Npc} for the first time.<br>
	 * It returns the specific HTML file associated with that NPC.
	 * @param npc The {@code Npc} being interacted with.
	 * @param player The {@code Player} who initiated the conversation.
	 * @return A {@code String} representing the path to the HTML message.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return npc.getId() + ".html";
	}
	
	/**
	 * Handles logic when an {@code Npc} is attacked by a {@code Player}.<br>
	 * This method checks for zone boundaries and handles specific boss mechanics.
	 * @param npc The {@code Npc} being attacked.
	 * @param attacker The {@code Player} performing the attack.
	 * @param damage The amount of damage dealt to the {@code Npc}.
	 * @param isPet Whether the attack was performed by a pet.
	 */
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isPet)
	{
		_lastAction = System.currentTimeMillis();
		
		// Level Check
		if (attacker.getLevel() < MIN_LEVEL_TO_ATTACK)
		{
			attacker.teleToLocation(KICK_LOC, true);
			return;
		}
		
		if (npc.getId() == LILITH)
		{
			if (GrandBossManager.getInstance().getStatus(LILITH) != FIGHTING)
			{
				GrandBossManager.getInstance().setStatus(LILITH, FIGHTING);
			}
			
			// Wake up Minions
			if (BOSS_ZONE != null)
			{
				for (Creature c : BOSS_ZONE.getCharactersInside())
				{
					if ((c != null) && c.isNpc() && !c.isDead())
					{
						if (ArrayUtil.contains(LILITH_MINIONS, c.getId()))
						{
							c.asAttackable().addDamageHate(attacker, 0, 999);
							c.getAI().setIntentionAttack(attacker);
						}
					}
				}
			}
			
			if (_isInitialBarrier || _isTimedBarrier)
			{
				if ((System.currentTimeMillis() - _lastHitTime) > 60000)
				{
					_hitCounter.put(npc, 0);
				}
				_lastHitTime = System.currentTimeMillis();
				
				final int hits = _hitCounter.merge(npc, 1, Integer::sum);
				final int required = _isInitialBarrier ? HITS_TO_BREAK_INITIAL : HITS_TO_BREAK_TIMED;
				if (hits >= required)
				{
					enterVulnerableState(npc);
				}
			}
		}
		
		// Anti-Kite
		if (npc.isMinion() || npc.isRaid())
		{
			if ((BOSS_ZONE != null) && !BOSS_ZONE.isInsideZone(attacker))
			{
				attacker.doDie(null);
			}
			if ((BOSS_ZONE != null) && !BOSS_ZONE.isInsideZone(npc))
			{
				final Spawn spawn = npc.getSpawn();
				if (spawn != null)
				{
					npc.teleToLocation(spawn.getX(), spawn.getY(), spawn.getZ());
				}
				else
				{
					npc.teleToLocation(SPAWN_LOC, true);
				}
			}
		}
	}
	
	/**
	 * This method is called when an {@code Npc} is killed.<br>
	 * It handles the logic for spawning new entities and updating boss statuses.<br>
	 * It manages quest timers and respawn data based on which entity died.
	 * @param npc The {@code Npc} that was killed.
	 * @param killer The {@code Player} who dealt the killing blow.
	 * @param isPet Whether the kill was performed by a pet.
	 */
	@Override
	public void onKill(Npc npc, Player killer, boolean isPet)
	{
		if (npc.getId() == LILITH)
		{
			notifyEvent("cancel_timers", null, null);
			
			// Fix: Spawn Cubic at Boss location
			addSpawn(EXIST_CUBIC, npc.getLocation(), false, 900000);
			
			GrandBossManager.getInstance().setStatus(LILITH, DEAD);
			
			final long respawnTime = getNextRespawnTime();
			final StatSet info = GrandBossManager.getInstance().getStatSet(LILITH);
			info.set("respawn_time", respawnTime);
			GrandBossManager.getInstance().setStatSet(LILITH, info);
			
			startQuestTimer("unlock_lilith", respawnTime - System.currentTimeMillis(), null, null);
			
			_lilithBoss = null;
		}
	}
	
	/**
	 * Handles logic when an {@code Npc} casts a specific {@code Skill}.<br>
	 * This method checks if the caster is a minion and triggers aggro behavior.<br>
	 * It forces the minion to target the {@code Player} if certain conditions are met.
	 * @param npc The {@code Npc} performing the action.
	 * @param caster The {@code Player} who cast the skill.
	 * @param skill The {@code Skill} being used.
	 * @param targets A collection of {@code WorldObject} entities targeted by the skill.
	 * @param isPet Whether the action was performed by a pet.
	 */
	@Override
	public void onSkillSee(Npc npc, Player caster, Skill skill, Collection<WorldObject> targets, boolean isPet)
	{
		if (ArrayUtil.contains(LILITH_MINIONS, npc.getId()) && getRandomBoolean())
		{
			if (skill.getAbnormalType() == AbnormalType.HP_RECOVER)
			{
				if (!npc.isCastingNow())
				{
					final WorldObject target = npc.getTarget();
					if ((target != npc) && (target != caster) && (target != _lilithBoss))
					{
						npc.asAttackable().clearAggroList();
						npc.setTarget(caster);
						npc.asAttackable().addDamageHate(caster, 500, 99999);
						npc.getAI().setIntentionAttack(caster);
					}
				}
			}
		}
	}
	
	/**
	 * Sets up the initial barrier state for a specific {@code Npc}.<br>
	 * This method clears existing barriers and makes the entity invulnerable.<br>
	 * It also applies the required skill effects and resets internal counters.
	 * @param npc The {@code Npc} instance to receive the initial barrier.
	 */
	private void applyInitialBarrier(Npc npc)
	{
		cleanBarriers(npc);
		_isInitialBarrier = true;
		_isTimedBarrier = false;
		_hitCounter.put(npc, 0);
		
		npc.setInvul(true);
		if (BARRIER_INITIAL.getSkill() != null)
		{
			BARRIER_INITIAL.getSkill().applyEffects(npc, npc);
		}
		
		cancelQuestTimer("END_VULNERABILITY", npc, null);
		cancelQuestTimer("END_TIMED_BARRIER", npc, null);
	}
	
	/**
	 * Applies a timed barrier effect to the specified {@code Npc}.<br>
	 * This method clears previous barriers and sets the NPC to an invulnerable state.<br>
	 * It also starts a quest timer to manage the duration of the barrier.
	 * @param npc The {@code Npc} instance to receive the timed barrier.
	 */
	private void applyTimedBarrier(Npc npc)
	{
		cleanBarriers(npc);
		_isInitialBarrier = false;
		_isTimedBarrier = true;
		_hitCounter.put(npc, 0);
		
		npc.setInvul(true);
		if (BARRIER_TIMED.getSkill() != null)
		{
			BARRIER_TIMED.getSkill().applyEffects(npc, npc);
		}
		
		startQuestTimer("END_TIMED_BARRIER", TIME_BARRIER_LIMIT, npc, null);
	}
	
	/**
	 * Removes all active barriers from the {@code Npc}.<br>
	 * Sets the boss to a vulnerable state by disabling invulnerability.<br>
	 * Resets internal counters and starts a timer for the vulnerability period.
	 * @param npc The {@code Npc} instance to modify.
	 */
	private void enterVulnerableState(Npc npc)
	{
		cleanBarriers(npc);
		_isInitialBarrier = false;
		_isTimedBarrier = false;
		_hitCounter.put(npc, 0);
		
		npc.setInvul(false);
		startQuestTimer("END_VULNERABILITY", TIME_VULNERABLE, npc, null);
	}
	
	/**
	 * Removes active barrier effects from the {@code Npc}.<br>
	 * This method stops specific skill effects and sets invulnerability to {@code false}.
	 * @param npc The {@code Npc} instance to clean.
	 */
	private void cleanBarriers(Npc npc)
	{
		final Skill initialSkill = BARRIER_INITIAL.getSkill();
		if (initialSkill != null)
		{
			npc.stopSkillEffects(initialSkill);
		}
		
		final Skill timedSkill = BARRIER_TIMED.getSkill();
		if (timedSkill != null)
		{
			npc.stopSkillEffects(timedSkill);
		}
		
		npc.setInvul(false);
	}
	
	// NEW SCHEDULE: Saturday 20:00
	/**
	 * Calculates the timestamp for the next scheduled respawn.<br>
	 * The boss is set to appear every Saturday at 20:00.<br>
	 * It checks if the time has already passed and moves to the following week if necessary.
	 * @return The system time in milliseconds for the next spawn event.
	 */
	private long getNextRespawnTime()
	{
		final Calendar now = Calendar.getInstance();
		
		// Set to Saturday 20:00
		final Calendar nextSpawn = (Calendar) now.clone();
		nextSpawn.set(Calendar.HOUR_OF_DAY, 20);
		nextSpawn.set(Calendar.MINUTE, 0);
		nextSpawn.set(Calendar.SECOND, 0);
		nextSpawn.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);
		
		// If Saturday 20:00 already passed this week, move to next week
		if (nextSpawn.getTimeInMillis() < System.currentTimeMillis())
		{
			nextSpawn.add(Calendar.WEEK_OF_YEAR, 1);
		}
		
		return nextSpawn.getTimeInMillis();
	}
	
	/**
	 * This is the entry point for the {@code Lilith} class.<br>
	 * It initializes a new instance of the {@code Lilith} object.
	 * @param args The command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		new Lilith();
	}
}
