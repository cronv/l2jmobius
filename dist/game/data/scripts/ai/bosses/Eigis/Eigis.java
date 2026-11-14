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
package ai.bosses.Eigis;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.data.SpawnTable;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.spawns.Spawn;
import org.l2jmobius.gameserver.entity.zone.ZoneType;
import org.l2jmobius.gameserver.entity.zone.type.ArenaZone;
import org.l2jmobius.gameserver.managers.DatabaseSpawnManager;
import org.l2jmobius.gameserver.managers.GlobalVariablesManager;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.enums.SkillFinishType;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillUse;

/**
 * This class handles the artificial intelligence and behavior for the {@code Eigis} boss.<br>
 * It manages specific combat patterns, skill usage, and state logic for this entity.
 * @author Notorion
 */
public class Eigis extends Script
{
	// NPCs
	private static final int EIGIS = 29385;
	private static final Location EIGIS_LOCATION = new Location(-23172, -222237, -3504, 0);
	private static final String EIGIS_ALIVE_VAR = "EIGIS_ALIVE";
	private static final int INVISIBLE_NPC = 18919;
	private static final int INVISIBLE_NPC_2 = 18920;
	
	// Skills
	private static final SkillHolder COMMON_SKILL = new SkillHolder(34108, 1); // Blade of Souls
	private static final SkillHolder SPECIAL_AOE_1 = new SkillHolder(34113, 1); // Dark Wave Spray
	private static final SkillHolder SPECIAL_AOE_VISUAL_2 = new SkillHolder(34114, 1); // Gate of Thousand Flashes
	private static final SkillHolder JUMP_TARGET_VISUAL_1 = new SkillHolder(34116, 1); // Dark Shadow Clash
	private static final SkillHolder JUMP_SKILL_DAMAGE_2 = new SkillHolder(34109, 1); // Blade Temptation
	private static final SkillHolder JUMP_IMPACT_VISUAL_3 = new SkillHolder(34110, 1); // Price for Temptation
	private static final SkillHolder NPC_AOE_SKILL = new SkillHolder(34115, 1); // Tear to Shreds
	private static final int NPC_LIFETIME = 6000;
	
	// Zone
	private static final ArenaZone EIGIS_ZONE = ZoneManager.getInstance().getZoneByName("Eigis_Seat_Zone", ArenaZone.class);
	
	// Barrier
	private static final int BARRIER_DURATION_MILLIS = 600000; // 10 minutes
	private static final int HIT_COUNT = 2000; // 2000 Number of attacks needed to destroy the barrier
	private static final int HIT_COUNT_RENEW = 500; // 500 hits in 60 seconds to continue without the barrier
	private static final int RENEW_DURATION_MILLIS = 600000; // 60 seconds of vulnerability
	private static final SkillHolder LIMIT_BARRIER = new SkillHolder(29515, 1);
	private boolean _barrierActivated = false;
	
	// Misc
	private boolean _specialActivated = false;
	private final AtomicBoolean _isUsingSpecialSkill = new AtomicBoolean(false);
	private final AtomicBoolean _isUsingSpecialSkill2 = new AtomicBoolean(false);
	private final AtomicBoolean _isUsingSpecialSkill3 = new AtomicBoolean(false);
	private boolean _bossInCombat = false;
	private final AtomicInteger _targetLossCount = new AtomicInteger(0);
	private boolean _vulnerablePhase = false;
	private final Map<Npc, Integer> _eigisHits = new ConcurrentHashMap<>();
	
	/**
	 * Initializes the {@code Eigis} boss logic.<br>
	 * Registers all necessary event IDs for the boss.<br>
	 * Sets up the weekly spawn and despawn schedules.<br>
	 * Checks the current status to trigger initial spawning or removal.
	 */
	public Eigis()
	{
		addAttackId(EIGIS);
		addSpawnId(EIGIS);
		addKillId(EIGIS);
		addAggroRangeEnterId(EIGIS);
		addExitZoneId(EIGIS_ZONE.getId());
		
		final long currentTime = System.currentTimeMillis();
		Calendar calendarEigisStart = Calendar.getInstance();
		Calendar calendarEigisSeal = Calendar.getInstance();
		
		calendarEigisStart.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
		calendarEigisStart.set(Calendar.HOUR_OF_DAY, 23);
		calendarEigisStart.set(Calendar.MINUTE, 0);
		calendarEigisStart.set(Calendar.SECOND, 0);
		
		calendarEigisSeal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
		calendarEigisSeal.set(Calendar.HOUR_OF_DAY, 2);
		calendarEigisSeal.set(Calendar.MINUTE, 0);
		calendarEigisSeal.set(Calendar.SECOND, 0);
		
		if (calendarEigisStart.getTimeInMillis() < currentTime)
		{
			calendarEigisStart.add(Calendar.WEEK_OF_YEAR, 1);
		}
		
		if (calendarEigisSeal.getTimeInMillis() < currentTime)
		{
			calendarEigisSeal.add(Calendar.WEEK_OF_YEAR, 1);
		}
		
		boolean bossAlive = GlobalVariablesManager.getInstance().getBoolean(EIGIS_ALIVE_VAR, false);
		if (bossAlive && (SpawnTable.getInstance().getAnySpawn(EIGIS) == null))
		{
			spawnEigis();
		}
		else if (!bossAlive && (SpawnTable.getInstance().getAnySpawn(EIGIS) != null))
		{
			despawnEigis();
		}
		
		ThreadPool.scheduleAtFixedRate(this::spawnEigis, calendarEigisStart.getTimeInMillis() - currentTime, 604800000L); // 7 days
		
		ThreadPool.scheduleAtFixedRate(this::despawnEigis, calendarEigisSeal.getTimeInMillis() - currentTime, 604800000L); // 7 days
	}
	
	/**
	 * Handles the creation of the {@code Eigis} NPC in the game world.<br>
	 * It checks if the boss is already alive before spawning it.<br>
	 * This method sets up the initial spawn properties and updates global variables.
	 */
	private void spawnEigis()
	{
		if (!GlobalVariablesManager.getInstance().getBoolean(EIGIS_ALIVE_VAR, false) || (SpawnTable.getInstance().getAnySpawn(EIGIS) == null))
		{
			final Npc npc = addSpawn(EIGIS, EIGIS_LOCATION);
			npc.setRandomWalking(false);
			npc.setRandomAnimation(false);
			
			final Spawn spawn = npc.getSpawn();
			spawn.setRespawnDelay(0);
			spawn.startRespawn();
			DatabaseSpawnManager.getInstance().addNewSpawn(spawn, true);
			
			GlobalVariablesManager.getInstance().set(EIGIS_ALIVE_VAR, true);
			// LOGGER.info("Eigis spawned.");
		}
	}
	
	/**
	 * Removes all active instances of the {@code Eigis} boss from the world.<br>
	 * This method deletes every spawned {@code Npc} associated with the boss.<br>
	 * It also updates the global variable to set the boss status to {@code false}.
	 */
	private void despawnEigis()
	{
		if (SpawnTable.getInstance().getAnySpawn(EIGIS) != null)
		{
			for (Npc npc : SpawnTable.getInstance().getAnySpawn(EIGIS).getSpawnedNpcs())
			{
				npc.deleteMe();
			}
			
			GlobalVariablesManager.getInstance().set(EIGIS_ALIVE_VAR, false);
			// LOGGER.info("Eigis has been despawned.");
		}
	}
	
	/**
	 * This method is called when the {@code Npc} is spawned.<br>
	 * It resets the combat status to {@code false}.<br>
	 * It starts several quest timers for the {@code npc}.
	 * @param npc The {@code Npc} instance that was just spawned.
	 */
	@Override
	public void onSpawn(Npc npc)
	{
		_bossInCombat = false;
		startQuestTimer("checkCombatStatus", 1000, npc, null, true);
		startQuestTimer("checkPosition", 5000, npc, null, true);
	}
	
	/**
	 * This method is called when a {@code Player} enters the aggro range of an {@code Npc}.<br>
	 * It sets the boss to combat mode and resets specific quest timers.<br>
	 * It also checks if special mechanics should be activated based on health levels.
	 * @param npc The {@code Npc} instance that is being entered by a player.
	 * @param player The {@code Player} instance that entered the aggro range.
	 * @param isSummon A boolean indicating if the interaction involves a summon.
	 */
	@Override
	public void onAggroRangeEnter(Npc npc, Player player, boolean isSummon)
	{
		_bossInCombat = true;
		cancelQuestTimer("checkTargetLost", npc, null);
		checkCombatStatus(npc);
		if (_specialActivated && (npc.getCurrentHp() < (npc.getMaxHp() * 0.99)))
		{
			activateSpecialMechanics(npc);
		}
	}
	
	/**
	 * This method is called when a {@code Creature} leaves a specific {@code ZoneType}.<br>
	 * It checks if the creature is the boss {@code EIGIS}.<br>
	 * If it is, the boss teleports to its default location.<br>
	 * The boss also clears its current target and cancels special skills.
	 * @param creature The {@code Creature} that exited the zone.
	 * @param zone The {@code ZoneType} that was exited.
	 */
	@Override
	public void onExitZone(Creature creature, ZoneType zone)
	{
		if ((creature instanceof Npc) && (creature.getId() == EIGIS))
		{
			final Npc npc = creature.asNpc();
			npc.teleToLocation(EIGIS_LOCATION);
			npc.setTarget(null);
			cancelSpecialSkills(npc);
		}
	}
	
	/**
	 * Handles the logic when an {@code Npc} is attacked by a {@code Player}.<br>
	 * This method manages boss phases, invulnerability states, and special mechanics.<br>
	 * It tracks hit counts to trigger barrier effects or vulnerability windows.
	 * @param npc The {@code Npc} being attacked.
	 * @param attacker The {@code Player} performing the attack.
	 * @param damage The amount of damage dealt.
	 * @param isSummon Whether the attacker is a summon.
	 * @param skill The {@code Skill} used for the attack.
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
			_eigisHits.put(npc, 0);
		}
		
		if (_vulnerablePhase)
		{
			int hits = _eigisHits.getOrDefault(npc, 0) + 1;
			_eigisHits.put(npc, hits);
			
			if (hits >= HIT_COUNT_RENEW)
			{
				cancelQuestTimer("activate_barrier", npc, null);
				startQuestTimer("activate_barrier", RENEW_DURATION_MILLIS, npc, null);
				_eigisHits.put(npc, 0);
			}
		}
		else
		{
			int hits = _eigisHits.getOrDefault(npc, 0) + 1;
			_eigisHits.put(npc, hits);
			
			if (hits >= HIT_COUNT)
			{
				npc.stopSkillEffects(LIMIT_BARRIER.getSkill());
				npc.setInvul(false);
				cancelQuestTimer("remove_barrier", npc, null);
				_vulnerablePhase = true;
				startQuestTimer("activate_barrier", RENEW_DURATION_MILLIS, npc, null);
				_eigisHits.put(npc, 0);
			}
		}
		
		_bossInCombat = true;
		if (!_specialActivated && (npc.getCurrentHp() < (npc.getMaxHp() * 0.99)))
		{
			_specialActivated = true;
			activateSpecialMechanics(npc);
		}
	}
	
	/**
	 * Enables unique behaviors for the boss during combat.<br>
	 * This method starts a quest timer to balance skill usage.
	 * @param npc The {@code Npc} instance to update.
	 */
	private void activateSpecialMechanics(Npc npc)
	{
		if (_bossInCombat)
		{
			startQuestTimer("SkillsBalancer", 2000, npc, null);
		}
	}
	
	/**
	 * Handles specific boss events and logic updates.<br>
	 * This method processes various triggers like barrier activation, skill usage, and target tracking.<br>
	 * It ensures the {@code Npc} behaves correctly based on the provided {@code event} string.
	 * @param event The name of the event to process.
	 * @param npc The {@code Npc} instance for which the event is triggered.
	 * @param player The {@code Player} involved in the interaction, if any.
	 * @return The result of the superclass {@code onEvent} method.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if ((npc != null) && (npc.getId() == EIGIS))
		{
			switch (event)
			{
				case "activate_barrier":
				{
					_barrierActivated = true;
					LIMIT_BARRIER.getSkill().applyEffects(npc, npc);
					npc.setInvul(true);
					_vulnerablePhase = false;
					startQuestTimer("remove_barrier", BARRIER_DURATION_MILLIS, npc, null);
					_eigisHits.put(npc, 0);
					break;
				}
				case "remove_barrier":
				{
					_barrierActivated = false;
					npc.stopSkillEffects(LIMIT_BARRIER.getSkill());
					npc.setInvul(false);
					_eigisHits.put(npc, 0);
					break;
				}
				case "EigisSkills1":
				{
					if (_bossInCombat && !_isUsingSpecialSkill2.get() && !_isUsingSpecialSkill3.get())
					{
						useEigisSkills1(npc);
					}
					break;
				}
				case "EigisSkills2":
				{
					if (_bossInCombat && !_isUsingSpecialSkill.get() && !_isUsingSpecialSkill3.get())
					{
						useEigisSkills2(npc);
					}
					break;
				}
				case "EigisSkills3":
				{
					if (_bossInCombat && !_isUsingSpecialSkill.get() && !_isUsingSpecialSkill2.get())
					{
						useEigisSkills3(npc);
					}
					break;
				}
				case "SkillsBalancer":
				{
					if (_bossInCombat)
					{
						if (!npc.isDead() && !_isUsingSpecialSkill.get() && !_isUsingSpecialSkill2.get() && !_isUsingSpecialSkill3.get())
						{
							boolean hasValidTarget = false;
							for (Creature target : npc.getAttackByList())
							{
								if ((target instanceof Player) && !target.isDead() && npc.isInsideRadius3D(target, 2000))
								{
									hasValidTarget = true;
									break;
								}
							}
							
							if (hasValidTarget)
							{
								final int chance = getRandom(100);
								if (chance < 33)
								{
									useEigisSkills1(npc);
								}
								else if (chance < 66)
								{
									useEigisSkills2(npc);
								}
								else
								{
									useEigisSkills3(npc);
								}
								
								startQuestTimer("SkillsBalancer", getRandom(10000, 35000), npc, null);
							}
							else
							{
								cancelQuestTimer("SkillsBalancer", npc, null);
								_bossInCombat = false;
								_specialActivated = false;
								resetSkillFlags();
							}
						}
					}
					else
					{
						cancelQuestTimer("SkillsBalancer", npc, null);
					}
					break;
				}
				case "checkTargetRange":
				{
					if ((player != null) && !player.isDead())
					{
						final double distance = player.calculateDistance3D(npc);
						if ((distance < 700) || (distance > 2000))
						{
							_targetLossCount.incrementAndGet();
							if (_targetLossCount.get() >= 2)
							{
								cancelSkill3(npc);
							}
							else
							{
								AtomicReference<Player> newTarget = new AtomicReference<>();
								if (findTargetPlayer(npc, newTarget))
								{
									player = newTarget.get();
								}
								else
								{
									cancelSkill3(npc);
								}
							}
						}
					}
					else
					{
						cancelSkill3(npc);
					}
					break;
				}
				case "checkCombatStatus":
				{
					checkCombatStatus(npc);
					break;
				}
				case "checkTargetLost":
				{
					cancelSpecialSkills(npc);
					break;
				}
				case "checkPosition":
				{
					if (!EIGIS_ZONE.isInsideZone(npc))
					{
						npc.teleToLocation(EIGIS_LOCATION);
						npc.setTarget(null);
					}
					break;
				}
			}
		}
		
		return super.onEvent(event, npc, player);
	}
	
	/**
	 * Executes the first set of special skills for the boss.<br>
	 * This method checks if the boss is in combat and not already using a skill.<br>
	 * It disables common skills, clears debuffs, and schedules various actions like casting AOE attacks.
	 * @param npc The {@code Npc} instance performing the skills.
	 */
	private void useEigisSkills1(Npc npc)
	{
		if (!_bossInCombat || !_isUsingSpecialSkill.compareAndSet(false, true))
		{
			return;
		}
		
		if (npc.getAttackByList().isEmpty())
		{
			return;
		}
		
		npc.disableSkill(COMMON_SKILL.getSkill(), 8000);
		cancelDebuffs(npc);
		
		ThreadPool.schedule(() ->
		{
			if (!npc.isDead() && _bossInCombat)
			{
				final boolean useComeToMe = Rnd.get(100) < 48;
				if (useComeToMe)
				{
					ThreadPool.schedule(() ->
					{
						useComeToMeSkill(npc);
					}, 1000);
				}
				
				ThreadPool.schedule(() ->
				{
					if (!npc.isDead() && _bossInCombat)
					{
						npc.doCast(SPECIAL_AOE_1.getSkill());
					}
				}, 3000);
				
				ThreadPool.schedule(() ->
				{
					if (!npc.isDead() && _bossInCombat && _isUsingSpecialSkill.get())
					{
						npc.broadcastPacket(new MagicSkillUse(npc, npc, SPECIAL_AOE_VISUAL_2.getSkillId(), 1, 0, 0));
					}
				}, 4000);
			}
			
			ThreadPool.schedule(() ->
			{
				resetSkillFlags();
			}, 4000);
			
		}, 1000);
	}
	
	// Presentation skill Lv.1 Teleport (34388)
	/**
	 * Executes the "Come To Me" skill for a specific NPC.<br>
	 * This method casts the skill and schedules a delayed action.
	 * @param npc The {@code Npc} instance that will perform the skill.
	 */
	private void useComeToMeSkill(Npc npc)
	{
		npc.doCast(SkillData.getInstance().getSkill(34388, 1));
	}
	
	/**
	 * Removes all active debuffs from the specified NPC.<br>
	 * This method checks if the {@code Npc} is alive before processing.<br>
	 * It iterates through all effects and stops those identified as debuffs.
	 * @param npc The {@code Npc} instance to clear debuffs from.
	 */
	private void cancelDebuffs(Npc npc)
	{
		if ((npc == null) || npc.isDead())
		{
			return;
		}
		
		npc.getEffectList().getEffects().forEach(effect ->
		{
			if (effect.getSkill().isDebuff())
			{
				npc.getEffectList().stopSkillEffects(SkillFinishType.REMOVED, effect.getSkill());
			}
		});
	}
	
	/**
	 * Executes the second set of special skills for the boss.<br>
	 * This method handles complex mechanics including summoning invisible NPCs and casting area-of-effect skills.<br>
	 * It manages skill cooldowns, debuff removal, and scheduled tasks using {@code ThreadPool}.
	 * @param npc The {@code Npc} instance of the boss performing the actions.
	 */
	private void useEigisSkills2(Npc npc)
	{
		if (!_bossInCombat || !_isUsingSpecialSkill2.compareAndSet(false, true))
		{
			return;
		}
		
		npc.disableSkill(COMMON_SKILL.getSkill(), 7000);
		cancelDebuffs(npc);
		
		ThreadPool.schedule(() ->
		{
			if (Rnd.get(100) < 45)
			{
				useComeToMeSkill(npc);
			}
		}, 1000);
		
		// Presentation Skill Lv.1 Tear to Shreds (34392)
		ThreadPool.schedule(() ->
		{
			npc.doCast(SkillData.getInstance().getSkill(34392, 1));
		}, 3000);
		
		if (npc.getAttackByList().isEmpty())
		{
			return;
		}
		
		if (EIGIS_ZONE == null)
		{
			return;
		}
		
		ThreadPool.schedule(() ->
		{
			final Location bossLocation = npc.getLocation();
			final List<Npc> spawnedNpcs = new ArrayList<>();
			for (int i = 0; i < 6; i++)
			{
				for (int j = 0; j < 6; j++)
				{
					int x = bossLocation.getX() + (int) ((i - 2.5) * 500);
					int y = bossLocation.getY() + (int) ((j - 2.5) * 500);
					Location spawnLoc = new Location(x, y, bossLocation.getZ());
					
					if (!EIGIS_ZONE.isInsideZone(spawnLoc))
					{
						continue;
					}
					
					final Npc newNpc = addSpawn(INVISIBLE_NPC, spawnLoc, false, NPC_LIFETIME);
					newNpc.setHeading(0);
					// newNpc.setName("Swords Eigis");
					spawnedNpcs.add(newNpc);
					newNpc.setInvul(true);
					ThreadPool.schedule(() ->
					{
						if (!newNpc.isDead())
						{
							newNpc.doCast(NPC_AOE_SKILL.getSkill());
						}
					}, 4000);
				}
			}
			
			ThreadPool.schedule(() ->
			{
				final List<Npc> spawnedNpcsCopy = new ArrayList<>(spawnedNpcs);
				for (Npc npcYellow : spawnedNpcsCopy)
				{
					final Location loc = npcYellow.getLocation();
					final int npcHeading = getRandom(0, 65535);
					final Location spawnLoc = calculatePointFromLocation(loc.getX(), loc.getY(), loc.getZ(), 100, npcHeading);
					
					if (!EIGIS_ZONE.isInsideZone(spawnLoc))
					{
						continue;
					}
					
					Npc newNpc = addSpawn(INVISIBLE_NPC_2, spawnLoc, false, NPC_LIFETIME);
					
					// newNpc.setName("Swords Eigis");
					spawnedNpcs.add(newNpc);
					newNpc.setInvul(true);
					ThreadPool.schedule(() ->
					{
						if (!newNpc.isDead())
						{
							newNpc.setHeading(npcHeading);
							newNpc.teleToLocation(newNpc.getX(), newNpc.getY(), newNpc.getZ(), npcHeading);
							
							ThreadPool.schedule(() ->
							{
								if (!newNpc.isDead())
								{
									newNpc.doCast(NPC_AOE_SKILL.getSkill());
								}
							}, 4000);
						}
					}, 50);
				}
			}, 1000);
			
			ThreadPool.schedule(() ->
			{
				resetSkillFlags();
			}, 4000);
			
		}, 5000);
	}
	
	/**
	 * Calculates a new {@code Location} based on an offset.<br>
	 * It uses the provided distance and heading to determine the new coordinates.
	 * @param x The starting X coordinate.
	 * @param y The starting Y coordinate.
	 * @param z The Z coordinate to maintain.
	 * @param distance The distance to move from the start point.
	 * @param heading The angle in degrees to face while moving.
	 * @return A new {@code Location} object representing the calculated destination.
	 */
	private Location calculatePointFromLocation(int x, int y, int z, int distance, int heading)
	{
		final double angle = Math.toRadians(heading * 0.0054931640625);
		final int newX = x + (int) (distance * Math.cos(angle));
		final int newY = y + (int) (distance * Math.sin(angle));
		return new Location(newX, newY, z);
	}
	
	/**
	 * Executes the third special skill for the boss.<br>
	 * This method handles target selection and scheduled skill execution.<br>
	 * It manages visual effects and damage over a specific timeframe.
	 * @param npc The {@code Npc} instance performing the action.
	 */
	private void useEigisSkills3(Npc npc)
	{
		if (!_bossInCombat || !_isUsingSpecialSkill3.compareAndSet(false, true))
		{
			return;
		}
		
		npc.disableSkill(COMMON_SKILL.getSkill(), 8000);
		final AtomicReference<Player> targetPlayer = new AtomicReference<>();
		ThreadPool.schedule(() ->
		{
			if (!findTargetPlayer(npc, targetPlayer))
			{
				cancelSkill3(npc);
				npc.enableSkill(COMMON_SKILL.getSkill());
				return;
			}
			
			final Player target = targetPlayer.get();
			if ((target != null) && !target.isDead())
			{
				npc.setTarget(target);
				
				ThreadPool.schedule(() ->
				{
					if (!npc.isDead() && _bossInCombat)
					{
						npc.setTarget(target);
						npc.broadcastPacket(new MagicSkillUse(npc, target, JUMP_TARGET_VISUAL_1.getSkillId(), 1, 3000, 0));
					}
				}, 2000);
				
				ThreadPool.schedule(() ->
				{
					if (!npc.isDead() && _bossInCombat)
					{
						npc.setTarget(target);
						npc.doCast(JUMP_SKILL_DAMAGE_2.getSkill());
					}
				}, 1000);
				
				ThreadPool.schedule(() ->
				{
					if (!npc.isDead() && _bossInCombat)
					{
						npc.broadcastPacket(new MagicSkillUse(npc, target, JUMP_IMPACT_VISUAL_3.getSkillId(), 1, 2000, 0));
						
						_isUsingSpecialSkill3.set(false);
					}
					else
					{
					}
				}, 6000);
			}
			else
			{
				cancelSkill3(npc);
				npc.enableSkill(COMMON_SKILL.getSkill());
			}
		}, 1000);
	}
	
	/**
	 * Searches for a {@code Player} within a specific distance range of the {@code Npc}.<br>
	 * If a player is found, it selects one at random and updates the {@code targetPlayer} reference.
	 * @param npc The {@code Npc} performing the search.
	 * @param targetPlayer An {@code AtomicReference} to store the selected {@code Player}.
	 * @return {@code true} if a player was found and assigned, otherwise {@code false}.
	 */
	private boolean findTargetPlayer(Npc npc, AtomicReference<Player> targetPlayer)
	{
		final List<Player> playersInRange = new ArrayList<>();
		for (Creature target : npc.getAttackByList())
		{
			if (target instanceof Player)
			{
				final Player player = (Player) target;
				final double distance = npc.calculateDistance3D(player);
				if ((distance >= 700) && (distance <= 2000))
				{
					playersInRange.add(player);
				}
			}
		}
		
		if (!playersInRange.isEmpty())
		{
			targetPlayer.set(playersInRange.get(Rnd.get(playersInRange.size())));
			return true;
		}
		
		return false;
	}
	
	/**
	 * Resets the status of special skills.<br>
	 * Sets all special skill flags to {@code false}.
	 */
	private void resetSkillFlags()
	{
		_isUsingSpecialSkill.set(false);
		_isUsingSpecialSkill2.set(false);
		_isUsingSpecialSkill3.set(false);
	}
	
	/**
	 * Stops the timer for the {@code checkTargetRange} quest.<br>
	 * Resets all skill flags for the NPC.
	 * @param npc The {@code Npc} instance to update.
	 */
	private void cancelSkill3(Npc npc)
	{
		cancelQuestTimer("checkTargetRange", npc, null);
		resetSkillFlags();
	}
	
	/**
	 * Resets the special skill status flags for an {@code Npc}.<br>
	 * Enables the common skill for the entity.<br>
	 * Cancels the "SkillsBalancer" quest timer.
	 * @param npc The {@code Npc} to update.
	 */
	private void cancelSpecialSkills(Npc npc)
	{
		_isUsingSpecialSkill.set(false);
		_isUsingSpecialSkill2.set(false);
		_isUsingSpecialSkill3.set(false);
		npc.enableSkill(COMMON_SKILL.getSkill());
		cancelQuestTimer("SkillsBalancer", npc, null);
	}
	
	/**
	 * Checks if the boss is currently engaged in combat.<br>
	 * It verifies if there are any living targets in the attack list.<br>
	 * If no targets remain, it resets the combat status and starts a timer.<br>
	 * If the boss is not in combat but has special mechanics active, it cancels them.
	 * @param npc The {@code Npc} instance to check.
	 */
	private void checkCombatStatus(Npc npc)
	{
		if (_bossInCombat)
		{
			boolean hasTarget = false;
			for (Creature target : npc.getAttackByList())
			{
				if (!target.isDead())
				{
					hasTarget = true;
					break;
				}
			}
			
			if (!hasTarget)
			{
				_bossInCombat = false;
				startQuestTimer("checkTargetLost", 8000, npc, null);
			}
			else
			{
				cancelQuestTimer("checkTargetLost", npc, null);
			}
		}
		else
		{
			if (_specialActivated)
			{
				cancelSpecialSkills(npc);
				_specialActivated = false;
			}
		}
	}
	
	/**
	 * This method is called when the {@code Npc} is killed.<br>
	 * It resets all boss mechanics and internal states.<br>
	 * It also clears active quest timers for the entity.
	 * @param npc The {@code Npc} that was defeated.
	 * @param killer The {@code Player} who dealt the final blow.
	 * @param isSummon Indicates if the kill was performed by a summon.
	 */
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		_bossInCombat = false;
		_specialActivated = false;
		_isUsingSpecialSkill.set(false);
		_isUsingSpecialSkill2.set(false);
		_isUsingSpecialSkill3.set(false);
		GlobalVariablesManager.getInstance().set(EIGIS_ALIVE_VAR, false);
		npc.setInvul(false);
		_eigisHits.clear();
		_barrierActivated = false;
		_vulnerablePhase = false;
		cancelQuestTimer("SkillsBalancer", npc, null);
		cancelQuestTimer("checkTargetLost", npc, null);
		cancelQuestTimer("checkCombatStatus", npc, null);
		cancelQuestTimer("checkPosition", npc, null);
		cancelQuestTimer("activate_barrier", npc, null);
		cancelQuestTimer("remove_barrier", npc, null);
	}
	
	/**
	 * This is the main entry point for the {@code Eigis} class.<br>
	 * It initializes a new instance of the {@code Eigis} object.
	 * @param args The command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		new Eigis();
	}
}
