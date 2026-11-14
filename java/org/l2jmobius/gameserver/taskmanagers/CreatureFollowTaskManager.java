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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.ai.CreatureAI;
import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;

/**
 * Manages the background tasks for creatures that are currently following a target.<br>
 * It handles periodic updates to ensure smooth movement and behavior logic.<br>
 * This class helps optimize performance by offloading follow calculations from the main thread.
 * @author Mobius
 */
public class CreatureFollowTaskManager
{
	protected static final Map<Creature, Integer> NORMAL_FOLLOW_CREATURES = new ConcurrentHashMap<>();
	protected static final Map<Creature, Integer> ATTACK_FOLLOW_CREATURES = new ConcurrentHashMap<>();
	protected static final AtomicBoolean WORKING_NORMAL = new AtomicBoolean();
	protected static final AtomicBoolean WORKING_ATTACK = new AtomicBoolean();
	
	/**
	 * Initializes the task manager for creature following behavior.<br>
	 * This constructor schedules periodic tasks to handle normal and attack follows.<br>
	 * It sets up the background threads required for these AI behaviors.
	 */
	protected CreatureFollowTaskManager()
	{
		ThreadPool.schedulePriorityTaskAtFixedRate(new CreatureFollowNormalTask(), 1000, 1000);
		ThreadPool.schedulePriorityTaskAtFixedRate(new CreatureFollowAttackTask(), 500, 500);
	}
	
	protected class CreatureFollowNormalTask implements Runnable
	{
		@Override
		public void run()
		{
			if (!WORKING_NORMAL.compareAndSet(false, true))
			{
				return;
			}
			
			try
			{
				if (!NORMAL_FOLLOW_CREATURES.isEmpty())
				{
					for (Creature creature : NORMAL_FOLLOW_CREATURES.keySet())
					{
						final Integer range = NORMAL_FOLLOW_CREATURES.get(creature);
						if (range != null)
						{
							follow(creature, range.intValue());
						}
					}
				}
			}
			finally
			{
				WORKING_NORMAL.set(false);
			}
		}
	}
	
	protected class CreatureFollowAttackTask implements Runnable
	{
		@Override
		public void run()
		{
			if (!WORKING_ATTACK.compareAndSet(false, true))
			{
				return;
			}
			
			try
			{
				if (!ATTACK_FOLLOW_CREATURES.isEmpty())
				{
					for (Creature creature : ATTACK_FOLLOW_CREATURES.keySet())
					{
						final Integer range = ATTACK_FOLLOW_CREATURES.get(creature);
						if (range != null)
						{
							follow(creature, range.intValue());
						}
					}
				}
			}
			finally
			{
				WORKING_ATTACK.set(false);
			}
		}
	}
	
	/**
	 * Makes a {@code Creature} move toward its current target.<br>
	 * This method checks if the creature has an active AI and a valid target.<br>
	 * It updates the movement intention based on the provided range.<br>
	 * If the creature is too far away or lacks AI, it will be removed from following.
	 * @param creature The {@code Creature} that should perform the follow action.
	 * @param range The distance to maintain from the target. Use -1 for a random value between 50 and 100.
	 */
	protected void follow(Creature creature, int range)
	{
		try
		{
			if (creature.hasAI())
			{
				final CreatureAI ai = creature.getAI();
				if (ai != null)
				{
					final WorldObject followTarget = ai.getTarget();
					if (followTarget == null)
					{
						if (creature.isSummon())
						{
							creature.asSummon().setFollowStatus(false);
						}
						
						ai.setIntentionIdle();
						return;
					}
					
					final int followRange = range == -1 ? Rnd.get(50, 100) : range;
					if (!creature.isInsideRadius3D(followTarget, followRange))
					{
						if (!creature.isInsideRadius3D(followTarget, 3000))
						{
							// If the target is too far (maybe also teleported).
							if (creature.isSummon())
							{
								creature.asSummon().setFollowStatus(false);
							}
							
							ai.setIntentionIdle();
							return;
						}
						
						ai.moveToPawn(followTarget, followRange);
						// GeoEngine can reject or truncate a route without ever raising ARRIVED.
						// Revalidate rather than leave the action waiting on an event that never comes.
						if (!creature.isMoving() && ((ai.getIntention() == Intention.ATTACK) || (ai.getIntention() == Intention.CAST)))
						{
							ai.notifyActionArrived();
						}
					}
					else if (!creature.isMoving() && ((ai.getIntention() == Intention.ATTACK) || (ai.getIntention() == Intention.CAST)))
					{
						// This task can observe arrival before the movement manager does.
						ai.notifyActionArrived();
					}
				}
				else
				{
					remove(creature);
				}
			}
			else
			{
				remove(creature);
			}
		}
		catch (Exception e)
		{
			// Ignore.
		}
	}
	
	/**
	 * Checks if a specific creature is currently being followed.<br>
	 * This method returns {@code true} if the creature exists in either follow map.<br>
	 * It returns {@code false} if the creature is not being tracked.
	 * @param creature The {@code Creature} to check.
	 * @return {@code true} if the creature is following, otherwise {@code false}.
	 */
	public boolean isFollowing(Creature creature)
	{
		return NORMAL_FOLLOW_CREATURES.containsKey(creature) || ATTACK_FOLLOW_CREATURES.containsKey(creature);
	}
	
	/**
	 * Adds a {@code Creature} to the normal follow list.<br>
	 * This method sets the following distance for the specified creature.<br>
	 * It initiates the follow behavior if it is not already active.
	 * @param creature The {@code Creature} to be added to the follow task.
	 * @param range The maximum distance allowed for the follow action.
	 */
	public void addNormalFollow(Creature creature, int range)
	{
		NORMAL_FOLLOW_CREATURES.putIfAbsent(creature, range);
		follow(creature, range);
	}
	
	/**
	 * Adds a {@code Creature} to the attack follow list.<br>
	 * This method sets the target and its movement range.<br>
	 * It initiates the following behavior for the specified creature.
	 * @param creature The {@code Creature} that will perform the follow action.
	 * @param range The maximum distance allowed for the follow action.
	 */
	public void addAttackFollow(Creature creature, int range)
	{
		ATTACK_FOLLOW_CREATURES.putIfAbsent(creature, range);
		follow(creature, range);
	}
	
	/**
	 * Removes a {@code Creature} from all follow tracking lists.<br>
	 * This method clears the creature from both normal and attack follow maps.
	 * @param creature The {@code Creature} to be removed.
	 */
	public void remove(Creature creature)
	{
		NORMAL_FOLLOW_CREATURES.remove(creature);
		ATTACK_FOLLOW_CREATURES.remove(creature);
	}
	
	/**
	 * Retrieves the singleton instance of {@code CreatureFollowTaskManager}.<br>
	 * Use this method to access the global manager for creature following tasks.
	 * @return The single instance of {@code CreatureFollowTaskManager}.
	 */
	public static CreatureFollowTaskManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final CreatureFollowTaskManager INSTANCE = new CreatureFollowTaskManager();
	}
}
