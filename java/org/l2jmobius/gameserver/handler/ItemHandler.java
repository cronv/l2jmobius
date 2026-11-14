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

import org.l2jmobius.gameserver.entity.item.EtcItem;

/**
 * This class manages the registration and execution of item-related handlers.<br>
 * It maps {@code EtcItem} objects to their corresponding {@code IItemHandler} implementations.
 * @author UnAfraid
 */
public class ItemHandler implements IHandler<IItemHandler, EtcItem>
{
	private final Map<String, IItemHandler> _datatable;
	
	/**
	 * Initializes a new instance of the {@code ItemHandler} class.<br>
	 * This constructor sets up the internal data table for item handlers.
	 */
	protected ItemHandler()
	{
		_datatable = new HashMap<>();
	}
	
	/**
	 * Registers a new {@code IItemHandler} into the system.<br>
	 * This method maps the handler to its class name in the internal table.<br>
	 * Use this to ensure specific items are processed by their correct logic.
	 * @param handler The {@code IItemHandler} instance to register.
	 */
	@Override
	public void registerHandler(IItemHandler handler)
	{
		_datatable.put(handler.getClass().getSimpleName(), handler);
	}
	
	/**
	 * Removes a specific handler from the internal registry.<br>
	 * This method uses the simple name of the {@code IItemHandler} class as the key.<br>
	 * The operation is thread-safe because it is synchronized.
	 * @param handler The {@code IItemHandler} instance to be removed.
	 */
	@Override
	public synchronized void removeHandler(IItemHandler handler)
	{
		_datatable.remove(handler.getClass().getSimpleName());
	}
	
	/**
	 * Retrieves the specific handler for a given {@code EtcItem}.<br>
	 * It looks up the handler using the item's unique name.<br>
	 * Returns {@code null} if the item or its name is missing.
	 * @param item The {@code EtcItem} to look up.
	 * @return The corresponding {@code IItemHandler} or {@code null}.
	 */
	@Override
	public IItemHandler getHandler(EtcItem item)
	{
		if ((item == null) || (item.getHandlerName() == null))
		{
			return null;
		}
		
		return _datatable.get(item.getHandlerName());
	}
	
	/**
	 * Returns the total number of registered handlers.<br>
	 * This count corresponds to the size of the internal {@code Map}.
	 * @return The number of items in the handler table.
	 */
	@Override
	public int size()
	{
		return _datatable.size();
	}
	
	/**
	 * Returns the global instance of the {@code ItemHandler}.<br>
	 * This method follows the singleton pattern.<br>
	 * Use this to access the main handler manager from anywhere in the code.
	 * @return The single instance of {@code ItemHandler}.
	 */
	public static ItemHandler getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final ItemHandler INSTANCE = new ItemHandler();
	}
}
