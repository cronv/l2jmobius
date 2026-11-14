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
import org.l2jmobius.gameserver.mechanics.events.holders.item.OnItemEnchantAdd;
import org.l2jmobius.gameserver.mechanics.events.listeners.ConsumerEventListener;

/**
 * Handles the logic for daily missions related to enchanting items.<br>
 * It listens for {@code OnItemEnchantAdd} events to track player progress.<br>
 * This class updates the {@code DailyMissionDataHolder} when an enchantment is successfully applied.
 * @author CostyKiller
 */
public class EnchantDailyMissionHandler extends AbstractDailyMissionHandler
{
	private final int _requiredMissionCompleteId;
	private final int _amount;
	private final int _minLevel;
	private final int _maxLevel;
	private final int _targetValue;
	private final Set<Integer> _itemIds = new HashSet<>();
	
	/**
	 * Initializes the handler with data from a {@code DailyMissionDataHolder}.<br>
	 * This constructor sets up all required mission parameters.
	 * @param holder The data holder containing the mission configuration.
	 */
	public EnchantDailyMissionHandler(DailyMissionDataHolder holder)
	{
		super(holder);
		_requiredMissionCompleteId = holder.getRequiredMissionCompleteId();
		_amount = holder.getRequiredCompletions();
		_minLevel = holder.getParams().getInt("minLevel", 85);
		_maxLevel = holder.getParams().getInt("maxLevel", Integer.MAX_VALUE);
		_targetValue = holder.getParams().getInt("targetValue", 0);
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
	}
	
	/**
	 * Initializes the handler by registering necessary listeners.<br>
	 * It sets up a {@code ConsumerEventListener} for item enchant events.<br>
	 * This allows the system to track when items are enchanted.
	 */
	@Override
	public void init()
	{
		Containers.Global().addListener(new ConsumerEventListener(Containers.Global(), EventType.ON_ITEM_ENCHANT_ADD, (OnItemEnchantAdd event) -> onItemEnchantAdd(event), this));
	}
	
	/**
	 * Checks if the player can currently perform this daily mission.<br>
	 * It verifies if the player has met the required progress to unlock it.
	 * @param player The {@code Player} object to check.
	 * @return {@code true} if the mission is available, otherwise {@code false}.
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
	 * Handles the logic when an item enchantment is added.<br>
	 * It checks if the player meets the level requirements.<br>
	 * It verifies if the item ID and enchantment level are valid for progress.
	 * @param event The {@code OnItemEnchantAdd} event containing the player and item details.
	 */
	private void onItemEnchantAdd(OnItemEnchantAdd event)
	{
		final Player player = event.getPlayer();
		if ((player.getLevel() < _minLevel) || (player.getLevel() > _maxLevel))
		{
			return;
		}
		
		if ((((_requiredMissionCompleteId != 0) && checkRequiredMission(player)) || (_requiredMissionCompleteId == 0)) //
			&& _itemIds.contains(event.getItem().getId()) && (event.getItem().getEnchantLevel() >= _targetValue))
		{
			processPlayerProgress(player);
		}
	}
	
	/**
	 * Updates the progress for a specific player's daily mission.<br>
	 * It checks if the player has met the required amount to unlock the mission.<br>
	 * If the goal is reached, it changes the status to {@code DailyMissionStatus.AVAILABLE}.<br>
	 * The updated data is then saved to the database.
	 * @param player The {@code Player} object whose progress needs to be processed.
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
	 * Verifies if the player has completed a specific required mission.<br>
	 * This check ensures the reward ID matches and the status is {@code COMPLETED}.
	 * @param player The {@code Player} object to check.
	 * @return {@code true} if all requirements are met, otherwise {@code false}.
	 */
	private boolean checkRequiredMission(Player player)
	{
		final DailyMissionPlayerEntry missionEntry = getPlayerEntry(player.getObjectId(), false);
		return (missionEntry != null) && (_requiredMissionCompleteId != 0) && (missionEntry.getRewardId() == _requiredMissionCompleteId) && (getStatus(player) == DailyMissionStatus.COMPLETED.getClientId());
	}
}
