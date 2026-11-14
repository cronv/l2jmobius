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
 * Manages the expiration of items based on their remaining lifetime.<br>
 * This task periodically checks for and removes expired {@code Item} instances from the world.
 * @author Mobius
 */
public class ItemLifeTimeTaskManager implements Runnable
{
	private static final Map<Item, Long> ITEMS = new ConcurrentHashMap<>();
	private static final AtomicBoolean WORKING = new AtomicBoolean();
	
	/**
	 * Initializes the task manager.<br>
	 * This constructor schedules the {@code run} method to execute every {@code 1000} milliseconds.<br>
	 * It sets up the background thread for managing item lifetimes.
	 */
	protected ItemLifeTimeTaskManager()
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
						item.endOfLife();
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
	 * Adds an {@code Item} to the tracking system.<br>
	 * It associates the item with a specific end time.<br>
	 * The item is only added if it is not already present.
	 * @param item The {@code Item} instance to track.
	 * @param endTime The timestamp when the item should expire.
	 */
	public void add(Item item, long endTime)
	{
		ITEMS.putIfAbsent(item, endTime);
	}
	
	/**
	 * Removes a specific {@code Item} from the active tracking list.<br>
	 * This method updates the internal map to stop monitoring the item's lifetime.
	 * @param item The {@code Item} instance to be removed.
	 */
	public void remove(Item item)
	{
		ITEMS.remove(item);
	}
	
	/**
	 * Retrieves the singleton instance of the {@code ItemLifeTimeTaskManager}.<br>
	 * Use this method to access the manager from anywhere in your code.
	 * @return The active {@code ItemLifeTimeTaskManager} instance.
	 */
	public static ItemLifeTimeTaskManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final ItemLifeTimeTaskManager INSTANCE = new ItemLifeTimeTaskManager();
	}
}
