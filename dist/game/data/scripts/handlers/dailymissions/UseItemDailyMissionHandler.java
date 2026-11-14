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
import org.l2jmobius.gameserver.mechanics.events.holders.item.OnItemUse;
import org.l2jmobius.gameserver.mechanics.events.listeners.ConsumerEventListener;
import org.l2jmobius.gameserver.network.serverpackets.pledgeV2.ExPledgeMissionInfo;
import org.l2jmobius.gameserver.network.serverpackets.pledgeV2.ExPledgeMissionRewardCount;

/**
 * Handles the logic for daily missions that require a player to use an item.<br>
 * It listens for {@code OnItemUse} events and updates the corresponding mission progress.
 * @author CostyKiller
 */
public class UseItemDailyMissionHandler extends AbstractDailyMissionHandler
{
	private final int _amount;
	private final int _minLevel;
	private final int _maxLevel;
	private final Set<Integer> _itemIds = new HashSet<>();
	
	/**
	 * Initializes the handler with data from a {@code DailyMissionDataHolder}.<br>
	 * It sets up requirements like amount, level range, and item IDs.
	 * @param holder The holder containing the mission configuration.
	 */
	public UseItemDailyMissionHandler(DailyMissionDataHolder holder)
	{
		super(holder);
		_amount = holder.getRequiredCompletions();
		_minLevel = holder.getParams().getInt("minLevel", 85);
		_maxLevel = holder.getParams().getInt("maxLevel", Integer.MAX_VALUE);
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
	 * Initializes the handler for daily missions involving item usage.<br>
	 * This method registers a {@code ConsumerEventListener} to listen for {@code EventType.ON_ITEM_USE} events.<br>
	 * It ensures that the system can track when players use specific items.
	 */
	@Override
	public void init()
	{
		Containers.Global().addListener(new ConsumerEventListener(Containers.Global(), EventType.ON_ITEM_USE, (OnItemUse event) -> onItemUse(event), this));
	}
	
	/**
	 * Checks if the daily mission is currently available for a specific player.<br>
	 * It verifies if the player has met the required progress to unlock the mission.
	 * @param player The {@code Player} object to check.
	 * @return {@code true} if the mission is ready to be completed, otherwise {@code false}.
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
	 * Handles the logic for when a player uses an item.<br>
	 * It checks if the player meets the level requirements.<br>
	 * It verifies if the used item is part of the required set.<br>
	 * If all conditions are met, it updates the player's progress.
	 * @param event The {@code OnItemUse} event containing the player and item details.
	 */
	private void onItemUse(OnItemUse event)
	{
		final Player player = event.getPlayer();
		if ((player.getLevel() < _minLevel) || (player.getLevel() > _maxLevel) || _itemIds.isEmpty())
		{
			return;
		}
		
		if (_itemIds.contains(event.getItem().getId()))
		{
			processPlayerProgress(player);
		}
	}
	
	/**
	 * Updates the progress for a player's daily mission.<br>
	 * This method checks if the player has met the required amount to complete the task.<br>
	 * It changes the status to {@code DailyMissionStatus.AVAILABLE} if the goal is reached.
	 * @param player The {@code Player} whose progress needs to be updated.
	 */
	private void processPlayerProgress(Player player)
	{
		final DailyMissionPlayerEntry entry = getPlayerEntry(player.getObjectId(), true);
		if (entry.getStatus() == DailyMissionStatus.NOT_AVAILABLE)
		{
			if (entry.increaseProgress() >= _amount)
			{
				entry.setStatus(DailyMissionStatus.AVAILABLE);
				player.sendPacket(new ExPledgeMissionInfo(player, getHolder()));
				player.sendPacket(new ExPledgeMissionRewardCount(player));
			}
			
			storePlayerEntry(entry);
		}
	}
}
