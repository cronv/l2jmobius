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
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerBecomeNoblesse;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerLogin;
import org.l2jmobius.gameserver.mechanics.events.listeners.ConsumerEventListener;

/**
 * Handles the logic for daily missions specific to {@code Noblesse} players.<br>
 * It manages mission progress and updates based on player actions and status changes.
 * @author CostyKiller
 */
public class NoblesseDailyMissionHandler extends AbstractDailyMissionHandler
{
	private final int _amount;
	private final int _minLevel;
	private final int _maxLevel;
	
	/**
	 * Initializes the handler with data from a {@code DailyMissionDataHolder}.<br>
	 * It sets the required amount and level limits for this mission.
	 * @param holder The holder containing the mission configuration.
	 */
	public NoblesseDailyMissionHandler(DailyMissionDataHolder holder)
	{
		super(holder);
		_amount = holder.getRequiredCompletions();
		_minLevel = holder.getParams().getInt("minLevel", 85);
		_maxLevel = holder.getParams().getInt("maxLevel", Integer.MAX_VALUE);
	}
	
	/**
	 * Initializes the handler by registering necessary event listeners.<br>
	 * This method sets up the {@code ConsumerEventListener} for the {@code ON_PLAYER_BECOME_NOBLESSE} event.<br>
	 * It ensures that the system can correctly track when a player achieves noblesse status.
	 */
	@Override
	public void init()
	{
		Containers.Global().addListener(new ConsumerEventListener(Containers.Global(), EventType.ON_PLAYER_BECOME_NOBLESSE, (OnPlayerBecomeNoblesse event) -> onPlayerBecomeNoblesse(event), this));
		Containers.Global().addListener(new ConsumerEventListener(Containers.Global(), EventType.ON_PLAYER_LOGIN, (OnPlayerLogin event) -> onPlayerLogin(event), this));
	}
	
	/**
	 * Checks if the daily mission is available for a specific player.<br>
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
	 * Handles the logic when a player becomes a noble.<br>
	 * It checks if the player level is within the allowed range.<br>
	 * It verifies that the player belongs to a clan and has a noble level of 1.<br>
	 * If all conditions are met, it updates the player progress.
	 * @param event The {@code OnPlayerBecomeNoblesse} event containing the player data.
	 */
	private void onPlayerBecomeNoblesse(OnPlayerBecomeNoblesse event)
	{
		final Player player = event.getPlayer();
		if ((player.getLevel() < _minLevel) || (player.getLevel() > _maxLevel))
		{
			return;
		}
		
		// Check if player is belonging to a clan and is noble.
		if ((player.getClan() != null) && (player.getNobleLevel() > 0) && (player.getNobleLevel() == 1))
		{
			processPlayerProgress(player);
		}
	}
	
	/**
	 * Handles the logic when a player logs into the game.<br>
	 * It checks if the player meets the required level range.<br>
	 * It also verifies if the player is a noble within a clan.<br>
	 * If all conditions are met, it processes the player's progress.
	 * @param event The {@code OnPlayerLogin} event containing the player data.
	 */
	private void onPlayerLogin(OnPlayerLogin event)
	{
		final Player player = event.getPlayer();
		if ((player.getLevel() < _minLevel) || (player.getLevel() > _maxLevel))
		{
			return;
		}
		
		// Check if player is belonging to a clan and is noble.
		if ((player.getClan() != null) && (player.getNobleLevel() > 0) && (player.getNobleLevel() == 1))
		{
			processPlayerProgress(player);
		}
	}
	
	/**
	 * Updates the progress for a specific player's daily mission.<br>
	 * It checks if the {@code DailyMissionStatus} is {@code NOT_AVAILABLE}.<br>
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
