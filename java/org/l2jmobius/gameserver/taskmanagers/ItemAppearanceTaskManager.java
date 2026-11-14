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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.entity.item.instance.Item;

/**
 * Manages the periodic updates for item appearances in the game world.<br>
 * This task ensures that visual properties of {@code Item} instances are refreshed correctly.
 * @author Mobius
 */
public class ItemAppearanceTaskManager implements Runnable
{
	private static final Map<Item, Long> ITEMS = new ConcurrentHashMap<>();
	private static final AtomicBoolean WORKING = new AtomicBoolean();
	
	/**
	 * Initializes the task manager.<br>
	 * This constructor schedules the {@code run} method to execute every 1000 milliseconds.
	 */
	protected ItemAppearanceTaskManager()
	{
		ThreadPool.scheduleAtFixedRate(this, 1000, 1000);
	}
	
	@Override
	public void run()
	{
		if (!WORKING.compareAndSet(false, true))
		{
			return;
		}
		
		try
		{
			if (!ITEMS.isEmpty())
			{
				final long currentTime = System.currentTimeMillis();
				final Iterator<Item> iterator = ITEMS.keySet().iterator();
				Item item;
				Long time;
				
				while (iterator.hasNext())
				{
					item = iterator.next();
					time = ITEMS.get(item);
					if ((time != null) && (currentTime > time.longValue()))
					{
						item.onVisualLifeTimeEnd();
						iterator.remove();
					}
				}
			}
		}
		finally
		{
			WORKING.set(false);
		}
	}
	
	/**
	 * Adds an {@code Item} to the task manager.<br>
	 * The item will be tracked until the specified {@code endTime}.<br>
	 * If the item is already present, no changes are made.
	 * @param item The {@code Item} instance to add.
	 * @param endTime The timestamp when the task should end.
	 */
	public void add(Item item, long endTime)
	{
		ITEMS.putIfAbsent(item, endTime);
	}
	
	/**
	 * Removes a specific {@code Item} from the active task list.<br>
	 * This method updates the internal collection to stop tracking the item.
	 * @param item The {@code Item} object to be removed.
	 */
	public void remove(Item item)
	{
		ITEMS.remove(item);
	}
	
	/**
	 * Retrieves the singleton instance of the {@code ItemAppearanceTaskManager}.<br>
	 * Use this method to access the manager from anywhere in your code.<br>
	 * This ensures that only one manager exists throughout the application life cycle.
	 * @return The global instance of {@code ItemAppearanceTaskManager}.
	 */
	public static ItemAppearanceTaskManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final ItemAppearanceTaskManager INSTANCE = new ItemAppearanceTaskManager();
	}
}
