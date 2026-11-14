/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2jmobius.gameserver.mechanics.script.timers;

import java.util.Objects;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Schedules and manages a single timed task for an associated event, npc or player.
 * @author UnAfraid
 * @param <T> The event type.
 */
public class TimerHolder<T> implements Runnable
{
	private final T _event;
	private final StatSet _params;
	private final long _time;
	private final Npc _npc;
	private final Player _player;
	private final boolean _isRepeating;
	private final IEventTimerEvent<T> _eventScript;
	private final IEventTimerCancel<T> _cancelScript;
	private final TimerExecutor<T> _postExecutor;
	private final ScheduledFuture<?> _task;
	
	/**
	 * Creates a timer, schedules the task and registers it with the npc and player when present.
	 * @param event The event associated with this timer.
	 * @param params The timer parameters.
	 * @param time The delay in milliseconds until the timer fires.
	 * @param npc The associated {@code Npc}, or {@code null}.
	 * @param player The associated {@code Player}, or {@code null}.
	 * @param isRepeating {@code true} if the timer repeats.
	 * @param eventScript The script run when the timer fires.
	 * @param cancelScript The script run when the timer is cancelled.
	 * @param postExecutor The executor handling post-execution cleanup.
	 */
	public TimerHolder(T event, StatSet params, long time, Npc npc, Player player, boolean isRepeating, IEventTimerEvent<T> eventScript, IEventTimerCancel<T> cancelScript, TimerExecutor<T> postExecutor)
	{
		final String className = getClass().getSimpleName();
		Objects.requireNonNull(event, className + ": \"event\" cannot be null!");
		Objects.requireNonNull(eventScript, className + ": \"script\" cannot be null!");
		Objects.requireNonNull(postExecutor, className + ": \"postExecutor\" cannot be null!");
		_event = event;
		_params = params;
		_time = time;
		_npc = npc;
		_player = player;
		_isRepeating = isRepeating;
		_eventScript = eventScript;
		_cancelScript = cancelScript;
		_postExecutor = postExecutor;
		_task = isRepeating ? ThreadPool.scheduleAtFixedRate(this, _time, _time) : ThreadPool.schedule(this, _time);
		
		if (npc != null)
		{
			npc.addTimerHolder(this);
		}
		
		if (player != null)
		{
			player.addTimerHolder(this);
		}
	}
	
	/**
	 * Retrieves the event associated with this timer.
	 * @return The event of type {@code T}.
	 */
	public T getEvent()
	{
		return _event;
	}
	
	/**
	 * Retrieves the parameters associated with this timer.
	 * @return The {@code StatSet} of timer parameters.
	 */
	public StatSet getParams()
	{
		return _params;
	}
	
	/**
	 * Retrieves the npc associated with this timer.
	 * @return The associated {@code Npc}, or {@code null}.
	 */
	public Npc getNpc()
	{
		return _npc;
	}
	
	/**
	 * Retrieves the player associated with this timer.
	 * @return The associated {@code Player}, or {@code null}.
	 */
	public Player getPlayer()
	{
		return _player;
	}
	
	/**
	 * Indicates whether this timer repeats.
	 * @return {@code true} if repeating, {@code false} if it runs once.
	 */
	public boolean isRepeating()
	{
		return _isRepeating;
	}
	
	/**
	 * Unregisters this timer from its npc and player, cancels the task and runs the cancel script.
	 */
	public void cancelTimer()
	{
		if (_npc != null)
		{
			_npc.removeTimerHolder(this);
		}
		
		if (_player != null)
		{
			_player.removeTimerHolder(this);
		}
		
		if ((_task == null) || _task.isCancelled() || _task.isDone())
		{
			return;
		}
		
		_task.cancel(false);
		_cancelScript.onTimerCancel(this);
	}
	
	/**
	 * Cancels the scheduled task without running the cancel script.
	 */
	public void cancelTask()
	{
		if ((_task != null) && !_task.isDone() && !_task.isCancelled())
		{
			_task.cancel(false);
		}
	}
	
	/**
	 * Retrieves the time left before the timer expires.
	 * @return The remaining milliseconds, or {@code -1} if the task is no longer active.
	 */
	public long getRemainingTime()
	{
		if ((_task == null) || _task.isCancelled() || _task.isDone())
		{
			return -1;
		}
		
		return _task.getDelay(TimeUnit.MILLISECONDS);
	}
	
	/**
	 * Checks whether this timer matches the given event, npc and player.
	 * @param event The event to compare.
	 * @param npc The {@code Npc} to compare.
	 * @param player The {@code Player} to compare.
	 * @return {@code true} if all three match, {@code false} otherwise.
	 */
	public boolean isEqual(T event, Npc npc, Player player)
	{
		return _event.equals(event) && (_npc == npc) && (_player == player);
	}
	
	/**
	 * Checks whether this timer equals another by event, npc and player.
	 * @param timer The other {@code TimerHolder} to compare.
	 * @return {@code true} if all three match, {@code false} otherwise.
	 */
	public boolean isEqual(TimerHolder<T> timer)
	{
		return _event.equals(timer._event) && (_npc == timer._npc) && (_player == timer._player);
	}
	
	@Override
	public void run()
	{
		// Notify the post executor to remove this timer from the map.
		_postExecutor.onTimerPostExecute(this);
		
		// Notify the script that the event has been fired.
		_eventScript.onTimerEvent(this);
	}
	
	/**
	 * Returns a string with the event, parameters, time, actors, repeating status and script names.
	 * @return The string representation.
	 */
	@Override
	public String toString()
	{
		return "event: " + _event + " params: " + _params + " time: " + _time + " npc: " + _npc + " player: " + _player + " repeating: " + _isRepeating + " script: " + _eventScript.getClass().getSimpleName() + " postExecutor: " + _postExecutor.getClass().getSimpleName();
	}
}
