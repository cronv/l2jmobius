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
package org.l2jmobius.gameserver.mechanics.events.holders.actor.player;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;
import org.l2jmobius.gameserver.mechanics.script.QuestFinishType;

/**
 * This class represents an event triggered when a {@code Player} completes a quest.<br>
 * It holds the necessary data to handle logic associated with quest completion.
 * @author UnAfraid
 */
public class OnPlayerQuestComplete implements IBaseEvent
{
	private final Player _player;
	private final int _questId;
	private final QuestFinishType _questType;
	
	/**
	 * Creates a new {@code OnPlayerQuestComplete} event.<br>
	 * This method initializes the event with specific player and quest data.
	 * @param player The {@code Player} who finished the quest.
	 * @param questId The unique identifier for the completed quest.
	 * @param questType The category of the quest being completed.
	 */
	public OnPlayerQuestComplete(Player player, int questId, QuestFinishType questType)
	{
		_player = player;
		_questId = questId;
		_questType = questType;
	}
	
	/**
	 * Retrieves the {@code Player} associated with this event.<br>
	 * This method returns the player who completed the quest.
	 * @return the {@code Player} object.
	 */
	public Player getPlayer()
	{
		return _player;
	}
	
	/**
	 * Retrieves the unique identifier for the quest.<br>
	 * This value corresponds to the {@code questId} provided during construction.
	 * @return The integer ID of the completed quest.
	 */
	public int getQuestId()
	{
		return _questId;
	}
	
	/**
	 * Retrieves the type of quest associated with this event.<br>
	 * This method returns the {@code QuestFinishType} assigned to the player.
	 * @return the {@code QuestFinishType} of the completed quest.
	 */
	public QuestFinishType getQuestType()
	{
		return _questType;
	}
	
	/**
	 * Returns the type of this event.<br>
	 * This method identifies the specific {@code EventType} associated with a quest completion.
	 * @return The {@code EventType} for player quest completion.
	 */
	@Override
	public EventType getType()
	{
		return EventType.ON_PLAYER_QUEST_COMPLETE;
	}
}
