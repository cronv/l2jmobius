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
 * Handles incoming voice-related commands from the client.<br>
 * This class processes {@code String} messages and routes them to the appropriate logic.<br>
 * It serves as a primary entry point for managing voiced interactions in the game.
 */
public class VoicedCommandHandler implements IHandler<IVoicedCommandHandler, String>
{
	private final Map<String, IVoicedCommandHandler> _datatable;
	
	/**
	 * Initializes a new instance of the {@code VoicedCommandHandler}.<br>
	 * This constructor sets up the internal data table for commands.
	 */
	protected VoicedCommandHandler()
	{
		_datatable = new HashMap<>();
	}
	
	/**
	 * Registers a new command handler into the system.<br>
	 * This method adds all commands from the provided {@code IVoicedCommandHandler} to the internal table.
	 * @param handler The {@code IVoicedCommandHandler} instance to register.
	 */
	@Override
	public void registerHandler(IVoicedCommandHandler handler)
	{
		for (String id : handler.getCommandList())
		{
			_datatable.put(id, handler);
		}
	}
	
	/**
	 * Removes a specific {@code IVoicedCommandHandler} from the internal registry.<br>
	 * This method clears all command IDs associated with the provided handler.<br>
	 * The operation is thread-safe because it uses the {@code synchronized} keyword.
	 * @param handler The {@code IVoicedCommandHandler} instance to be removed.
	 */
	@Override
	public synchronized void removeHandler(IVoicedCommandHandler handler)
	{
		for (String id : handler.getCommandList())
		{
			_datatable.remove(id);
		}
	}
	
	/**
	 * Retrieves the handler associated with a specific voice command.<br>
	 * It extracts the first word if the input contains spaces.
	 * @param voicedCommand The raw string input from the user.
	 * @return The {@code IVoicedCommandHandler} for the command, or {@code null} if not found.
	 */
	@Override
	public IVoicedCommandHandler getHandler(String voicedCommand)
	{
		return _datatable.get(voicedCommand.contains(" ") ? voicedCommand.substring(0, voicedCommand.indexOf(' ')) : voicedCommand);
	}
	
	/**
	 * Returns the total number of registered handlers.<br>
	 * This count represents the current size of the internal data table.
	 * @return The number of handlers currently in the map.
	 */
	@Override
	public int size()
	{
		return _datatable.size();
	}
	
	/**
	 * Provides the global instance of the {@code VoicedCommandHandler}.<br>
	 * This method follows the singleton pattern.<br>
	 * Use this to access the handler registry from anywhere in the code.
	 * @return The single instance of {@code VoicedCommandHandler}.
	 */
	public static VoicedCommandHandler getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final VoicedCommandHandler INSTANCE = new VoicedCommandHandler();
	}
}
