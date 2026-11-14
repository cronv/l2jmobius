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

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.managers.events.CrossEventManager;
import org.l2jmobius.gameserver.mechanics.variables.PlayerVariables;

/**
 * Manages the cross event progress of a {@code Player}.<br>
 * It holds the opened cells, the advanced reward count and the daily reset count, all kept in player variables.
 * @author Mobius
 */
public class CrossEventProgress
{
	private final Player _player;
	private int _advancedRewardCount;
	private final List<CrossEventHolder> _cells = new ArrayList<>();
	
	/**
	 * Creates a new {@code CrossEventProgress} for a specific player.
	 * @param player The {@code Player} object associated with this instance.
	 */
	public CrossEventProgress(Player player)
	{
		_player = player;
	}
	
	/**
	 * Retrieves the current count of daily resets for the player.<br>
	 * It checks the player's variables first.<br>
	 * If no value is found, it returns the global count from {@code CrossEventManager}.
	 * @return The number of daily resets as an {@code int}.
	 */
	public int getResetCount()
	{
		return _player.getVariables().getInt(PlayerVariables.CROSS_EVENT_DAILY_RESET_COUNT, CrossEventManager.getInstance().getDailyResets());
	}
	
	/**
	 * Updates the daily reset count for the player.<br>
	 * This value is stored in the {@code PlayerVariables} collection.
	 * @param dailyReset The new count to set for the daily reset.
	 */
	public void setResetCount(int dailyReset)
	{
		_player.getVariables().set(PlayerVariables.CROSS_EVENT_DAILY_RESET_COUNT, dailyReset);
	}
	
	/**
	 * Retrieves the total count of cross rewards.<br>
	 * This value represents the amount of advanced rewards earned.
	 * @return The number of cross rewards as an {@code int}.
	 */
	public int getAdvancedRewardCount()
	{
		return _advancedRewardCount;
	}
	
	/**
	 * Updates the total count of cross advanced rewards.<br>
	 * This method adds the specified amount to the current reward tally.
	 * @param value The amount to add to the reward count.
	 */
	public void addAdvancedRewardCount(int value)
	{
		_advancedRewardCount += value;
	}
	
	/**
	 * Retrieves the list of cross-cell events.<br>
	 * This method returns all active {@code CrossEventHolder} objects.
	 * @return a {@code List} containing all {@code CrossEventHolder} instances.
	 */
	public List<CrossEventHolder> getCells()
	{
		return _cells;
	}
	
	/**
	 * Resets and restores the cross-event data for this player.<br>
	 * It clears existing cells and rewards from the cache.<br>
	 * It then populates the active cells and available rewards based on player variables.
	 */
	public void restore()
	{
		_cells.clear();
		CrossEventManager.getInstance().resetAdvancedRewards(_player);
		
		final List<Integer> cells = _player.getVariables().getIntegerList(PlayerVariables.CROSS_EVENT_CELLS);
		if (!cells.isEmpty())
		{
			for (int id : cells)
			{
				_cells.add(new CrossEventHolder(id));
			}
		}
		
		final List<Integer> rewards = _player.getVariables().getIntegerList(PlayerVariables.CROSS_EVENT_REWARDS);
		if (!rewards.isEmpty())
		{
			for (int id : rewards)
			{
				CrossEventManager.getInstance().addRewardsAvailable(_player, id);
			}
		}
		
		addAdvancedRewardCount(_player.getVariables().getInt(PlayerVariables.CROSS_EVENT_ADVANCED_COUNT, 0));
	}
	
	/**
	 * Updates the player variables for the cross event.<br>
	 * This method populates cell IDs and available rewards.<br>
	 * It also sets the count of advanced rewards.
	 */
	public void store()
	{
		final List<Integer> cells = new ArrayList<>();
		_cells.forEach(data ->
		{
			cells.add(data.cellId());
		});
		_player.getVariables().setIntegerList(PlayerVariables.CROSS_EVENT_CELLS, cells);
		_player.getVariables().setIntegerList(PlayerVariables.CROSS_EVENT_REWARDS, CrossEventManager.getInstance().getPlayerRewardsAvailable(_player));
		_player.getVariables().set(PlayerVariables.CROSS_EVENT_ADVANCED_COUNT, _advancedRewardCount);
	}
}
