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

import java.util.HashMap;
import java.util.Map;

/**
 * This class handles the processing of bypass commands sent by players.<br>
 * It parses incoming {@code String} messages to execute specific server-side actions.
 * @author nBd
 */
public class BypassHandler implements IHandler<IBypassHandler, String>
{
	private final Map<String, IBypassHandler> _datatable;
	
	/**
	 * Initializes a new instance of the {@code BypassHandler}.<br>
	 * This constructor sets up the internal data table.
	 */
	protected BypassHandler()
	{
		_datatable = new HashMap<>();
	}
	
	/**
	 * Registers a new {@code I BypassHandler} into the system.<br>
	 * This method maps every command in the handler's list to the handler itself.<br>
	 * The commands are stored using lowercase keys for easier lookup.
	 * @param handler The {@code I BypassHandler} instance to register.
	 */
	@Override
	public void registerHandler(IBypassHandler handler)
	{
		for (String element : handler.getCommandList())
		{
			_datatable.put(element.toLowerCase(), handler);
		}
	}
	
	/**
	 * Removes a specific {@code I BypassHandler} from the internal registry.<br>
	 * This method clears all commands associated with the provided handler.<br>
	 * The operation is thread-safe because it is synchronized.
	 * @param handler The {@code I BypassHandler} instance to be removed.
	 */
	@Override
	public synchronized void removeHandler(IBypassHandler handler)
	{
		for (String element : handler.getCommandList())
		{
			_datatable.remove(element.toLowerCase());
		}
	}
	
	/**
	 * Retrieves a specific handler based on the provided command.<br>
	 * This method extracts the first word of the input string.<br>
	 * It converts the command to lowercase before searching.
	 * @param commandValue The raw command string to look up.
	 * @return The matching {@code IBypassHandler} or {@code null} if not found.
	 */
	@Override
	public IBypassHandler getHandler(String commandValue)
	{
		String command = commandValue;
		if (command.contains(" "))
		{
			command = command.substring(0, command.indexOf(' '));
		}
		
		return _datatable.get(command.toLowerCase());
	}
	
	/**
	 * Returns the total number of registered handlers.<br>
	 * This count includes all entries in the internal map.
	 * @return The number of handlers currently stored in the {@code BypassHandler}.
	 */
	@Override
	public int size()
	{
		return _datatable.size();
	}
	
	/**
	 * Retrieves the global instance of the {@code BypassHandler}.<br>
	 * This method follows the singleton pattern.<br>
	 * Use this to access the handler from anywhere in the code.
	 * @return The single instance of {@code BypassHandler}.
	 */
	public static BypassHandler getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final BypassHandler INSTANCE = new BypassHandler();
	}
}
