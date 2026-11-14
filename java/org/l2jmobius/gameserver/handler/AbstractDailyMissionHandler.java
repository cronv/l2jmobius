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
package org.l2jmobius.gameserver.handler;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.DailyMissionCategory;
import org.l2jmobius.gameserver.entity.actor.enums.player.DailyMissionStatus;
import org.l2jmobius.gameserver.entity.actor.holders.player.DailyMissionDataHolder;
import org.l2jmobius.gameserver.entity.actor.holders.player.DailyMissionPlayerEntry;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.enums.SpecialItemType;
import org.l2jmobius.gameserver.entity.item.holders.ItemHolder;
import org.l2jmobius.gameserver.mechanics.events.ListenersContainer;
import org.l2jmobius.gameserver.mechanics.variables.PlayerVariables;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * This class serves as the base handler for managing daily missions in the game.<br>
 * It provides common functionality for tracking and processing mission progress for {@code Player} objects.
 * @author Sdw, NasSeKa, Norvox
 */
public abstract class AbstractDailyMissionHandler extends ListenersContainer
{
	private static final String REPLACE_ENTRY_SQL = "REPLACE INTO character_daily_rewards (charId, rewardId, status, progress, lastCompleted) VALUES (?, ?, ?, ?, ?)";
	private static final Logger BATCH_LOGGER = Logger.getLogger(AbstractDailyMissionHandler.class.getName());
	
	/** Progress that has not reached the database yet, written out by the task below. */
	private static final Set<DailyMissionPlayerEntry> PENDING_ENTRIES = ConcurrentHashMap.newKeySet();
	private static final long FLUSH_DELAY = 60000;
	private static final int FLUSH_BATCH_SIZE = 500;
	static
	{
		ThreadPool.scheduleAtFixedRate(AbstractDailyMissionHandler::flushPendingEntries, FLUSH_DELAY, FLUSH_DELAY);
	}
	
	protected Logger LOGGER = Logger.getLogger(getClass().getName());
	
	private final Map<Integer, DailyMissionPlayerEntry> _entries = new ConcurrentHashMap<>();
	private final DailyMissionDataHolder _holder;
	
	/**
	 * Initializes the handler with specific mission data.<br>
	 * This constructor sets up the {@code _holder} field.<br>
	 * It also calls the {@code init()} method to prepare the handler.
	 * @param holder The {@code DailyMissionDataHolder} containing the necessary mission information.
	 */
	protected AbstractDailyMissionHandler(DailyMissionDataHolder holder)
	{
		_holder = holder;
		init();
	}
	
	/**
	 * Retrieves the data holder for daily missions.<br>
	 * This provides access to the shared mission information.
	 * @return the {@code DailyMissionDataHolder} instance.
	 */
	public DailyMissionDataHolder getHolder()
	{
		return _holder;
	}
	
	public abstract boolean isAvailable(Player player);
	
	/**
	 * Checks if the current mission is a level up mission.<br>
	 * This method currently always returns {@code false}.
	 * @return {@code true} if it is a level up mission, otherwise {@code false}.
	 */
	public boolean isLevelUpMission()
	{
		return false;
	}
	
	public abstract void init();
	
	/**
	 * Retrieves the current status of a daily mission for a specific player.<br>
	 * It returns the client ID associated with the mission status.<br>
	 * If no entry exists, it returns the ID for {@code NOT_AVAILABLE}.
	 * @param player The {@code Player} object to check.
	 * @return The integer client ID of the mission status.
	 */
	public int getStatus(Player player)
	{
		final DailyMissionPlayerEntry entry = getPlayerEntry(player.getObjectId(), false);
		return entry != null ? entry.getStatus().getClientId() : DailyMissionStatus.NOT_AVAILABLE.getClientId();
	}
	
	/**
	 * Retrieves the current progress of a player's daily mission.<br>
	 * It checks if the player has an active entry in the system.<br>
	 * If no entry exists, it returns {@code 0}.
	 * @param player The {@code Player} object to check.
	 * @return The current progress value as an {@code int}.
	 */
	public int getProgress(Player player)
	{
		final DailyMissionPlayerEntry entry = getPlayerEntry(player.getObjectId(), false);
		return entry != null ? entry.getProgress() : 0;
	}
	
	/**
	 * Checks if a player has finished a mission recently.<br>
	 * It retrieves the {@code DailyMissionPlayerEntry} for the given {@code Player}.<br>
	 * The method returns {@code true} only if the entry exists and was completed recently.
	 * @param player The {@code Player} to check.
	 * @return {@code true} if the mission was recently completed, otherwise {@code false}.
	 */
	public boolean isRecentlyCompleted(Player player)
	{
		final DailyMissionPlayerEntry entry = getPlayerEntry(player.getObjectId(), false);
		return (entry != null) && entry.isRecentlyCompleted();
	}
	
	/**
	 * Checks if a {@code Player} can claim their daily mission reward.<br>
	 * If the requirements are met, it grants the rewards and updates the player's status.
	 * @param player The {@code Player} requesting the reward.
	 * @return {@code true} if the reward was successfully given, or {@code false} otherwise.
	 */
	public boolean requestReward(Player player)
	{
		if (isAvailable(player) || isLevelUpMission())
		{
			giveRewards(player);
			
			final DailyMissionPlayerEntry entry = getPlayerEntry(player.getObjectId(), true);
			if (!_holder.isOneTime())
			{
				if (_holder.getCategory() == DailyMissionCategory.NORMAL.getClientId())
				{
					final int doneDailyMissions = player.getVariables().getInt(PlayerVariables.DAILY_MISSION_COUNT, 0);
					player.getVariables().set(PlayerVariables.DAILY_MISSION_COUNT, doneDailyMissions + 1);
				}
				entry.setStatus(DailyMissionStatus.NOT_AVAILABLE);
				entry.setProgress(0);
			}
			else
			{
				entry.setStatus(DailyMissionStatus.COMPLETED);
				final List<Integer> missions = player.getVariables().getIntegerList(PlayerVariables.DAILY_MISSION_ONE_TIME);
				missions.add(_holder.getId());
				player.getVariables().setIntegerList(PlayerVariables.DAILY_MISSION_ONE_TIME, missions);
			}
			
			entry.setLastCompleted(System.currentTimeMillis());
			entry.setRecentlyCompleted(true);
			storePlayerEntry(entry);
			
			return true;
		}
		
		return false;
	}
	
	/**
	 * Grants all available rewards to the specified {@code Player}.<br>
	 * This method iterates through the rewards stored in the {@code DailyMissionDataHolder}.
	 * @param player The {@code Player} who will receive the items.
	 */
	protected void giveRewards(Player player)
	{
		for (ItemHolder reward : _holder.getRewards())
		{
			final int rewardCount = (int) reward.getCount();
			if (reward.getId() == SpecialItemType.CLAN_REPUTATION.getClientId())
			{
				player.getClan().addReputationScore(rewardCount);
				player.increaseClanContribution(rewardCount);
				final SystemMessage sm = new SystemMessage(SystemMessageId.CLAN_REPUTATION_POINTS_S1_3);
				sm.addLong(rewardCount);
				player.sendPacket(sm);
			}
			else if (reward.getId() == SpecialItemType.FAME.getClientId())
			{
				player.setFame(player.getFame() + rewardCount);
				final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_ACQUIRED_S1_INDIVIDUAL_FAME);
				sm.addLong(rewardCount);
				player.sendPacket(sm);
				player.broadcastUserInfo();
			}
			else
			{
				player.addItem(ItemProcessType.REWARD, reward, player, true);
			}
		}
	}
	
	/**
	 * Saves a {@code DailyMissionPlayerEntry} to the database.<br>
	 * This method updates the character daily rewards table.<br>
	 * It also adds the entry to the internal cache if it is missing.
	 * @param entry The {@code DailyMissionPlayerEntry} object containing the data to save.
	 */
	protected void storePlayerEntry(DailyMissionPlayerEntry entry)
	{
		final Integer entryObjectId = entry.getObjectId();
		final int rewardId = entry.getRewardId();
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(REPLACE_ENTRY_SQL))
		{
			ps.setInt(1, entryObjectId);
			ps.setInt(2, rewardId);
			ps.setInt(3, entry.getStatus().getClientId());
			ps.setInt(4, entry.getProgress());
			ps.setLong(5, entry.getLastCompleted());
			ps.execute();
			
			// Cache if not exists.
			_entries.computeIfAbsent(entryObjectId, _ -> entry);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Error while saving reward " + rewardId + " for player: " + entryObjectId + " in database: ", e);
		}
	}
	
	/**
	 * Queues a {@code DailyMissionPlayerEntry} for the next batch write instead of writing it at once.<br>
	 * This is meant for progress that moves on every kill, where one write per event floods the connection pool.
	 * @param entry The {@code DailyMissionPlayerEntry} object containing the data to save.
	 */
	protected void storePlayerEntryAsync(DailyMissionPlayerEntry entry)
	{
		// Cache if not exists.
		_entries.computeIfAbsent(entry.getObjectId(), _ -> entry);
		PENDING_ENTRIES.add(entry);
	}
	
	/**
	 * Writes every queued entry to the database in batches.<br>
	 * The queue empties before the write, so an entry that moves during the write is kept for the next pass.
	 */
	public static void flushPendingEntries()
	{
		if (PENDING_ENTRIES.isEmpty())
		{
			return;
		}
		
		final List<DailyMissionPlayerEntry> entries = new ArrayList<>(PENDING_ENTRIES);
		PENDING_ENTRIES.removeAll(entries);
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(REPLACE_ENTRY_SQL))
		{
			int batched = 0;
			for (DailyMissionPlayerEntry entry : entries)
			{
				ps.setInt(1, entry.getObjectId());
				ps.setInt(2, entry.getRewardId());
				ps.setInt(3, entry.getStatus().getClientId());
				ps.setInt(4, entry.getProgress());
				ps.setLong(5, entry.getLastCompleted());
				ps.addBatch();
				
				// Send the rows away in chunks, so a long queue does not build one huge packet.
				if (++batched == FLUSH_BATCH_SIZE)
				{
					ps.executeBatch();
					batched = 0;
				}
			}
			
			if (batched > 0)
			{
				ps.executeBatch();
			}
		}
		catch (Exception e)
		{
			// The write is idempotent, so the queue takes the entries back for another try.
			PENDING_ENTRIES.addAll(entries);
			BATCH_LOGGER.log(Level.WARNING, "Error while saving queued daily mission entries in database: ", e);
		}
	}
	
	/**
	 * Retrieves the {@code DailyMissionPlayerEntry} for a specific player.<br>
	 * It checks the internal cache before querying the database.<br>
	 * If no entry exists and {@code createIfNone} is {@code true}, it creates a new one.
	 * @param objectId The unique identifier of the player character.
	 * @param createIfNone Whether to create a new entry if none is found in the cache or database.
	 * @return The {@code DailyMissionPlayerEntry} for the given ID, or {@code null} if not found and creation is disabled.
	 */
	protected DailyMissionPlayerEntry getPlayerEntry(int objectId, boolean createIfNone)
	{
		DailyMissionPlayerEntry entry = _entries.get(objectId);
		if (entry != null)
		{
			return entry;
		}
		
		final int holderId = _holder.getId();
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT * FROM character_daily_rewards WHERE charId = ? AND rewardId = ?"))
		{
			ps.setInt(1, objectId);
			ps.setInt(2, holderId);
			try (ResultSet rs = ps.executeQuery())
			{
				if (rs.next())
				{
					entry = new DailyMissionPlayerEntry(rs.getInt("charId"), rs.getInt("rewardId"), rs.getInt("status"), rs.getInt("progress"), rs.getLong("lastCompleted"));
					_entries.put(objectId, entry);
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Error while loading reward " + holderId + " for player: " + objectId + " in database: ", e);
		}
		
		if ((entry == null) && createIfNone)
		{
			entry = new DailyMissionPlayerEntry(objectId, holderId);
			_entries.put(objectId, entry);
		}
		
		return entry;
	}
}
