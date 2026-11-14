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

import org.l2jmobius.gameserver.mechanics.skill.targets.AffectScope;

/**
 * This class handles the logic for {@code AffectScope} types.<br>
 * It processes how effects are applied to different scopes within the game world.
 * @author Nik
 */
public class AffectScopeHandler implements IHandler<IAffectScopeHandler, Enum<AffectScope>>
{
	private final Map<Enum<AffectScope>, IAffectScopeHandler> _datatable;
	
	/**
	 * Initializes the internal data table.<br>
	 * This constructor sets up the {@code _datatable} map for storing handlers.
	 */
	protected AffectScopeHandler()
	{
		_datatable = new HashMap<>();
	}
	
	/**
	 * Registers a new {@code IAffectScopeHandler} into the system.<br>
	 * This method maps the handler to its specific scope type.<br>
	 * Use this to ensure that effects are processed correctly.
	 * @param handler The {@code IAffectScopeHandler} instance to register.
	 */
	@Override
	public void registerHandler(IAffectScopeHandler handler)
	{
		_datatable.put(handler.getAffectScopeType(), handler);
	}
	
	/**
	 * Removes a specific handler from the internal registry.<br>
	 * This method ensures that the {@code IAffectScopeHandler} is no longer associated with its scope type.<br>
	 * It uses synchronization to ensure thread safety during removal.
	 * @param handler The {@code IAAffectScopeHandler} instance to be removed.
	 */
	@Override
	public synchronized void removeHandler(IAffectScopeHandler handler)
	{
		_datatable.remove(handler.getAffectScopeType());
	}
	
	/**
	 * Retrieves the handler associated with a specific {@code AffectScope}.<br>
	 * This method looks up the handler in the internal data table.
	 * @param affectScope The {@code Enum<AffectScope>} to look up.
	 * @return The corresponding {@code IAffectScopeHandler} or {@code null} if not found.
	 */
	@Override
	public IAffectScopeHandler getHandler(Enum<AffectScope> affectScope)
	{
		return _datatable.get(affectScope);
	}
	
	/**
	 * Returns the total number of registered handlers.<br>
	 * This count corresponds to the number of entries in the internal map.
	 * @return The size of the handler table as an {@code int}.
	 */
	@Override
	public int size()
	{
		return _datatable.size();
	}
	
	/**
	 * Returns the singleton instance of {@code AffectScopeHandler}.<br>
	 * Use this method to access the global handler for affect scopes.<br>
	 * This ensures that only one instance exists throughout the application.
	 * @return The single instance of {@code AffectScopeHandler}.
	 */
	public static AffectScopeHandler getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final AffectScopeHandler INSTANCE = new AffectScopeHandler();
	}
}
