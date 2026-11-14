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

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.DailyMissionStatus;
import org.l2jmobius.gameserver.entity.actor.holders.player.DailyMissionDataHolder;
import org.l2jmobius.gameserver.entity.actor.holders.player.DailyMissionPlayerEntry;
import org.l2jmobius.gameserver.handler.AbstractDailyMissionHandler;
import org.l2jmobius.gameserver.mechanics.events.Containers;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerBecomeExalted;
import org.l2jmobius.gameserver.mechanics.events.listeners.ConsumerEventListener;

/**
 * Handles the logic for daily missions related to the {@code Exalted} status.<br>
 * It processes events triggered when a {@code Player} becomes exalted.
 * @author CostyKiller
 */
public class ExaltedDailyMissionHandler extends AbstractDailyMissionHandler
{
	private final int _amount;
	private final int _minLevel;
	private final int _maxLevel;
	
	/**
	 * Initializes the handler with data from a {@code DailyMissionDataHolder}.<br>
	 * It sets the required completion amount and level limits.
	 * @param holder The holder containing the mission configuration data.
	 */
	public ExaltedDailyMissionHandler(DailyMissionDataHolder holder)
	{
		super(holder);
		_amount = holder.getRequiredCompletions();
		_minLevel = holder.getParams().getInt("minLevel", 85);
		_maxLevel = holder.getParams().getInt("maxLevel", Integer.MAX_VALUE);
	}
	
	/**
	 * Initializes the handler by registering necessary event listeners.<br>
	 * It listens for {@code ON_PLAYER_BECOME_EXALTED} events globally.
	 */
	@Override
	public void init()
	{
		Containers.Global().addListener(new ConsumerEventListener(Containers.Global(), EventType.ON_PLAYER_BECOME_EXALTED, (OnPlayerBecomeExalted event) -> onPlayerBecomeExalted(event), this));
	}
	
	/**
	 * Checks if the daily mission is currently available for a specific player.<br>
	 * It verifies if the player has met the required progress to unlock it.
	 * @param player The {@code Player} object to check.
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
	 * Handles the logic when a player reaches an exalted status.<br>
	 * It checks if the {@code Player} level is within the allowed range.<br>
	 * It verifies that the player belongs to a clan and has reached noble level 2.<br>
	 * If all conditions are met, it updates the mission progress.
	 * @param event The {@code OnPlayerBecomeExalted} event containing the player data.
	 */
	private void onPlayerBecomeExalted(OnPlayerBecomeExalted event)
	{
		final Player player = event.getPlayer();
		final int level = player.getLevel();
		if ((level < _minLevel) || (level > _maxLevel))
		{
			return;
		}
		
		// Check if player is belonging to a clan and is exalted
		if ((player.getClan() != null) && (player.getNobleLevel() > 0))
		{
			if (player.getNobleLevel() == 2)
			{
				processPlayerProgress(player);
			}
		}
	}
	
	/**
	 * Updates the progress for a specific player's daily mission.<br>
	 * This method checks if the player has met the required amount to unlock the mission.<br>
	 * It updates the {@code DailyMissionStatus} and saves the data if necessary.
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
}
