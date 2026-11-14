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
import org.l2jmobius.gameserver.entity.actor.Creature;

/**
 * Manages the periodic task of updating visibility between creatures.<br>
 * It ensures that {@code Creature} entities correctly perceive each other in the game world.
 * @author Mobius
 */
public class CreatureSeeTaskManager implements Runnable
{
	private static final Set<Creature> CREATURES = ConcurrentHashMap.newKeySet();
	private static final AtomicBoolean WORKING = new AtomicBoolean();
	
	/**
	 * Initializes the task manager for creature visibility.<br>
	 * This constructor schedules the {@code run} method to execute every {@code 1000} milliseconds.
	 */
	protected CreatureSeeTaskManager()
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
			for (Creature creature : CREATURES)
			{
				creature.updateSeenCreatures();
			}
		}
		finally
		{
			WORKING.set(false);
		}
	}
	
	/**
	 * Adds a {@code Creature} to the task manager.<br>
	 * This allows the system to track and process the creature.
	 * @param creature The {@code Creature} instance to be added.
	 */
	public void add(Creature creature)
	{
		CREATURES.add(creature);
	}
	
	/**
	 * Removes a specific {@code Creature} from the task manager.<br>
	 * This method updates the internal set of tracked entities.
	 * @param creature The {@code Creature} object to be removed.
	 */
	public void remove(Creature creature)
	{
		CREATURES.remove(creature);
	}
	
	/**
	 * Retrieves the singleton instance of the {@code CreatureSeeTaskManager}.<br>
	 * Use this method to access the global task manager.
	 * @return The active {@code CreatureSeeTaskManager} instance.
	 */
	public static CreatureSeeTaskManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final CreatureSeeTaskManager INSTANCE = new CreatureSeeTaskManager();
	}
}
