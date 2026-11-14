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
package org.l2jmobius.gameserver.handler;

import java.util.HashMap;
import java.util.Map;

/**
 * This class handles incoming user commands from the game client.<br>
 * It processes specific requests and routes them to the appropriate logic.<br>
 * It implements {@code IHandler} to manage communication between users and the server.
 */
public class UserCommandHandler implements IHandler<IUserCommandHandler, Integer>
{
	private final Map<Integer, IUserCommandHandler> _datatable;
	
	/**
	 * Initializes a new instance of the {@code UserCommandHandler}.<br>
	 * This constructor sets up the internal data table.
	 */
	protected UserCommandHandler()
	{
		_datatable = new HashMap<>();
	}
	
	/**
	 * Registers a new command handler into the system.<br>
	 * This method adds all commands from the provided {@code IUserCommandHandler} to the internal table.
	 * @param handler The {@code IUserCommandHandler} instance to register.
	 */
	@Override
	public void registerHandler(IUserCommandHandler handler)
	{
		for (int id : handler.getCommandList())
		{
			_datatable.put(id, handler);
		}
	}
	
	/**
	 * Removes all command mappings associated with a specific {@code IUserCommandHandler}.<br>
	 * This method updates the internal data table by clearing entries for each command ID.
	 * @param handler The {@code IUserCommandHandler} instance to remove from the system.
	 */
	@Override
	public synchronized void removeHandler(IUserCommandHandler handler)
	{
		for (int id : handler.getCommandList())
		{
			_datatable.remove(id);
		}
	}
	
	/**
	 * Retrieves the handler associated with a specific command.<br>
	 * This method looks up the command ID in the internal data table.
	 * @param userCommand The unique integer ID of the user command.
	 * @return The {@code IUserCommandHandler} for the given command, or {@code null} if not found.
	 */
	@Override
	public IUserCommandHandler getHandler(Integer userCommand)
	{
		return _datatable.get(userCommand);
	}
	
	/**
	 * Returns the total number of registered handlers.<br>
	 * This count includes all entries in the internal map.
	 * @return The number of handlers currently stored.
	 */
	@Override
	public int size()
	{
		return _datatable.size();
	}
	
	/**
	 * Provides the global instance of the {@code UserCommandHandler}.<br>
	 * This method follows the singleton pattern.<br>
	 * Use this to access the handler manager from anywhere in your code.
	 * @return The single shared instance of {@code UserCommandHandler}.
	 */
	public static UserCommandHandler getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final UserCommandHandler INSTANCE = new UserCommandHandler();
	}
}
