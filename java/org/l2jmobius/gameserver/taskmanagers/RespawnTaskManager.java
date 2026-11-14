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
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.spawns.Spawn;

/**
 * Manages the periodic respawning of {@code Npc} entities at designated {@code Spawn} locations.<br>
 * This class runs as a background task to ensure that dead NPCs are replaced according to server rules.
 * @author Mobius
 */
public class RespawnTaskManager implements Runnable
{
	private static final Map<Npc, Long> PENDING_RESPAWNS = new ConcurrentHashMap<>();
	private static final AtomicBoolean WORKING = new AtomicBoolean();
	
	/**
	 * Initializes a new instance of the {@code RespawnTaskManager}.<br>
	 * This constructor schedules the task to run every {@code 1000} milliseconds.<br>
	 * It ensures that NPC respawns are processed automatically by the thread pool.
	 */
	protected RespawnTaskManager()
	{
		ThreadPool.scheduleAtFixedRate(this, 0, 1000);
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
			if (!PENDING_RESPAWNS.isEmpty())
			{
				final long currentTime = System.currentTimeMillis();
				final Iterator<Npc> iterator = PENDING_RESPAWNS.keySet().iterator();
				Npc npc;
				Long time;
				
				while (iterator.hasNext())
				{
					npc = iterator.next();
					time = PENDING_RESPAWNS.get(npc);
					if ((time != null) && (currentTime > time.longValue()))
					{
						iterator.remove();
						
						final Spawn spawn = npc.getSpawn();
						if (spawn != null)
						{
							spawn.respawnNpc(npc);
							spawn._scheduledCount--;
						}
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
	 * Adds an {@code Npc} to the pending respawn queue.<br>
	 * The task will be scheduled for a specific amount of time.
	 * @param npc The {@code Npc} instance to be added.
	 * @param time The delay in milliseconds before the respawn occurs.
	 */
	public void add(Npc npc, long time)
	{
		PENDING_RESPAWNS.put(npc, time);
	}
	
	/**
	 * Gets the singleton instance of the {@code RespawnTaskManager}.<br>
	 * Use this method to access the global manager for NPC respawns.
	 * @return The single active instance of {@code RespawnTaskManager}.
	 */
	public static RespawnTaskManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final RespawnTaskManager INSTANCE = new RespawnTaskManager();
	}
}
