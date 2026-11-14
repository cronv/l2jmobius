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

import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;

/**
 * Handles the logic for shifting actions between different instances.<br>
 * It processes {@code IActionShiftHandler} requests based on the provided {@code InstanceType}.
 * @author UnAfraid
 */
public class ActionShiftHandler implements IHandler<IActionShiftHandler, InstanceType>
{
	private final Map<InstanceType, IActionShiftHandler> _actionsShift;
	
	/**
	 * Initializes the {@code _actionsShift} map.<br>
	 * This constructor sets up the internal storage for action handlers.
	 */
	protected ActionShiftHandler()
	{
		_actionsShift = new EnumMap<>(InstanceType.class);
	}
	
	/**
	 * Registers a new {@code IActionShiftHandler} into the system.<br>
	 * This method maps the handler to its specific {@code InstanceType}.<br>
	 * Use this to ensure actions are handled correctly for different instances.
	 * @param handler The {@code IActionShiftHandler} instance to register.
	 */
	@Override
	public void registerHandler(IActionShiftHandler handler)
	{
		_actionsShift.put(handler.getInstanceType(), handler);
	}
	
	/**
	 * Removes a specific handler from the internal registry.<br>
	 * This method updates the map associated with {@code InstanceType}.
	 * @param handler The {@code IActionShiftHandler} to be removed.
	 */
	@Override
	public synchronized void removeHandler(IActionShiftHandler handler)
	{
		_actionsShift.remove(handler.getInstanceType());
	}
	
	/**
	 * Retrieves the handler associated with a specific {@code InstanceType}.<br>
	 * This method searches through the instance type hierarchy.<br>
	 * It returns the first matching handler it finds or {@code null} if none exist.
	 * @param iType The {@code InstanceType} to look up.
	 * @return The corresponding {@code IActionShiftHandler} or {@code null}.
	 */
	@Override
	public IActionShiftHandler getHandler(InstanceType iType)
	{
		IActionShiftHandler result = null;
		for (InstanceType t = iType; t != null; t = t.getParent())
		{
			result = _actionsShift.get(t);
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
		return _actionsShift.size();
	}
	
	/**
	 * Returns the global instance of {@code ActionShiftHandler}.<br>
	 * This method provides access to the singleton object.<br>
	 * Use this instead of creating a new instance.
	 * @return The singleton instance of {@code ActionShiftHandler}
	 */
	public static ActionShiftHandler getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final ActionShiftHandler INSTANCE = new ActionShiftHandler();
	}
}
