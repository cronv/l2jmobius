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
package org.l2jmobius.gameserver.mechanics.script;

import java.util.concurrent.ScheduledFuture;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;

/**
 * Manages a timed event for a quest, firing after a delay and optionally repeating.
 */
public class QuestTimer
{
	protected final String _name;
	protected final Quest _quest;
	protected final Npc _npc;
	protected final Player _player;
	protected final boolean _isRepeating;
	protected ScheduledFuture<?> _scheduler;
	
	/**
	 * Creates and schedules a quest timer, registering it with the npc and player when non-null.
	 * @param quest The associated {@code Quest}.
	 * @param name The timer name.
	 * @param time The delay in milliseconds before triggering.
	 * @param npc The {@code Npc} to link to, or {@code null}.
	 * @param player The {@code Player} to link to, or {@code null}.
	 * @param repeating {@code true} if the timer fires repeatedly.
	 */
	public QuestTimer(Quest quest, String name, long time, Npc npc, Player player, boolean repeating)
	{
		_quest = quest;
		_name = name;
		_npc = npc;
		_player = player;
		_isRepeating = repeating;
		
		if (repeating)
		{
			_scheduler = ThreadPool.scheduleAtFixedRate(new ScheduleTimerTask(), time, time); // Prepare auto end task.
		}
		else
		{
			_scheduler = ThreadPool.schedule(new ScheduleTimerTask(), time); // Prepare auto end task.
		}
		
		if (npc != null)
		{
			npc.addQuestTimer(this);
		}
		
		if (player != null)
		{
			player.addQuestTimer(this);
		}
	}
	
	/**
	 * Cancels the timer and unregisters it from the associated npc and player.
	 */
	public void cancel()
	{
		cancelTask();
		
		if (_npc != null)
		{
			_npc.removeQuestTimer(this);
		}
		
		if (_player != null)
		{
			_player.removeQuestTimer(this);
		}
	}
	
	/**
	 * Cancels the scheduled task and unregisters the timer from its quest.
	 */
	public void cancelTask()
	{
		if ((_scheduler != null) && !_scheduler.isDone() && !_scheduler.isCancelled())
		{
			_scheduler.cancel(false);
			_scheduler = null;
		}
		
		_quest.removeQuestTimer(this);
	}
	
	/**
	 * Checks whether this timer matches the given quest, name, npc and player.
	 * @param quest The {@code Quest} to compare.
	 * @param name The name to compare.
	 * @param npc The {@code Npc} to compare.
	 * @param player The {@code Player} to compare.
	 * @return {@code true} if all fields match, {@code false} otherwise.
	 */
	public boolean equals(Quest quest, String name, Npc npc, Player player)
	{
		if ((quest == null) || (quest != _quest))
		{
			return false;
		}
		
		if ((name == null) || !name.equals(_name))
		{
			return false;
		}
		
		return (npc == _npc) && (player == _player);
	}
	
	/**
	 * Indicates whether the timer is scheduled and running.
	 * @return {@code true} if active, {@code false} otherwise.
	 */
	public boolean isActive()
	{
		return (_scheduler != null) && !_scheduler.isCancelled() && !_scheduler.isDone();
	}
	
	/**
	 * Indicates whether the timer repeats.
	 * @return {@code true} if repeating, {@code false} otherwise.
	 */
	public boolean isRepeating()
	{
		return _isRepeating;
	}
	
	/**
	 * Retrieves the associated quest.
	 * @return The {@code Quest}.
	 */
	public Quest getQuest()
	{
		return _quest;
	}
	
	/**
	 * Retrieves the associated npc.
	 * @return The {@code Npc}.
	 */
	public Npc getNpc()
	{
		return _npc;
	}
	
	/**
	 * Retrieves the associated player.
	 * @return The {@code Player}.
	 */
	public Player getPlayer()
	{
		return _player;
	}
	
	/**
	 * Returns the timer name.
	 * @return The timer name as a {@code String}.
	 */
	@Override
	public String toString()
	{
		return _name;
	}
	
	public class ScheduleTimerTask implements Runnable
	{
		@Override
		public void run()
		{
			if (_scheduler == null)
			{
				return;
			}
			
			if (!_isRepeating)
			{
				cancel();
			}
			
			_quest.notifyEvent(_name, _npc, _player);
		}
	}
}
