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
 * This class represents an event triggered when a {@code Player} provides an answer to a dialogue.<br>
 * It holds the necessary data for processing player interactions with NPCs or other entities.
 * @author UnAfraid
 */
public class OnPlayerDlgAnswer implements IBaseEvent
{
	private final Player _player;
	private final int _messageId;
	private final int _answer;
	private final int _requesterId;
	
	/**
	 * Creates a new {@code OnPlayerDlgAnswer} event.<br>
	 * This method initializes the data for a player's dialogue response.
	 * @param player The {@code Player} who provided the answer.
	 * @param messageId The unique identifier of the dialogue message.
	 * @param answer The specific choice selected by the player.
	 * @param requesterId The ID of the entity that requested the dialogue.
	 */
	public OnPlayerDlgAnswer(Player player, int messageId, int answer, int requesterId)
	{
		_player = player;
		_messageId = messageId;
		_answer = answer;
		_requesterId = requesterId;
	}
	
	/**
	 * Retrieves the {@code Player} associated with this event.<br>
	 * This method returns the player who triggered the dialog answer.
	 * @return the {@code Player} object.
	 */
	public Player getPlayer()
	{
		return _player;
	}
	
	/**
	 * Retrieves the unique identifier for the message.<br>
	 * This ID corresponds to the specific dialogue being answered.
	 * @return The {@code int} value of the message ID.
	 */
	public int getMessageId()
	{
		return _messageId;
	}
	
	/**
	 * Retrieves the answer provided by the player.<br>
	 * This value corresponds to the specific choice made in a dialogue.
	 * @return The {@code int} value of the selected answer.
	 */
	public int getAnswer()
	{
		return _answer;
	}
	
	/**
	 * Retrieves the unique identifier of the player who requested this action.<br>
	 * This ID is used to identify the original sender of the dialogue request.
	 * @return The {@code int} ID of the requester.
	 */
	public int getRequesterId()
	{
		return _requesterId;
	}
	
	/**
	 * Retrieves the type of this event.<br>
	 * This method returns a constant from the {@code EventType} enum.
	 * @return the {@code EventType} associated with this event.
	 */
	@Override
	public EventType getType()
	{
		return EventType.ON_PLAYER_DLG_ANSWER;
	}
}
