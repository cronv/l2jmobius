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
package org.l2jmobius.gameserver.mechanics.events.returns;

import org.l2jmobius.gameserver.network.enums.ChatType;

/**
 * This class handles the return data for chat filtering events.<br>
 * It provides information about messages that were processed by the filter system.
 * @author UnAfraid
 */
public class ChatFilterReturn extends AbstractEventReturn
{
	private final String _filteredText;
	private final ChatType _chatType;
	
	/**
	 * Creates a new {@code ChatFilterReturn} object.<br>
	 * This constructor initializes the filter results and behavior flags.
	 * @param filteredText The text that remains after filtering.
	 * @param newChatType The new {@code ChatType} to assign to the message.
	 * @param override Whether to override existing values.
	 * @param abort Whether to stop processing if a filter is triggered.
	 */
	public ChatFilterReturn(String filteredText, ChatType newChatType, boolean override, boolean abort)
	{
		super(override, abort);
		_filteredText = filteredText;
		_chatType = newChatType;
	}
	
	/**
	 * Retrieves the text that was processed by the filter.<br>
	 * This method returns the final string content.
	 * @return The filtered {@code String} value.
	 */
	public String getFilteredText()
	{
		return _filteredText;
	}
	
	/**
	 * Retrieves the chat type associated with this event.<br>
	 * This is used to identify how the message should be handled.
	 * @return the {@code ChatType} of the filtered message.
	 */
	public ChatType getChatType()
	{
		return _chatType;
	}
}
