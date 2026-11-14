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
package ai.bosses.Guillotine;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.data.SpawnTable;
import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.entity.spawns.Spawn;
import org.l2jmobius.gameserver.entity.zone.type.ArenaZone;
import org.l2jmobius.gameserver.managers.DatabaseSpawnManager;
import org.l2jmobius.gameserver.managers.GlobalVariablesManager;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.mechanics.skill.SkillCaster;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;

/**
 * This class handles the artificial intelligence for the {@code Guillotine} boss.<br>
 * It manages the boss's behaviors, skills, and interactions during combat.
 * @author Notorion
 */
public class Guillotine extends Script
{
	// NPC
	private static final int MAIN_BOSS_ID = 29402;
	private static Npc _spawnedMainBoss;
	private static final Location SPAWN_LOCATION = new Location(44967, 155944, -2708);
	private static final ArenaZone ARENA_ZONE = ZoneManager.getInstance().getZoneByName("RaidBossGuillotine", ArenaZone.class);
	
	// Barrier skill and attack control
	private static final SkillHolder LIMIT_BARRIER = new SkillHolder(29518, 1);
	private static final int HIT_COUNT = 500;
	private boolean _barrierActivated = false;
	private final Map<Npc, Integer> _guillotineHits = new ConcurrentHashMap<>();
	private final Map<Npc, Boolean> _clonesTemp94 = new ConcurrentHashMap<>();
	private final Map<Npc, Boolean> _clonesTemp75 = new ConcurrentHashMap<>();
	private final Map<Npc, Boolean> _clonesTemp42 = new ConcurrentHashMap<>();
	
	// NPCs of Clones and Slaves
	private static final int CLONE_NPC_ID = 29403;
	private static final int SLAVE1_NPC_ID = 29404;
	private static final int SLAVE2_NPC_ID = 29405;
	private static final int SLAVE3_NPC_ID = 29406;
	
	// ID of temporary Clones NPCs
	private static final int CLONESTEMP94_NPC_ID = 29403;
	private static final int CLONESTEMP75_NPC_ID = 29403;
	private static final int CLONESTEMP42_NPC_ID = 29403;
	
	// Temporary clones control variables
	private boolean _spawningClonesTemp94 = false;
	private boolean _spawningClonesTemp75 = false;
	private boolean _spawningClonesTemp42 = false;
	private boolean _spawningClones95 = false;
	private boolean _spawningClones75 = false;
	private boolean _spawningClones50 = false;
	private boolean _spawningSlaves93 = false;
	private boolean _spawningSlaves70 = false;
	private boolean _spawningSlaves50 = false;
	private boolean _spawningSlaves45 = false;
	private boolean _spawningSlaves40 = false;
	private boolean _spawningSlaves25 = false;
	private boolean _spawningSlaves15 = false;
	private boolean _spawningSlaves5 = false;
	private boolean _spawningSlaves1 = false;
	
	// Slaves Effects Skills
	private static final SkillHolder SLAVE1_DEATH_SKILL = new SkillHolder(34464, 1);
	private static final SkillHolder SLAVE2_DEATH_SKILL = new SkillHolder(34465, 1);
	private static final SkillHolder SLAVE3_DEATH_SKILL = new SkillHolder(34466, 1);
	private final Map<Npc, Boolean> _deathSkillUsed = new ConcurrentHashMap<>();
	
	/**
	 * Initializes the {@code Guillotine} script.<br>
	 * Registers attack and kill IDs for the boss and its slaves.<br>
	 * Schedules the periodic spawning of the boss based on a weekly timer.
	 */
	public Guillotine()
	{
		addAttackId(SLAVE1_NPC_ID, SLAVE2_NPC_ID, SLAVE3_NPC_ID);
		addKillId(SLAVE1_NPC_ID, SLAVE2_NPC_ID, SLAVE3_NPC_ID);
		addAttackId(MAIN_BOSS_ID);
		addKillId(MAIN_BOSS_ID);
		
		final long currentTime = System.currentTimeMillis();
		final Calendar calendarGuillotineStart = Calendar.getInstance();
		
		// Spawn time to 21h00 Thursday.
		calendarGuillotineStart.set(Calendar.DAY_OF_WEEK, Calendar.THURSDAY);
		calendarGuillotineStart.set(Calendar.HOUR_OF_DAY, 21);
		calendarGuillotineStart.set(Calendar.MINUTE, 0);
		calendarGuillotineStart.set(Calendar.SECOND, 0);
		
		if (GlobalVariablesManager.getInstance().getBoolean("GUILLOTINE_ALIVE", true))
		{
			spawnGuillotine();
		}
		
		if (calendarGuillotineStart.getTimeInMillis() < currentTime)
		{
			calendarGuillotineStart.add(Calendar.WEEK_OF_YEAR, 1);
		}
		
		ThreadPool.scheduleAtFixedRate(this::spawnGuillotine, calendarGuillotineStart.getTimeInMillis() - currentTime, 604800000);
	}
	
	/**
	 * Spawns the main boss and initializes its state.<br>
	 * This method removes existing spawns for {@code MAIN_BOSS_ID}.<br>
	 * It creates a new {@code Npc} and sets global variables to true.<br>
	 * Finally, it applies the initial barrier effects and starts quest timers.
	 */
	private void spawnGuillotine()
	{
		final Spawn oldSpawn = SpawnTable.getInstance().getAnySpawn(MAIN_BOSS_ID);
		if (oldSpawn != null)
		{
			if (oldSpawn.getLastSpawn() != null)
			{
				oldSpawn.getLastSpawn().deleteMe();
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
			final Npc boss = DatabaseSpawnManager.getInstance().addNewSpawn(spawn, false);
			_spawnedMainBoss = boss;
			GlobalVariablesManager.getInstance().set("GUILLOTINE_ALIVE", true);
			// System.out.println("Guillotine Manager: Guillotine (29402) spawned successfully.");
			
			// Barrier
			LIMIT_BARRIER.getSkill().applyEffects(_spawnedMainBoss, _spawnedMainBoss);
			_spawnedMainBoss.setInvul(true);
			_barrierActivated = true;
			startQuestTimer("guillotine_barrier_start", 1000, _spawnedMainBoss, null);
			
			startQuestTimer("check_arena", 10000, null, null, true);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * Handles specific events for the {@code Guillotine} script.<br>
	 * It checks if the boss is in the arena and monitors its health when requested.
	 * @param event The name of the event to trigger.
	 * @param npc The {@code Npc} instance involved in the event.
	 * @param player The {@code Player} instance involved in the event.
	 * @return The result of the super class {@code onEvent} method.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if ("check_arena".equals(event) && (_spawnedMainBoss != null))
		{
			checkBossInArena();
			checkBossHP();
		}
		
		return super.onEvent(event, npc, player);
	}
	
	/**
	 * Handles the logic when an {@code Npc} is attacked by a {@code Player}.<br>
	 * This method checks for specific boss mechanics like barriers and zone requirements.<br>
	 * It also triggers slave-specific behaviors if the target is not the main boss.
	 * @param npc The {@code Npc} being attacked.
	 * @param attacker The {@code Player} performing the attack.
	 * @param damage The amount of damage dealt to the {@code Npc}.
	 * @param isSummon Whether the attack was performed by a summoned entity.
	 */
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	{
		if (npc.getId() == MAIN_BOSS_ID)
		{
			if (_barrierActivated)
			{
				final int hits = _guillotineHits.getOrDefault(npc, 0) + 1;
				_guillotineHits.put(npc, hits);
				
				if (hits >= HIT_COUNT)
				{
					_barrierActivated = false;
					npc.stopSkillEffects(LIMIT_BARRIER.getSkill());
					npc.setInvul(false);
				}
				return;
			}
			
			if ((ARENA_ZONE != null) && !ARENA_ZONE.isInsideZone(attacker))
			{
				// If the player is not in the arena, teleports to Dion. Protection Boss.
				attacker.teleToLocation(new Location(15804, 142347, -2680), false);
				return;
			}
		}
		
		handleSlaveLogic(npc);
	}
	
	/**
	 * Processes the logic for slave NPCs.<br>
	 * It checks if the {@code Npc} is one of the three specific slaves.<br>
	 * It prevents the NPC from dying by resetting its health to 2 if it reaches 1.<br>
	 * It triggers a death skill if the HP percentage falls below 30.
	 * @param npc The {@code Npc} instance to process.
	 */
	private void handleSlaveLogic(Npc npc)
	{
		final int npcId = npc.getId();
		if ((npcId == SLAVE1_NPC_ID) || (npcId == SLAVE2_NPC_ID) || (npcId == SLAVE3_NPC_ID))
		{
			final double currentHp = npc.getCurrentHp();
			final double maxHp = npc.getMaxHp();
			final double hpPercentage = (currentHp / maxHp) * 100;
			if (currentHp <= 1)
			{
				npc.setCurrentHp(2);
			}
			
			if (hpPercentage <= 30)
			{
				applyDeathSkill(npc);
			}
		}
	}
	
	/**
	 * Triggers a special death skill for an {@code Npc}.<br>
	 * This method ensures the skill is only used once per entity.<br>
	 * It schedules the removal of the {@code Npc} after the animation finishes.
	 * @param npc The {@code Npc} instance that will perform the death skill.
	 */
	private void applyDeathSkill(Npc npc)
	{
		if (_deathSkillUsed.getOrDefault(npc, false))
		{
			return;
		}
		
		_deathSkillUsed.put(npc, true);
		SkillHolder skillToUse = null;
		
		switch (npc.getId())
		{
			case SLAVE1_NPC_ID:
			{
				skillToUse = SLAVE1_DEATH_SKILL;
				break;
			}
			case SLAVE2_NPC_ID:
			{
				skillToUse = SLAVE2_DEATH_SKILL;
				break;
			}
			case SLAVE3_NPC_ID:
			{
				skillToUse = SLAVE3_DEATH_SKILL;
				break;
			}
		}
		
		if ((skillToUse != null) && (skillToUse.getSkill() != null))
		{
			SkillCaster.triggerCast(npc, npc, skillToUse.getSkill());
			ThreadPool.schedule(npc::deleteMe, 1500);
		}
	}
	
	/**
	 * Verifies if the main boss is currently inside the arena zone.<br>
	 * If the boss is outside the {@code ARENA_ZONE}, it teleports back to the {@code SPAWN_LOCATION}.
	 */
	private void checkBossInArena()
	{
		if ((ARENA_ZONE != null) && (_spawnedMainBoss != null) && !ARENA_ZONE.isInsideZone(_spawnedMainBoss))
		{
			_spawnedMainBoss.teleToLocation(SPAWN_LOCATION, false);
		}
	}
	
	/**
	 * Checks the current health of the main boss.<br>
	 * Triggers different spawn events for clones and slaves based on health percentages.<br>
	 * Manages the lifecycle of temporary clones by deleting them at specific thresholds.
	 */
	private void checkBossHP()
	{
		if (_spawnedMainBoss != null)
		{
			final double currentHP = _spawnedMainBoss.getCurrentHp();
			final long maxHP = _spawnedMainBoss.getMaxHp();
			final int currentHPPercentage = (int) ((currentHP / maxHP) * 100);
			if ((currentHPPercentage <= 97) && !_spawningClones95)
			{
				_spawningClones95 = true;
				spawnClones95();
			}
			else if ((currentHPPercentage <= 75) && !_spawningClones75)
			{
				_spawningClones75 = true;
				spawnClones75();
			}
			else if ((currentHPPercentage <= 50) && !_spawningClones50)
			{
				_spawningClones50 = true;
				spawnClones50();
			}
			
			if ((currentHPPercentage <= 94) && !_spawningClonesTemp94)
			{
				_spawningClonesTemp94 = true;
				spawnClonesTemp94();
			}
			
			if ((currentHPPercentage <= 78) && _spawningClonesTemp94)
			{
				final List<Npc> clonesTemp94Copy = new ArrayList<>(_clonesTemp94.keySet());
				for (Npc clone : clonesTemp94Copy)
				{
					if ((clone != null) && !clone.isDead())
					{
						clone.deleteMe();
					}
				}
				
				_clonesTemp94.clear();
			}
			
			if ((currentHPPercentage <= 75) && !_spawningClonesTemp75)
			{
				_spawningClonesTemp75 = true;
				spawnClonesTemp75();
			}
			else if ((currentHPPercentage <= 42) && !_spawningClonesTemp42)
			{
				_spawningClonesTemp42 = true;
				spawnClonesTemp42();
			}
			
			if ((currentHPPercentage <= 93) && !_spawningSlaves93)
			{
				_spawningSlaves93 = true;
				spawnSlavesAroundBoss(SLAVE1_NPC_ID, 25, 500, 600);
			}
			else if ((currentHPPercentage <= 70) && !_spawningSlaves70)
			{
				_spawningSlaves70 = true;
				spawnSlavesAroundBoss(SLAVE1_NPC_ID, 18, 500, 600);
				spawnSlavesAroundBoss(SLAVE2_NPC_ID, 18, 500, 600);
			}
			else if ((currentHPPercentage <= 54) && !_spawningSlaves50)
			{
				_spawningSlaves50 = true;
				spawnSlavesAroundBoss(SLAVE1_NPC_ID, 30, 500, 600);
				spawnSlavesAroundBoss(SLAVE2_NPC_ID, 25, 500, 600);
				spawnSlavesAroundBoss(SLAVE3_NPC_ID, 25, 500, 600);
			}
			else if ((currentHPPercentage <= 47) && !_spawningSlaves45)
			{
				_spawningSlaves45 = true;
				spawnSlavesAroundBoss(SLAVE1_NPC_ID, 26, 500, 600);
				spawnSlavesAroundBoss(SLAVE2_NPC_ID, 26, 500, 600);
				spawnSlavesAroundBoss(SLAVE3_NPC_ID, 26, 500, 600);
			}
			else if ((currentHPPercentage <= 37) && !_spawningSlaves40)
			{
				_spawningSlaves40 = true;
				spawnSlavesAroundBoss(SLAVE1_NPC_ID, 22, 500, 600);
				spawnSlavesAroundBoss(SLAVE2_NPC_ID, 24, 500, 600);
				spawnSlavesAroundBoss(SLAVE3_NPC_ID, 22, 500, 600);
			}
			else if ((currentHPPercentage <= 28) && !_spawningSlaves25)
			{
				_spawningSlaves25 = true;
				spawnSlavesAroundBoss(SLAVE1_NPC_ID, 24, 500, 600);
				spawnSlavesAroundBoss(SLAVE2_NPC_ID, 25, 500, 600);
				spawnSlavesAroundBoss(SLAVE3_NPC_ID, 24, 500, 600);
			}
			else if ((currentHPPercentage <= 18) && !_spawningSlaves15)
			{
				_spawningSlaves15 = true;
				spawnSlavesAroundBoss(SLAVE1_NPC_ID, 30, 500, 600);
				spawnSlavesAroundBoss(SLAVE2_NPC_ID, 25, 500, 600);
				spawnSlavesAroundBoss(SLAVE3_NPC_ID, 25, 500, 600);
			}
			else if ((currentHPPercentage <= 10) && !_spawningSlaves5)
			{
				_spawningSlaves5 = true;
				spawnSlavesAroundBoss(SLAVE1_NPC_ID, 25, 500, 600);
				spawnSlavesAroundBoss(SLAVE2_NPC_ID, 25, 500, 600);
				spawnSlavesAroundBoss(SLAVE3_NPC_ID, 30, 500, 600);
			}
			else if ((currentHPPercentage <= 4) && !_spawningSlaves1)
			{
				_spawningSlaves1 = true;
				spawnSlavesAroundBoss(SLAVE1_NPC_ID, 25, 500, 600);
				spawnSlavesAroundBoss(SLAVE2_NPC_ID, 30, 500, 600);
				spawnSlavesAroundBoss(SLAVE3_NPC_ID, 30, 500, 600);
			}
		}
	}
	
	/**
	 * Spawns a specific number of slave NPCs around the main boss.<br>
	 * The slaves are placed at random positions within a circular area.
	 * @param npcId The unique identifier for the slave NPC to spawn.
	 * @param count The total number of slaves to create.
	 * @param minRadius The minimum distance from the boss for spawning.
	 * @param maxRadius The maximum distance from the boss for spawning.
	 */
	private void spawnSlavesAroundBoss(int npcId, int count, int minRadius, int maxRadius)
	{
		final Location bossLoc = _spawnedMainBoss.getLocation();
		for (int i = 0; i < count; i++)
		{
			final double angle = (2 * Math.PI * i) / count;
			final int radius = getRandom(minRadius, maxRadius);
			final int x = bossLoc.getX() + (int) (radius * Math.cos(angle));
			final int y = bossLoc.getY() + (int) (radius * Math.sin(angle));
			final int z = bossLoc.getZ();
			addSpawn(npcId, new Location(x, y, z));
		}
	}
	
	/**
	 * Spawns temporary clones at specific locations.<br>
	 * Clears the existing {@code _clonesTemp94} list before spawning new ones.<br>
	 * Uses the {@code CLONESTEMP94_NPC_ID} to create each clone.
	 */
	private void spawnClonesTemp94()
	{
		final Location[] cloneLocations94 =
		{
			new Location(44331, 155883, -2734),
			new Location(44465, 155501, -2734),
			new Location(44836, 155344, -2734)
		};
		
		_clonesTemp94.clear();
		
		for (Location loc : cloneLocations94)
		{
			final Npc clone = addSpawn(CLONESTEMP94_NPC_ID, loc);
			_clonesTemp94.put(clone, true);
		}
	}
	
	/**
	 * Spawns specific clones at predefined locations.<br>
	 * These clones are added to the {@code _clonesTemp75} map for tracking.
	 */
	private void spawnClonesTemp75()
	{
		final Location[] cloneLocations75 =
		{
			new Location(45430, 156351, -2734),
			new Location(45600, 156007, -2734),
			new Location(45095, 156548, -2713)
		};
		
		for (Location loc : cloneLocations75)
		{
			final Npc clone = addSpawn(CLONESTEMP75_NPC_ID, loc);
			_clonesTemp75.put(clone, true);
		}
	}
	
	/**
	 * Spawns the temporary clones at specific locations.<br>
	 * This method populates the {@code _clonesTemp42} map with new {@code Npc} instances.<br>
	 * It uses a predefined array of {@code Location} coordinates.
	 */
	private void spawnClonesTemp42()
	{
		final Location[] cloneLocations42 =
		{
			new Location(44332, 155884, -2734),
			new Location(44464, 155502, -2734),
			new Location(44837, 155345, -2734)
		};
		
		for (Location loc : cloneLocations42)
		{
			final Npc clone = addSpawn(CLONESTEMP42_NPC_ID, loc);
			_clonesTemp42.put(clone, true);
		}
	}
	
	/**
	 * Spawns clones at specific predefined locations.<br>
	 * This method adds multiple {@code Location} objects to the spawn list.<br>
	 * It also broadcasts a screen message to players.
	 */
	private void spawnClones95()
	{
		final Location[] cloneLocations95 =
		{
			new Location(45773, 156665, -2713),
			new Location(45196, 156999, -2713),
			new Location(44550, 156926, -2713)
		};
		
		for (Location loc : cloneLocations95)
		{
			addSpawn(CLONE_NPC_ID, loc);
		}
		
		_spawnedMainBoss.broadcastPacket(new ExShowScreenMessage(NpcStringId.GUILLOTINE_SUMMONS_HIS_CLONE, ExShowScreenMessage.TOP_CENTER, 10000, true));
	}
	
	/**
	 * Spawns clones at specific predefined locations.<br>
	 * It iterates through a set of {@code Location} coordinates to add them to the world.<br>
	 * A screen message is broadcasted to players when the process completes.
	 */
	private void spawnClones75()
	{
		final Location[] cloneLocations75 =
		{
			new Location(44731, 154876, -2713),
			new Location(44135, 155223, -2713),
			new Location(43868, 155843, -2713),
			new Location(44027, 156500, -2713)
		};
		
		for (Location loc : cloneLocations75)
		{
			addSpawn(CLONE_NPC_ID, loc);
		}
		
		_spawnedMainBoss.broadcastPacket(new ExShowScreenMessage(NpcStringId.GUILLOTINE_SUMMONS_HIS_CLONE, ExShowScreenMessage.TOP_CENTER, 10000, true));
	}
	
	/**
	 * Spawns clones at specific predefined locations.<br>
	 * This method adds {@code CLONE_NPC_ID} to the current spawn list.<br>
	 * It also broadcasts a screen message to all players.
	 */
	private void spawnClones50()
	{
		final Location[] cloneLocations50 =
		{
			new Location(46021, 156054, -2712),
			new Location(45881, 155405, -2712),
			new Location(45396, 154969, -2712)
		};
		
		for (Location loc : cloneLocations50)
		{
			addSpawn(CLONE_NPC_ID, loc);
		}
		
		_spawnedMainBoss.broadcastPacket(new ExShowScreenMessage(NpcStringId.GUILLOTINE_SUMMONS_HIS_CLONE, ExShowScreenMessage.TOP_CENTER, 10000, true));
	}
	
	/**
	 * Checks if the Guillotine boss is currently active.<br>
	 * It returns {@code true} if the main boss has been spawned.<br>
	 * It returns {@code false} otherwise.
	 * @return {@code true} if the boss exists, {@code false} otherwise.
	 */
	public boolean isGuillotineActive()
	{
		return _spawnedMainBoss != null;
	}
	
	/**
	 * This method is called when an {@code Npc} is killed.<br>
	 * It handles the cleanup logic specifically for the main boss.<br>
	 * If the main boss dies, it removes all active clones and slaves from the world.<br>
	 * It also updates global variables and resets spawning flags.
	 * @param npc The {@code Npc} that was killed.
	 * @param killer The {@code Player} who dealt the killing blow.
	 * @param isSummon Indicates if the kill was performed by a summon.
	 */
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		if (npc.getId() == MAIN_BOSS_ID)
		{
			cancelQuestTimers("check_arena");
			
			World.forEachVisibleObject(npc, Npc.class, spawnedNpc ->
			{
				if ((spawnedNpc != null) && ((spawnedNpc.getId() == CLONE_NPC_ID) || (spawnedNpc.getId() == SLAVE1_NPC_ID) || (spawnedNpc.getId() == SLAVE2_NPC_ID) || (spawnedNpc.getId() == SLAVE3_NPC_ID) || ((spawnedNpc.getId() == CLONESTEMP94_NPC_ID) && _clonesTemp94.containsKey(spawnedNpc)) || ((spawnedNpc.getId() == CLONESTEMP75_NPC_ID) && _clonesTemp75.containsKey(spawnedNpc)) || ((spawnedNpc.getId() == CLONESTEMP42_NPC_ID) && _clonesTemp42.containsKey(spawnedNpc))))
				{
					spawnedNpc.deleteMe();
				}
			});
			
			_spawnedMainBoss = null;
			GlobalVariablesManager.getInstance().set("GUILLOTINE_ALIVE", false);
			// System.out.println("RaidBoss: Guillotine(29402) status is 2 (Dead)");
			_spawningClones95 = false;
			_spawningClones75 = false;
			_spawningClones50 = false;
			
			_spawningClonesTemp94 = false;
			_spawningClonesTemp75 = false;
			_spawningClonesTemp42 = false;
		}
	}
	
	/**
	 * This is the entry point for the {@code Guillotine} class.<br>
	 * It initializes a new instance of the {@code Guillotine} script.
	 * @param args Command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		new Guillotine();
	}
}
