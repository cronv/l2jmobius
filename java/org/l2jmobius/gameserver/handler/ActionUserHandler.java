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
 * This class handles user-specific actions within the game server.<br>
 * It processes incoming {@code String} commands and routes them to the appropriate {@code IActionUserHandler}.
 * @author UnAfraid
 */
public class ActionUserHandler implements IHandler<IActionUserHandler, String>
{
	private final Map<String, IActionUserHandler> _actions = new HashMap<>();
	
	/**
	 * Creates a new instance of the {@code ActionUserHandler}.<br>
	 * This constructor is protected to restrict direct instantiation.<br>
	 * Use {@code getInstance} instead.
	 */
	protected ActionUserHandler()
	{
	}
	
	/**
	 * Registers a new action handler into the system.<br>
	 * The handler is mapped using its simple class name as the key.
	 * @param handler The {@code IActionUserHandler} instance to register.
	 */
	@Override
	public void registerHandler(IActionUserHandler handler)
	{
		_actions.put(handler.getClass().getSimpleName(), handler);
	}
	
	/**
	 * Removes a specific handler from the internal registry.<br>
	 * This method uses the simple name of the {@code IActionUserHandler} class as the key.<br>
	 * The operation is thread-safe because it is synchronized.
	 * @param handler The {@code IActionUserHandler} instance to be removed.
	 */
	@Override
	public synchronized void removeHandler(IActionUserHandler handler)
	{
		_actions.remove(handler.getClass().getSimpleName());
	}
	
	/**
	 * Retrieves a specific handler based on its unique name.<br>
	 * This method looks up the name in the internal registry.
	 * @param name The unique identifier for the handler to find.
	 * @return The {@code IActionUserHandler} associated with the given name, or {@code null} if not found.
	 */
	@Override
	public IActionUserHandler getHandler(String name)
	{
		return _actions.get(name);
	}
	
	/**
	 * Returns the total number of registered handlers.<br>
	 * This count includes all entries in the internal map.
	 * @return The current size of the handler collection as an {@code int}.
	 */
	@Override
	public int size()
	{
		return _actions.size();
	}
	
	/**
	 * Gets the single global instance of {@code ActionUserHandler}.<br>
	 * This method follows the singleton pattern.<br>
	 * Use this to access the handler manager from anywhere in your code.
	 * @return The shared {@code ActionUserHandler} instance.
	 */
	public static ActionUserHandler getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final ActionUserHandler INSTANCE = new ActionUserHandler();
	}
}
