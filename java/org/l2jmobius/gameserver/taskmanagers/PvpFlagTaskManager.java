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

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.entity.actor.Player;

/**
 * Manages the periodic updates for player PvP flags.<br>
 * This task ensures that {@code Player} entities have their PvP status correctly synchronized.<br>
 * It runs as a background thread to handle these checks efficiently.
 * @author Mobius
 */
public class PvpFlagTaskManager implements Runnable
{
	private static final Set<Player> PLAYERS = ConcurrentHashMap.newKeySet();
	private static final AtomicBoolean WORKING = new AtomicBoolean();
	
	/**
	 * Initializes the {@code PvpFlagTaskManager}.<br>
	 * This constructor schedules the task to run periodically.<br>
	 * It sets up the execution frequency for player flag updates.
	 */
	protected PvpFlagTaskManager()
	{
		ThreadPool.schedulePriorityTaskAtFixedRate(this, 1000, 1000);
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
			if (!PLAYERS.isEmpty())
			{
				final long currentTime = System.currentTimeMillis();
				for (Player player : PLAYERS)
				{
					final long pvpFlagLasts = player.getPvpFlagLasts();
					if (currentTime > pvpFlagLasts)
					{
						player.stopPvPFlag();
					}
					else if (currentTime > (pvpFlagLasts - 20000))
					{
						player.updatePvPFlag(2);
					}
					else
					{
						player.updatePvPFlag(1);
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
	 * Adds a {@code Player} to the active task list.<br>
	 * This method registers the player for PvP flag management.
	 * @param player The {@code Player} object to add.
	 */
	public void add(Player player)
	{
		PLAYERS.add(player);
	}
	
	/**
	 * Removes a {@code Player} from the active task manager.<br>
	 * This method updates the internal set of tracked players.
	 * @param player The {@code Player} object to be removed.
	 */
	public void remove(Player player)
	{
		PLAYERS.remove(player);
	}
	
	/**
	 * Retrieves the singleton instance of the {@code PvpFlagTaskManager}.<br>
	 * Use this method to access the global task manager.
	 * @return The active {@code PvpFlagTaskManager} instance.
	 */
	public static PvpFlagTaskManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final PvpFlagTaskManager INSTANCE = new PvpFlagTaskManager();
	}
}
