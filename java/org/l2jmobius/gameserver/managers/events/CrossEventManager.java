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
package org.l2jmobius.gameserver.managers.events;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.holders.player.CrossEventAdvancedRewardHolder;
import org.l2jmobius.gameserver.entity.actor.holders.player.CrossEventHolder;
import org.l2jmobius.gameserver.entity.actor.holders.player.CrossEventProgress;
import org.l2jmobius.gameserver.entity.actor.holders.player.CrossEventRegularRewardHolder;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.network.serverpackets.crossevent.ExCrossEventInfo;

/**
 * Manages the logic and lifecycle of cross-server events.<br>
 * It handles event data, rewards, and synchronization between different game instances.
 * @author Smoto
 */
public class CrossEventManager
{
	private final List<CrossEventRegularRewardHolder> _regularRewards = new ArrayList<>();
	private final List<CrossEventAdvancedRewardHolder> _advancedRewards = new ArrayList<>();
	private final Map<String, List<Integer>> _rewardsAvailable = new ConcurrentHashMap<>();
	private int _ticketId = 60833; // Summer Flower Coupon.
	private int _displayId = 1;
	private int _endTime;
	private int _dailyReset;
	private int _resetCostAmount;
	
	/**
	 * Sets the unique identifier for the event ticket.<br>
	 * This value is used to identify which specific ticket type is active.
	 * @param ticketId The {@code int} ID of the ticket to set.
	 */
	public void setTicketId(int ticketId)
	{
		_ticketId = ticketId;
	}
	
	/**
	 * Retrieves the unique identifier for the current ticket.<br>
	 * This value is used to identify specific event rewards.
	 * @return The {@code int} value of the ticket ID.
	 */
	public int getTicketId()
	{
		return _ticketId;
	}
	
	/**
	 * Sets the unique identifier for displaying this event.<br>
	 * This value is used to identify the event in the user interface.
	 * @param displayId The new {@code int} ID to assign to the display.
	 */
	public void setDisplayId(int displayId)
	{
		_displayId = displayId;
	}
	
	/**
	 * Retrieves the unique identifier used for displaying this event.<br>
	 * This value is used to distinguish between different events in the user interface.
	 * @return The current {@code int} display ID.
	 */
	public int getDisplayId()
	{
		return _displayId;
	}
	
	/**
	 * Sets the end time for the cross event.<br>
	 * This value determines when the event will conclude.
	 * @param endTime The new end time to be assigned.
	 */
	public void setEndTime(int endTime)
	{
		_endTime = endTime;
	}
	
	/**
	 * Retrieves the end time of the current event.<br>
	 * The value is returned as an {@code int}.
	 * @return The end time of the event.
	 */
	public int getEndTime()
	{
		return _endTime;
	}
	
	/**
	 * Retrieves the list of regular rewards for the cross event.<br>
	 * This method returns all {@code CrossEventRegularRewardHolder} objects currently stored in the manager.
	 * @return a {@code List} containing all regular reward holders.
	 */
	public List<CrossEventRegularRewardHolder> getRegularRewardsList()
	{
		return _regularRewards;
	}
	
	/**
	 * Retrieves the list of advanced rewards for cross events.<br>
	 * This method returns all {@code CrossEventAdvancedRewardHolder} objects currently stored in the manager.
	 * @return a {@code List} containing all {@code CrossEventAdvancedRewardHolder} instances.
	 */
	public List<CrossEventAdvancedRewardHolder> getAdvancedRewardList()
	{
		return _advancedRewards;
	}
	
	/**
	 * Sets the number of daily resets allowed.<br>
	 * This value is stored in the {@code _dailyReset} field.
	 * @param dailyReset The number of daily resets to set.
	 */
	public void setDailyResets(int dailyReset)
	{
		_dailyReset = dailyReset;
	}
	
	/**
	 * Retrieves the number of daily resets configured for the event.<br>
	 * This value is stored in the {@code _dailyReset} field.
	 * @return The total count of daily resets as an {@code int}.
	 */
	public int getDailyResets()
	{
		return _dailyReset;
	}
	
	/**
	 * Sets the cost required to perform a reset.<br>
	 * This value is stored in the {@code _resetCostAmount} field.
	 * @param resetCostAmount The amount of currency needed for a reset.
	 */
	public void setResetCostAmount(int resetCostAmount)
	{
		_resetCostAmount = resetCostAmount;
	}
	
	/**
	 * Retrieves the cost required to perform a reset.<br>
	 * This value is stored in the {@code _resetCostAmount} field.
	 * @return The amount of currency needed for a reset.
	 */
	public int getResetCostAmount()
	{
		return _resetCostAmount;
	}
	
	/**
	 * Retrieves the total count of game tickets held by a player.<br>
	 * It checks the inventory for an item with the ID {@code _ticketId}.<br>
	 * If no such item is found, it returns {@code 0}.
	 * @param player The {@code Player} whose inventory will be checked.
	 * @return The number of tickets in the player's inventory.
	 */
	public int getGameTickets(Player player)
	{
		final Item item = player.getInventory().getItemByItemId(_ticketId);
		if (item != null)
		{
			return (int) item.getCount();
		}
		
		return 0;
	}
	
	/**
	 * Retrieves the list of rewards available for a specific player.<br>
	 * This method checks if any rewards are assigned to the account name of the {@code Player}.<br>
	 * If no rewards exist, it returns an empty list.
	 * @param player The {@code Player} object to check for rewards.
	 * @return A {@code List} of integers representing the available reward IDs.
	 */
	public List<Integer> getPlayerRewardsAvailable(Player player)
	{
		final String accountName = player.getAccountName();
		if (!_rewardsAvailable.containsKey(accountName))
		{
			return Collections.emptyList();
		}
		
		return _rewardsAvailable.get(accountName);
	}
	
	/**
	 * Adds a specific reward to the list of available rewards for a player.<br>
	 * This method identifies the player by their account name.<br>
	 * It stores the {@code reward} in a list associated with that account.
	 * @param player The {@code Player} object whose rewards are being updated.
	 * @param reward The {@code int} value representing the reward to add.
	 */
	public void addRewardsAvailable(Player player, int reward)
	{
		final String accountName = player.getAccountName();
		final List<Integer> rewards = _rewardsAvailable.computeIfAbsent(accountName, _ -> new ArrayList<>());
		
		rewards.add(reward);
	}
	
	/**
	 * Checks if the {@code Player} is eligible for advanced rewards.<br>
	 * It scans the player's cross event cells to see if they meet specific requirements.<br>
	 * If a reward is available, it updates the player's status and sends an update packet.
	 * @param player The {@code Player} object to check.
	 */
	public void checkAdvancedRewardAvailable(Player player)
	{
		final CrossEventProgress crossEventProgress = player.getCrossEventProgress();
		final List<CrossEventHolder> cells = crossEventProgress.getCells();
		if (!rewardExist(player, 1) && (cells.stream().filter(c -> c.getHorizontal() == 0).count() == 4))
		{
			addRewardsAvailable(player, 1);
			crossEventProgress.addAdvancedRewardCount(1);
		}
		
		if (!rewardExist(player, 2) && (cells.stream().filter(c -> c.getHorizontal() == 1).count() == 4))
		{
			addRewardsAvailable(player, 2);
			crossEventProgress.addAdvancedRewardCount(1);
		}
		
		if (!rewardExist(player, 3) && (cells.stream().filter(c -> c.getHorizontal() == 2).count() == 4))
		{
			addRewardsAvailable(player, 3);
			crossEventProgress.addAdvancedRewardCount(1);
		}
		
		if (!rewardExist(player, 4) && (cells.stream().filter(c -> c.getHorizontal() == 3).count() == 4))
		{
			addRewardsAvailable(player, 4);
			crossEventProgress.addAdvancedRewardCount(1);
		}
		
		if (!rewardExist(player, 5) && (cells.stream().filter(c -> c.getVertical() == 0).count() == 4))
		{
			addRewardsAvailable(player, 5);
			crossEventProgress.addAdvancedRewardCount(1);
		}
		
		if (!rewardExist(player, 6) && (cells.stream().filter(c -> c.getVertical() == 1).count() == 4))
		{
			addRewardsAvailable(player, 6);
			crossEventProgress.addAdvancedRewardCount(1);
		}
		
		if (!rewardExist(player, 7) && (cells.stream().filter(c -> c.getVertical() == 2).count() == 4))
		{
			addRewardsAvailable(player, 7);
			crossEventProgress.addAdvancedRewardCount(1);
		}
		
		if (!rewardExist(player, 8) && (cells.stream().filter(c -> c.getVertical() == 3).count() == 4))
		{
			addRewardsAvailable(player, 8);
			crossEventProgress.addAdvancedRewardCount(1);
		}
		
		player.sendPacket(new ExCrossEventInfo(player));
	}
	
	/**
	 * Checks if a specific reward is available for a {@code Player}.<br>
	 * It looks up the rewards associated with the player's account name.
	 * @param player The {@code Player} to check.
	 * @param reward The {@code int} ID of the reward to find.
	 * @return {@code true} if the reward exists in the list, otherwise {@code false}.
	 */
	private boolean rewardExist(Player player, int reward)
	{
		final String accountName = player.getAccountName();
		if (!_rewardsAvailable.containsKey(accountName))
		{
			return false;
		}
		
		final List<Integer> list = _rewardsAvailable.get(accountName);
		return list.contains(reward);
	}
	
	/**
	 * This method clears the advanced rewards for a specific player.<br>
	 * It removes the reward data associated with the {@code Player} account name.<br>
	 * Use this to reset progress when necessary.
	 * @param player The {@code Player} whose rewards need to be cleared.
	 */
	public void resetAdvancedRewards(Player player)
	{
		_rewardsAvailable.remove(player.getAccountName());
	}
	
	/**
	 * Retrieves the singleton instance of the {@code CrossEventManager}.<br>
	 * Use this method to access the global manager for cross-event logic.
	 * @return The single instance of {@code CrossEventManager}.
	 */
	public static CrossEventManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final CrossEventManager INSTANCE = new CrossEventManager();
	}
}
