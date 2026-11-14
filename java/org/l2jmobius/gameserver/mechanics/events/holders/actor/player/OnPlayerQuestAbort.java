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

/**
 * This class represents an event triggered when a {@code Player} aborts a quest.<br>
 * It holds the necessary data for handling quest cancellation logic.
 * @author Sdw
 */
public class OnPlayerQuestAbort implements IBaseEvent
{
	private final Player _player;
	private final int _questId;
	
	/**
	 * Creates a new {@code OnPlayerQuestAbort} event.<br>
	 * This event is triggered when a player cancels a specific quest.
	 * @param player The {@code Player} who aborted the quest.
	 * @param questId The unique identifier for the quest that was cancelled.
	 */
	public OnPlayerQuestAbort(Player player, int questId)
	{
		_player = player;
		_questId = questId;
	}
	
	/**
	 * Retrieves the {@code Player} associated with this event.
	 * @return the {@code Player} object.
	 */
	public Player getPlayer()
	{
		return _player;
	}
	
	/**
	 * Retrieves the unique identifier for the quest. <br>
	 * This ID corresponds to the quest that was aborted by the player.
	 * @return The {@code int} value of the quest ID.
	 */
	public int getQuestId()
	{
		return _questId;
	}
	
	/**
	 * Retrieves the type of this event.<br>
	 * This method returns a constant from the {@code EventType} enum.
	 * @return the {@code EventType} associated with this event.
	 */
	@Override
	public EventType getType()
	{
		return EventType.ON_PLAYER_QUEST_ABORT;
	}
}
