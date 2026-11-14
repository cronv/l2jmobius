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
package org.l2jmobius.gameserver.handler;

import java.util.EnumMap;
import java.util.Map;

import org.l2jmobius.gameserver.network.enums.ChatType;

/**
 * This class manages the processing of various chat messages within the game.<br>
 * It acts as a central handler for different {@code ChatType} actions.
 * @author durgus
 */
public class ChatHandler implements IHandler<IChatHandler, ChatType>
{
	private final Map<ChatType, IChatHandler> _datatable = new EnumMap<>(ChatType.class);
	
	/**
	 * Private constructor for the {@code ChatHandler} singleton.<br>
	 * This prevents other classes from creating new instances of this class.
	 */
	protected ChatHandler()
	{
	}
	
	/**
	 * Registers a new chat handler into the system.<br>
	 * This method maps each {@code ChatType} to the provided {@code IChatHandler}.<br>
	 * It ensures that all supported types for the handler are correctly stored.
	 * @param handler The {@code IChatHandler} instance to register.
	 */
	@Override
	public void registerHandler(IChatHandler handler)
	{
		for (ChatType type : handler.getChatTypeList())
		{
			_datatable.put(type, handler);
		}
	}
	
	/**
	 * Removes a specific {@code IChatHandler} from the internal registry.<br>
	 * This method clears all {@code ChatType} entries associated with the provided handler.<br>
	 * It ensures thread safety by using the {@code synchronized} keyword.
	 * @param handler The {@code IChatHandler} instance to be removed.
	 */
	@Override
	public synchronized void removeHandler(IChatHandler handler)
	{
		for (ChatType type : handler.getChatTypeList())
		{
			_datatable.remove(type);
		}
	}
	
	/**
	 * Retrieves the specific handler for a given chat type.<br>
	 * This method looks up the handler in the internal data table.
	 * @param chatType The {@code ChatType} to look up.
	 * @return The corresponding {@code IChatHandler} or {@code null} if not found.
	 */
	@Override
	public IChatHandler getHandler(ChatType chatType)
	{
		return _datatable.get(chatType);
	}
	
	/**
	 * Returns the total number of registered handlers.<br>
	 * This count includes all entries in the internal {@code Map}.
	 * @return The number of active handlers.
	 */
	@Override
	public int size()
	{
		return _datatable.size();
	}
	
	/**
	 * Provides the global instance of the {@code ChatHandler}.<br>
	 * This method follows the singleton pattern.<br>
	 * Use this to access the chat handling system from anywhere in the code.
	 * @return The single instance of {@code ChatHandler}.
	 */
	public static ChatHandler getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final ChatHandler INSTANCE = new ChatHandler();
	}
}
