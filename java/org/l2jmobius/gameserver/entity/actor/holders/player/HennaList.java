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
package org.l2jmobius.gameserver.entity.actor.holders.player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.config.custom.PremiumSystemConfig;
import org.l2jmobius.gameserver.data.xml.HennaData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.tasks.player.HennaDurationTask;
import org.l2jmobius.gameserver.entity.item.Henna;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.mechanics.events.EventDispatcher;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerHennaAdd;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerHennaRemove;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.stats.BaseStat;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.enums.UserInfoType;
import org.l2jmobius.gameserver.network.serverpackets.HennaInfo;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * Manages the hennas of a {@code Player}.<br>
 * It handles adding, removing and expiring the dyes worn in the henna slots.<br>
 * This class interacts with the database to persist the henna information.
 * @author Mobius
 */
public class HennaList
{
	private static final Logger LOGGER = Logger.getLogger(HennaList.class.getName());
	
	private static final String RESTORE_CHAR_HENNAS = "SELECT slot,symbol_id FROM character_hennas WHERE charId=? AND class_index=?";
	private static final String ADD_CHAR_HENNA = "REPLACE INTO character_hennas (charId,symbol_id,slot,class_index) VALUES (?,?,?,?)";
	private static final String DELETE_CHAR_HENNA = "DELETE FROM character_hennas WHERE charId=? AND slot=? AND class_index=?";
	
	private final Player _player;
	private final Henna[] _hennas = new Henna[4];
	private final Map<BaseStat, Integer> _baseStats = new ConcurrentHashMap<>();
	private final Map<Integer, ScheduledFuture<?>> _removeSchedules = new ConcurrentHashMap<>(4);
	
	/**
	 * Creates a new {@code HennaList} for a specific player.
	 * @param player The {@code Player} object associated with this henna list.
	 */
	public HennaList(Player player)
	{
		_player = player;
	}
	
	/**
	 * Restores the character's henna data from the database.<br>
	 * This method clears existing henna tasks and schedules new ones based on remaining duration.<br>
	 * It also applies any skills associated with the restored hennas.<br>
	 * Finally, it recalculates the player's stats based on the updated henna modifiers.
	 */
	public void restore()
	{
		for (int i = 1; i < 5; i++)
		{
			_hennas[i - 1] = null;
		}
		
		// Cancel and remove existing running tasks.
		for (Entry<Integer, ScheduledFuture<?>> entry : _removeSchedules.entrySet())
		{
			final ScheduledFuture<?> task = entry.getValue();
			if ((task != null) && !task.isCancelled() && !task.isDone())
			{
				task.cancel(true);
			}
			
			_removeSchedules.remove(entry.getKey());
		}
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(RESTORE_CHAR_HENNAS))
		{
			statement.setInt(1, _player.getObjectId());
			statement.setInt(2, _player.getClassIndex());
			try (ResultSet rset = statement.executeQuery())
			{
				int slot;
				int symbolId;
				final long currentTime = System.currentTimeMillis();
				while (rset.next())
				{
					slot = rset.getInt("slot");
					if ((slot < 1) || (slot > 4))
					{
						continue;
					}
					
					symbolId = rset.getInt("symbol_id");
					if (symbolId == 0)
					{
						continue;
					}
					
					final Henna henna = HennaData.getInstance().getHenna(symbolId);
					
					// Task for henna duration.
					if (henna.getDuration() > 0)
					{
						final long remainingTime = _player.getVariables().getLong("HennaDuration" + slot, currentTime) - currentTime;
						if (remainingTime < 0)
						{
							remove(slot);
							continue;
						}
						
						// Add the new task.
						_removeSchedules.put(slot, ThreadPool.schedule(new HennaDurationTask(_player, slot), currentTime + remainingTime));
					}
					
					_hennas[slot - 1] = henna;
					
					// Reward henna skills.
					for (Skill skill : henna.getSkills())
					{
						_player.addSkill(skill, false);
					}
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Failed restoring character " + _player + " hennas.", e);
		}
		
		// Calculate henna modifiers of this player.
		recalculate(false);
	}
	
	/**
	 * Adds a new {@code Henna} to the player.<br>
	 * This method checks for available slots and validates premium requirements.<br>
	 * It updates character stats, saves data to the database, and handles duration tasks.
	 * @param henna The {@code Henna} object to be added.
	 * @return {@code true} if the henna was successfully added, {@code false} otherwise.
	 */
	public boolean add(Henna henna)
	{
		for (int i = 1; i < 5; i++)
		{
			// Check for retail premium dyes slot.
			if (!PremiumSystemConfig.PREMIUM_HENNA_SLOT_ALL_DYES)
			{
				if (i == 4)
				{
					if ((_hennas[3] != null) || !henna.isPremium())
					{
						return false;
					}
				}
				else if (henna.isPremium())
				{
					continue;
				}
			}
			
			if (_hennas[i - 1] == null)
			{
				_hennas[i - 1] = henna;
				
				// Calculate Henna modifiers of this Player.
				recalculate(true);
				
				try (Connection con = DatabaseFactory.getConnection();
					PreparedStatement statement = con.prepareStatement(ADD_CHAR_HENNA))
				{
					statement.setInt(1, _player.getObjectId());
					statement.setInt(2, henna.getDyeId());
					statement.setInt(3, i);
					statement.setInt(4, _player.getClassIndex());
					statement.execute();
				}
				catch (Exception e)
				{
					LOGGER.log(Level.SEVERE, "Failed saving character henna.", e);
				}
				
				// Task for henna duration.
				final int duration = henna.getDuration();
				if (duration > 0)
				{
					final long currentTime = System.currentTimeMillis();
					final long durationInMillis = duration * 60000;
					_player.getVariables().set("HennaDuration" + i, currentTime + durationInMillis);
					_removeSchedules.put(i, ThreadPool.schedule(new HennaDurationTask(_player, i), currentTime + durationInMillis));
				}
				
				// Reward henna skills.
				for (Skill skill : henna.getSkills())
				{
					_player.addSkill(skill, false);
				}
				
				// Send Server->Client HennaInfo packet to this Player.
				_player.sendPacket(new HennaInfo(_player));
				
				// Send Server->Client UserInfo packet to this Player.
				_player.broadcastUserInfo(UserInfoType.BASE_STATS, UserInfoType.MAX_HPCPMP, UserInfoType.STATS, UserInfoType.SPEED);
				
				// Notify to scripts.
				if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_HENNA_ADD, _player))
				{
					EventDispatcher.getInstance().notifyEventAsync(new OnPlayerHennaAdd(_player, henna), _player);
				}
				
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Removes the henna from a specific slot.<br>
	 * This method updates the database and recalculates player stats.<br>
	 * It also handles dye restoration and skill removal.
	 * @param slot The index of the henna slot to clear.
	 * @return {@code true} if the henna was successfully removed, {@code false} otherwise.
	 */
	public boolean remove(int slot)
	{
		if ((slot < 1) || (slot > 4))
		{
			return false;
		}
		
		final Henna henna = _hennas[slot - 1];
		if (henna == null)
		{
			return false;
		}
		
		_hennas[slot - 1] = null;
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(DELETE_CHAR_HENNA))
		{
			statement.setInt(1, _player.getObjectId());
			statement.setInt(2, slot);
			statement.setInt(3, _player.getClassIndex());
			statement.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Failed removing character henna.", e);
		}
		
		// Calculate Henna modifiers of this Player.
		recalculate(true);
		
		// Send Server->Client HennaInfo packet to this Player.
		_player.sendPacket(new HennaInfo(_player));
		
		// Send Server->Client UserInfo packet to this Player.
		_player.broadcastUserInfo(UserInfoType.BASE_STATS, UserInfoType.MAX_HPCPMP, UserInfoType.STATS, UserInfoType.SPEED);
		
		final long currentTime = System.currentTimeMillis();
		final long timeLeft = _player.getVariables().getLong("HennaDuration" + slot, currentTime) - currentTime;
		final int hennaDuration = henna.getDuration();
		if ((hennaDuration < 0) || (timeLeft > 0))
		{
			// Add the recovered dyes to the player's inventory and notify them.
			final long cancelFee = henna.getCancelFee();
			if ((cancelFee > 0) && (_player.hasPremiumStatus() || (slot != 4)))
			{
				_player.reduceAdena(ItemProcessType.FEE, cancelFee, _player, false);
			}
			
			final long cancelCount = henna.getCancelCount();
			if (cancelCount > 0)
			{
				final int dyeItemId = henna.getDyeItemId();
				_player.getInventory().addItem(ItemProcessType.RESTORE, dyeItemId, cancelCount, _player, null);
				final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_OBTAINED_S1_X_S2);
				sm.addItemName(dyeItemId);
				sm.addLong(cancelCount);
				_player.sendPacket(sm);
			}
		}
		
		_player.sendPacket(SystemMessageId.TATTOO_WAS_REMOVED);
		
		// Remove henna duration task.
		if (hennaDuration > 0)
		{
			_player.getVariables().remove("HennaDuration" + slot);
			if (_removeSchedules.get(slot) != null)
			{
				_removeSchedules.get(slot).cancel(false);
				_removeSchedules.remove(slot);
			}
		}
		
		// Remove henna skills.
		for (Skill skill : henna.getSkills())
		{
			_player.removeSkill(skill, false);
		}
		
		// Notify to scripts.
		if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_HENNA_REMOVE, _player))
		{
			EventDispatcher.getInstance().notifyEventAsync(new OnPlayerHennaRemove(_player, henna), _player);
		}
		
		return true;
	}
	
	/**
	 * Cancels every pending henna duration task.<br>
	 * This method is called when the player logs out or is removed from the world.
	 */
	public void stopSchedules()
	{
		for (Entry<Integer, ScheduledFuture<?>> entry : _removeSchedules.entrySet())
		{
			final ScheduledFuture<?> task = entry.getValue();
			if ((task != null) && !task.isCancelled() && !task.isDone())
			{
				task.cancel(false);
			}
			
			_removeSchedules.remove(entry.getKey());
		}
	}
	
	/**
	 * Calculates the number of available empty henna slots.<br>
	 * The total capacity depends on the player class level.<br>
	 * It subtracts occupied slots from the maximum allowed amount.
	 * @return The count of remaining empty henna slots.
	 */
	public int getEmptySlots()
	{
		int totalSlots = 0;
		final int classLevel = _player.getPlayerClass().level();
		if (classLevel == 1)
		{
			totalSlots = 2;
		}
		else if (classLevel > 1)
		{
			totalSlots = 3;
		}
		
		for (int i = 0; i < 3; i++)
		{
			if (_hennas[i] != null)
			{
				totalSlots--;
			}
		}
		
		if (totalSlots <= 0)
		{
			return 0;
		}
		
		return totalSlots;
	}
	
	/**
	 * Retrieves a {@code Henna} object from a specific slot.<br>
	 * The valid range for the slot is between 1 and 4 inclusive.
	 * @param slot The index of the henna slot to retrieve.
	 * @return The {@code Henna} object at the given slot, or {@code null} if the slot is invalid.
	 */
	public Henna get(int slot)
	{
		if ((slot < 1) || (slot > 4))
		{
			return null;
		}
		
		return _hennas[slot - 1];
	}
	
	/**
	 * Checks if the character has any active hennas.<br>
	 * It iterates through the internal henna collection.
	 * @return {@code true} if at least one non-null henna exists, otherwise {@code false}.
	 */
	public boolean hasAny()
	{
		for (Henna henna : _hennas)
		{
			if (henna != null)
			{
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Retrieves the list of henna items.
	 * @return an array of {@code Henna} objects.
	 */
	public Henna[] getAll()
	{
		return _hennas;
	}
	
	/**
	 * Retrieves the henna value for a specific base statistic.<br>
	 * It returns {@code 0} if no value is found for the given stat.
	 * @param stat The {@code BaseStat} to check.
	 * @return The integer value associated with the stat.
	 */
	public int getValue(BaseStat stat)
	{
		return _baseStats.getOrDefault(stat, 0);
	}
	
	/**
	 * Retrieves the base statistics for henna.
	 * @return a {@code Map} containing {@code BaseStat} keys and their corresponding integer values.
	 */
	public Map<BaseStat, Integer> getBaseStats()
	{
		return _baseStats;
	}
	
	/**
	 * Recalculates the base statistics for all active henna items.<br>
	 * It clears and rebuilds the {@code _baseStats} map by summing values from every {@code Henna}.<br>
	 * If {@code recalculatePlayerStat} is {@code true}, it also updates the player's current stats and regeneration.
	 * @param recalculatePlayerStat Determines whether to update the player's active statistics after updating henna totals.
	 */
	private void recalculate(boolean recalculatePlayerStat)
	{
		_baseStats.clear();
		for (Henna henna : _hennas)
		{
			if (henna == null)
			{
				continue;
			}
			
			for (Entry<BaseStat, Integer> entry : henna.getBaseStats().entrySet())
			{
				_baseStats.merge(entry.getKey(), entry.getValue(), Integer::sum);
			}
		}
		
		if (recalculatePlayerStat)
		{
			_player.getStat().recalculateStats(true);
			_player.getStatus().startHpMpRegeneration();
		}
	}
}
