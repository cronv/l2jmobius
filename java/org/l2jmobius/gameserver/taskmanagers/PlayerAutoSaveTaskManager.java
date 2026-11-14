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
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.entity.actor.Player;

/**
 * Manages the periodic saving of player data to the database.<br>
 * This task iterates through all active {@code Player} objects and triggers their save process.<br>
 * It ensures that character progress is preserved automatically at regular intervals.
 * @author Mobius
 */
public class PlayerAutoSaveTaskManager implements Runnable
{
	private static final Map<Player, Long> PLAYER_TIMES = new ConcurrentHashMap<>();
	private static final AtomicBoolean WORKING = new AtomicBoolean();
	
	/**
	 * Initializes the task manager for automatic player saves.<br>
	 * This constructor schedules a recurring task to run every {@code 1000} milliseconds.<br>
	 * It ensures that player data is saved periodically in the background.
	 */
	protected PlayerAutoSaveTaskManager()
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
			if (!PLAYER_TIMES.isEmpty())
			{
				final long currentTime = System.currentTimeMillis();
				final Iterator<Player> iterator = PLAYER_TIMES.keySet().iterator();
				Player player;
				Long time;
				
				while (iterator.hasNext())
				{
					player = iterator.next();
					time = PLAYER_TIMES.get(player);
					
					if ((time != null) && (currentTime > time.longValue()))
					{
						if (player.isOnline())
						{
							player.autoSave();
							PLAYER_TIMES.put(player, currentTime + GeneralConfig.CHAR_DATA_STORE_INTERVAL);
							break; // Prevent SQL flood.
						}
						
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
	 * Adds a {@code Player} to the auto-save task manager.<br>
	 * This method schedules the next save time for the given player.<br>
	 * It uses the interval defined in {@code GeneralConfig}.
	 * @param player The {@code Player} to be added to the manager.
	 */
	public void add(Player player)
	{
		PLAYER_TIMES.put(player, System.currentTimeMillis() + GeneralConfig.CHAR_DATA_STORE_INTERVAL);
	}
	
	/**
	 * Removes a {@code Player} from the auto-save task manager.<br>
	 * This stops the system from tracking save times for the specified player.
	 * @param player The {@code Player} to be removed.
	 */
	public void remove(Player player)
	{
		PLAYER_TIMES.remove(player);
	}
	
	/**
	 * Gets the singleton instance of the {@code PlayerAutoSaveTaskManager}.<br>
	 * Use this method to access the global manager for player auto-saves.
	 * @return The single instance of {@code PlayerAutoSaveTaskManager}.
	 */
	public static PlayerAutoSaveTaskManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final PlayerAutoSaveTaskManager INSTANCE = new PlayerAutoSaveTaskManager();
	}
}
