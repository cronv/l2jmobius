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
package org.l2jmobius.gameserver.mechanics.skill;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.mechanics.skill.enums.SkillFinishType;

/**
 * Handles the logic for completing a buff effect on a {@code Creature}.<br>
 * This task is executed when a skill's duration expires or is removed.<br>
 * It ensures that all associated mechanics are properly cleaned up.
 * @author Mobius
 */
public class BuffFinishTask
{
	private final Map<BuffInfo, AtomicInteger> _buffInfos = new ConcurrentHashMap<>();
	private ScheduledFuture<?> _task = null;
	private boolean _stopped = false;
	
	private class BuffFinishRunnable implements Runnable
	{
		@Override
		public void run()
		{
			for (Entry<BuffInfo, AtomicInteger> entry : _buffInfos.entrySet())
			{
				final BuffInfo info = entry.getKey();
				final Creature effected = info.getEffected();
				if ((effected != null) && (entry.getValue().incrementAndGet() > info.getAbnormalTime()))
				{
					// Stop this exact buff once, a newer buff of the same skill must not be affected.
					ThreadPool.execute(() -> effected.getEffectList().remove(info, SkillFinishType.NORMAL, true, true));
					removeBuffInfo(info);
				}
			}
		}
	}
	
	/**
	 * Removes a specific {@code BuffInfo} from the active tracking list.<br>
	 * If no buffs remain, it cancels the background task.
	 * @param info The {@code BuffInfo} object to remove.
	 */
	public synchronized void removeBuffInfo(BuffInfo info)
	{
		_buffInfos.remove(info);
		
		if (_buffInfos.isEmpty() && (_task != null))
		{
			_task.cancel(true);
			_task = null;
		}
	}
	
	/**
	 * Adds a new {@code BuffInfo} object to the tracking system.<br>
	 * This method starts the background task if it is not already running.<br>
	 * It ensures that buff durations are monitored correctly.
	 * @param info The {@code BuffInfo} to be added.
	 */
	public synchronized void addBuffInfo(BuffInfo info)
	{
		_buffInfos.put(info, new AtomicInteger());
		
		if ((_task == null) && !_stopped)
		{
			_task = ThreadPool.scheduleAtFixedRate(new BuffFinishRunnable(), 0, 1000);
		}
	}
	
	/**
	 * Starts the background task to manage buff expirations.<br>
	 * This method initializes the {@code _stopped} flag to {@code false}.<br>
	 * It schedules a new runnable if there are active buffs and no task is running.
	 */
	public synchronized void start()
	{
		_stopped = false;
		
		if (!_buffInfos.isEmpty() && (_task == null))
		{
			_task = ThreadPool.scheduleAtFixedRate(new BuffFinishRunnable(), 0, 1000);
		}
	}
	
	/**
	 * Stops the background task for processing buff finishes.<br>
	 * Sets the {@code _stopped} flag to {@code true}.<br>
	 * Cancels the active {@code ScheduledFuture} if it exists.
	 */
	public synchronized void stop()
	{
		_stopped = true;
		
		if (_task != null)
		{
			_task.cancel(true);
		}
	}
}
