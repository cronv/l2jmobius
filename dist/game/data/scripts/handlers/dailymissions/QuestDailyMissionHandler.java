/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package handlers.dailymissions;

import org.l2jmobius.gameserver.data.xml.NewQuestData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.DailyMissionStatus;
import org.l2jmobius.gameserver.entity.actor.holders.player.DailyMissionDataHolder;
import org.l2jmobius.gameserver.entity.actor.holders.player.DailyMissionPlayerEntry;
import org.l2jmobius.gameserver.handler.AbstractDailyMissionHandler;
import org.l2jmobius.gameserver.mechanics.events.Containers;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerQuestComplete;
import org.l2jmobius.gameserver.mechanics.events.listeners.ConsumerEventListener;
import org.l2jmobius.gameserver.mechanics.script.QuestState;
import org.l2jmobius.gameserver.mechanics.script.newquestdata.NewQuest;

/**
 * This class handles the logic for daily missions that are triggered by completing quests.<br>
 * It processes quest completion events to update a player's {@code DailyMissionDataHolder}.<br>
 * It ensures that progress is correctly tracked and rewarded based on specific quest criteria.
 * @author UnAfraid
 */
public class QuestDailyMissionHandler extends AbstractDailyMissionHandler
{
	private final int _amount;
	private final int _questId;
	
	/**
	 * Creates a new instance of {@code QuestDailyMissionHandler}.<br>
	 * This constructor initializes the handler with specific mission data.<br>
	 * It sets the required completion amount from the provided holder.
	 * @param holder The {@code DailyMissionDataHolder} containing the mission details.
	 */
	public QuestDailyMissionHandler(DailyMissionDataHolder holder)
	{
		super(holder);
		_amount = holder.getRequiredCompletions();
		_questId = holder.getParams().getInt("questId", 0);
	}
	
	/**
	 * Initializes the handler for daily missions.<br>
	 * This method registers a listener for quest completion events.<br>
	 * It ensures that {@code onQuestComplete} is called when a player finishes a quest.
	 */
	@Override
	public void init()
	{
		Containers.Players().addListener(new ConsumerEventListener(this, EventType.ON_PLAYER_QUEST_COMPLETE, (OnPlayerQuestComplete event) -> onQuestComplete(event), this));
	}
	
	/**
	 * Checks if the daily mission is ready for the player.<br>
	 * It verifies if the required progress has been met.
	 * @param player The {@code Player} to check.
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
	 * Handles the logic when a player completes a quest.<br>
	 * It checks if the quest is of type {@code QuestFinishType.DAILY}.<br>
	 * If it is, it updates the progress for that specific daily mission.<br>
	 * The status changes to {@code DailyMissionStatus.AVAILABLE} once the required amount is met.
	 * @param event The {@code OnPlayerQuestComplete} event containing player and quest details.
	 */
	private void onQuestComplete(OnPlayerQuestComplete event)
	{
		// Check if player has active quest the quest id specified
		if (_questId == event.getQuestId())
		{
			final NewQuest quest = NewQuestData.getInstance().getQuestById(_questId);
			if (quest != null)
			{
				final Player player = event.getPlayer();
				final QuestState qs = player.getQuestState(quest.getName());
				if ((qs != null) && qs.isCompleted())
				{
					processPlayerProgress(player);
				}
			}
		}
	}
	
	/**
	 * Updates the progress for a player's daily mission.<br>
	 * This method checks if the player has met the required amount to unlock the mission.<br>
	 * It updates the status to {@code DailyMissionStatus.AVAILABLE} if the goal is reached.
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
