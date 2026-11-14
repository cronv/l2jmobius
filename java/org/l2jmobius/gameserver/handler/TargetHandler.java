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

import org.l2jmobius.gameserver.mechanics.skill.targets.TargetType;

/**
 * This class handles the logic for processing target types within the game server.<br>
 * It manages how different {@code TargetType} values are interpreted and applied.
 * @author UnAfraid
 */
public class TargetHandler implements IHandler<ITargetTypeHandler, Enum<TargetType>>
{
	private final Map<Enum<TargetType>, ITargetTypeHandler> _datatable;
	
	/**
	 * Initializes a new instance of the {@code TargetHandler}.<br>
	 * This constructor sets up the internal data table.
	 */
	protected TargetHandler()
	{
		_datatable = new HashMap<>();
	}
	
	/**
	 * Registers a new handler for a specific target type.<br>
	 * This method adds the {@code handler} to the internal data table.<br>
	 * The mapping is based on the target type defined within the {@code ITargetTypeHandler}.
	 * @param handler The {@code ITargetTypeHandler} instance to register.
	 */
	@Override
	public void registerHandler(ITargetTypeHandler handler)
	{
		_datatable.put(handler.getTargetType(), handler);
	}
	
	/**
	 * Removes a specific handler from the internal registry.<br>
	 * This method ensures that the {@code ITargetTypeHandler} is no longer associated with its target type.<br>
	 * It uses synchronization to ensure thread safety during removal.
	 * @param handler The {@code ITargetTypeHandler} instance to be removed.
	 */
	@Override
	public synchronized void removeHandler(ITargetTypeHandler handler)
	{
		_datatable.remove(handler.getTargetType());
	}
	
	/**
	 * Retrieves the handler associated with a specific {@code TargetType}.<br>
	 * This method looks up the handler in the internal data table.
	 * @param targetType The {@code Enum<TargetType>} to look up.
	 * @return The corresponding {@code ITargetTypeHandler} or {@code null} if not found.
	 */
	@Override
	public ITargetTypeHandler getHandler(Enum<TargetType> targetType)
	{
		return _datatable.get(targetType);
	}
	
	/**
	 * Returns the total number of registered handlers.<br>
	 * This count represents the size of the internal {@code Map}.
	 * @return The number of handlers currently in the collection.
	 */
	@Override
	public int size()
	{
		return _datatable.size();
	}
	
	/**
	 * Gets the singleton instance of the {@code TargetHandler}.<br>
	 * This method provides a global access point to the handler.
	 * @return The single instance of {@code TargetHandler}.
	 */
	public static TargetHandler getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final TargetHandler INSTANCE = new TargetHandler();
	}
}
