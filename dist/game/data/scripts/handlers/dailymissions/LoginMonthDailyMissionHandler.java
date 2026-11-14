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
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerLogin;
import org.l2jmobius.gameserver.mechanics.events.listeners.ConsumerEventListener;

/**
 * Handles the logic for monthly login daily missions.<br>
 * This class manages how players receive and progress through these specific rewards.<br>
 * It ensures that {@code DailyMissionDataHolder} is updated correctly upon player actions.
 * @author Iris, Mobius
 */
public class LoginMonthDailyMissionHandler extends AbstractDailyMissionHandler
{
	/**
	 * Constructs a new {@code LoginMonthDailyMissionHandler}.<br>
	 * This constructor initializes the handler with the provided data holder.
	 * @param holder The {@code DailyMissionDataHolder} containing mission information.
	 */
	public LoginMonthDailyMissionHandler(DailyMissionDataHolder holder)
	{
		super(holder);
	}
	
	/**
	 * Checks if the login month daily mission is available for a specific player.<br>
	 * It verifies that the player has an entry and that its status is {@code AVAILABLE}.
	 * @param player The {@code Player} to check.
	 * @return {@code true} if the mission is available, {@code false} otherwise.
	 */
	@Override
	public boolean isAvailable(Player player)
	{
		final DailyMissionPlayerEntry entry = getPlayerEntry(player.getObjectId(), false);
		return (entry != null) && (entry.getStatus() == DailyMissionStatus.AVAILABLE);
	}
	
	/**
	 * Initializes the handler by registering necessary events.<br>
	 * This method sets up the {@code ON_PLAYER_LOGIN} listener.
	 */
	@Override
	public void init()
	{
		Containers.Global().addListener(new ConsumerEventListener(this, EventType.ON_PLAYER_LOGIN, (OnPlayerLogin event) -> onPlayerLogin(event), this));
	}
	
	/**
	 * Handles the logic when a player logs into the game.<br>
	 * It checks if the monthly daily mission is already completed.<br>
	 * If not finished, it sets the progress to 1 and updates the status to {@code AVAILABLE}.<br>
	 * The updated entry is then saved to the database.
	 * @param event The {@code OnPlayerLogin} event containing the player data.
	 */
	private void onPlayerLogin(OnPlayerLogin event)
	{
		final DailyMissionPlayerEntry entry = getPlayerEntry(event.getPlayer().getObjectId(), true);
		if (entry.getStatus() != DailyMissionStatus.COMPLETED)
		{
			entry.setProgress(1);
			entry.setStatus(DailyMissionStatus.AVAILABLE);
		}
		
		storePlayerEntry(entry);
	}
}
