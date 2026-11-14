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
import org.l2jmobius.gameserver.mechanics.events.holders.item.OnItemCompound;
import org.l2jmobius.gameserver.mechanics.events.listeners.ConsumerEventListener;

/**
 * This class handles daily missions that require multiple conditions to be met.<br>
 * It processes complex requirements by checking various {@code EventType} triggers.<br>
 * It manages the progression of compound tasks for players.
 * @author CostyKiller
 */
public class CompoundDailyMissionHandler extends AbstractDailyMissionHandler
{
	private final int _requiredMissionCompleteId;
	private final int _amount;
	private final int _minLevel;
	private final int _maxLevel;
	private final Set<Integer> _itemIds = new HashSet<>();
	private final int _itemId;
	
	/**
	 * Initializes a new instance of the {@code CompoundDailyMissionHandler}.<br>
	 * This constructor sets up the mission requirements from the provided holder.<br>
	 * It extracts level limits, required amounts, and item IDs for tracking progress.
	 * @param holder The {@code DailyMissionDataHolder} containing the configuration data.
	 */
	public CompoundDailyMissionHandler(DailyMissionDataHolder holder)
	{
		super(holder);
		_requiredMissionCompleteId = holder.getRequiredMissionCompleteId();
		_amount = holder.getRequiredCompletions();
		_minLevel = holder.getParams().getInt("minLevel", 0);
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
	 * Initializes the handler by registering a global listener.<br>
	 * This allows the system to track {@code ON_ITEM_COMPOUND} events.<br>
	 * It connects the {@code ConsumerEventListener} to the {@code Containers.Global()} instance.
	 */
	@Override
	public void init()
	{
		Containers.Global().addListener(new ConsumerEventListener(Containers.Global(), EventType.ON_ITEM_COMPOUND, (OnItemCompound event) -> onItemCompound(event), this));
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
	 * Handles the logic for when a player interacts with an item compound.<br>
	 * It checks if the {@code Player} meets the required level range.<br>
	 * It verifies if the necessary prerequisite missions are completed.<br>
	 * It updates progress if the item matches the required IDs.
	 * @param event The {@code OnItemCompound} event containing player and item data.
	 */
	private void onItemCompound(OnItemCompound event)
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
	 * It checks if the player has met the required amount to unlock the mission.<br>
	 * If the requirements are met, it changes the status to {@code DailyMissionStatus.AVAILABLE}.<br>
	 * The updated entry is then saved to the database.
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
	 * It verifies that the mission entry exists and matches the required ID.<br>
	 * The status must be set to {@code DailyMissionStatus.COMPLETED}.
	 * @param player The {@code Player} object to check.
	 * @return {@code true} if all requirements are met, otherwise {@code false}.
	 */
	private boolean checkRequiredMission(Player player)
	{
		final DailyMissionPlayerEntry missionEntry = getPlayerEntry(player.getObjectId(), false);
		return (missionEntry != null) && (_requiredMissionCompleteId != 0) && (missionEntry.getRewardId() == _requiredMissionCompleteId) && (getStatus(player) == DailyMissionStatus.COMPLETED.getClientId());
	}
}
