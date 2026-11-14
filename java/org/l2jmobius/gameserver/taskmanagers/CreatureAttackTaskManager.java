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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.Weapon;
import org.l2jmobius.gameserver.network.serverpackets.Attack;

/**
 * Manages the execution of attack tasks for {@code Creature} entities.<br>
 * It handles the logic required to process attacks within the game world.
 * @author Mobius
 */
public class CreatureAttackTaskManager
{
	private static final Set<Map<Creature, ScheduledAttack>> ATTACK_POOLS = ConcurrentHashMap.newKeySet();
	private static final Set<Map<Creature, ScheduledFinish>> FINISH_POOLS = ConcurrentHashMap.newKeySet();
	private static final int POOL_SIZE = 300;
	private static final int TASK_DELAY = 10;
	
	/**
	 * Initializes a new instance of the {@code CreatureAttackTaskManager}.<br>
	 * This constructor is protected to restrict instantiation.
	 */
	protected CreatureAttackTaskManager()
	{
	}
	
	private class ScheduleAttackTask implements Runnable
	{
		private final Map<Creature, ScheduledAttack> _creatureAttackData;
		
		public ScheduleAttackTask(Map<Creature, ScheduledAttack> creatureattackData)
		{
			_creatureAttackData = creatureattackData;
		}
		
		@Override
		public void run()
		{
			if (_creatureAttackData.isEmpty())
			{
				return;
			}
			
			final long currentTime = System.currentTimeMillis();
			final Iterator<Creature> iterator = _creatureAttackData.keySet().iterator();
			Creature creature;
			ScheduledAttack scheduledAttack;
			
			while (iterator.hasNext())
			{
				creature = iterator.next();
				scheduledAttack = _creatureAttackData.get(creature);
				
				if ((scheduledAttack != null) && (currentTime >= scheduledAttack.endTime))
				{
					iterator.remove();
					TYPE_SELECT: switch (scheduledAttack.type)
					{
						case NORMAL:
						{
							creature.onHitTimeNotDual(scheduledAttack.weapon, scheduledAttack.attack, scheduledAttack.hitTime, scheduledAttack.attackTime);
							break TYPE_SELECT;
						}
						case DUAL_FIRST:
						{
							creature.onFirstHitTimeForDual(scheduledAttack.weapon, scheduledAttack.attack, scheduledAttack.hitTime, scheduledAttack.attackTime, scheduledAttack.delayForSecondAttack);
							break TYPE_SELECT;
						}
						case DUAL_SECOND:
						{
							creature.onSecondHitTimeForDual(scheduledAttack.weapon, scheduledAttack.attack, scheduledAttack.hitTime, scheduledAttack.delayForSecondAttack, scheduledAttack.attackTime);
							break TYPE_SELECT;
						}
					}
				}
			}
		}
	}
	
	private class ScheduleAbortTask implements Runnable
	{
		private final Map<Creature, ScheduledFinish> _creatureFinishData;
		
		public ScheduleAbortTask(Map<Creature, ScheduledFinish> creatureFinishData)
		{
			_creatureFinishData = creatureFinishData;
		}
		
		@Override
		public void run()
		{
			if (_creatureFinishData.isEmpty())
			{
				return;
			}
			
			final long currentTime = System.currentTimeMillis();
			final Iterator<Creature> iterator = _creatureFinishData.keySet().iterator();
			Creature creature;
			ScheduledFinish scheduledFinish;
			
			while (iterator.hasNext())
			{
				creature = iterator.next();
				scheduledFinish = _creatureFinishData.get(creature);
				
				if ((scheduledFinish != null) && (currentTime >= scheduledFinish.endTime))
				{
					iterator.remove();
					creature.onAttackFinish(scheduledFinish.attack);
				}
			}
		}
	}
	
	/**
	 * Handles the logic for a standard single-hit attack.<br>
	 * This method schedules a {@code ScheduledAttackType.NORMAL} task.<br>
	 * It is used when an attack does not involve dual-strike mechanics.
	 * @param creature The {@code Creature} being attacked.
	 * @param weapon The {@code Weapon} used for the attack.
	 * @param attack The {@code Attack} data packet.
	 * @param hitTime The time at which the hit occurs.
	 * @param attackTime The total duration of the attack.
	 */
	public void onHitTimeNotDual(Creature creature, Weapon weapon, Attack attack, int hitTime, int attackTime)
	{
		scheduleAttack(ScheduledAttackType.NORMAL, creature, weapon, attack, hitTime, attackTime, 0, hitTime);
	}
	
	/**
	 * Handles the timing for the first hit of a dual attack.<br>
	 * This method schedules the initial part of a two-hit sequence.
	 * @param creature The {@code Creature} being attacked.
	 * @param weapon The {@code Weapon} used to perform the attack.
	 * @param attack The {@code Attack} data for this action.
	 * @param hitTime The time at which the first hit occurs.
	 * @param attackTime The total duration of the attack animation.
	 * @param delayForSecondAttack The time delay before the second hit happens.
	 */
	public void onFirstHitTimeForDual(Creature creature, Weapon weapon, Attack attack, int hitTime, int attackTime, int delayForSecondAttack)
	{
		scheduleAttack(ScheduledAttackType.DUAL_FIRST, creature, weapon, attack, hitTime, attackTime, delayForSecondAttack, hitTime);
	}
	
	/**
	 * Handles the timing for the second hit of a dual attack.<br>
	 * This method schedules the second part of a dual attack sequence.
	 * @param creature The {@code Creature} being attacked.
	 * @param weapon The {@code Weapon} used to perform the attack.
	 * @param attack The {@code Attack} data for the current action.
	 * @param hitTime The time at which the hit occurs.
	 * @param attackTime The total duration of the attack.
	 * @param delayForSecondAttack The delay before the second hit happens.
	 */
	public void onSecondHitTimeForDual(Creature creature, Weapon weapon, Attack attack, int hitTime, int attackTime, int delayForSecondAttack)
	{
		scheduleAttack(ScheduledAttackType.DUAL_SECOND, creature, weapon, attack, hitTime, attackTime, delayForSecondAttack, delayForSecondAttack);
	}
	
	/**
	 * Schedules a new attack task for a specific creature.<br>
	 * This method adds the attack to an existing pool or creates a new one if needed.
	 * @param type The {@code ScheduledAttackType} of the attack.
	 * @param creature The {@code Creature} performing the attack.
	 * @param weapon The {@code Weapon} used for the attack.
	 * @param attack The {@code Attack} data packet.
	 * @param hitTime The time at which the hit occurs.
	 * @param attackTime The time at which the attack starts.
	 * @param delayForSecondAttack The delay before the second attack in a dual sequence.
	 * @param taskDelay The base delay for the task execution.
	 */
	private void scheduleAttack(ScheduledAttackType type, Creature creature, Weapon weapon, Attack attack, int hitTime, int attackTime, int delayForSecondAttack, int taskDelay)
	{
		final ScheduledAttack scheduledAttack = new ScheduledAttack(type, weapon, attack, hitTime, attackTime, delayForSecondAttack, taskDelay + System.currentTimeMillis());
		
		for (Map<Creature, ScheduledAttack> pool : ATTACK_POOLS)
		{
			if (pool.size() < POOL_SIZE)
			{
				pool.put(creature, scheduledAttack);
				return;
			}
		}
		
		final Map<Creature, ScheduledAttack> pool = new ConcurrentHashMap<>();
		pool.put(creature, scheduledAttack);
		ThreadPool.schedulePriorityTaskAtFixedRate(new ScheduleAttackTask(pool), TASK_DELAY, TASK_DELAY);
		ATTACK_POOLS.add(pool);
	}
	
	/**
	 * Handles the completion of an attack for a specific creature.<br>
	 * This method schedules the finish event based on the provided delay.
	 * @param creature The {@code Creature} that performed the attack.
	 * @param attack The {@code Attack} object associated with the action.
	 * @param taskDelay The amount of time to wait before finishing the task.
	 */
	public void onAttackFinish(Creature creature, Attack attack, int taskDelay)
	{
		final ScheduledFinish scheduledFinish = new ScheduledFinish(attack, taskDelay + System.currentTimeMillis());
		
		for (Map<Creature, ScheduledFinish> pool : FINISH_POOLS)
		{
			if (pool.size() < POOL_SIZE)
			{
				pool.put(creature, scheduledFinish);
				return;
			}
		}
		
		final Map<Creature, ScheduledFinish> pool = new ConcurrentHashMap<>();
		pool.put(creature, scheduledFinish);
		ThreadPool.schedulePriorityTaskAtFixedRate(new ScheduleAbortTask(pool), TASK_DELAY, TASK_DELAY);
		FINISH_POOLS.add(pool);
	}
	
	/**
	 * Stops all scheduled attacks for a specific creature.<br>
	 * This method removes the {@code creature} from both attack and finish pools.<br>
	 * It ensures that no further attack tasks are processed for this entity.
	 * @param creature The {@code Creature} whose attacks should be cancelled.
	 */
	public void abortAttack(Creature creature)
	{
		for (Map<Creature, ScheduledAttack> pool : ATTACK_POOLS)
		{
			if (pool.remove(creature) != null)
			{
				break;
			}
		}
		
		for (Map<Creature, ScheduledFinish> pool : FINISH_POOLS)
		{
			if (pool.remove(creature) != null)
			{
				return;
			}
		}
	}
	
	private class ScheduledAttack
	{
		public final ScheduledAttackType type;
		public final Weapon weapon;
		public final Attack attack;
		public final int hitTime;
		public final int attackTime;
		public final int delayForSecondAttack;
		public final long endTime;
		
		public ScheduledAttack(ScheduledAttackType type, Weapon weapon, Attack attack, int hitTime, int attackTime, int delayForSecondAttack, long endTime)
		{
			this.type = type;
			this.weapon = weapon;
			this.attack = attack;
			this.hitTime = hitTime;
			this.attackTime = attackTime;
			this.delayForSecondAttack = delayForSecondAttack;
			this.endTime = endTime;
		}
	}
	
	private class ScheduledFinish
	{
		public final Attack attack;
		public final long endTime;
		
		public ScheduledFinish(Attack attack, long endTime)
		{
			this.attack = attack;
			this.endTime = endTime;
		}
	}
	
	private enum ScheduledAttackType
	{
		NORMAL,
		DUAL_FIRST,
		DUAL_SECOND,
	}
	
	/**
	 * Retrieves the singleton instance of the {@code CreatureAttackTaskManager}.<br>
	 * Use this method to access the global manager for creature attack tasks.
	 * @return The single shared instance of {@code CreatureAttackTaskManager}.
	 */
	public static CreatureAttackTaskManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final CreatureAttackTaskManager INSTANCE = new CreatureAttackTaskManager();
	}
}
