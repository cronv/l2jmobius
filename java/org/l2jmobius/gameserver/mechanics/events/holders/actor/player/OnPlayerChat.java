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
import org.l2jmobius.gameserver.network.enums.ChatType;

/**
 * This class represents an event triggered when a {@code Player} sends a chat message.<br>
 * It holds the necessary data for processing various types of {@code ChatType} messages.
 * @author UnAfraid
 */
public class OnPlayerChat implements IBaseEvent
{
	private final Player _player;
	private final String _target;
	private final String _text;
	private final ChatType _type;
	
	/**
	 * Creates a new {@code OnPlayerChat} event.<br>
	 * This method initializes the chat data for a specific player.<br>
	 * It stores the sender, target, message content, and chat category.
	 * @param player The {@code Player} who sent the message.
	 * @param target The name of the recipient or target.
	 * @param text The actual message string sent by the player.
	 * @param type The {@code ChatType} of the message.
	 */
	public OnPlayerChat(Player player, String target, String text, ChatType type)
	{
		_player = player;
		_target = target;
		_text = text;
		_type = type;
	}
	
	/**
	 * Retrieves the {@code Player} associated with this chat event.<br>
	 * This method returns the player who sent the message.
	 * @return the {@code Player} object.
	 */
	public Player getPlayer()
	{
		return _player;
	}
	
	/**
	 * Retrieves the name of the target player.<br>
	 * This is used when a {@code Player} sends a chat message to someone else.
	 * @return The {@code String} representing the target's name.
	 */
	public String getTarget()
	{
		return _target;
	}
	
	/**
	 * Retrieves the message content sent by the player.<br>
	 * This method returns the raw {@code String} value of the chat text.
	 * @return The chat text as a {@code String}.
	 */
	public String getText()
	{
		return _text;
	}
	
	/**
	 * Retrieves the type of chat message sent by the player.<br>
	 * This value is used to determine how the message should be displayed.
	 * @return the {@code ChatType} of this event.
	 */
	public ChatType getChatType()
	{
		return _type;
	}
	
	/**
	 * Retrieves the type of this event.<br>
	 * This method returns a constant value from {@code EventType}.
	 * @return The {@code EventType} associated with this chat event.
	 */
	@Override
	public EventType getType()
	{
		return EventType.ON_PLAYER_CHAT;
	}
}
