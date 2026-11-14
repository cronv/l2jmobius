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
package org.l2jmobius.gameserver.taskmanagers;

import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.entity.item.enums.ItemLocation;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.managers.ItemsOnGroundManager;

/**
 * Manages the periodic removal of items left on the ground.<br>
 * This task identifies and deletes {@code Item} instances that exceed their allowed lifespan.<br>
 * It helps maintain server performance by cleaning up unnecessary objects in the world.
 */
public class ItemsAutoDestroyTaskManager implements Runnable
{
	private static final Set<Item> ITEMS = ConcurrentHashMap.newKeySet();
	
	/**
	 * Initializes the task manager for automatically destroying items.<br>
	 * This constructor schedules a recurring task every {@code 5000} milliseconds.<br>
	 * It ensures that old items are cleaned up from the world periodically.
	 */
	protected ItemsAutoDestroyTaskManager()
	{
		ThreadPool.scheduleAtFixedRate(this, 5000, 5000);
	}
	
	@Override
	public void run()
	{
		if (ITEMS.isEmpty())
		{
			return;
		}
		
		final long currentTime = System.currentTimeMillis();
		final Iterator<Item> iterator = ITEMS.iterator();
		Item itemInstance;
		
		while (iterator.hasNext())
		{
			itemInstance = iterator.next();
			final long dropTime = itemInstance.getDropTime();
			if ((dropTime == 0) || (itemInstance.getItemLocation() != ItemLocation.VOID))
			{
				iterator.remove();
			}
			else
			{
				final long autoDestroyTime;
				if (itemInstance.getTemplate().getAutoDestroyTime() > 0)
				{
					autoDestroyTime = itemInstance.getTemplate().getAutoDestroyTime();
				}
				else if (itemInstance.getTemplate().hasExImmediateEffect())
				{
					autoDestroyTime = GeneralConfig.HERB_AUTO_DESTROY_TIME;
				}
				else
				{
					autoDestroyTime = ((GeneralConfig.AUTODESTROY_ITEM_AFTER == 0) ? 3600000 : GeneralConfig.AUTODESTROY_ITEM_AFTER * 1000);
				}
				
				if ((currentTime - dropTime) > autoDestroyTime)
				{
					itemInstance.decayMe();
					if (GeneralConfig.SAVE_DROPPED_ITEM)
					{
						ItemsOnGroundManager.getInstance().removeObject(itemInstance);
					}
					
					iterator.remove();
				}
			}
		}
	}
	
	/**
	 * Adds a new {@code Item} to the auto-destroy tracking list.<br>
	 * This method updates the drop time of the item before adding it.
	 * @param item The {@code Item} instance to be tracked.
	 */
	public void addItem(Item item)
	{
		item.setDropTime(System.currentTimeMillis());
		ITEMS.add(item);
	}
	
	/**
	 * Retrieves the singleton instance of the {@code ItemsAutoDestroyTaskManager}.<br>
	 * This method provides a global access point to the task manager.
	 * @return The active instance of {@code ItemsAutoDestroyTaskManager}.
	 */
	public static ItemsAutoDestroyTaskManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final ItemsAutoDestroyTaskManager INSTANCE = new ItemsAutoDestroyTaskManager();
	}
}
