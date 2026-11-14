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

import org.l2jmobius.gameserver.mechanics.skill.targets.AffectObject;

/**
 * This class handles the processing of {@code AffectObject} effects within the game server.<br>
 * It manages how specific status effects are applied to and interact with objects.
 * @author Nik
 */
public class AffectObjectHandler implements IHandler<IAffectObjectHandler, Enum<AffectObject>>
{
	private final Map<Enum<AffectObject>, IAffectObjectHandler> _datatable;
	
	/**
	 * Initializes the internal data table.<br>
	 * This constructor sets up the {@code _datatable} map.<br>
	 * It is used to store handlers for different {@code AffectObject} types.
	 */
	protected AffectObjectHandler()
	{
		_datatable = new HashMap<>();
	}
	
	/**
	 * Registers a new {@code IAffectObjectHandler} into the system.<br>
	 * This method maps the handler to its specific affect object type.<br>
	 * Use this to ensure that effects are processed correctly.
	 * @param handler The {@code IAffectObjectHandler} instance to register.
	 */
	@Override
	public void registerHandler(IAffectObjectHandler handler)
	{
		_datatable.put(handler.getAffectObjectType(), handler);
	}
	
	/**
	 * Removes a specific handler from the internal registry.<br>
	 * This method ensures that the {@code IAffectObjectHandler} is no longer associated with its target type.<br>
	 * It uses synchronization to ensure thread safety during removal.
	 * @param handler The {@code IAffectObjectHandler} instance to be removed.
	 */
	@Override
	public synchronized void removeHandler(IAffectObjectHandler handler)
	{
		_datatable.remove(handler.getAffectObjectType());
	}
	
	/**
	 * Retrieves the handler associated with a specific {@code AffectObject} type.<br>
	 * This method looks up the handler in the internal data table.
	 * @param targetType The {@code Enum<AffectObject>} to search for.
	 * @return The corresponding {@code IAffectObjectHandler} or {@code null} if not found.
	 */
	@Override
	public IAffectObjectHandler getHandler(Enum<AffectObject> targetType)
	{
		return _datatable.get(targetType);
	}
	
	/**
	 * Returns the total number of registered handlers.<br>
	 * This count corresponds to the entries in the internal map.
	 * @return The size of the handler table as an {@code int}.
	 */
	@Override
	public int size()
	{
		return _datatable.size();
	}
	
	/**
	 * Gets the singleton instance of the {@code AffectObjectHandler}.<br>
	 * Use this method to access the global handler manager.
	 * @return The single instance of {@code AffectObjectHandler}.
	 */
	public static AffectObjectHandler getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final AffectObjectHandler INSTANCE = new AffectObjectHandler();
	}
}
