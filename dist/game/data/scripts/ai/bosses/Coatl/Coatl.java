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
package ai.bosses.Coatl;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.data.SpawnTable;
import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.Summon;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.entity.spawns.Spawn;
import org.l2jmobius.gameserver.entity.zone.type.ArenaZone;
import org.l2jmobius.gameserver.managers.DatabaseSpawnManager;
import org.l2jmobius.gameserver.managers.GlobalVariablesManager;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.mechanics.skill.BuffInfo;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.SkillCaster;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;

/**
 * This class handles the artificial intelligence for the {@code Coatl} boss.<br>
 * It manages the behavior, skills, and logic of the entity during combat.
 * @author Notorion
 */
public class Coatl extends Script
{
	// NPCs
	private static final int MAIN_BOSS_ID = 29408; // Coatl RaidBoss
	private static final int INVISIBLE_COATL_ID = 29413; // Coatl invisible
	private static final int FLAME_TOTEM_ID = 29409;
	private static final int KASHA_TOTEM_ID = 29412;
	private static final int EARTH_TOTEM_ID = 29411;
	private static final int WATER_TOTEM_ID = 29410;
	private static final int[] TOTEM_IDS =
	{
		WATER_TOTEM_ID,
		KASHA_TOTEM_ID,
		EARTH_TOTEM_ID,
		FLAME_TOTEM_ID
	};
	
	// Locations
	private static final Location FLAME_TOTEM_LOCATION = new Location(-83613, 209856, -5248, 371);
	private static final Location KASHA_TOTEM_LOCATION = new Location(-84417, 210668, -5254, 16503);
	private static final Location EARTH_TOTEM_LOCATION = new Location(-85226, 209857, -5258, 32689);
	private static final Location WATER_TOTEM_LOCATION = new Location(-84415, 209044, -5258, 49298);
	private static final Location INVISIBLE_COATL_LOCATION = new Location(-84416, 209860, -5254);
	private static final Location SPAWN_LOCATION = new Location(-84416, 209860, -5254);
	private static final Location GLUDIO_EXIT = new Location(-14575, 121425, -3011); // Kick player loc
	
	// Skills
	private static final SkillHolder WATER_TOTEM_SKILL = new SkillHolder(34807, 1); // Totem Protection - Water
	private static final SkillHolder EARTH_TOTEM_SKILL = new SkillHolder(34808, 1); // Totem Protection - Ice
	private static final SkillHolder KASHA_TOTEM_SKILL = new SkillHolder(34809, 1); // Totem Protection - Kasha
	private static final int FLAME_TOTEM_SKILL_ID = 34806; // Totem Protection - Fire
	private static final SkillHolder FLAME_EXPLOSION_SKILL = new SkillHolder(34805, 1); // Coatl's Rage - Fire
	private static final SkillHolder EARTH_EXPLOSION_SKILL = new SkillHolder(34811, 1); // Coatl's Rage - Ice
	private static final SkillHolder KASHA_EXPLOSION_SKILL = new SkillHolder(34812, 1); // Coatl's Rage - Kasha
	private static final SkillHolder WATER_EXPLOSION_SKILL = new SkillHolder(34813, 1); // Coatl's Rage - Water
	private static final SkillHolder SKILL_WATER = new SkillHolder(34807, 1);
	private static final SkillHolder SKILL_KASHA = new SkillHolder(34809, 1);
	private static final SkillHolder SKILL_EARTH = new SkillHolder(34808, 1);
	private static final SkillHolder SKILL_FLAME = new SkillHolder(34806, 1);
	private static final SkillHolder SKILL_1 = new SkillHolder(34789, 1); // Coatl Skill - Coatl's Fireball
	private static final SkillHolder SKILL_2 = new SkillHolder(34790, 1); // Coatl Skill - Coatl's Stone Throw
	private static final SkillHolder SKILL_3 = new SkillHolder(34791, 1); // Coatl Skill - Coatl's Kasha Wave
	private static final SkillHolder SKILL_4 = new SkillHolder(34792, 1); // Coatl Skill - Coatl's Frozen Field
	
	private static final SkillHolder BARRIER_INITIAL = new SkillHolder(29518, 1); // Barrier 1
	private static final SkillHolder BARRIER_TIMED = new SkillHolder(29515, 1); // Barrier 2
	
	private static final int HITS_TO_BREAK = 2000; // 2000 hits
	private static final long TIME_RESET_INACTIVITY = 10 * 60000; // 10 Minutes reset HP
	
	// Misc
	private static final ArenaZone ARENA_ZONE = ZoneManager.getInstance().getZoneByName("Coatls_Lair", ArenaZone.class);
	
	private final Set<Integer> _involvedPlayers = new HashSet<>();
	private final Map<Creature, Integer> _aggroList = new ConcurrentHashMap<>();
	private final Map<String, Object[]> _timerParameters = new HashMap<>();
	private final Queue<Runnable> _specialMechanicsQueue = new ArrayDeque<>();
	
	private boolean _isInitialBarrier = false;
	private boolean _isTimedBarrier = false;
	private final Map<Npc, Integer> _hitCounter = new ConcurrentHashMap<>();
	private long _lastAction;
	
	private boolean _combatLoopStarted = false;
	private boolean _specialMechanicsActive = false;
	private boolean _hp76Triggered = false;
	private boolean _hp70Triggered = false;
	private boolean _hp50Triggered = false;
	private boolean _hp40Triggered = false;
	private boolean _hp30Triggered = false;
	private boolean _hp10Triggered = false;
	private static Npc _spawnedMainBoss;
	private static Npc _invisibleCoatl;
	private static Npc _flameTotem;
	private static Npc _kashaTotem;
	private static Npc _earthTotem;
	private static Npc _waterTotem;
	
	/**
	 * Private constructor for the {@code Coatl} class.<br>
	 * This constructor initializes the boss spawning logic.<br>
	 * It sets up the weekly spawn schedule and starts the quest timer.
	 */
	private Coatl()
	{
		addAttackId(MAIN_BOSS_ID);
		addKillId(MAIN_BOSS_ID);
		
		final long currentTime = System.currentTimeMillis();
		final Calendar calendar = Calendar.getInstance();
		
		// Spawn time to 21:00 Monday.
		calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
		calendar.set(Calendar.HOUR_OF_DAY, 21);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		
		if (GlobalVariablesManager.getInstance().getBoolean("COATL_ALIVE", true))
		{
			spawnCoatl();
		}
		
		if (calendar.getTimeInMillis() < currentTime)
		{
			calendar.add(Calendar.WEEK_OF_YEAR, 1);
		}
		
		ThreadPool.scheduleAtFixedRate(this::spawnCoatl, calendar.getTimeInMillis() - currentTime, 604800000);
		startQuestTimer("check_arena", 10000, null, null, true);
	}
	
	/**
	 * Initializes the boss encounter by spawning all required entities.<br>
	 * It removes old spawns and creates the main boss at its location.<br>
	 * The method also places several totems and sets global variables to true.<br>
	 * Finally, it applies initial barriers and starts the activity timer.
	 */
	private void spawnCoatl()
	{
		final Spawn oldSpawn = SpawnTable.getInstance().getAnySpawn(MAIN_BOSS_ID);
		if (oldSpawn != null)
		{
			final Npc lastSpawn = oldSpawn.getLastSpawn();
			if (lastSpawn != null)
			{
				lastSpawn.deleteMe();
			}
			
			SpawnTable.getInstance().removeSpawn(oldSpawn);
		}
		
		try
		{
			final NpcTemplate template = NpcData.getInstance().getTemplate(MAIN_BOSS_ID);
			final Spawn spawn = new Spawn(template);
			spawn.setXYZ(SPAWN_LOCATION);
			spawn.setHeading(0);
			spawn.setRespawnDelay(0);
			_spawnedMainBoss = DatabaseSpawnManager.getInstance().addNewSpawn(spawn, false);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		GlobalVariablesManager.getInstance().set("COATL_ALIVE", true);
		// System.out.println("Coatl Manager: Coatl (29408) spawned successfully.");
		
		// Spawn.
		_invisibleCoatl = addSpawn(INVISIBLE_COATL_ID, INVISIBLE_COATL_LOCATION);
		_invisibleCoatl.setImmobilized(true);
		_flameTotem = addSpawn(FLAME_TOTEM_ID, FLAME_TOTEM_LOCATION);
		_flameTotem.setImmobilized(true);
		_kashaTotem = addSpawn(KASHA_TOTEM_ID, KASHA_TOTEM_LOCATION);
		_kashaTotem.setImmobilized(true);
		_earthTotem = addSpawn(EARTH_TOTEM_ID, EARTH_TOTEM_LOCATION);
		_earthTotem.setImmobilized(true);
		_waterTotem = addSpawn(WATER_TOTEM_ID, WATER_TOTEM_LOCATION);
		_waterTotem.setImmobilized(true);
		
		applyInitialBarrier(_spawnedMainBoss);
		
		_lastAction = System.currentTimeMillis();
		startQuestTimer("check_activity_task", 60000, null, null);
	}
	
	/**
	 * Handles various game events for the boss mechanics.<br>
	 * It processes skills, totem effects, and arena checks based on the provided event string.
	 * @param event The name of the event to process.
	 * @param npc The {@code Npc} instance involved in the event.
	 * @param player The {@code Player} instance involved in the event.
	 * @return The result of the super class method call.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "check_arena":
			{
				if (_spawnedMainBoss != null)
				{
					checkPlayersInArena();
					checkBossHP();
				}
				break;
			}
			case "check_activity_task":
			{
				if ((_lastAction + TIME_RESET_INACTIVITY) < System.currentTimeMillis())
				{
					if ((_spawnedMainBoss != null) && !_spawnedMainBoss.isDead())
					{
						if (_spawnedMainBoss.getCurrentHp() < _spawnedMainBoss.getMaxHp())
						{
							resetCoatl(_spawnedMainBoss);
						}
					}
				}
				else
				{
					startQuestTimer("check_activity_task", 60000, null, null);
				}
				break;
			}
			case "SKILL_LOOP":
			{
				if ((_spawnedMainBoss != null) && !_spawnedMainBoss.isDead())
				{
					if (!_specialMechanicsActive && !_spawnedMainBoss.isCastingNow())
					{
						manageSkills(_spawnedMainBoss);
					}
					
					boolean playersInZone = false;
					if ((ARENA_ZONE != null) && !ARENA_ZONE.getCharactersInside().isEmpty())
					{
						for (Creature c : ARENA_ZONE.getCharactersInside())
						{
							if (c.isPlayer() && !c.isDead())
							{
								playersInZone = true;
								break;
							}
						}
					}
					
					if (playersInZone)
					{
						startQuestTimer("SKILL_LOOP", 250, _spawnedMainBoss, null);
					}
					else
					{
						_combatLoopStarted = false;
					}
				}
				else
				{
					_combatLoopStarted = false;
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
			case "castWaterTotemSkill":
			{
				if (npc != null)
				{
					npc.setTarget(npc);
					npc.doCast(WATER_TOTEM_SKILL.getSkill());
				}
				break;
			}
			case "castKashaTotemSkill":
			{
				if (npc != null)
				{
					npc.setTarget(npc);
					npc.doCast(KASHA_TOTEM_SKILL.getSkill());
				}
				break;
			}
			case "castEarthTotemSkill":
			{
				if (npc != null)
				{
					npc.setTarget(npc);
					npc.doCast(EARTH_TOTEM_SKILL.getSkill());
				}
				break;
			}
			case "castFlameTotemSkill":
			{
				if (npc != null)
				{
					npc.setTarget(npc);
					final Skill flameTotemSkill = SkillData.getInstance().getSkill(FLAME_TOTEM_SKILL_ID, 1);
					if (flameTotemSkill != null)
					{
						npc.doCast(flameTotemSkill);
					}
				}
				break;
			}
			case "dispel_boss_buffs":
			{
				dispelBossBuffs(_spawnedMainBoss);
				break;
			}
			case "waterTotemMechanicsEnd":
			{
				final Object[] parameters = _timerParameters.get("waterTotemMechanicsEnd");
				if (parameters != null)
				{
					final int waterBuffId = (int) parameters[0];
					final int effectSkillId = (int) parameters[1];
					cancelQuestTimer("coatl_common_skills", _spawnedMainBoss, null);
					killPlayersInArena(waterBuffId);
					castSkillOnTotems(SkillData.getInstance().getSkill(effectSkillId, 1));
					cancelTotemTimers();
				}
				break;
			}
			case "kashaTotemMechanicsEnd":
			{
				final Object[] parameters = _timerParameters.get("kashaTotemMechanicsEnd");
				if (parameters != null)
				{
					final int kashaBuffId = (int) parameters[0];
					final int effectSkillId = (int) parameters[1];
					cancelQuestTimer("coatl_common_skills", _spawnedMainBoss, null);
					killPlayersInArena(kashaBuffId);
					castSkillOnTotems(SkillData.getInstance().getSkill(effectSkillId, 1));
					cancelTotemTimers();
				}
				break;
			}
			case "earthTotemMechanicsEnd":
			{
				final Object[] parameters = _timerParameters.get("earthTotemMechanicsEnd");
				if (parameters != null)
				{
					final int earthBuffId = (int) parameters[0];
					final int effectSkillId = (int) parameters[1];
					cancelQuestTimer("coatl_common_skills", _spawnedMainBoss, null);
					killPlayersInArena(earthBuffId);
					castSkillOnTotems(SkillData.getInstance().getSkill(effectSkillId, 1));
					cancelTotemTimers();
				}
				break;
			}
			case "flameTotemMechanicsEnd":
			{
				final Object[] parameters = _timerParameters.get("flameTotemMechanicsEnd");
				if (parameters != null)
				{
					final int flameBuffId = (int) parameters[0];
					final int effectSkillId = (int) parameters[1];
					cancelQuestTimer("coatl_common_skills", _spawnedMainBoss, null);
					killPlayersInArena(flameBuffId);
					castSkillOnTotems(SkillData.getInstance().getSkill(effectSkillId, 1));
					cancelTotemTimers();
				}
				break;
			}
			case "castWaterExplosionSkill":
			{
				if (npc != null)
				{
					npc.setTarget(npc);
					npc.doCast(WATER_EXPLOSION_SKILL.getSkill());
				}
				break;
			}
			case "castKashaExplosionSkill":
			{
				if (npc != null)
				{
					npc.setTarget(npc);
					npc.doCast(KASHA_EXPLOSION_SKILL.getSkill());
				}
				break;
			}
			case "castEarthExplosionSkill":
			{
				if (npc != null)
				{
					npc.setTarget(npc);
					npc.doCast(EARTH_EXPLOSION_SKILL.getSkill());
				}
				break;
			}
			case "castFlameExplosionSkill":
			{
				if (npc != null)
				{
					npc.setTarget(npc);
					npc.doCast(FLAME_EXPLOSION_SKILL.getSkill());
				}
				break;
			}
		}
		
		return super.onEvent(event, npc, player);
	}
	
	/**
	 * Updates the aggro level for a specific creature.<br>
	 * This method calculates a new value based on damage and random factors.<br>
	 * It ensures the highest aggro value is stored in the internal list.
	 * @param attacker The {@code Creature} that initiated the attack.
	 * @param damage The amount of damage dealt by the attacker.
	 */
	private void addAggro(Creature attacker, int damage)
	{
		if ((attacker == null) || attacker.isDead())
		{
			return;
		}
		
		final int newAggroVal = damage + getRandom(3000);
		final int aggroVal = _aggroList.getOrDefault(attacker, 0) + 1000;
		if (aggroVal < newAggroVal)
		{
			_aggroList.put(attacker, newAggroVal);
		}
	}
	
	/**
	 * Handles the logic for selecting and casting skills for an {@code Npc}.<br>
	 * It checks if the {@code Npc} is currently busy or if special mechanics are active.<br>
	 * The method updates the aggro list and selects a target to attack.<br>
	 * It then chooses a skill based on random weights and attempts to cast it.
	 * @param npc The {@code Npc} instance whose skills need to be managed.
	 */
	private void manageSkills(Npc npc)
	{
		if (npc.isCastingNow() || npc.isCoreAIDisabled())
		{
			return;
		}
		
		if (_specialMechanicsActive)
		{
			return;
		}
		
		if (npc.isAttackingNow())
		{
			npc.abortAttack();
		}
		
		_aggroList.forEach((attacker, aggro) ->
		{
			if ((attacker == null) || attacker.isDead() || (npc.calculateDistance3D(attacker) > 3000))
			{
				_aggroList.remove(attacker);
			}
		});
		
		final Creature target = getTargetLogic(npc);
		if ((target == null) || target.isDead())
		{
			return;
		}
		
		npc.setTarget(target);
		
		SkillHolder skillToCast = null;
		final int randomSkill = getRandom(100);
		
		if (randomSkill < 20)
		{
			skillToCast = SKILL_4; // 20%
		}
		else if (randomSkill < 40)
		{
			skillToCast = SKILL_3; // 20%
		}
		else if (randomSkill < 70)
		{
			skillToCast = SKILL_2; // 30%
		}
		else
		{
			skillToCast = SKILL_1; // 30%
		}
		
		final Skill skillToCastSkill = skillToCast.getSkill();
		if (SkillCaster.checkUseConditions(npc, skillToCastSkill))
		{
			npc.doCast(skillToCastSkill);
		}
		else if (SkillCaster.checkUseConditions(npc, SKILL_2.getSkill()))
		{
			npc.doCast(SKILL_2.getSkill());
		}
		else if (SkillCaster.checkUseConditions(npc, SKILL_1.getSkill()))
		{
			npc.doCast(SKILL_1.getSkill());
		}
	}
	
	/**
	 * Stops the active timers for all totem skills.<br>
	 * This method cancels specific quest timers for water, kasha, earth, and flame totems.
	 */
	private void cancelTotemTimers()
	{
		cancelQuestTimer("castWaterTotemSkill", _waterTotem, null);
		cancelQuestTimer("castKashaTotemSkill", _kashaTotem, null);
		cancelQuestTimer("castEarthTotemSkill", _earthTotem, null);
		cancelQuestTimer("castFlameTotemSkill", _flameTotem, null);
	}
	
	/**
	 * This method identifies players within the {@code ARENA_ZONE}.<br>
	 * It checks if each player is missing a specific buff.<br>
	 * Players without the required buff are killed by the boss.
	 * @param buffId The unique identifier of the required buff.
	 */
	private void killPlayersInArena(int buffId)
	{
		for (Creature creature : ARENA_ZONE.getCharactersInside())
		{
			if (!creature.isPlayer() && !creature.isSummon())
			{
				continue;
			}
			
			boolean hasBuff = false;
			for (BuffInfo effect : creature.getEffectList().getEffects())
			{
				if (effect.getSkill().getId() == buffId)
				{
					hasBuff = true;
					break;
				}
			}
			
			if (!hasBuff)
			{
				creature.doDie(_spawnedMainBoss);
			}
		}
	}
	
	/**
	 * This method triggers a specific skill on all active totems.<br>
	 * It checks if each totem exists before casting the skill.<br>
	 * Each totem is set to target itself during the cast.
	 * @param skill The {@code Skill} object to be cast by the totems.
	 */
	public void castSkillOnTotems(Skill skill)
	{
		if (_waterTotem != null)
		{
			_waterTotem.setTarget(_waterTotem);
			_waterTotem.doCast(skill);
		}
		
		if (_kashaTotem != null)
		{
			_kashaTotem.setTarget(_kashaTotem);
			_kashaTotem.doCast(skill);
		}
		
		if (_earthTotem != null)
		{
			_earthTotem.setTarget(_earthTotem);
			_earthTotem.doCast(skill);
		}
		
		if (_flameTotem != null)
		{
			_flameTotem.setTarget(_flameTotem);
			_flameTotem.doCast(skill);
		}
	}
	
	/**
	 * Verifies the status of players within the arena zone.<br>
	 * Resets the boss position and health if it leaves the designated area.<br>
	 * Teleports players who target the boss from outside the zone to a specific exit.<br>
	 * Updates the list of involved players based on their current location.
	 */
	private void checkPlayersInArena()
	{
		if ((_spawnedMainBoss != null) && !ARENA_ZONE.isInsideZone(_spawnedMainBoss))
		{
			_spawnedMainBoss.stopMove(null);
			_spawnedMainBoss.setXYZ(SPAWN_LOCATION.getX(), SPAWN_LOCATION.getY(), SPAWN_LOCATION.getZ());
			_spawnedMainBoss.setCurrentHp(_spawnedMainBoss.getMaxHp());
			_spawnedMainBoss.broadcastPacket(new org.l2jmobius.gameserver.network.serverpackets.ValidateLocation(_spawnedMainBoss));
		}
		
		if (ARENA_ZONE != null)
		{
			for (Creature creature : ARENA_ZONE.getCharactersInside())
			{
				if (!creature.isPlayer())
				{
					continue;
				}
				if ((creature.getTarget() == _spawnedMainBoss) && !ARENA_ZONE.isInsideZone(creature))
				{
					creature.teleToLocation(GLUDIO_EXIT, true);
					continue;
				}
				if (ARENA_ZONE.isInsideZone(creature))
				{
					final int objectId = creature.getObjectId();
					if (!_involvedPlayers.contains(objectId))
					{
						_involvedPlayers.add(objectId);
					}
				}
			}
		}
	}
	
	/**
	 * Handles the logic when an {@code Npc} is attacked by a {@code Player}.<br>
	 * It checks if the attacker is inside the required zone.<br>
	 * If the attacker is outside, it teleports them and handles summons if {@code isSummon} is {@code true}.<br>
	 * It also manages boss barriers and updates aggro based on damage.
	 * @param npc The {@code Npc} being attacked.
	 * @param attacker The {@code Player} performing the attack.
	 * @param damage The amount of damage dealt.
	 * @param isSummon Whether the attack was performed by a summon.
	 */
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	{
		_lastAction = System.currentTimeMillis();
		
		if ((attacker != null) && (ARENA_ZONE != null) && !ARENA_ZONE.isInsideZone(attacker))
		{
			if (isSummon)
			{
				for (Summon summon : attacker.getServitors().values())
				{
					if (summon != null)
					{
						summon.unSummon(attacker);
					}
				}
			}
			attacker.teleToLocation(GLUDIO_EXIT, true);
			return;
		}
		
		if (npc.getId() == MAIN_BOSS_ID)
		{
			if (!_combatLoopStarted)
			{
				_combatLoopStarted = true;
				startQuestTimer("SKILL_LOOP", 1000, npc, null);
			}
			
			if (_isInitialBarrier || _isTimedBarrier)
			{
				int hits = _hitCounter.merge(npc, 1, Integer::sum);
				if (hits >= HITS_TO_BREAK)
				{
					enterVulnerableState(npc);
				}
			}
		}
		
		checkBossHP();
		addAggro(attacker, damage);
	}
	
	/**
	 * Checks the current health percentage of the main boss.<br>
	 * Triggers specific mechanics when the boss reaches certain HP thresholds.<br>
	 * Handles the death logic if the boss health reaches 0.
	 */
	private void checkBossHP()
	{
		if (_spawnedMainBoss != null)
		{
			final double currentHP = _spawnedMainBoss.getCurrentHp();
			final long maxHP = _spawnedMainBoss.getMaxHp();
			final int currentHPPercentage = (int) ((currentHP / maxHP) * 100);
			if (currentHPPercentage <= 0)
			{
				onNpcDeath(_spawnedMainBoss);
				return;
			}
			
			final boolean isCastingNow = _spawnedMainBoss.isCastingNow();
			if ((currentHPPercentage <= 76) && !_hp76Triggered && !isCastingNow)
			{
				_hp76Triggered = true;
				_specialMechanicsQueue.offer(this::startRandomTotemMechanics);
				processSpecialMechanicsQueue();
			}
			else if ((currentHPPercentage <= 70) && !_hp70Triggered && !isCastingNow)
			{
				_hp70Triggered = true;
				_specialMechanicsQueue.offer(this::startRandomTotemMechanics);
				processSpecialMechanicsQueue();
			}
			else if ((currentHPPercentage <= 50) && !_hp50Triggered && !isCastingNow)
			{
				_hp50Triggered = true;
				_specialMechanicsQueue.offer(this::startRandomTotemMechanics);
				processSpecialMechanicsQueue();
			}
			else if ((currentHPPercentage <= 40) && !_hp40Triggered && !isCastingNow)
			{
				_hp40Triggered = true;
				_specialMechanicsQueue.offer(this::startRandomTotemMechanics);
				processSpecialMechanicsQueue();
			}
			else if ((currentHPPercentage <= 30) && !_hp30Triggered && !isCastingNow)
			{
				_hp30Triggered = true;
				_specialMechanicsQueue.offer(this::startRandomTotemMechanics);
				processSpecialMechanicsQueue();
			}
			else if ((currentHPPercentage <= 10) && !_hp10Triggered && !isCastingNow)
			{
				_hp10Triggered = true;
				_specialMechanicsQueue.offer(this::startRandomTotemMechanics);
				processSpecialMechanicsQueue();
			}
		}
	}
	
	/**
	 * Handles the execution of special boss mechanics.<br>
	 * This method checks if a new mechanic is waiting in {@code _specialMechanicsQueue}.<br>
	 * If one exists and none are currently active, it polls and runs the next {@code Runnable}.
	 */
	private void processSpecialMechanicsQueue()
	{
		if (!_specialMechanicsActive && !_specialMechanicsQueue.isEmpty())
		{
			_specialMechanicsActive = true;
			final Runnable mechanic = _specialMechanicsQueue.poll();
			mechanic.run();
		}
	}
	
	/**
	 * Removes specific active buffs from the boss.<br>
	 * This method checks if the {@code Npc} is alive before clearing effects.<br>
	 * It stops skills for water, kasha, earth, and flame elements.<br>
	 * It also removes a specific effect with ID {@code 46797}.
	 * @param boss The {@code Npc} instance to clear buffs from.
	 */
	private void dispelBossBuffs(Npc boss)
	{
		if ((boss != null) && !boss.isDead())
		{
			boss.stopSkillEffects(SKILL_WATER.getSkill());
			boss.stopSkillEffects(SKILL_KASHA.getSkill());
			boss.stopSkillEffects(SKILL_EARTH.getSkill());
			boss.stopSkillEffects(SKILL_FLAME.getSkill());
			boss.stopSkillEffects(null, 46797);
		}
	}
	
	/**
	 * Initiates special mechanics when the boss reaches 78% health.<br>
	 * This method triggers specific skills for all active totems.<br>
	 * It sets each totem to target itself and starts their respective skill casts.
	 */
	private void start78PercentMechanics()
	{
		if (_waterTotem != null)
		{
			_waterTotem.setTarget(_waterTotem);
			_waterTotem.doCast(WATER_TOTEM_SKILL.getSkill());
			startQuestTimer("castWaterTotemSkill", 3100, _waterTotem, null, true);
		}
		
		if (_kashaTotem != null)
		{
			_kashaTotem.setTarget(_kashaTotem);
			_kashaTotem.doCast(KASHA_TOTEM_SKILL.getSkill());
			startQuestTimer("castKashaTotemSkill", 3100, _kashaTotem, null, true);
		}
		
		if (_earthTotem != null)
		{
			_earthTotem.setTarget(_earthTotem);
			_earthTotem.doCast(EARTH_TOTEM_SKILL.getSkill());
			startQuestTimer("castEarthTotemSkill", 3100, _earthTotem, null, true);
		}
		
		if (_flameTotem != null)
		{
			_flameTotem.setTarget(_flameTotem);
			
			final Skill flameTotemSkill = SkillData.getInstance().getSkill(FLAME_TOTEM_SKILL_ID, 1);
			if (flameTotemSkill != null)
			{
				_flameTotem.doCast(flameTotemSkill);
				startQuestTimer("castFlameTotemSkill", 3100, _flameTotem, null, true);
			}
		}
	}
	
	/**
	 * Selects and starts a random totem mechanic for the boss.<br>
	 * It picks one of the four available types using {@code Rnd}.<br>
	 * After starting the mechanic, it resets the active status and processes the queue.<br>
	 * Finally, it starts a quest timer to dispel boss buffs.
	 */
	private void startRandomTotemMechanics()
	{
		final int totemIndex = Rnd.get(TOTEM_IDS.length);
		switch (totemIndex)
		{
			case 0: // Water
			{
				startWaterTotemMechanics();
				break;
			}
			case 1: // Kasha
			{
				startKashaTotemMechanics();
				break;
			}
			case 2: // Earth
			{
				startEarthTotemMechanics();
				break;
			}
			case 3: // Flame
			{
				startFlameTotemMechanics();
				break;
			}
		}
		
		_specialMechanicsActive = false;
		processSpecialMechanicsQueue();
		
		startQuestTimer("dispel_boss_buffs", 250, null, null, true);
	}
	
	/**
	 * Initializes the water totem mechanics for the boss.<br>
	 * Applies protection skills to the main boss.<br>
	 * Triggers specific cast skills for both the main and invisible bosses.<br>
	 * Starts the 78 percent health mechanics.<br>
	 * Sets up timers for water explosion effects.
	 */
	private void startWaterTotemMechanics()
	{
		final int bossMainCastSkillId = 34800;
		final int coatlInvisibleSkillId = 34796;
		final int waterBuffId = 34807;
		final int effectSkillId = 34810;
		final Skill kashaProtection = SkillData.getInstance().getSkill(34816, 1);
		if (kashaProtection != null)
		{
			kashaProtection.applyEffects(_spawnedMainBoss, _spawnedMainBoss);
		}
		
		Skill skill = SkillData.getInstance().getSkill(bossMainCastSkillId, 1);
		if (!_spawnedMainBoss.isSkillDisabled(skill))
		{
			_spawnedMainBoss.setTarget(_spawnedMainBoss);
			_spawnedMainBoss.doCast(skill);
		}
		
		if (_invisibleCoatl != null)
		{
			skill = SkillData.getInstance().getSkill(coatlInvisibleSkillId, 1);
			if (!_invisibleCoatl.isSkillDisabled(skill))
			{
				_invisibleCoatl.setTarget(_invisibleCoatl);
				_invisibleCoatl.doCast(skill);
			}
		}
		
		start78PercentMechanics();
		_timerParameters.put("waterTotemMechanicsEnd", new Object[]
		{
			waterBuffId,
			effectSkillId
		});
		
		startQuestTimer("waterTotemMechanicsEnd", 9000L, _spawnedMainBoss, null);
		startQuestTimer("castWaterExplosionSkill", 9000L, _invisibleCoatl, null);
	}
	
	/**
	 * Initializes the Kasha totem mechanics for the boss.<br>
	 * Applies protection skills to the main boss.<br>
	 * Triggers specific cast skills for both the main and invisible bosses.<br>
	 * Starts the 78 percent health mechanics.<br>
	 * Sets up timers for the explosion effects.
	 */
	private void startKashaTotemMechanics()
	{
		final int bossMainCastSkillId = 34799;
		final int coatlInvisibleSkillId = 34795;
		final int kashaBuffId = 34809;
		final int effectSkillId = 34810;
		final Skill kashaProtection = SkillData.getInstance().getSkill(34816, 1);
		if (kashaProtection != null)
		{
			kashaProtection.applyEffects(_spawnedMainBoss, _spawnedMainBoss);
		}
		
		Skill skill = SkillData.getInstance().getSkill(bossMainCastSkillId, 1);
		if (!_spawnedMainBoss.isSkillDisabled(skill))
		{
			_spawnedMainBoss.setTarget(_spawnedMainBoss);
			_spawnedMainBoss.doCast(skill);
		}
		
		if (_invisibleCoatl != null)
		{
			skill = SkillData.getInstance().getSkill(coatlInvisibleSkillId, 1);
			if (!_invisibleCoatl.isSkillDisabled(skill))
			{
				_invisibleCoatl.setTarget(_invisibleCoatl);
				_invisibleCoatl.doCast(skill);
			}
		}
		
		start78PercentMechanics();
		_timerParameters.put("kashaTotemMechanicsEnd", new Object[]
		{
			kashaBuffId,
			effectSkillId
		});
		
		startQuestTimer("kashaTotemMechanicsEnd", 9000L, _spawnedMainBoss, null);
		startQuestTimer("castKashaExplosionSkill", 9000L, _invisibleCoatl, null);
	}
	
	/**
	 * Initializes the mechanics for the Earth Totem. <br>
	 * It applies protection skills to the main boss and triggers specific cast sequences. <br>
	 * This method also starts the 78 percent health mechanics and sets up timers for explosions.
	 */
	private void startEarthTotemMechanics()
	{
		final int bossMainCastSkillId = 34798;
		final int coatlInvisibleSkillId = 34794;
		final int earthBuffId = 34808;
		final int effectSkillId = 34810;
		final Skill kashaProtection = SkillData.getInstance().getSkill(34816, 1);
		if (kashaProtection != null)
		{
			kashaProtection.applyEffects(_spawnedMainBoss, _spawnedMainBoss);
		}
		
		Skill skill = SkillData.getInstance().getSkill(bossMainCastSkillId, 1);
		if (!_spawnedMainBoss.isSkillDisabled(skill))
		{
			_spawnedMainBoss.setTarget(_spawnedMainBoss);
			_spawnedMainBoss.doCast(skill);
		}
		
		if (_invisibleCoatl != null)
		{
			skill = SkillData.getInstance().getSkill(coatlInvisibleSkillId, 1);
			if (!_invisibleCoatl.isSkillDisabled(skill))
			{
				_invisibleCoatl.setTarget(_invisibleCoatl);
				_invisibleCoatl.doCast(skill);
			}
		}
		
		start78PercentMechanics();
		_timerParameters.put("earthTotemMechanicsEnd", new Object[]
		{
			earthBuffId,
			effectSkillId
		});
		
		startQuestTimer("earthTotemMechanicsEnd", 9000L, _spawnedMainBoss, null);
		startQuestTimer("castEarthExplosionSkill", 9000L, _invisibleCoatl, null);
	}
	
	/**
	 * Initializes the flame totem mechanics for the boss fight.<br>
	 * This method applies protection skills and triggers specific cast sequences.<br>
	 * It also starts timers for the flame explosion effects.
	 */
	private void startFlameTotemMechanics()
	{
		final int bossMainCastSkillId = 34797;
		final int coatlInvisibleSkillId = 34793;
		final int flameBuffId = 34806;
		final int effectSkillId = 34810;
		final Skill kashaProtection = SkillData.getInstance().getSkill(34816, 1);
		if (kashaProtection != null)
		{
			kashaProtection.applyEffects(_spawnedMainBoss, _spawnedMainBoss);
		}
		
		Skill skill = SkillData.getInstance().getSkill(bossMainCastSkillId, 1);
		if (!_spawnedMainBoss.isSkillDisabled(skill))
		{
			_spawnedMainBoss.setTarget(_spawnedMainBoss);
			_spawnedMainBoss.doCast(skill);
		}
		
		if (_invisibleCoatl != null)
		{
			skill = SkillData.getInstance().getSkill(coatlInvisibleSkillId, 1);
			if (!_invisibleCoatl.isSkillDisabled(skill))
			{
				_invisibleCoatl.setTarget(_invisibleCoatl);
				_invisibleCoatl.doCast(skill);
			}
		}
		
		start78PercentMechanics();
		_timerParameters.put("flameTotemMechanicsEnd", new Object[]
		{
			flameBuffId,
			effectSkillId
		});
		
		startQuestTimer("flameTotemMechanicsEnd", 9000L, _spawnedMainBoss, null);
		startQuestTimer("castFlameExplosionSkill", 9000L, _invisibleCoatl, null);
	}
	
	/**
	 * This method is called when an {@code Npc} is killed.<br>
	 * It resets the boss state and clears all active quest timers.<br>
	 * It also removes all spawned totems from the world.
	 * @param npc The {@code Npc} that was killed.
	 * @param killer The {@code Player} who dealt the killing blow.
	 * @param isSummon Indicates if the kill was performed by a summon.
	 */
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		cancelQuestTimers("check_arena");
		cancelQuestTimer("remove_barrier", npc, null);
		cancelQuestTimer("check_activity_task", npc, null);
		cancelQuestTimer("END_VULNERABILITY", npc, null);
		cancelQuestTimer("END_TIMED_BARRIER", npc, null);
		
		_combatLoopStarted = false;
		_spawnedMainBoss = null;
		GlobalVariablesManager.getInstance().set("COATL_ALIVE", false);
		// System.out.println("RaidBoss: Coatl(29408) status is 2 (Dead)");
		if (_invisibleCoatl != null)
		{
			_invisibleCoatl.deleteMe();
		}
		
		if (_flameTotem != null)
		{
			_flameTotem.deleteMe();
		}
		
		if (_kashaTotem != null)
		{
			_kashaTotem.deleteMe();
		}
		
		if (_earthTotem != null)
		{
			_earthTotem.deleteMe();
		}
		
		if (_waterTotem != null)
		{
			_waterTotem.deleteMe();
		}
		
		_invisibleCoatl = null;
		_flameTotem = null;
		_kashaTotem = null;
		_earthTotem = null;
		_waterTotem = null;
		
		cancelQuestTimers("dispel_boss_buffs");
	}
	
	/**
	 * Handles the logic that occurs when an {@code Npc} dies.<br>
	 * This method checks if the dead entity is the main boss.<br>
	 * If it is, it triggers the kill sequence and clears player data.<br>
	 * It also ensures any invisible boss instances become invincible.
	 * @param npc The {@code Npc} that has died.
	 */
	public void onNpcDeath(Npc npc)
	{
		if (npc == _spawnedMainBoss)
		{
			onKill(npc, null, false);
			
			_involvedPlayers.clear();
			if (_invisibleCoatl != null)
			{
				_invisibleCoatl.setInvul(true);
			}
		}
	}
	
	/**
	 * Sets up the initial barrier state for a specific {@code Npc}.<br>
	 * This method clears existing barriers and makes the NPC invulnerable.<br>
	 * It also applies the initial barrier skill effects if they are defined.
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
	 * Clears existing barriers and applies a new timed barrier to the {@code Npc}.<br>
	 * Sets the NPC to an invulnerable state.<br>
	 * Applies the skill effects from {@code BARRIER_TIMED} if available.<br>
	 * Starts a quest timer for 600000 milliseconds.
	 * @param npc The {@code Npc} instance to receive the barrier.
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
		
		startQuestTimer("END_TIMED_BARRIER", 600000, npc, null);
	}
	
	/**
	 * Removes all active barriers from the {@code Npc}.<br>
	 * Sets the invulnerability status to {@code false}.<br>
	 * Resets the hit counter for this entity.<br>
	 * Starts a random timer to end the vulnerable state.
	 * @param npc The {@code Npc} instance entering the vulnerable state.
	 */
	private void enterVulnerableState(Npc npc)
	{
		cleanBarriers(npc);
		_isInitialBarrier = false;
		_isTimedBarrier = false;
		_hitCounter.put(npc, 0);
		
		npc.setInvul(false);
		
		final long randomTime = Rnd.get(20000, 60000);
		startQuestTimer("END_VULNERABILITY", randomTime, npc, null);
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
	
	/**
	 * Resets the {@code Npc} to its initial state.<br>
	 * This method restores health and mana values.<br>
	 * It teleports the entity to the spawn location and clears all active effects.<br>
	 * It also resets combat flags and timers.
	 * @param npc The {@code Npc} instance to reset.
	 */
	private void resetCoatl(Npc npc)
	{
		npc.setCurrentHp(npc.getMaxHp());
		npc.setCurrentMp(npc.getMaxMp());
		npc.teleToLocation(SPAWN_LOCATION, true);
		npc.stopAllEffects();
		cancelQuestTimer("SKILL_LOOP", npc, null);
		_combatLoopStarted = false;
		
		if (npc.isAttackable())
		{
			npc.asAttackable().clearAggroList();
		}
		
		applyInitialBarrier(npc);
		_lastAction = System.currentTimeMillis();
		startQuestTimer("check_activity_task", 60000, null, null);
	}
	
	/**
	 * Determines the next {@code Creature} for the boss to attack.<br>
	 * It selects the highest aggro target or a random nearby player in the arena.<br>
	 * If a summon is targeted, it switches focus to the owner of that summon.
	 * @param npc The {@code Npc} instance performing the logic.
	 * @return The selected {@code Creature} target or {@code null}.
	 */
	private Creature getTargetLogic(Npc npc)
	{
		Creature target = _aggroList.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(null);
		
		if (Rnd.get(100) < 30)
		{
			final List<Player> validTargets = new ArrayList<>();
			if (ARENA_ZONE != null)
			{
				for (Player p : ARENA_ZONE.getPlayersInside())
				{
					if ((p != null) && !p.isDead() && !p.isInvul() && p.isSpawned() && (npc.calculateDistance3D(p) <= 1000))
					{
						validTargets.add(p);
					}
				}
			}
			
			if (!validTargets.isEmpty())
			{
				target = validTargets.get(Rnd.get(validTargets.size()));
			}
		}
		
		if ((target == null) || target.isDead())
		{
			target = (Creature) npc.getTarget();
		}
		
		if ((target != null) && target.isSummon())
		{
			Player owner = target.asSummon().getOwner();
			if ((owner != null) && !owner.isDead() && (ARENA_ZONE != null) && ARENA_ZONE.isInsideZone(owner))
			{
				target = owner;
			}
		}
		
		return target;
	}
	
	/**
	 * Checks if the main boss has been spawned.<br>
	 * Returns {@code true} if {@code _spawnedMainBoss} is not {@code null}.<br>
	 * Otherwise, it returns {@code false}.
	 * @return a boolean indicating the active status of the boss.
	 */
	public boolean isCoatlActive()
	{
		return _spawnedMainBoss != null;
	}
	
	/**
	 * This is the main entry point for the {@code Coatl} class.<br>
	 * It initializes a new instance of the {@code Coatl} object.
	 * @param args The command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		new Coatl();
	}
}
