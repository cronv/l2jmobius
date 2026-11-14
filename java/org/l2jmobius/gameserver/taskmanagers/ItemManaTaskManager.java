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
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.instance.Item;

/**
 * Manages the periodic task of updating mana for items held by players.<br>
 * It iterates through active items to ensure their mana values are correctly processed.
 * @author Mobius
 */
public class ItemManaTaskManager implements Runnable
{
	private static final Map<Item, Long> ITEMS = new ConcurrentHashMap<>();
	private static final int MANA_CONSUMPTION_RATE = 60000;
	private static final AtomicBoolean WORKING = new AtomicBoolean();
	
	/**
	 * Initializes a new instance of the {@code ItemManaTaskManager}.<br>
	 * This constructor schedules the task to run at a fixed rate.<br>
	 * It sets up the background thread for managing item mana.
	 */
	protected ItemManaTaskManager()
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
						iterator.remove();
						
						final Player player = item.asPlayer();
						if ((player == null) || player.getOfflineMode().isEnabled())
						{
							continue;
						}
						
						item.decreaseMana(item.isEquipped());
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
	 * Adds an {@code Item} to the mana consumption tracking system.<br>
	 * This method registers the item and sets its next consumption time.
	 * @param item The {@code Item} instance to be added.
	 */
	public void add(Item item)
	{
		ITEMS.computeIfAbsent(item, k -> System.currentTimeMillis() + MANA_CONSUMPTION_RATE);
	}
	
	/**
	 * Retrieves the singleton instance of the {@code ItemManaTaskManager}.<br>
	 * Use this method to access the global manager for item mana tasks.
	 * @return The active {@code ItemManaTaskManager} instance.
	 */
	public static ItemManaTaskManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final ItemManaTaskManager INSTANCE = new ItemManaTaskManager();
	}
}
