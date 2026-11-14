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
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerAuctionWin;
import org.l2jmobius.gameserver.mechanics.events.listeners.ConsumerEventListener;

/**
 * Handles the logic for daily missions related to auction house activities.<br>
 * It tracks player progress when they win items in an auction.<br>
 * This class ensures that {@code DailyMissionDataHolder} is updated correctly based on auction events.
 * @author CostyKiller
 */
public class AuctionDailyMissionHandler extends AbstractDailyMissionHandler
{
	private final int _amount;
	private final int _minLevel;
	private final int _maxLevel;
	
	/**
	 * Initializes the handler for auction-related daily missions.<br>
	 * This constructor sets up the required completion amount from the data holder.
	 * @param holder The {@code DailyMissionDataHolder} containing mission requirements.
	 */
	public AuctionDailyMissionHandler(DailyMissionDataHolder holder)
	{
		super(holder);
		_amount = holder.getRequiredCompletions();
		_minLevel = holder.getParams().getInt("minLevel", 85);
		_maxLevel = holder.getParams().getInt("maxLevel", Integer.MAX_VALUE);
	}
	
	/**
	 * Initializes the handler for auction daily missions.<br>
	 * This method registers a listener for the {@code ON_PLAYER_AUCTION_WIN} event.<br>
	 * It ensures that progress is tracked when a player wins an auction.
	 */
	@Override
	public void init()
	{
		Containers.Players().addListener(new ConsumerEventListener(this, EventType.ON_PLAYER_AUCTION_WIN, (OnPlayerAuctionWin event) -> onPlayerAuctionWin(event), this));
	}
	
	/**
	 * Checks if the auction daily mission is ready for the player.<br>
	 * It verifies if the player has met the required progress amount.<br>
	 * The method returns {@code true} if the status is {@code AVAILABLE}.
	 * @param player The {@code Player} to check.
	 * @return {@code true} if available, {@code false} otherwise.
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
	 * Handles the logic when a player wins an auction.<br>
	 * This method identifies the winning {@code Player}.<br>
	 * It then updates their daily mission progress.
	 * @param event The {@code OnPlayerAuctionWin} event containing the winner details.
	 */
	private void onPlayerAuctionWin(OnPlayerAuctionWin event)
	{
		final Player player = event.getPlayer();
		if ((player.getLevel() < _minLevel) || (player.getLevel() > _maxLevel))
		{
			return;
		}
		
		processPlayerProgress(player);
	}
	
	/**
	 * Updates the progress for a player's daily mission.<br>
	 * It checks if the player has met the required {@code _amount}.<br>
	 * If the goal is reached, it changes the status to {@code DailyMissionStatus.AVAILABLE}.<br>
	 * The updated entry is saved to the database.
	 * @param player The {@code Player} whose progress needs to be processed.
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
