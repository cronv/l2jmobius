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
package handlers.dailymissions;

import java.util.HashSet;
import java.util.Set;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.DailyMissionStatus;
import org.l2jmobius.gameserver.entity.actor.holders.player.DailyMissionDataHolder;
import org.l2jmobius.gameserver.entity.actor.holders.player.DailyMissionPlayerEntry;
import org.l2jmobius.gameserver.handler.AbstractDailyMissionHandler;
import org.l2jmobius.gameserver.mechanics.events.Containers;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.item.OnItemCombination;
import org.l2jmobius.gameserver.mechanics.events.listeners.ConsumerEventListener;

/**
 * This class handles daily missions that require players to combine specific items.<br>
 * It listens for {@code OnItemCombination} events to track and complete these objectives.
 * @author CostyKiller
 */
public class CombinationDailyMissionHandler extends AbstractDailyMissionHandler
{
	private final int _requiredMissionCompleteId;
	private final int _amount;
	private final int _minLevel;
	private final int _maxLevel;
	private final Set<Integer> _itemIds = new HashSet<>();
	private final int _itemId;
	
	/**
	 * Initializes the handler with data from a {@code DailyMissionDataHolder}.<br>
	 * This constructor sets up requirements like item IDs and level limits.
	 * @param holder The holder containing the mission configuration data.
	 */
	public CombinationDailyMissionHandler(DailyMissionDataHolder holder)
	{
		super(holder);
		_requiredMissionCompleteId = holder.getRequiredMissionCompleteId();
		_amount = holder.getRequiredCompletions();
		_minLevel = holder.getParams().getInt("minLevel", 85);
		_maxLevel = holder.getParams().getInt("maxLevel", Integer.MAX_VALUE);
		_itemId = holder.getParams().getInt("itemId", 0);
		final String itemIds = holder.getParams().getString("itemIds", "");
		if (!itemIds.isEmpty())
		{
			for (String s : itemIds.split(","))
			{
				final int id = Integer.parseInt(s);
				if (!_itemIds.contains(id))
				{
					_itemIds.add(id);
				}
			}
		}
		else if (_itemId != 0)
		{
			_itemIds.add(holder.getParams().getInt("itemId"));
		}
	}
	
	/**
	 * Initializes the handler for combination missions.<br>
	 * This method registers a listener to detect item combinations.<br>
	 * It uses {@code EventType.ON_ITEM_COMBINATION} to trigger logic.
	 */
	@Override
	public void init()
	{
		Containers.Global().addListener(new ConsumerEventListener(Containers.Global(), EventType.ON_ITEM_COMBINATION, (OnItemCombination event) -> onItemCombination(event), this));
	}
	
	/**
	 * Checks if the daily mission is currently available for a specific player.<br>
	 * It verifies the player's progress and current status.
	 * @param player The {@code Player} to check.
	 * @return {@code true} if the mission is ready, otherwise {@code false}.
	 */
	@Override
	public boolean isAvailable(Player player)
	{
		final DailyMissionPlayerEntry entry = getPlayerEntry(player.getObjectId(), false);
		if (entry != null)
		{
			switch (entry.getStatus())
			{
				case NOT_AVAILABLE: // Initial state
				{
					if (entry.getProgress() >= _amount)
					{
						entry.setStatus(DailyMissionStatus.AVAILABLE);
						storePlayerEntry(entry);
					}
					break;
				}
				case AVAILABLE:
				{
					return true;
				}
			}
		}
		
		return false;
	}
	
	/**
	 * Handles the logic for when a player combines items.<br>
	 * It checks if the player meets the level requirements.<br>
	 * It verifies if the combined item matches the required IDs.<br>
	 * If all conditions are met, it updates the player progress.
	 * @param event The {@code OnItemCombination} event containing the player and item details.
	 */
	private void onItemCombination(OnItemCombination event)
	{
		final Player player = event.getPlayer();
		if ((player.getLevel() < _minLevel) || (player.getLevel() > _maxLevel))
		{
			return;
		}
		
		if (((_requiredMissionCompleteId != 0) && checkRequiredMission(player)) || (_requiredMissionCompleteId == 0))
		{
			if (!_itemIds.isEmpty())
			{
				for (int item : _itemIds)
				{
					// Check if used item has been obtained from item combination or alchemy
					if (event.getItem().getId() == item)
					{
						processPlayerProgress(player);
					}
				}
			}
			else
			{
				processPlayerProgress(player);
			}
		}
	}
	
	/**
	 * Updates the progress for a specific player's daily mission.<br>
	 * This method checks if the player has met the required amount to unlock the mission.<br>
	 * It updates the {@code DailyMissionStatus} and saves the data if necessary.
	 * @param player The {@code Player} object to process.
	 */
	private void processPlayerProgress(Player player)
	{
		final DailyMissionPlayerEntry entry = getPlayerEntry(player.getObjectId(), true);
		if (entry.getStatus() == DailyMissionStatus.NOT_AVAILABLE)
		{
			if (entry.increaseProgress() >= _amount)
			{
				entry.setStatus(DailyMissionStatus.AVAILABLE);
			}
			
			storePlayerEntry(entry);
		}
	}
	
	/**
	 * Checks if the player has completed a specific required mission.<br>
	 * This method verifies that the mission entry exists and matches the required ID.<br>
	 * It also ensures the current status is {@code DailyMissionStatus.COMPLETED}.
	 * @param player The {@code Player} object to check.
	 * @return {@code true} if all requirements are met, otherwise {@code false}.
	 */
	private boolean checkRequiredMission(Player player)
	{
		final DailyMissionPlayerEntry missionEntry = getPlayerEntry(player.getObjectId(), false);
		return (missionEntry != null) && (_requiredMissionCompleteId != 0) && (missionEntry.getRewardId() == _requiredMissionCompleteId) && (getStatus(player) == DailyMissionStatus.COMPLETED.getClientId());
	}
}
