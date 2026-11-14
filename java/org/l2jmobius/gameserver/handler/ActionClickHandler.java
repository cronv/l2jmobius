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

import java.util.EnumMap;
import java.util.Map;

import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;

/**
 * This class handles the processing of action clicks within the game world.<br>
 * It manages how interactions are executed based on the {@code InstanceType}.
 */
public class ActionClickHandler implements IHandler<IActionClickHandler, InstanceType>
{
	private final Map<InstanceType, IActionClickHandler> _actions;
	
	/**
	 * Initializes the internal action handler map.<br>
	 * This constructor sets up the {@code _actions} map using {@code InstanceType}.
	 */
	protected ActionClickHandler()
	{
		_actions = new EnumMap<>(InstanceType.class);
	}
	
	/**
	 * Registers a new action click handler.<br>
	 * This method adds the provided {@code IActionClickHandler} to the internal map.<br>
	 * The handler is mapped based on its specific instance type.
	 * @param handler The {@code IActionClickHandler} to register.
	 */
	@Override
	public void registerHandler(IActionClickHandler handler)
	{
		_actions.put(handler.getInstanceType(), handler);
	}
	
	/**
	 * Removes a specific handler from the internal action map.<br>
	 * This method is thread-safe because it is synchronized.<br>
	 * It uses the {@code InstanceType} of the provided handler to identify which entry to remove.
	 * @param handler The {@code IActionClickHandler} instance to be removed.
	 */
	@Override
	public synchronized void removeHandler(IActionClickHandler handler)
	{
		_actions.remove(handler.getInstanceType());
	}
	
	/**
	 * Retrieves the action handler for a specific instance type.<br>
	 * This method searches through the hierarchy of the provided {@code InstanceType}.<br>
	 * It returns the first matching handler it finds starting from the given type.
	 * @param iType The {@code InstanceType} to look up.
	 * @return The corresponding {@code IActionClickHandler} or {@code null} if no match is found.
	 */
	@Override
	public IActionClickHandler getHandler(InstanceType iType)
	{
		IActionClickHandler result = null;
		for (InstanceType t = iType; t != null; t = t.getParent())
		{
			result = _actions.get(t);
			if (result != null)
			{
				break;
			}
		}
		
		return result;
	}
	
	/**
	 * Returns the total number of registered handlers.<br>
	 * This count includes all entries in the internal map.
	 * @return The number of active handlers.
	 */
	@Override
	public int size()
	{
		return _actions.size();
	}
	
	/**
	 * Returns the singleton instance of {@code ActionClickHandler}.<br>
	 * Use this method to access the global handler manager.
	 * @return The single instance of {@code ActionClickHandler}.
	 */
	public static ActionClickHandler getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final ActionClickHandler INSTANCE = new ActionClickHandler();
	}
}
