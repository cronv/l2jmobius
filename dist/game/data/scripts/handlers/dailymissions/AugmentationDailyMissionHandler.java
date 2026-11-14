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
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerAugment;
import org.l2jmobius.gameserver.mechanics.events.listeners.ConsumerEventListener;
import org.l2jmobius.gameserver.mechanics.options.AugmentationInstance;

/**
 * Handles the logic for daily missions related to item augmentations.<br>
 * It listens for {@code OnPlayerAugment} events to track player progress.<br>
 * This class updates the {@code DailyMissionDataHolder} when an augmentation is performed.
 * @author CostyKiller
 */
public class AugmentationDailyMissionHandler extends AbstractDailyMissionHandler
{
	private final int _missionId;
	private final int _amount;
	private final int _minLevel;
	private final int _maxLevel;
	private final Set<Integer> _mineralIds = new HashSet<>();
	
	/**
	 * Initializes the handler with data from a {@code DailyMissionDataHolder}.<br>
	 * It sets up mission requirements like amount and level ranges.<br>
	 * It also parses specific mineral IDs if they are provided.
	 * @param holder The data container for the daily mission.
	 */
	public AugmentationDailyMissionHandler(DailyMissionDataHolder holder)
	{
		super(holder);
		_missionId = holder.getId();
		_amount = holder.getRequiredCompletions();
		_minLevel = holder.getParams().getInt("minLevel", 85);
		_maxLevel = holder.getParams().getInt("maxLevel", Integer.MAX_VALUE);
		final String mineralIds = holder.getParams().getString("mineralIds", "");
		if (!mineralIds.isEmpty())
		{
			for (String s : mineralIds.split(","))
			{
				final int id = Integer.parseInt(s);
				if (!_mineralIds.contains(id))
				{
					_mineralIds.add(id);
				}
			}
		}
	}
	
	/**
	 * Initializes the handler for augmentation daily missions.<br>
	 * This method registers a listener to track player augmentation events.<br>
	 * It uses {@code Containers.Global()} to monitor these actions.
	 */
	@Override
	public void init()
	{
		Containers.Global().addListener(new ConsumerEventListener(Containers.Global(), EventType.ON_PLAYER_AUGMENT, (OnPlayerAugment event) -> onPlayerAugment(event), this));
	}
	
	/**
	 * Checks if the daily mission is currently available for a specific player.<br>
	 * It verifies the status of the {@code DailyMissionPlayerEntry}.<br>
	 * The method returns {@code true} if the mission is ready to be completed.
	 * @param player The {@code Player} object to check.
	 * @return {@code true} if available, otherwise {@code false}.
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
	 * Handles the logic for when a player performs an augmentation.<br>
	 * It checks if the player meets the required level range.<br>
	 * It verifies if the specific mineral requirements are met before updating progress.
	 * @param event The {@code OnPlayerAugment} event containing the player and item data.
	 */
	private void onPlayerAugment(OnPlayerAugment event)
	{
		final Player player = event.getPlayer();
		final int playerLevel = player.getLevel();
		if ((playerLevel < _minLevel) || (playerLevel > _maxLevel))
		{
			return;
		}
		
		// Only missions with specific augment stones.
		switch (_missionId)
		{
			case 3055:
			case 3056:
			case 3057:
			{
				// Check if used item has been augmented with specified stones.
				final AugmentationInstance augmentation = event.getItem().getAugmentation();
				if (augmentation != null)
				{
					for (int mineralId : _mineralIds)
					{
						if ((augmentation.getMineralId() == mineralId) && player.getInventory().getItemByItemId(event.getItem().getId()).isAugmented())
						{
							processPlayerProgress(player);
						}
					}
				}
				break;
			}
			default:
			{
				if (_mineralIds.isEmpty())
				{
					processPlayerProgress(player);
				}
				break;
			}
		}
	}
	
	/**
	 * Updates the progress for a player's daily mission.<br>
	 * It checks if the current status is {@code NOT_AVAILABLE}.<br>
	 * If the progress meets the required amount, it changes the status to {@code AVAILABLE}.<br>
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
}
