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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.mechanics.buylist.Product;

/**
 * Manages the asynchronous processing of buy list updates.<br>
 * This class handles the background tasks required to update {@code Product} data.<br>
 * It ensures that buy list calculations do not block the main game thread.
 * @author Mobius
 */
public class BuyListTaskManager
{
	protected static final Map<Product, Long> PRODUCTS = new ConcurrentHashMap<>();
	protected static final List<Product> PENDING_UPDATES = new ArrayList<>();
	protected static final AtomicBoolean WORKING_PRODUCTS = new AtomicBoolean();
	protected static final AtomicBoolean WORKING_SAVES = new AtomicBoolean();
	
	/**
	 * Initializes the {@code BuyListTaskManager}.<br>
	 * This constructor starts the background tasks for product updates and saving.<br>
	 * It schedules periodic execution using the {@code ThreadPool}.
	 */
	protected BuyListTaskManager()
	{
		ThreadPool.scheduleAtFixedRate(new BuyListProductTask(), 1000, 60000);
		ThreadPool.scheduleAtFixedRate(new BuyListSaveTask(), 50, 50);
	}
	
	protected class BuyListProductTask implements Runnable
	{
		@Override
		public void run()
		{
			if (!WORKING_PRODUCTS.compareAndSet(false, true))
			{
				return;
			}
			
			try
			{
				final long currentTime = System.currentTimeMillis();
				for (Product product : PRODUCTS.keySet())
				{
					final Long time = PRODUCTS.get(product);
					if ((time != null) && (currentTime > time.longValue()))
					{
						PRODUCTS.remove(product);
						synchronized (PENDING_UPDATES)
						{
							if (!PENDING_UPDATES.contains(product))
							{
								PENDING_UPDATES.add(product);
							}
						}
					}
				}
			}
			finally
			{
				WORKING_PRODUCTS.set(false);
			}
		}
	}
	
	protected class BuyListSaveTask implements Runnable
	{
		@Override
		public void run()
		{
			if (!WORKING_SAVES.compareAndSet(false, true))
			{
				return;
			}
			
			try
			{
				if (!PENDING_UPDATES.isEmpty())
				{
					final Product product;
					synchronized (PENDING_UPDATES)
					{
						product = PENDING_UPDATES.get(0);
						PENDING_UPDATES.remove(product);
					}
					
					product.restock();
				}
			}
			finally
			{
				WORKING_SAVES.set(false);
			}
		}
	}
	
	/**
	 * Adds a new {@code Product} to the active list.<br>
	 * This method sets the expiration time for the product.<br>
	 * It only adds the product if it is not already present.
	 * @param product The {@code Product} object to add.
	 * @param endTime The timestamp when the product expires.
	 */
	public void add(Product product, long endTime)
	{
		PRODUCTS.putIfAbsent(product, endTime);
	}
	
	/**
	 * Updates the end time for a specific {@code Product}.<br>
	 * This method saves the new timestamp into the internal map.
	 * @param product The {@code Product} to be updated.
	 * @param endTime The new expiration time as a {@code long}.
	 */
	public void update(Product product, long endTime)
	{
		PRODUCTS.put(product, endTime);
	}
	
	/**
	 * Retrieves the time remaining until a product is restocked.<br>
	 * It looks up the delay in the internal products map.<br>
	 * If the product is not found, it returns {@code 0L}.
	 * @param product The {@code Product} to check for its restock delay.
	 * @return The remaining time in milliseconds as a {@code long}.
	 */
	public long getRestockDelay(Product product)
	{
		return PRODUCTS.getOrDefault(product, 0L);
	}
	
	/**
	 * Retrieves the single global instance of {@code BuyListTaskManager}.<br>
	 * This method follows the singleton pattern.
	 * @return The active {@code BuyListTaskManager} instance.
	 */
	public static BuyListTaskManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final BuyListTaskManager INSTANCE = new BuyListTaskManager();
	}
}
