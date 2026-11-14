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

import java.util.Calendar;

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
 * Handles the logic for daily missions specifically active during weekend login events.<br>
 * This class manages how players receive and complete these unique tasks when they log in.
 * @author Iris, Mobius
 */
public class LoginWeekendDailyMissionHandler extends AbstractDailyMissionHandler
{
	/**
	 * Initializes a new instance of the {@code LoginWeekendDailyMissionHandler}.<br>
	 * This constructor sets up the handler with the required mission data.
	 * @param holder The {@code DailyMissionDataHolder} containing the necessary mission information.
	 */
	public LoginWeekendDailyMissionHandler(DailyMissionDataHolder holder)
	{
		super(holder);
	}
	
	/**
	 * Checks if the daily mission is available for a specific player.<br>
	 * It verifies that the player has an entry and its status is {@code AVAILABLE}.
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
	 * This method sets up a listener for {@code EventType.ON_PLAYER_LOGIN}.<br>
	 * It ensures that the {@code onPlayerLogin} logic is triggered correctly.
	 */
	@Override
	public void init()
	{
		Containers.Global().addListener(new ConsumerEventListener(this, EventType.ON_PLAYER_LOGIN, (OnPlayerLogin event) -> onPlayerLogin(event), this));
	}
	
	/**
	 * Handles the logic for daily missions when a player logs in.<br>
	 * It checks if the mission is incomplete and meets specific time requirements.<br>
	 * If conditions are met, it updates the progress to 1 and sets the status to {@code DailyMissionStatus.AVAILABLE}.
	 * @param event The {@code OnPlayerLogin} event containing the player data.
	 */
	private void onPlayerLogin(OnPlayerLogin event)
	{
		final DailyMissionPlayerEntry entry = getPlayerEntry(event.getPlayer().getObjectId(), true);
		if (entry.getStatus() != DailyMissionStatus.COMPLETED)
		{
			final int currentDay = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
			if (((currentDay == Calendar.SATURDAY) || (currentDay == Calendar.SUNDAY) || (currentDay == Calendar.MONDAY)) //
				&& (Calendar.getInstance().get(Calendar.HOUR_OF_DAY) < 6) && (Calendar.getInstance().get(Calendar.MINUTE) < 30))
			{
				entry.setProgress(1);
				entry.setStatus(DailyMissionStatus.AVAILABLE);
			}
		}
		
		storePlayerEntry(entry);
	}
}
