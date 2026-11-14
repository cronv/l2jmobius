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
package ai.areas.Gracia.SeedOfAnnihilation;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.instance.Monster;
import org.l2jmobius.gameserver.entity.zone.ZoneType;
import org.l2jmobius.gameserver.entity.zone.type.EffectZone;
import org.l2jmobius.gameserver.managers.GlobalVariablesManager;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.mechanics.skill.SkillCaster;
import org.l2jmobius.gameserver.util.ArrayUtil;

/**
 * This class handles the artificial intelligence for the {@code SeedOfAnnihilation} entity.<br>
 * It defines the behaviors and logic for this specific NPC within its designated area.
 * @author Gigiikun, Altur, Mobius
 */
public class SeedOfAnnihilation extends Script
{
	private static final Map<Integer, Location> TELEPORT_ZONES = new HashMap<>();
	static
	{
		TELEPORT_ZONES.put(60002, new Location(-213175, 182648, -10992));
		TELEPORT_ZONES.put(60003, new Location(-181217, 186711, -10528));
		TELEPORT_ZONES.put(60004, new Location(-180211, 182984, -15152));
		TELEPORT_ZONES.put(60005, new Location(-179275, 186802, -10720));
	}
	
	private static final int ANNIHILATION_FURNACE = 18928;
	
	// Strength, Agility, Wisdom
	private static final int[] ZONE_BUFFS =
	{
		0,
		6443,
		6444,
		6442
	};
	
	// Strength, Agility, Wisdom for the monsters of the region
	private static final int[] ZONE_BUFFS_MONSTER =
	{
		0,
		6440,
		6441,
		6439
	};
	
	private static final int[][] ZONE_BUFFS_LIST =
	{
		// @formatter:off
		{1, 2, 3},
		{1, 3, 2},
		{2, 1, 3},
		{2, 3, 1},
		{3, 2, 1},
		{3, 1, 2}
		// @formatter:on
	};
	
	// Herbs of Mana left by the elites.
	private static final int HERB_OF_MANA = 8603;
	private static final int GREATER_HERB_OF_MANA = 8604;
	
	// 0: Bistakon, 1: Reptilikon, 2: Cokrakon
	private final SeedRegion[] _regionsData = new SeedRegion[3];
	private Long _seedsNextStatusChange;
	
	/**
	 * Initializes the {@code SeedOfAnnihilation} script.<br>
	 * This constructor loads region data and registers necessary zones, NPCs, and effects.
	 */
	public SeedOfAnnihilation()
	{
		loadSeedRegionData();
		for (int i : TELEPORT_ZONES.keySet())
		{
			addEnterZoneId(i);
		}
		
		for (SeedRegion element : _regionsData)
		{
			for (int elite_mob_id : element.elite_mob_ids)
			{
				addSpawnId(elite_mob_id);
				addKillId(elite_mob_id);
			}
		}
		
		addStartNpc(32739);
		addTalkId(32739);
		startEffectZonesControl();
	}
	
	/**
	 * Initializes the data for all seed regions.<br>
	 * This method populates the {@code _regionsData} array with specific coordinates and IDs.<br>
	 * It also checks and updates the global status change time using {@code GlobalVariablesManager}.<br>
	 * Finally, it assigns the correct active buffs to each region based on the current cycle.
	 */
	public void loadSeedRegionData()
	{
		// @formatter:off
		// Bistakon data
		_regionsData[0] = new SeedRegion(new int[] {22750, 22751, 22752, 22753}, new int[][] {{22746, 22746, 22746}, {22747, 22747, 22747}, {22748, 22748, 22748}, {22749, 22749, 22749}}, "Seed of Annihilation 1", "Seed of Annihilation 1 Monsters", new int[][] {{-180450, 185507, -10544, 11632}, {-180005, 185489, -10544, 11632}});
		
		// Reptilikon data
		_regionsData[1] = new SeedRegion(new int[] {22757, 22758, 22759}, new int[][] {{22754, 22755, 22756}}, "Seed of Annihilation 2", "Seed of Annihilation 2 Monsters", new int[][] {{-179600, 186998, -10704, 11632}, {-179295, 186444, -10704, 11632}});
		
		// Cokrakon data
		_regionsData[2] = new SeedRegion(new int[] {22763, 22764, 22765}, new int[][] {{22760, 22760, 22761}, {22760, 22760, 22762}, {22761, 22761, 22760}, {22761, 22761, 22762}, {22762, 22762, 22760}, {22762, 22762, 22761}}, "Seed of Annihilation 3", "Seed of Annihilation 3 Monsters", new int[][] {{-180971, 186361, -10528, 11632}, {-180758, 186739, -10528, 11632}});
		// @formatter:on
		
		int buffsNow = 0;
		final Long var = GlobalVariablesManager.getInstance().getLong("SeedNextStatusChange", 0);
		if (var < System.currentTimeMillis())
		{
			buffsNow = getRandom(ZONE_BUFFS_LIST.length);
			GlobalVariablesManager.getInstance().set("SeedBuffsList", String.valueOf(buffsNow));
			_seedsNextStatusChange = getNextSeedsStatusChangeTime();
			GlobalVariablesManager.getInstance().set("SeedNextStatusChange", String.valueOf(_seedsNextStatusChange));
		}
		else
		{
			_seedsNextStatusChange = var;
			buffsNow = GlobalVariablesManager.getInstance().getInt("SeedBuffsList", 0);
		}
		
		for (int i = 0; i < _regionsData.length; i++)
		{
			_regionsData[i].activeBuff = ZONE_BUFFS_LIST[buffsNow][i];
		}
	}
	
	/**
	 * Calculates the timestamp for the next status change.<br>
	 * This method determines when the seed regions should update their state.<br>
	 * It defaults to every Monday at 13:00 server time.
	 * @return The {@code Long} timestamp of the next scheduled change.
	 */
	private Long getNextSeedsStatusChangeTime()
	{
		final Calendar reenter = Calendar.getInstance();
		reenter.set(Calendar.SECOND, 0);
		reenter.set(Calendar.MINUTE, 0);
		reenter.set(Calendar.HOUR_OF_DAY, 13);
		reenter.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
		if (reenter.getTimeInMillis() <= System.currentTimeMillis())
		{
			reenter.add(Calendar.DAY_OF_MONTH, 7);
		}
		
		return reenter.getTimeInMillis();
	}
	
	/**
	 * Initializes the effect zones for each seed region.<br>
	 * This method spawns NPCs at specific locations and applies their display effects.<br>
	 * It also adds required skills to the corresponding {@code EffectZone}.<br>
	 * Finally, it starts a quest timer to handle status changes.
	 */
	private void startEffectZonesControl()
	{
		for (SeedRegion a_regionsData : _regionsData)
		{
			for (int j = 0; j < a_regionsData.af_spawns.length; j++)
			{
				a_regionsData.af_npcs[j] = addSpawn(ANNIHILATION_FURNACE, a_regionsData.af_spawns[j][0], a_regionsData.af_spawns[j][1], a_regionsData.af_spawns[j][2], a_regionsData.af_spawns[j][3], false, 0);
				a_regionsData.af_npcs[j].setDisplayEffect(a_regionsData.activeBuff);
			}
			
			a_regionsData.buff_zone.addSkill(ZONE_BUFFS[a_regionsData.activeBuff], 1);
			a_regionsData.monster_buff_zone.addSkill(ZONE_BUFFS_MONSTER[a_regionsData.activeBuff], 1);
		}
		
		startQuestTimer("ChangeSeedsStatus", _seedsNextStatusChange - System.currentTimeMillis(), null, null);
	}
	
	/**
	 * Spawns a group of minions for a specific monster.<br>
	 * It iterates through an array of IDs and adds each one to the NPC.
	 * @param npc The {@code Monster} that will act as the leader.
	 * @param mobIds An array of {@code int} values representing the minion IDs to spawn.
	 */
	private void spawnGroupOfMinion(Monster npc, int[] mobIds)
	{
		for (int mobId : mobIds)
		{
			addMinion(npc, mobId);
		}
	}
	
	/**
	 * This method is called when an {@code Npc} spawns.<br>
	 * It checks if the spawned entity belongs to a specific elite mob list.<br>
	 * If it matches, it triggers the spawning of minions for that NPC.
	 * @param npc The {@code Npc} instance that was just spawned.
	 */
	@Override
	public void onSpawn(Npc npc)
	{
		for (SeedRegion element : _regionsData)
		{
			if (ArrayUtil.contains(element.elite_mob_ids, npc.getId()))
			{
				spawnGroupOfMinion(npc.asMonster(), getRandomEntry(element.minion_lists));
			}
		}
	}
	
	/**
	 * Called when an elite mob is killed.<br>
	 * The elite drops Herbs of Mana: two 40% rolls for 8603 and one 29% roll for 8604.
	 * @param npc The {@code Npc} that was defeated.
	 * @param killer The {@code Player} who dealt the killing blow.
	 * @param isSummon Indicates if the killed entity was a summon.
	 */
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		if (getRandom(100) < 40)
		{
			npc.dropItem(killer, HERB_OF_MANA, 1);
		}
		
		if (getRandom(100) < 40)
		{
			npc.dropItem(killer, HERB_OF_MANA, 1);
		}
		
		if (getRandom(100) > 70)
		{
			npc.dropItem(killer, GREATER_HERB_OF_MANA, 1);
		}
	}
	
	/**
	 * Handles specific events for the Seed Of Annihilation script.<br>
	 * It manages status changes for seed regions and player transformations.
	 * @param event The name of the event to trigger.
	 * @param npc The {@code Npc} instance involved in the event.
	 * @param player The {@code Player} instance involved in the event.
	 * @return A {@code null} value after processing the event.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.equalsIgnoreCase("ChangeSeedsStatus"))
		{
			final int buffsNow = getRandom(ZONE_BUFFS_LIST.length);
			GlobalVariablesManager.getInstance().set("SeedBuffsList", String.valueOf(buffsNow));
			_seedsNextStatusChange = getNextSeedsStatusChangeTime();
			GlobalVariablesManager.getInstance().set("SeedNextStatusChange", String.valueOf(_seedsNextStatusChange));
			for (int i = 0; i < _regionsData.length; i++)
			{
				_regionsData[i].activeBuff = ZONE_BUFFS_LIST[buffsNow][i];
				for (Npc af : _regionsData[i].af_npcs)
				{
					af.setDisplayEffect(_regionsData[i].activeBuff);
				}
				
				final EffectZone zone = _regionsData[i].buff_zone;
				zone.clearSkills();
				zone.addSkill(ZONE_BUFFS[_regionsData[i].activeBuff], 1);
				
				final EffectZone monsterZone = _regionsData[i].monster_buff_zone;
				monsterZone.clearSkills();
				monsterZone.addSkill(ZONE_BUFFS_MONSTER[_regionsData[i].activeBuff], 1);
			}
			
			startQuestTimer("ChangeSeedsStatus", _seedsNextStatusChange - System.currentTimeMillis(), null, null);
		}
		else if (event.equalsIgnoreCase("transform"))
		{
			if (player.isAffectedBySkill(6408))
			{
				npc.showChatWindow(player, 2);
			}
			else
			{
				npc.setTarget(player);
				SkillCaster.triggerCast(npc, player, SkillData.getInstance().getSkill(6408, 1));
				SkillCaster.triggerCast(npc, player, SkillData.getInstance().getSkill(6649, 1));
				npc.showChatWindow(player, 1);
			}
		}
		
		return null;
	}
	
	/**
	 * Handles logic when a {@code Creature} enters a specific {@code ZoneType}.<br>
	 * This method checks if the zone is a teleport zone.<br>
	 * It teleports the creature and any nearby NPCs targeting it to the correct location.
	 * @param creature The {@code Creature} entering the zone.
	 * @param zone The {@code ZoneType} that was entered.
	 */
	@Override
	public void onEnterZone(Creature creature, ZoneType zone)
	{
		final int zoneId = zone.getId();
		if (TELEPORT_ZONES.containsKey(zoneId))
		{
			final Location teleLoc = TELEPORT_ZONES.get(zoneId);
			
			// Conditions for Quest 454
			World.forEachVisibleObjectInRange(creature, Npc.class, 500, npc ->
			{
				if ((npc.getId() == 32738) && (npc.getTarget() == creature))
				{
					npc.teleToLocation(teleLoc, false);
					npc.getAI().setIntentionFollow(creature);
				}
			});
			creature.teleToLocation(teleLoc, false);
		}
	}
	
	private static class SeedRegion
	{
		public int[] elite_mob_ids;
		public int[][] minion_lists;
		public EffectZone buff_zone;
		public EffectZone monster_buff_zone;
		public int[][] af_spawns;
		public Npc[] af_npcs = new Npc[2];
		public int activeBuff = 0;
		
		public SeedRegion(int[] emi, int[][] ml, String bz, String mbz, int[][] as)
		{
			elite_mob_ids = emi;
			minion_lists = ml;
			buff_zone = ZoneManager.getInstance().getZoneByName(bz, EffectZone.class);
			monster_buff_zone = ZoneManager.getInstance().getZoneByName(mbz, EffectZone.class);
			af_spawns = as;
		}
	}
	
	/**
	 * This is the entry point for the {@code SeedOfAnnihilation} class.<br>
	 * It initializes a new instance of the script to start the AI logic.
	 * @param args Command line arguments passed during startup.
	 */
	public static void main(String[] args)
	{
		new SeedOfAnnihilation();
	}
}
