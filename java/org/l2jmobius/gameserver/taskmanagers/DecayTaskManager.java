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
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.config.NpcConfig;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.config.custom.OfflinePlayConfig;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;

/**
 * Manages the periodic decay of items and other entities in the game world.<br>
 * It handles the removal of expired objects to maintain server performance.
 * @author Mobius
 */
public class DecayTaskManager implements Runnable
{
	private static final Map<Creature, Long> DECAY_SCHEDULES = new ConcurrentHashMap<>();
	private static final AtomicBoolean WORKING = new AtomicBoolean();
	
	/**
	 * Initializes a new instance of the {@code DecayTaskManager}.<br>
	 * This constructor starts the periodic task execution.<br>
	 * It schedules the {@code run} method to execute every {@code 1000} milliseconds.
	 */
	protected DecayTaskManager()
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
			if (!DECAY_SCHEDULES.isEmpty())
			{
				final long currentTime = System.currentTimeMillis();
				final Iterator<Creature> iterator = DECAY_SCHEDULES.keySet().iterator();
				Creature creature;
				Long time;
				
				while (iterator.hasNext())
				{
					creature = iterator.next();
					time = DECAY_SCHEDULES.get(creature);
					if ((time != null) && (currentTime > time.longValue()))
					{
						creature.onDecay();
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
	 * Adds a {@code Creature} to the decay schedule.<br>
	 * This method calculates the time until the creature decays based on its type and status.<br>
	 * It handles specific logic for NPCs, spoiled corpses, and players.
	 * @param creature The {@code Creature} to be added to the decay manager.
	 */
	public void add(Creature creature)
	{
		if (creature == null)
		{
			return;
		}
		
		long delay;
		if (creature.getTemplate() instanceof NpcTemplate)
		{
			delay = ((NpcTemplate) creature.getTemplate()).getCorpseTime();
		}
		else
		{
			delay = NpcConfig.DEFAULT_CORPSE_TIME;
		}
		
		if (creature.isAttackable() && (creature.asAttackable().isSpoiled() || creature.asAttackable().isSeeded()))
		{
			delay += NpcConfig.SPOILED_CORPSE_EXTEND_TIME;
		}
		
		if (creature.isPlayer())
		{
			final Player player = creature.asPlayer();
			if (player.getOfflineMode().isPlayRunning() && OfflinePlayConfig.OFFLINE_PLAY_LOGOUT_ON_DEATH)
			{
				delay = 10; // 10 seconds
			}
			else if (player.isInTimedHuntingZone())
			{
				delay = 600; // 10 minutes
			}
			else if (PlayerConfig.DISCONNECT_AFTER_DEATH)
			{
				delay = 3600; // 1 hour
			}
		}
		
		// Add to decay schedules.
		DECAY_SCHEDULES.put(creature, System.currentTimeMillis() + (delay * 1000));
	}
	
	/**
	 * Stops the decay process for a specific {@code Creature}.<br>
	 * This method removes the creature from the internal schedule.
	 * @param creature The {@code Creature} to remove from the decay task.
	 */
	public void cancel(Creature creature)
	{
		DECAY_SCHEDULES.remove(creature);
	}
	
	/**
	 * Calculates the time left before a {@code Creature} decays.<br>
	 * It returns the difference between the scheduled decay time and the current system time.<br>
	 * If no schedule exists, it returns {@code Long.MAX_VALUE}.
	 * @param creature The {@code Creature} to check for decay time.
	 * @return The remaining time in milliseconds or {@code Long.MAX_VALUE} if not scheduled.
	 */
	public long getRemainingTime(Creature creature)
	{
		final Long time = DECAY_SCHEDULES.get(creature);
		return time != null ? time.longValue() - System.currentTimeMillis() : Long.MAX_VALUE;
	}
	
	/**
	 * Generates a summary report of all active decay tasks.<br>
	 * This includes the total count and a list of every {@code Creature} with its remaining timer.
	 * @return A {@code String} containing the formatted task dump.
	 */
	@Override
	public String toString()
	{
		final StringBuilder ret = new StringBuilder();
		ret.append("============= DecayTask Manager Report ============");
		ret.append(System.lineSeparator());
		ret.append("Tasks count: ");
		ret.append(DECAY_SCHEDULES.size());
		ret.append(System.lineSeparator());
		ret.append("Tasks dump:");
		ret.append(System.lineSeparator());
		
		final long time = System.currentTimeMillis();
		for (Entry<Creature, Long> entry : DECAY_SCHEDULES.entrySet())
		{
			ret.append("Class/Name: ");
			ret.append(entry.getKey().getClass().getSimpleName());
			ret.append('/');
			ret.append(entry.getKey().getName());
			ret.append(" decay timer: ");
			ret.append(entry.getValue().longValue() - time);
			ret.append(System.lineSeparator());
		}
		
		return ret.toString();
	}
	
	/**
	 * Retrieves the singleton instance of the {@code DecayTaskManager}.<br>
	 * This method provides a global access point to the decay management system.
	 * @return The active {@code DecayTaskManager} instance.
	 */
	public static DecayTaskManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final DecayTaskManager INSTANCE = new DecayTaskManager();
	}
}
