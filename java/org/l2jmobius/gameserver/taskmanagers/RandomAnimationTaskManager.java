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
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.entity.actor.Npc;

/**
 * Manages the execution of random animations for {@code Npc} entities.<br>
 * This task runs periodically to trigger visual effects based on server configurations.
 * @author Mobius
 */
public class RandomAnimationTaskManager implements Runnable
{
	private static final Set<Npc> PENDING_ANIMATIONS = ConcurrentHashMap.newKeySet();
	private static final AtomicBoolean WORKING = new AtomicBoolean();
	
	/**
	 * Initializes a new instance of the {@code RandomAnimationTaskManager}.<br>
	 * This constructor schedules the task to run every {@code 1000} milliseconds.<br>
	 * It sets up the background thread for processing NPC animations.
	 */
	protected RandomAnimationTaskManager()
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
			final long currentTime = System.currentTimeMillis();
			for (Npc npc : PENDING_ANIMATIONS)
			{
				if (currentTime > npc.getNextRandomAnimationTime())
				{
					if (npc.isInActiveRegion() && !npc.isDead() && !npc.isInCombat() && !npc.isMoving() && !npc.hasBlockActions())
					{
						npc.onRandomAnimation(Rnd.get(2, 3));
					}
					
					final boolean isAttackable = npc.isAttackable();
					npc.setNextRandomAnimationTime(currentTime + (Rnd.get((isAttackable ? GeneralConfig.MIN_MONSTER_ANIMATION : GeneralConfig.MIN_NPC_ANIMATION), (isAttackable ? GeneralConfig.MAX_MONSTER_ANIMATION : GeneralConfig.MAX_NPC_ANIMATION)) * 1000));
				}
			}
		}
		finally
		{
			WORKING.set(false);
		}
	}
	
	/**
	 * Adds an {@code Npc} to the queue for random animations.<br>
	 * This method checks if the {@code npc} is eligible for animations.<br>
	 * It schedules the next animation based on configuration settings.
	 * @param npc The {@code Npc} instance to be added to the task manager.
	 */
	public void add(Npc npc)
	{
		if (npc.hasRandomAnimation() && !PENDING_ANIMATIONS.contains(npc))
		{
			final boolean attackable = npc.isAttackable();
			npc.setNextRandomAnimationTime(System.currentTimeMillis() + (Rnd.get((attackable ? GeneralConfig.MIN_MONSTER_ANIMATION : GeneralConfig.MIN_NPC_ANIMATION), (attackable ? GeneralConfig.MAX_MONSTER_ANIMATION : GeneralConfig.MAX_NPC_ANIMATION)) * 1000));
			PENDING_ANIMATIONS.add(npc);
		}
	}
	
	/**
	 * Removes a specific {@code Npc} from the pending animations list.<br>
	 * This stops the task manager from processing animations for this entity.
	 * @param npc The {@code Npc} object to remove.
	 */
	public void remove(Npc npc)
	{
		PENDING_ANIMATIONS.remove(npc);
	}
	
	/**
	 * Retrieves the global instance of the {@code RandomAnimationTaskManager}.<br>
	 * This method follows the singleton pattern.<br>
	 * Use this to access the manager from anywhere in your code.
	 * @return The single instance of {@code RandomAnimationTaskManager}.
	 */
	public static RandomAnimationTaskManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final RandomAnimationTaskManager INSTANCE = new RandomAnimationTaskManager();
	}
}
