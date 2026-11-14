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
import org.l2jmobius.gameserver.entity.actor.holders.player.Mentee;
import org.l2jmobius.gameserver.handler.AbstractDailyMissionHandler;
import org.l2jmobius.gameserver.managers.MentorManager;
import org.l2jmobius.gameserver.mechanics.events.Containers;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerMenteeTrainingComplete;
import org.l2jmobius.gameserver.mechanics.events.listeners.ConsumerEventListener;

/**
 * Handles the logic for daily missions related to the mentor system.<br>
 * It processes events triggered when a {@code Mentee} completes training tasks. This class ensures that progress is correctly updated within the {@code DailyMissionDataHolder}.
 * @author CostyKiller
 */
public class MentorDailyMissionHandler extends AbstractDailyMissionHandler
{
	private final int _amount;
	
	/**
	 * Initializes the handler with data from a {@code DailyMissionDataHolder}.<br>
	 * It sets the required amount and level limits for the mission.
	 * @param holder The holder containing the mission configuration.
	 */
	public MentorDailyMissionHandler(DailyMissionDataHolder holder)
	{
		super(holder);
		_amount = holder.getRequiredCompletions();
	}
	
	/**
	 * Initializes the handler by registering necessary event listeners.<br>
	 * It listens for {@code ON_PLAYER_MENTEE_TRAINING_COMPLETE} events.<br>
	 * This ensures that progress is tracked when a mentee finishes training.
	 */
	@Override
	public void init()
	{
		Containers.Global().addListener(new ConsumerEventListener(Containers.Global(), EventType.ON_PLAYER_MENTEE_TRAINING_COMPLETE, (OnPlayerMenteeTrainingComplete event) -> onPlayerMenteeTrainingComplete(event), this));
	}
	
	/**
	 * Checks if the daily mission is currently available for a specific player.<br>
	 * It verifies if the player has met the required progress to unlock the mission.
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
	 * Handles the logic when a player finishes training their mentee.<br>
	 * It checks if the mentor meets the required level range.<br>
	 * If valid, it updates the progress for the correct mentor.
	 * @param event The {@code OnPlayerMenteeTrainingComplete} event containing mentor and mentee data.
	 */
	private void onPlayerMenteeTrainingComplete(OnPlayerMenteeTrainingComplete event)
	{
		// Check mentor for penalty after mentee graduated.
		final Player player = event.getMentor();
		final Mentee mentor = MentorManager.getInstance().getMentor(event.getMentee().getObjectId());
		if ((player == mentor.getPlayer()) && (MentorManager.getInstance().getMentorPenalty(player.getObjectId()) > 0))
		{
			processPlayerProgress(player);
		}
	}
	
	/**
	 * Updates the progress of a player's daily mission.<br>
	 * This method checks if the {@code Player} has met the required amount to unlock the mission.<br>
	 * It updates the status to {@code DailyMissionStatus.AVAILABLE} if the goal is reached.
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
