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
package org.l2jmobius.gameserver.mechanics.script.timers;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Manages and executes timed tasks for game entities.
 * @author UnAfraid, Mobius
 * @param <T> The event type.
 */
public class TimerExecutor<T>
{
	private final Map<T, Set<TimerHolder<T>>> _timers = new ConcurrentHashMap<>();
	private final IEventTimerEvent<T> _eventListener;
	private final IEventTimerCancel<T> _cancelListener;
	
	/**
	 * Creates an executor with the given event and cancel listeners.
	 * @param eventListener The listener handling timer events.
	 * @param cancelListener The listener handling timer cancellations.
	 */
	public TimerExecutor(IEventTimerEvent<T> eventListener, IEventTimerCancel<T> cancelListener)
	{
		_eventListener = eventListener;
		_cancelListener = cancelListener;
	}
	
	/**
	 * Registers a timer, replacing and cancelling any equal existing timer.
	 * @param holder The {@code TimerHolder} to register.
	 * @return {@code true} if added, {@code false} otherwise.
	 */
	private boolean addTimer(TimerHolder<T> holder)
	{
		final Set<TimerHolder<T>> timers = _timers.computeIfAbsent(holder.getEvent(), _ -> ConcurrentHashMap.newKeySet());
		removeAndCancelTimers(timers, holder::isEqual);
		return timers.add(holder);
	}
	
	/**
	 * Creates and registers a timer with explicit parameters and event listener.
	 * @param event The event to trigger.
	 * @param params Additional timer parameters.
	 * @param time The delay in milliseconds before expiry.
	 * @param npc The associated {@code Npc}, or {@code null}.
	 * @param player The associated {@code Player}, or {@code null}.
	 * @param eventTimer The {@code IEventTimerEvent} run on expiry.
	 * @return {@code true} if added, {@code false} otherwise.
	 */
	public boolean addTimer(T event, StatSet params, long time, Npc npc, Player player, IEventTimerEvent<T> eventTimer)
	{
		return addTimer(new TimerHolder<>(event, params, time, npc, player, false, eventTimer, _cancelListener, this));
	}
	
	/**
	 * Creates and registers a timer with the given event listener.
	 * @param event The event to trigger.
	 * @param time The delay in milliseconds before expiry.
	 * @param eventTimer The {@code IEventTimerEvent} run on expiry.
	 * @return {@code true} if added, {@code false} otherwise.
	 */
	public boolean addTimer(T event, long time, IEventTimerEvent<T> eventTimer)
	{
		return addTimer(new TimerHolder<>(event, null, time, null, null, false, eventTimer, _cancelListener, this));
	}
	
	/**
	 * Creates and registers a timer using the default event listener.
	 * @param event The event to trigger.
	 * @param params Additional timer parameters.
	 * @param time The delay in milliseconds before expiry.
	 * @param npc The associated {@code Npc}, or {@code null}.
	 * @param player The associated {@code Player}, or {@code null}.
	 * @return {@code true} if added, {@code false} otherwise.
	 */
	public boolean addTimer(T event, StatSet params, long time, Npc npc, Player player)
	{
		return addTimer(event, params, time, npc, player, _eventListener);
	}
	
	/**
	 * Creates and registers a timer using the default event listener.
	 * @param event The event to trigger.
	 * @param time The delay in milliseconds before expiry.
	 * @param npc The associated {@code Npc}, or {@code null}.
	 * @param player The associated {@code Player}, or {@code null}.
	 * @return {@code true} if added, {@code false} otherwise.
	 */
	public boolean addTimer(T event, long time, Npc npc, Player player)
	{
		return addTimer(event, null, time, npc, player, _eventListener);
	}
	
	/**
	 * Creates and registers a repeating timer with explicit parameters and event listener.
	 * @param event The event to trigger.
	 * @param params Additional timer parameters.
	 * @param time The interval in milliseconds between repetitions.
	 * @param npc The associated {@code Npc}, or {@code null}.
	 * @param player The associated {@code Player}, or {@code null}.
	 * @param eventTimer The {@code IEventTimerEvent} run on each repetition.
	 * @return {@code true} if added, {@code false} otherwise.
	 */
	private boolean addRepeatingTimer(T event, StatSet params, long time, Npc npc, Player player, IEventTimerEvent<T> eventTimer)
	{
		return addTimer(new TimerHolder<>(event, params, time, npc, player, true, eventTimer, _cancelListener, this));
	}
	
	/**
	 * Creates and registers a repeating timer using the default event listener.
	 * @param event The event to trigger.
	 * @param time The interval in milliseconds between repetitions.
	 * @param npc The associated {@code Npc}, or {@code null}.
	 * @param player The associated {@code Player}, or {@code null}.
	 * @return {@code true} if added, {@code false} otherwise.
	 */
	public boolean addRepeatingTimer(T event, long time, Npc npc, Player player)
	{
		return addRepeatingTimer(event, null, time, npc, player, _eventListener);
	}
	
	/**
	 * Removes a non-repeating timer after execution, pruning empty event sets.
	 * @param holder The {@code TimerHolder} that just executed.
	 */
	public void onTimerPostExecute(TimerHolder<T> holder)
	{
		// Remove non repeating timer upon execute.
		if (!holder.isRepeating())
		{
			final T eventId = holder.getEvent();
			final Set<TimerHolder<T>> timers = _timers.get(eventId);
			if ((timers == null) || timers.isEmpty())
			{
				return;
			}
			
			// Remove the timer
			removeAndCancelTimers(timers, holder::isEqual);
			
			// If there's no events inside that set remove it.
			if (timers.isEmpty())
			{
				_timers.remove(eventId);
			}
		}
	}
	
	/**
	 * Cancels every managed timer and clears the collection.
	 */
	public void cancelAllTimers()
	{
		for (Set<TimerHolder<T>> set : _timers.values())
		{
			for (TimerHolder<T> timer : set)
			{
				timer.cancelTimer();
			}
		}
		
		_timers.clear();
	}
	
	/**
	 * Checks whether a matching timer exists for the given event, npc and player.
	 * @param event The event to check.
	 * @param npc The associated {@code Npc}.
	 * @param player The associated {@code Player}.
	 * @return {@code true} if a matching timer exists, {@code false} otherwise.
	 */
	public boolean hasTimer(T event, Npc npc, Player player)
	{
		final Set<TimerHolder<T>> timers = _timers.get(event);
		if ((timers == null) || timers.isEmpty())
		{
			return false;
		}
		
		for (TimerHolder<T> holder : timers)
		{
			if (holder.isEqual(event, npc, player))
			{
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Cancels and removes all timers registered for the given event.
	 * @param event The event whose timers to cancel.
	 * @return {@code true} if timers were cancelled, {@code false} otherwise.
	 */
	public boolean cancelTimers(T event)
	{
		final Set<TimerHolder<T>> timers = _timers.remove(event);
		if ((timers == null) || timers.isEmpty())
		{
			return false;
		}
		
		timers.forEach(TimerHolder::cancelTimer);
		return true;
	}
	
	/**
	 * Cancels the timer matching the given event, npc and player.
	 * @param event The event of the timer.
	 * @param npc The associated {@code Npc}.
	 * @param player The associated {@code Player}.
	 * @return Always {@code false}.
	 */
	public boolean cancelTimer(T event, Npc npc, Player player)
	{
		final Set<TimerHolder<T>> timers = _timers.get(event);
		if ((timers == null) || timers.isEmpty())
		{
			return false;
		}
		
		removeAndCancelTimers(timers, timer -> timer.isEqual(event, npc, player));
		return false;
	}
	
	/**
	 * Cancels every timer associated with the given npc.
	 * @param npc The {@code Npc} whose timers to cancel.
	 */
	public void cancelTimersOf(Npc npc)
	{
		removeAndCancelTimers(timer -> timer.getNpc() == npc);
	}
	
	/**
	 * Removes and cancels all timers across every event set matching the condition.
	 * @param condition The predicate selecting timers to remove.
	 */
	private void removeAndCancelTimers(Predicate<TimerHolder<T>> condition)
	{
		Objects.requireNonNull(condition);
		
		final Collection<Set<TimerHolder<T>>> allTimers = _timers.values();
		for (Set<TimerHolder<T>> timers : allTimers)
		{
			removeAndCancelTimers(timers, condition);
		}
	}
	
	/**
	 * Removes and cancels timers in the given set matching the condition.
	 * @param timers The set of timers to process.
	 * @param condition The predicate selecting timers to remove.
	 */
	private void removeAndCancelTimers(Set<TimerHolder<T>> timers, Predicate<TimerHolder<T>> condition)
	{
		Objects.requireNonNull(timers);
		Objects.requireNonNull(condition);
		
		final Iterator<TimerHolder<T>> it = timers.iterator();
		while (it.hasNext())
		{
			final TimerHolder<T> timer = it.next();
			if (condition.test(timer))
			{
				it.remove();
				timer.cancelTimer();
			}
		}
	}
}
