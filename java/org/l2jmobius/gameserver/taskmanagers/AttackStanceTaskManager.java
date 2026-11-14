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
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Summon;
import org.l2jmobius.gameserver.network.serverpackets.AutoAttackStop;

/**
 * Manages the background tasks related to creature attack stances.<br>
 * This class handles the logic for updating and maintaining {@code Creature} states during combat.
 * @author Luca Baldi, Mobius
 */
public class AttackStanceTaskManager implements Runnable
{
	private static final Logger LOGGER = Logger.getLogger(AttackStanceTaskManager.class.getName());
	
	public static final long COMBAT_TIME = 15000;
	
	private static final Map<Creature, Long> CREATURE_ATTACK_STANCES = new ConcurrentHashMap<>();
	private static final AtomicBoolean WORKING = new AtomicBoolean();
	
	/**
	 * Initializes the {@code AttackStanceTaskManager}.<br>
	 * This constructor schedules a recurring task to manage creature attack stances.<br>
	 * It sets up the manager to run every {@code 1000} milliseconds.
	 */
	protected AttackStanceTaskManager()
	{
		ThreadPool.schedulePriorityTaskAtFixedRate(this, 0, 1000);
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
			if (!CREATURE_ATTACK_STANCES.isEmpty())
			{
				try
				{
					final long currentTime = System.currentTimeMillis();
					final Iterator<Creature> iterator = CREATURE_ATTACK_STANCES.keySet().iterator();
					Creature creature;
					Long time;
					
					while (iterator.hasNext())
					{
						creature = iterator.next();
						time = CREATURE_ATTACK_STANCES.get(creature);
						if ((time != null) && ((currentTime - time.longValue()) > COMBAT_TIME))
						{
							creature.broadcastPacket(new AutoAttackStop(creature.getObjectId()));
							creature.getAI().setAutoAttacking(false);
							if (creature.isPlayer() && creature.hasSummon())
							{
								creature.asPlayer().clearDamageTaken();
								final Summon pet = creature.getPet();
								if (pet != null)
								{
									pet.broadcastPacket(new AutoAttackStop(pet.getObjectId()));
								}
								
								creature.getServitors().values().forEach(s -> s.broadcastPacket(new AutoAttackStop(s.getObjectId())));
							}
							
							iterator.remove();
						}
					}
				}
				catch (Exception e)
				{
					// Unless caught here, players remain in attack positions.
					LOGGER.log(Level.WARNING, "Error in AttackStanceTaskManager: " + e.getMessage(), e);
				}
			}
		}
		finally
		{
			WORKING.set(false);
		}
	}
	
	/**
	 * Registers a {@code Creature} to be tracked for its attack stance.<br>
	 * This method updates the timestamp in the internal tracking map.<br>
	 * If the provided {@code creature} is {@code null}, no action is taken.
	 * @param creature The {@code Creature} to add to the task manager.
	 */
	public void addAttackStanceTask(Creature creature)
	{
		if (creature == null)
		{
			return;
		}
		
		CREATURE_ATTACK_STANCES.put(creature, System.currentTimeMillis());
	}
	
	/**
	 * Removes the attack stance task for a specific {@code Creature}.<br>
	 * This method handles both regular creatures and summons.<br>
	 * If the creature is a summon, it removes the task for the owner instead.
	 * @param creature The {@code Creature} to remove from the task manager.
	 */
	public void removeAttackStanceTask(Creature creature)
	{
		Creature actor = creature;
		if (actor != null)
		{
			if (actor.isSummon())
			{
				actor = actor.asPlayer();
			}
			
			CREATURE_ATTACK_STANCES.remove(actor);
		}
	}
	
	/**
	 * Checks if a specific creature has an active attack stance task.<br>
	 * This method handles {@code Summon} objects by checking their owner instead.<br>
	 * It returns {@code false} if the provided {@code Creature} is {@code null}.
	 * @param creature The {@code Creature} to check.
	 * @return {@code true} if the task exists, otherwise {@code false}.
	 */
	public boolean hasAttackStanceTask(Creature creature)
	{
		Creature actor = creature;
		if (actor != null)
		{
			if (actor.isSummon())
			{
				actor = actor.asPlayer();
			}
			
			return CREATURE_ATTACK_STANCES.containsKey(actor);
		}
		
		return false;
	}
	
	/**
	 * Gets the singleton instance of the {@code AttackStanceTaskManager}.<br>
	 * This method provides a global access point to the manager.<br>
	 * Use this instead of creating a new object.
	 * @return The single instance of {@code AttackStanceTaskManager}.
	 */
	public static AttackStanceTaskManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final AttackStanceTaskManager INSTANCE = new AttackStanceTaskManager();
	}
}
