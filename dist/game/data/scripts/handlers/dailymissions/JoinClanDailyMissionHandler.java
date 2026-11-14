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

import java.util.function.Consumer;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.DailyMissionStatus;
import org.l2jmobius.gameserver.entity.actor.holders.player.DailyMissionDataHolder;
import org.l2jmobius.gameserver.entity.actor.holders.player.DailyMissionPlayerEntry;
import org.l2jmobius.gameserver.handler.AbstractDailyMissionHandler;
import org.l2jmobius.gameserver.mechanics.events.Containers;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerClanCreate;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerClanJoin;
import org.l2jmobius.gameserver.mechanics.events.listeners.ConsumerEventListener;

/**
 * Handles the logic for the daily mission that requires a player to join a clan.<br>
 * It listens for {@code OnPlayerClanJoin} events to verify completion.<br>
 * This class updates the {@code DailyMissionDataHolder} when the requirements are met.
 * @author kamikadzz
 */
public class JoinClanDailyMissionHandler extends AbstractDailyMissionHandler
{
	/**
	 * Initializes the handler for joining a clan daily mission.<br>
	 * This constructor sets up the required data holder.
	 * @param holder The {@code DailyMissionDataHolder} used to manage mission progress.
	 */
	public JoinClanDailyMissionHandler(DailyMissionDataHolder holder)
	{
		super(holder);
	}
	
	/**
	 * Checks if the daily mission is available for a specific player.<br>
	 * It verifies that the player has an entry and the status is {@code AVAILABLE}.
	 * @param player The {@code Player} to check.
	 * @return {@code true} if the mission is available, otherwise {@code false}.
	 */
	@Override
	public boolean isAvailable(Player player)
	{
		final DailyMissionPlayerEntry entry = getPlayerEntry(player.getObjectId(), false);
		return (entry != null) && (entry.getStatus() == DailyMissionStatus.AVAILABLE);
	}
	
	/**
	 * Initializes the handler by registering event listeners.<br>
	 * It listens for both clan join and clan create events.<br>
	 * These listeners trigger the mission processing logic.
	 */
	@Override
	public void init()
	{
		Containers.Global().addListener(new ConsumerEventListener(this, EventType.ON_PLAYER_CLAN_JOIN, (Consumer<OnPlayerClanJoin>) this::onPlayerClanJoin, this));
		Containers.Global().addListener(new ConsumerEventListener(this, EventType.ON_PLAYER_CLAN_CREATE, (Consumer<OnPlayerClanCreate>) this::onPlayerClanCreate, this));
	}
	
	/**
	 * Handles the logic when a player joins a clan.<br>
	 * This method retrieves the daily mission data for the joining player.<br>
	 * It then triggers the process to update the specific mission progress.
	 * @param event The {@code OnPlayerClanJoin} event containing the player and clan details.
	 */
	private void onPlayerClanJoin(OnPlayerClanJoin event)
	{
		final DailyMissionPlayerEntry missionData = getPlayerEntry(event.getClanMember().getPlayer().getObjectId(), true);
		processMission(missionData);
	}
	
	/**
	 * Handles the logic when a player creates a new clan.<br>
	 * This method retrieves the daily mission data for the player.<br>
	 * It then processes the mission progress based on the creation event.
	 * @param event The {@code OnPlayerClanCreate} event containing the player information.
	 */
	private void onPlayerClanCreate(OnPlayerClanCreate event)
	{
		final DailyMissionPlayerEntry missionData = getPlayerEntry(event.getPlayer().getObjectId(), true);
		processMission(missionData);
	}
	
	/**
	 * Updates the progress of a specific daily mission.<br>
	 * It checks if the mission is already finished.<br>
	 * If not, it sets the progress to {@code 1}.<br>
	 * Finally, it saves the updated data to the database.
	 * @param missionData The {@code DailyMissionPlayerEntry} object containing the current mission state.
	 */
	private void processMission(DailyMissionPlayerEntry missionData)
	{
		if (missionData.getProgress() == 1)
		{
			missionData.setStatus(DailyMissionStatus.COMPLETED);
		}
		else
		{
			missionData.setProgress(1);
			missionData.setStatus(DailyMissionStatus.AVAILABLE);
		}
		
		storePlayerEntry(missionData);
	}
}
