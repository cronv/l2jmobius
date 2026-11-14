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

import org.l2jmobius.gameserver.mechanics.punishment.PunishmentType;

/**
 * This class manages the various handlers for different types of punishments.<br>
 * It maps each {@code PunishmentType} to its corresponding {@code IPunishmentHandler}.
 * @author UnAfraid
 */
public class PunishmentHandler implements IHandler<IPunishmentHandler, PunishmentType>
{
	private final Map<PunishmentType, IPunishmentHandler> _handlers = new EnumMap<>(PunishmentType.class);
	
	/**
	 * Creates a new instance of the {@code PunishmentHandler}.<br>
	 * This constructor is protected to restrict direct instantiation.<br>
	 * Use {@code getInstance} instead.
	 */
	protected PunishmentHandler()
	{
	}
	
	/**
	 * Registers a new punishment handler into the system.<br>
	 * This method maps the handler to its specific {@code PunishmentType}.
	 * @param handler The {@code IPunishmentHandler} instance to register.
	 */
	@Override
	public void registerHandler(IPunishmentHandler handler)
	{
		_handlers.put(handler.getType(), handler);
	}
	
	/**
	 * Removes a specific handler from the internal registry.<br>
	 * This method ensures that the {@code IPunishmentHandler} is no longer used.<br>
	 * It uses synchronization to ensure thread safety during removal.
	 * @param handler The {@code IPunishmentHandler} instance to be removed.
	 */
	@Override
	public synchronized void removeHandler(IPunishmentHandler handler)
	{
		_handlers.remove(handler.getType());
	}
	
	/**
	 * Retrieves the handler associated with a specific punishment type.<br>
	 * This method looks up the {@code IPunishmentHandler} in the internal map.
	 * @param val The {@code PunishmentType} to look up.
	 * @return The corresponding {@code IPunishmentHandler} or {@code null} if no handler is registered.
	 */
	@Override
	public IPunishmentHandler getHandler(PunishmentType val)
	{
		return _handlers.get(val);
	}
	
	/**
	 * Returns the total number of registered handlers.<br>
	 * This count corresponds to the size of the internal {@code Map}.
	 * @return The number of active handlers.
	 */
	@Override
	public int size()
	{
		return _handlers.size();
	}
	
	/**
	 * Returns the single global instance of {@code PunishmentHandler}.<br>
	 * This method follows the singleton pattern.<br>
	 * Use this to access punishment management features throughout the server.
	 * @return The shared {@code PunishmentHandler} instance.
	 */
	public static PunishmentHandler getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final PunishmentHandler INSTANCE = new PunishmentHandler();
	}
}
