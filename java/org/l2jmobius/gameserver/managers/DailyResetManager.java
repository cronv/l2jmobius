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
package org.l2jmobius.gameserver.managers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.time.TimeUtil;
import org.l2jmobius.gameserver.config.AttendanceRewardsConfig;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.HuntPassConfig;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.config.TrainingCampConfig;
import org.l2jmobius.gameserver.config.WorldExchangeConfig;
import org.l2jmobius.gameserver.data.holders.LimitShopProductHolder;
import org.l2jmobius.gameserver.data.holders.TimedHuntingZoneHolder;
import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.data.xml.LimitShopCraftData;
import org.l2jmobius.gameserver.data.xml.LimitShopData;
import org.l2jmobius.gameserver.data.xml.MableGameData;
import org.l2jmobius.gameserver.data.xml.PrimeShopData;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.data.xml.TimedHuntingZoneData;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.holders.player.Recommendations;
import org.l2jmobius.gameserver.entity.actor.holders.player.SubClassHolder;
import org.l2jmobius.gameserver.entity.actor.stat.PlayerStat;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.clan.ClanMember;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.events.EventDispatcher;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.OnDailyReset;
import org.l2jmobius.gameserver.mechanics.olympiad.Olympiad;
import org.l2jmobius.gameserver.mechanics.primeshop.PrimeShopGroup;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.variables.AccountVariables;
import org.l2jmobius.gameserver.mechanics.variables.ClanVariables;
import org.l2jmobius.gameserver.mechanics.variables.PlayerVariables;
import org.l2jmobius.gameserver.network.serverpackets.ExVoteSystemInfo;
import org.l2jmobius.gameserver.network.serverpackets.ExWorldChatCnt;

/**
 * Manages the server-wide daily reset process.<br>
 * This class handles resetting various game mechanics, rewards, and data structures at a scheduled time.<br>
 * It ensures that daily content is refreshed for all players on the {@code World}.
 * @author Mobius
 */
public class DailyResetManager
{
	private static final Logger LOGGER = Logger.getLogger(DailyResetManager.class.getName());
	
	private static final Set<Integer> RESET_SKILLS = new HashSet<>();
	static
	{
		RESET_SKILLS.add(2510); // Wondrous Cubic
		RESET_SKILLS.add(22180); // Wondrous Cubic - 1 time use
	}
	public static final Set<Integer> RESET_ITEMS = new HashSet<>();
	static
	{
		RESET_ITEMS.add(47387); // Balthus Knights Supply Items
		RESET_ITEMS.add(60011); // Festival Fairy's Good Luck Bag
	}
	public static final Set<String> RESET_CLAN_INSTANCES = new HashSet<>();
	static
	{
		RESET_CLAN_INSTANCES.add("TOH_GOLDBERG_DONE"); // Goldberg Instance
		RESET_CLAN_INSTANCES.add("TOH_MARYREED_DONE"); // MaryReed Instance
		RESET_CLAN_INSTANCES.add("TOH_TAUTI_DONE"); // Tauti Instance
	}
	
	/**
	 * Initializes the daily reset manager.<br>
	 * This constructor schedules the {@code onReset} task to run every day at 6:30 AM.<br>
	 * It also schedules a global save task to run every 30 minutes.
	 */
	protected DailyResetManager()
	{
		// Schedule reset everyday at 6:30.
		final long nextResetTime = TimeUtil.getNextTime(6, 30).getTimeInMillis();
		final long currentTime = System.currentTimeMillis();
		
		// Get today's 6:30 AM timestamp.
		final Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.HOUR_OF_DAY, 6);
		calendar.set(Calendar.MINUTE, 30);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		final long currentResetTime = calendar.getTimeInMillis();
		
		// Check if 24 hours have passed since the last daily reset.
		if ((currentTime < currentResetTime) || (GlobalVariablesManager.getInstance().getLong(GlobalVariablesManager.DAILY_TASK_RESET, 0) > currentResetTime))
		{
			LOGGER.info(getClass().getSimpleName() + ": Next schedule at " + TimeUtil.getDateTimeString(nextResetTime) + ".");
		}
		else
		{
			LOGGER.info(getClass().getSimpleName() + ": Daily task will run now.");
			onReset();
		}
		
		// Daily reset task.
		final long startDelay = Math.max(0, nextResetTime - currentTime);
		ThreadPool.scheduleAtFixedRate(this::onReset, startDelay, 86400000); // 86400000 = 1 day
		
		// Global save task.
		ThreadPool.scheduleAtFixedRate(this::onSave, 1800000, 1800000); // 1800000 = 30 minutes
	}
	
	/**
	 * Performs the daily reset of all game tasks.<br>
	 * Updates global variables and resets various player mechanics.<br>
	 * Handles specific logic for weekly and monthly events.<br>
	 * Notifies listeners and saves current player variables.
	 */
	private void onReset()
	{
		LOGGER.info("Starting reset of daily tasks...");
		
		// Store last reset time.
		GlobalVariablesManager.getInstance().set(GlobalVariablesManager.DAILY_TASK_RESET, System.currentTimeMillis());
		
		// Wednesday weekly tasks.
		final Calendar calendar = Calendar.getInstance();
		if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.WEDNESDAY)
		{
			clanLeaderApply();
			resetClanContribution();
			resetDailyMissionRewards();
			resetTimedHuntingZonesWeekly();
			resetVitalityWeekly();
			resetPrivateStoreHistory();
			resetWeeklyLimitShopData();
			resetThroneOfHeroesWeekly();
		}
		else // All days, except Wednesday.
		{
			resetVitalityDaily();
		}
		
		if (HuntPassConfig.ENABLE_HUNT_PASS && (calendar.get(Calendar.DAY_OF_MONTH) == HuntPassConfig.HUNT_PASS_PERIOD))
		{
			resetHuntPass();
		}
		
		if (calendar.get(Calendar.DAY_OF_MONTH) == 1)
		{
			resetCeremonyOfChaos();
			resetMonthlyLimitShopData();
		}
		
		// Daily tasks.
		resetAttendanceRewards();
		resetDailyLimitedDrops();
		resetDailySkills();
		resetDailyItems();
		resetDailyPrimeShopData();
		resetDailyLimitShopData();
		resetHomunculusResetPoints();
		resetRecommends();
		resetTimedHuntingZones();
		resetTrainingCamp();
		resetWorldChatPoints();
		checkWeekSwap();
		
		// Trigger daily reset event.
		if (EventDispatcher.getInstance().hasListener(EventType.ON_DAILY_RESET))
		{
			EventDispatcher.getInstance().notifyEvent(new OnDailyReset());
		}
		
		// Store player variables.
		for (Player player : World.getPlayers())
		{
			player.getVariables().storeMe();
			player.getAccountVariables().storeMe();
		}
		
		LOGGER.info("Daily tasks reset completed.");
	}
	
	/**
	 * Checks if the current time has passed the scheduled week swap.<br>
	 * Updates the {@code IS_EVEN_WEEK} status in {@code GlobalVariablesManager}.<br>
	 * Sets the next swap timestamp to the upcoming Wednesday at 06:25.
	 */
	private void checkWeekSwap()
	{
		final long nextEvenWeekSwap = GlobalVariablesManager.getInstance().getLong(GlobalVariablesManager.NEXT_EVEN_WEEK_SWAP, 0);
		if (nextEvenWeekSwap < System.currentTimeMillis())
		{
			final boolean isEvenWeek = GlobalVariablesManager.getInstance().getBoolean(GlobalVariablesManager.IS_EVEN_WEEK, true);
			GlobalVariablesManager.getInstance().set(GlobalVariablesManager.IS_EVEN_WEEK, !isEvenWeek);
			final Calendar calendar = TimeUtil.getNextDayTime(Calendar.WEDNESDAY, 6, 25);
			GlobalVariablesManager.getInstance().set(GlobalVariablesManager.NEXT_EVEN_WEEK_SWAP, calendar.getTimeInMillis());
		}
	}
	
	/**
	 * Saves the current state of various game systems.<br>
	 * It stores data for {@code GlobalVariablesManager}.<br>
	 * It also saves {@code Olympiad} status if a competition is active.
	 */
	private void onSave()
	{
		GlobalVariablesManager.getInstance().storeMe();
		
		if (WorldExchangeConfig.WORLD_EXCHANGE_LAZY_UPDATE)
		{
			WorldExchangeManager.getInstance().storeMe();
		}
		
		if (Olympiad.getInstance().inCompPeriod())
		{
			Olympiad.getInstance().saveOlympiadStatus();
			LOGGER.info("Olympiad System: Data updated.");
		}
		
		MableGameData.getInstance().save();
	}
	
	/**
	 * Updates the leader of each clan in the database.<br>
	 * This method checks for new leader IDs and assigns them to the correct {@code ClanMember}.<br>
	 * It logs a message once all updates are complete.
	 */
	private void clanLeaderApply()
	{
		for (Clan clan : ClanTable.getInstance().getClans())
		{
			if (clan.getNewLeaderId() != 0)
			{
				final ClanMember member = clan.getClanMember(clan.getNewLeaderId());
				if (member == null)
				{
					continue;
				}
				
				clan.setNewLeader(member);
			}
		}
		
		LOGGER.info("Clan leaders have been updated.");
	}
	
	/**
	 * Resets and distributes daily vitality points to all players.<br>
	 * This method checks if the vitality system is enabled in {@code PlayerConfig}.<br>
	 * It adds a portion of {@code PlayerStat.MAX_VITALITY_POINTS} to online characters.<br>
	 * It also updates the database for offline characters.
	 */
	private void resetVitalityDaily()
	{
		if (!PlayerConfig.ENABLE_VITALITY)
		{
			return;
		}
		
		int vitality = PlayerStat.MAX_VITALITY_POINTS / 4;
		for (Player player : World.getPlayers())
		{
			final int VP = player.getVitalityPoints();
			player.setVitalityPoints(VP + vitality, false);
			for (SubClassHolder subclass : player.getSubClasses().values())
			{
				final int VPS = subclass.getVitalityPoints();
				subclass.setVitalityPoints(VPS + vitality);
			}
		}
		
		try (Connection con = DatabaseFactory.getConnection())
		{
			try (PreparedStatement st = con.prepareStatement("UPDATE character_subclasses SET vitality_points = IF(vitality_points = ?, vitality_points, vitality_points + ?)"))
			{
				st.setInt(1, PlayerStat.MAX_VITALITY_POINTS);
				st.setInt(2, vitality);
				st.execute();
			}
			
			try (PreparedStatement st = con.prepareStatement("UPDATE characters SET vitality_points = IF(vitality_points = ?, vitality_points, vitality_points + ?)"))
			{
				st.setInt(1, PlayerStat.MAX_VITALITY_POINTS);
				st.setInt(2, vitality);
				st.execute();
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Error while updating vitality", e);
		}
		
		LOGGER.info("Daily vitality added successfully.");
	}
	
	/**
	 * Resets the weekly vitality points for all players.<br>
	 * This method checks if {@code PlayerConfig.ENABLE_VITALITY} is true before proceeding.<br>
	 * It updates both online players and database records to the maximum value defined in {@code PlayerStat}.
	 */
	private void resetVitalityWeekly()
	{
		if (!PlayerConfig.ENABLE_VITALITY)
		{
			return;
		}
		
		for (Player player : World.getPlayers())
		{
			player.setVitalityPoints(PlayerStat.MAX_VITALITY_POINTS, false);
			for (SubClassHolder subclass : player.getSubClasses().values())
			{
				subclass.setVitalityPoints(PlayerStat.MAX_VITALITY_POINTS);
			}
		}
		
		try (Connection con = DatabaseFactory.getConnection())
		{
			try (PreparedStatement st = con.prepareStatement("UPDATE character_subclasses SET vitality_points = ?"))
			{
				st.setInt(1, PlayerStat.MAX_VITALITY_POINTS);
				st.execute();
			}
			
			try (PreparedStatement st = con.prepareStatement("UPDATE characters SET vitality_points = ?"))
			{
				st.setInt(1, PlayerStat.MAX_VITALITY_POINTS);
				st.execute();
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Error while updating vitality", e);
		}
		
		LOGGER.info("Vitality points have been reset.");
	}
	
	/**
	 * Resets the daily usage for specific skills.<br>
	 * This method removes records from the database for offline characters.<br>
	 * It iterates through all IDs defined in {@code RESET_SKILLS}.
	 */
	private void resetDailySkills()
	{
		// Update data for offline players.
		try (Connection con = DatabaseFactory.getConnection())
		{
			for (int skillId : RESET_SKILLS)
			{
				try (PreparedStatement ps = con.prepareStatement("DELETE FROM character_skills_save WHERE skill_id=? AND charId IN (SELECT charId FROM characters WHERE online = 0)"))
				{
					ps.setInt(1, skillId);
					ps.execute();
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Could not reset daily skill reuse: ", e);
		}
		
		// Update data for online players.
		// final Set<Player> updates = new HashSet<>();
		for (int skillId : RESET_SKILLS)
		{
			final Skill skill = SkillData.getInstance().getSkill(skillId, 1 /* No known need for more levels */);
			if (skill != null)
			{
				for (Player player : World.getPlayers())
				{
					if (player.hasSkillReuse(skill.getReuseHashCode()))
					{
						player.removeTimeStamp(skill);
						// updates.add(player);
					}
				}
			}
		}
		
		// for (Player player : updates)
		// {
		// player.sendSkillList();
		// }
		
		LOGGER.info("Daily skill reuse cleaned.");
	}
	
	/**
	 * Resets the daily reuse timers for specific items.<br>
	 * This method clears data for offline players in the database.<br>
	 * It also updates the inventory of all online {@code Player} objects.
	 */
	private void resetDailyItems()
	{
		// Update data for offline players.
		try (Connection con = DatabaseFactory.getConnection())
		{
			for (int itemId : RESET_ITEMS)
			{
				try (PreparedStatement ps = con.prepareStatement("DELETE FROM character_item_reuse_save WHERE itemId=? AND charId IN (SELECT charId FROM characters WHERE online = 0)"))
				{
					ps.setInt(1, itemId);
					ps.execute();
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Could not reset daily item reuse: ", e);
		}
		
		// Update data for online players.
		boolean update;
		for (Player player : World.getPlayers())
		{
			update = false;
			for (int itemId : RESET_ITEMS)
			{
				for (Item item : player.getInventory().getAllItemsByItemId(itemId))
				{
					player.getItemReuseTimeStamps().remove(item.getObjectId());
					update = true;
				}
			}
			
			if (update)
			{
				player.sendItemList();
			}
		}
		
		LOGGER.info("Daily item reuse cleaned.");
	}
	
	/**
	 * Resets the daily world chat points for all players.<br>
	 * This method updates offline characters in the database.<br>
	 * It also resets the points for all online {@code Player} objects.<br>
	 * The operation is skipped if {@code GeneralConfig.ENABLE_WORLD_CHAT} is false.
	 */
	private void resetWorldChatPoints()
	{
		if (!GeneralConfig.ENABLE_WORLD_CHAT)
		{
			return;
		}
		
		// Update data for offline players.
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("UPDATE character_variables SET val = ? WHERE var = ? AND charId IN (SELECT charId FROM characters WHERE online = 0)"))
		{
			ps.setInt(1, 0);
			ps.setString(2, PlayerVariables.WORLD_CHAT_VARIABLE_NAME);
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Could not reset daily world chat points: ", e);
		}
		
		// Update data for online players.
		for (Player player : World.getPlayers())
		{
			player.setWorldChatUsed(0);
			player.sendPacket(new ExWorldChatCnt(player));
		}
		
		LOGGER.info("Daily world chat points have been reset.");
	}
	
	/**
	 * Resets the recommendation system data for all characters.<br>
	 * Updates the database records for character recommendations.<br>
	 * Synchronizes the local player data with the new values.<br>
	 * Sends updated information to all active players.
	 */
	private void resetRecommends()
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			try (PreparedStatement ps = con.prepareStatement("UPDATE character_reco_bonus SET rec_left = ?, rec_have = 0 WHERE rec_have <= 20"))
			{
				ps.setInt(1, 0); // Rec left = 0
				ps.execute();
			}
			
			try (PreparedStatement ps = con.prepareStatement("UPDATE character_reco_bonus SET rec_left = ?, rec_have = GREATEST(rec_have - 20,0) WHERE rec_have > 20"))
			{
				ps.setInt(1, 0); // Rec left = 0
				ps.execute();
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Could not reset Recommendations System: ", e);
		}
		
		for (Player player : World.getPlayers())
		{
			final Recommendations recommendations = player.getRecommendations();
			recommendations.setLeft(0);
			recommendations.setHave(recommendations.getHave() - 20);
			player.sendPacket(new ExVoteSystemInfo(player));
			player.broadcastUserInfo();
		}
	}
	
	/**
	 * Resets the training camp duration for all players.<br>
	 * This method clears data for offline accounts in the database.<br>
	 * It also calls {@code resetTraingCampDuration()} for all online {@code Player} objects.<br>
	 * The operation only runs if {@code TrainingCampConfig.TRAINING_CAMP_ENABLE} is true.
	 */
	private void resetTrainingCamp()
	{
		if (TrainingCampConfig.TRAINING_CAMP_ENABLE)
		{
			// Update data for offline players.
			try (Connection con = DatabaseFactory.getConnection();
				PreparedStatement ps = con.prepareStatement("DELETE FROM account_gsdata WHERE var = ? AND account_name NOT IN (SELECT account_name FROM characters WHERE online = 1)"))
			{
				ps.setString(1, "TRAINING_CAMP_DURATION");
				ps.executeUpdate();
			}
			catch (Exception e)
			{
				LOGGER.log(Level.SEVERE, "Could not reset Training Camp: ", e);
			}
			
			// Update data for online players.
			for (Player player : World.getPlayers())
			{
				player.resetTraingCampDuration();
			}
			
			LOGGER.info("Training Camp durations have been reset.");
		}
	}
	
	/**
	 * Resets the daily mission count for all players.<br>
	 * This method clears the {@code DAILY_MISSION_COUNT} variable from the database for offline characters.<br>
	 * It also removes the same variable from the memory of all online {@code Player} objects.
	 */
	private void resetDailyMissionRewards()
	{
		// Update data for offline players.
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("DELETE FROM character_variables WHERE var = ? AND charId IN (SELECT charId FROM characters WHERE online = 0)"))
		{
			ps.setString(1, PlayerVariables.DAILY_MISSION_COUNT);
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Could not reset Clan Mission Rewards: ", e);
		}
		
		// Update data for online players.
		for (Player player : World.getPlayers())
		{
			player.getVariables().remove(PlayerVariables.DAILY_MISSION_COUNT);
		}
		
		LOGGER.info("Clan Mission Rewards have been reset.");
	}
	
	/**
	 * Resets the clan contribution values for all players.<br>
	 * Updates online player variables to store previous values.<br>
	 * Updates offline character variables in the database.
	 */
	private void resetClanContribution()
	{
		// Update data for online players.
		for (Player player : World.getPlayers())
		{
			player.getVariables().set(PlayerVariables.CLAN_CONTRIBUTION_PREVIOUS, player.getClanContribution());
			player.getVariables().set(PlayerVariables.CLAN_CONTRIBUTION_TOTAL_PREVIOUS, player.getClanContributionTotal());
			player.getVariables().remove(PlayerVariables.CLAN_CONTRIBUTION);
		}
		
		// Update data for offline players.
		try (Connection con = DatabaseFactory.getConnection())
		{
			// Clear any leftover previous values first, otherwise the rename below collides with the (charId, var) primary key.
			try (PreparedStatement ps = con.prepareStatement("DELETE FROM character_variables WHERE `var` IN ('CLAN_CONTRIBUTION_PREVIOUS', 'CLAN_CONTRIBUTION_TOTAL_PREVIOUS') AND charId IN (SELECT charId FROM characters WHERE online = 0)"))
			{
				ps.executeUpdate();
			}
			
			try (PreparedStatement ps = con.prepareStatement("UPDATE character_variables SET var = 'CLAN_CONTRIBUTION_PREVIOUS' WHERE `var` = 'CLAN_CONTRIBUTION' AND charId IN (SELECT charId FROM characters WHERE online = 0)"))
			{
				ps.executeUpdate();
			}
			
			// The total is only copied, the running total itself is kept the same way it is for online players.
			try (PreparedStatement ps = con.prepareStatement("INSERT INTO character_variables (charId, var, val) SELECT charId, 'CLAN_CONTRIBUTION_TOTAL_PREVIOUS', val FROM (SELECT charId, val FROM character_variables WHERE `var` = 'CLAN_CONTRIBUTION_TOTAL' AND charId IN (SELECT charId FROM characters WHERE online = 0)) AS total ON DUPLICATE KEY UPDATE val = VALUES(val)"))
			{
				ps.executeUpdate();
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Could not reset Clan contributions: ", e);
		}
		
		LOGGER.info("Clan contributions have been reset.");
	}
	
	/**
	 * Resets the weekly progress for the Throne of Heroes event.<br>
	 * This method clears specific variables from both offline and online clans.<br>
	 * It removes completion flags like {@code TOH_GOLDBERG_DONE} from clan data.
	 */
	private void resetThroneOfHeroesWeekly()
	{
		// Update data for offline players.
		try (Connection con = DatabaseFactory.getConnection())
		{
			for (String clanInstances : RESET_CLAN_INSTANCES)
			{
				try (PreparedStatement ps = con.prepareStatement("DELETE FROM clan_variables WHERE var=?"))
				{
					ps.setString(1, clanInstances);
					ps.execute();
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, getClass().getSimpleName() + ": Could not reset Throne of Heroes entries: " + e);
		}
		
		// Update data for online players.
		final Set<Clan> clans = new HashSet<>();
		for (Player player : World.getPlayers())
		{
			final Clan clan = player.getClan();
			if (clan != null)
			{
				clans.add(clan);
			}
		}
		
		for (Clan clan : clans)
		{
			final ClanVariables variables = clan.getVariables();
			variables.remove("TOH_GOLDBERG_DONE");
			variables.remove("TOH_MARYREED_DONE");
			variables.remove("TOH_TAUTI_DONE");
		}
		
		LOGGER.info("Throne of Heroes entries have been reset.");
	}
	
	/**
	 * Resets the data for all daily timed hunting zones.<br>
	 * This method clears variables for both online and offline players.<br>
	 * It skips any zones marked as weekly in {@code TimedHuntingZoneData}.
	 */
	private void resetTimedHuntingZones()
	{
		for (TimedHuntingZoneHolder holder : TimedHuntingZoneData.getInstance().getAllHuntingZones())
		{
			if (holder.isWeekly())
			{
				continue;
			}
			
			// Update data for offline players.
			final int zoneId = holder.getZoneId();
			try (Connection con = DatabaseFactory.getConnection();
				PreparedStatement ps = con.prepareStatement("DELETE FROM character_variables WHERE var IN (?, ?, ?) AND charId IN (SELECT charId FROM characters WHERE online = 0)"))
			{
				ps.setString(1, PlayerVariables.HUNTING_ZONE_ENTRY + zoneId);
				ps.setString(2, PlayerVariables.HUNTING_ZONE_TIME + zoneId);
				ps.setString(3, PlayerVariables.HUNTING_ZONE_REMAIN_REFILL + zoneId);
				ps.executeUpdate();
			}
			catch (Exception e)
			{
				LOGGER.log(Level.SEVERE, "Could not reset Special Hunting Zones: ", e);
			}
			
			// Update data for online players.
			for (Player player : World.getPlayers())
			{
				player.getVariables().remove(PlayerVariables.HUNTING_ZONE_ENTRY + zoneId);
				player.getVariables().remove(PlayerVariables.HUNTING_ZONE_TIME + zoneId);
				player.getVariables().remove(PlayerVariables.HUNTING_ZONE_REMAIN_REFILL + zoneId);
			}
		}
		
		LOGGER.info("Special Hunting Zones have been reset.");
	}
	
	/**
	 * Resets the data for all weekly special hunting zones.<br>
	 * This method clears variables for both online and offline players.<br>
	 * It removes entry, time, and refill data from {@code PlayerVariables}.
	 */
	private void resetTimedHuntingZonesWeekly()
	{
		for (TimedHuntingZoneHolder holder : TimedHuntingZoneData.getInstance().getAllHuntingZones())
		{
			if (!holder.isWeekly())
			{
				continue;
			}
			
			// Update data for offline players.
			final int zoneId = holder.getZoneId();
			try (Connection con = DatabaseFactory.getConnection();
				PreparedStatement ps = con.prepareStatement("DELETE FROM character_variables WHERE var IN (?, ?, ?) AND charId IN (SELECT charId FROM characters WHERE online = 0)"))
			{
				ps.setString(1, PlayerVariables.HUNTING_ZONE_ENTRY + zoneId);
				ps.setString(2, PlayerVariables.HUNTING_ZONE_TIME + zoneId);
				ps.setString(3, PlayerVariables.HUNTING_ZONE_REMAIN_REFILL + zoneId);
				ps.executeUpdate();
			}
			catch (Exception e)
			{
				LOGGER.log(Level.SEVERE, "Could not reset Weekly Special Hunting Zones: ", e);
			}
			
			// Update data for online players.
			for (Player player : World.getPlayers())
			{
				player.getVariables().remove(PlayerVariables.HUNTING_ZONE_ENTRY + zoneId);
				player.getVariables().remove(PlayerVariables.HUNTING_ZONE_TIME + zoneId);
				player.getVariables().remove(PlayerVariables.HUNTING_ZONE_REMAIN_REFILL + zoneId);
			}
		}
		
		LOGGER.info("Weekly Special Hunting Zones have been reset.");
	}
	
	/**
	 * Resets the Homunculus Reset Points for all players.<br>
	 * This method updates offline characters in the database to {@code 0}.<br>
	 * It also resets the variables for all currently online players.
	 */
	private void resetHomunculusResetPoints()
	{
		// Update data for offline players.
		try (Connection con = DatabaseFactory.getConnection())
		{
			try (PreparedStatement ps = con.prepareStatement("UPDATE character_variables SET val = ? WHERE var IN (?, ?, ?, ?) AND charId IN (SELECT charId FROM characters WHERE online = 0)"))
			{
				ps.setInt(1, 0);
				ps.setString(2, PlayerVariables.HOMUNCULUS_USED_RESET_VP);
				ps.setString(3, PlayerVariables.HOMUNCULUS_USED_VP_CONVERT);
				ps.setString(4, PlayerVariables.HOMUNCULUS_USED_RESET_KILLS);
				ps.setString(5, PlayerVariables.HOMUNCULUS_USED_KILL_CONVERT);
				ps.execute();
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, getClass().getSimpleName() + ": Could not reset Homunculus Reset Points: " + e);
		}
		
		// Update data for online players.
		for (Player player : World.getPlayers())
		{
			final PlayerVariables variables = player.getVariables();
			variables.set(PlayerVariables.HOMUNCULUS_USED_RESET_VP, 0);
			variables.set(PlayerVariables.HOMUNCULUS_USED_VP_CONVERT, 0);
			variables.set(PlayerVariables.HOMUNCULUS_USED_RESET_KILLS, 0);
			variables.set(PlayerVariables.HOMUNCULUS_USED_KILL_CONVERT, 0);
		}
		
		LOGGER.info("Homunculus Reset Points have been reset.");
	}
	
	/**
	 * Resets the attendance rewards for all players.<br>
	 * This method clears the {@code PlayerVariables.ATTENDANCE_DATE} variable.<br>
	 * It handles both online and offline characters based on the account sharing configuration.
	 */
	private void resetAttendanceRewards()
	{
		if (AttendanceRewardsConfig.ATTENDANCE_REWARDS_SHARE_ACCOUNT)
		{
			// Update data for offline players.
			try (Connection con = DatabaseFactory.getConnection())
			{
				try (PreparedStatement ps = con.prepareStatement("DELETE FROM account_gsdata WHERE var = ? AND account_name NOT IN (SELECT account_name FROM characters WHERE online = 1)"))
				{
					ps.setString(1, PlayerVariables.ATTENDANCE_DATE);
					ps.execute();
				}
			}
			catch (Exception e)
			{
				LOGGER.log(Level.SEVERE, getClass().getSimpleName() + ": Could not reset Attendance Rewards: " + e);
			}
			
			// Update data for online players.
			for (Player player : World.getPlayers())
			{
				player.getAccountVariables().remove(PlayerVariables.ATTENDANCE_DATE);
			}
			
			LOGGER.info("Account shared Attendance Rewards have been reset.");
		}
		else
		{
			// Update data for offline players.
			try (Connection con = DatabaseFactory.getConnection())
			{
				try (PreparedStatement ps = con.prepareStatement("DELETE FROM character_variables WHERE var = ? AND charId IN (SELECT charId FROM characters WHERE online = 0)"))
				{
					ps.setString(1, PlayerVariables.ATTENDANCE_DATE);
					ps.execute();
				}
			}
			catch (Exception e)
			{
				LOGGER.log(Level.SEVERE, getClass().getSimpleName() + ": Could not reset Attendance Rewards: " + e);
			}
			
			// Update data for online players.
			for (Player player : World.getPlayers())
			{
				player.getVariables().remove(PlayerVariables.ATTENDANCE_DATE);
			}
			
			LOGGER.info("Attendance Rewards have been reset.");
		}
	}
	
	/**
	 * Resets the daily limited drop counts for all players.<br>
	 * This method clears variables for offline characters in the database.<br>
	 * It also removes these variables from online {@code Player} objects.
	 */
	private void resetDailyLimitedDrops()
	{
		// Update data for offline players.
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("DELETE FROM character_variables WHERE var LIKE ? AND charId IN (SELECT charId FROM characters WHERE online = 0)"))
		{
			ps.setString(1, PlayerVariables.LIMITED_DROP_COUNT_VAR + "%");
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Could not reset limited drop count: " + e.getMessage());
		}
		
		// Update data for online players.
		final List<String> removedVariables = new ArrayList<>();
		for (Player player : World.getPlayers())
		{
			final PlayerVariables playerVariables = player.getVariables();
			for (String variableName : playerVariables.getSet().keySet())
			{
				if (variableName.startsWith(PlayerVariables.LIMITED_DROP_COUNT_VAR))
				{
					removedVariables.add(variableName);
				}
			}
			
			if (!removedVariables.isEmpty())
			{
				for (String removedVariable : removedVariables)
				{
					playerVariables.remove(removedVariable);
				}
				
				removedVariables.clear();
			}
		}
		
		LOGGER.info(getClass().getSimpleName() + " limited drop count has been reset.");
	}
	
	/**
	 * Resets the daily Prime Shop data for all players.<br>
	 * This method clears variables for both online and offline accounts.<br>
	 * It removes entries from the database and updates active player objects.
	 */
	private void resetDailyPrimeShopData()
	{
		for (PrimeShopGroup holder : PrimeShopData.getInstance().getPrimeItems().values())
		{
			// Update data for offline players.
			try (Connection con = DatabaseFactory.getConnection();
				PreparedStatement ps = con.prepareStatement("DELETE FROM account_gsdata WHERE var = ? AND account_name NOT IN (SELECT account_name FROM characters WHERE online = 1)"))
			{
				ps.setString(1, AccountVariables.PRIME_SHOP_PRODUCT_DAILY_COUNT + holder.getBrId());
				ps.executeUpdate();
			}
			catch (Exception e)
			{
				LOGGER.log(Level.SEVERE, getClass().getSimpleName() + ": Could not reset PrimeShopData: " + e);
			}
			
			// Update data for online players.
			for (Player player : World.getPlayers())
			{
				player.getAccountVariables().remove(AccountVariables.PRIME_SHOP_PRODUCT_DAILY_COUNT + holder.getBrId());
			}
		}
		
		LOGGER.info("PrimeShopData have been reset.");
	}
	
	/**
	 * Resets the daily purchase limits for the {@code LimitShop}.<br>
	 * This method clears data for offline players via a database query.<br>
	 * It also removes active product variables for all online players.
	 */
	private void resetDailyLimitShopData()
	{
		// Update data for offline players.
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("DELETE FROM account_gsdata WHERE var LIKE ? AND account_name NOT IN (SELECT account_name FROM characters WHERE online = 1)"))
		{
			ps.setString(1, AccountVariables.LCOIN_SHOP_PRODUCT_DAILY_COUNT + "%");
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, getClass().getSimpleName() + ": Could not reset daily LimitShopData: " + e);
		}
		
		// Create a set of active product variables first.
		final Set<String> productVariables = new HashSet<>();
		for (LimitShopProductHolder holder : LimitShopData.getInstance().getProducts())
		{
			productVariables.add(AccountVariables.LCOIN_SHOP_PRODUCT_DAILY_COUNT + holder.getProductionId());
		}
		for (LimitShopProductHolder holder : LimitShopCraftData.getInstance().getProducts())
		{
			productVariables.add(AccountVariables.LCOIN_SHOP_PRODUCT_DAILY_COUNT + holder.getProductionId());
		}
		
		// Now remove for each online player using the collected product variables.
		for (Player player : World.getPlayers())
		{
			final AccountVariables accountVariables = player.getAccountVariables();
			for (String variableName : productVariables)
			{
				accountVariables.remove(variableName);
			}
		}
		
		LOGGER.info("Daily LimitShopData have been reset.");
	}
	
	/**
	 * Resets the weekly purchase limits for the {@code LimitShop}.<br>
	 * This method clears data for offline players via a database query.<br>
	 * It also removes active product variables for all online players.
	 */
	private void resetWeeklyLimitShopData()
	{
		// Update data for offline players.
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("DELETE FROM account_gsdata WHERE var LIKE ? AND account_name NOT IN (SELECT account_name FROM characters WHERE online = 1)"))
		{
			ps.setString(1, AccountVariables.LCOIN_SHOP_PRODUCT_WEEKLY_COUNT + "%");
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, getClass().getSimpleName() + ": Could not reset weekly LimitShopData: " + e);
		}
		
		// Create a set of active product variables first.
		final Set<String> productVariables = new HashSet<>();
		for (LimitShopProductHolder holder : LimitShopData.getInstance().getProducts())
		{
			productVariables.add(AccountVariables.LCOIN_SHOP_PRODUCT_WEEKLY_COUNT + holder.getProductionId());
		}
		for (LimitShopProductHolder holder : LimitShopCraftData.getInstance().getProducts())
		{
			productVariables.add(AccountVariables.LCOIN_SHOP_PRODUCT_WEEKLY_COUNT + holder.getProductionId());
		}
		
		// Now remove for each online player using the collected product variables.
		for (Player player : World.getPlayers())
		{
			final AccountVariables accountVariables = player.getAccountVariables();
			for (String variableName : productVariables)
			{
				accountVariables.remove(variableName);
			}
		}
		
		LOGGER.info("Weekly LimitShopData have been reset.");
	}
	
	/**
	 * Resets the monthly limit shop data for all players.<br>
	 * This method clears variables for offline accounts in the database.<br>
	 * It also removes active product variables from online {@code Player} objects.
	 */
	private void resetMonthlyLimitShopData()
	{
		// Update data for offline players.
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("DELETE FROM account_gsdata WHERE var LIKE ? AND account_name NOT IN (SELECT account_name FROM characters WHERE online = 1)"))
		{
			ps.setString(1, AccountVariables.LCOIN_SHOP_PRODUCT_MONTHLY_COUNT + "%");
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, getClass().getSimpleName() + ": Could not reset monthly LimitShopData: " + e);
		}
		
		// Create a set of active product variables first.
		final Set<String> productVariables = new HashSet<>();
		for (LimitShopProductHolder holder : LimitShopData.getInstance().getProducts())
		{
			productVariables.add(AccountVariables.LCOIN_SHOP_PRODUCT_MONTHLY_COUNT + holder.getProductionId());
		}
		for (LimitShopProductHolder holder : LimitShopCraftData.getInstance().getProducts())
		{
			productVariables.add(AccountVariables.LCOIN_SHOP_PRODUCT_MONTHLY_COUNT + holder.getProductionId());
		}
		
		// Now remove for each online player using the collected product variables.
		for (Player player : World.getPlayers())
		{
			final AccountVariables accountVariables = player.getAccountVariables();
			for (String variableName : productVariables)
			{
				accountVariables.remove(variableName);
			}
		}
		
		LOGGER.info("Monthly LimitShopData have been reset.");
	}
	
	/**
	 * Resets the Ceremony of Chaos event data.<br>
	 * This method updates global variables and clears marks for both online and offline players.<br>
	 * It also removes the {@code CEREMONY_OF_CHAOS_MARKS} variable from active player records.
	 */
	private void resetCeremonyOfChaos()
	{
		// Set monthly true hero.
		GlobalVariablesManager.getInstance().set(GlobalVariablesManager.COC_TRUE_HERO, GlobalVariablesManager.getInstance().getInt(GlobalVariablesManager.COC_TOP_MEMBER, 0));
		GlobalVariablesManager.getInstance().set(GlobalVariablesManager.COC_TRUE_HERO_REWARDED, false);
		
		// Reset monthly winner.
		GlobalVariablesManager.getInstance().set(GlobalVariablesManager.COC_TOP_MARKS, 0);
		GlobalVariablesManager.getInstance().set(GlobalVariablesManager.COC_TOP_MEMBER, 0);
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("DELETE FROM character_variables WHERE var = ? AND charId IN (SELECT charId FROM characters WHERE online = 0)"))
		{
			ps.setString(1, PlayerVariables.CEREMONY_OF_CHAOS_MARKS);
			ps.execute();
		}
		catch (Exception e)
		{
			LOGGER.severe(getClass().getSimpleName() + ": Could not reset Ceremony Of Chaos victories: " + e);
		}
		
		// Update data for online players.
		for (Player player : World.getPlayers())
		{
			player.getVariables().remove(PlayerVariables.CEREMONY_OF_CHAOS_MARKS);
		}
		
		LOGGER.info("Ceremony of Chaos variables have been reset.");
	}
	
	/**
	 * Resets the hunt pass data for all players.<br>
	 * This method clears entries from the database table.<br>
	 * It also resets the hunt pass status for all currently online players.
	 */
	private void resetHuntPass()
	{
		// Update data for offline players.
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement("DELETE FROM huntpass"))
		{
			statement.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, getClass().getSimpleName() + ": Could not delete entries from hunt pass: " + e);
		}
		
		// Update data for online players.
		for (Player player : World.getPlayers())
		{
			player.getHuntPass().resetHuntPass();
		}
		
		LOGGER.info("HuntPassData have been reset.");
	}
	
	/**
	 * Clears all existing records from the private store history.<br>
	 * This method calls the {@code reset()} method on the {@code PrivateStoreHistoryManager}.<br>
	 * Any errors during this process are logged as a severe error.
	 */
	public void resetPrivateStoreHistory()
	{
		try
		{
			PrivateStoreHistoryManager.getInstance().reset();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, getClass().getSimpleName() + ": Could not reset private store history! " + e);
		}
		
		LOGGER.info("Private store history records have been reset.");
	}
	
	/**
	 * Retrieves the singleton instance of the {@code DailyResetManager}.<br>
	 * This method provides a global access point to the manager.
	 * @return The active {@code DailyResetManager} instance.
	 */
	public static DailyResetManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final DailyResetManager INSTANCE = new DailyResetManager();
	}
}
