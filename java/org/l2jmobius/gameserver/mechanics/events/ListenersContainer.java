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
package org.l2jmobius.gameserver.mechanics.events;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.function.Predicate;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.mechanics.events.listeners.AbstractEventListener;

/**
 * This class acts as a central registry for managing event listeners.<br>
 * It stores and organizes {@code AbstractEventListener} instances to handle game events efficiently.
 * @author UnAfraid, Mobius
 */
public class ListenersContainer
{
	private volatile Map<EventType, Queue<AbstractEventListener>> _listeners = null;
	
	/**
	 * Registers a new listener to the system.<br>
	 * This method adds the provided {@code AbstractEventListener} to its specific event type queue.<br>
	 * It ensures that the listener will be notified when relevant events occur.
	 * @param listener The {@code AbstractEventListener} to register.
	 * @return The same {@code AbstractEventListener} instance that was added.
	 */
	public AbstractEventListener addListener(AbstractEventListener listener)
	{
		if (listener == null)
		{
			throw new NullPointerException("Listener cannot be null!");
		}
		
		// The listener joins the queue inside the mapping, otherwise a concurrent removal can drop the queue first and lose it.
		getListeners().compute(listener.getType(), (_, queue) ->
		{
			final Queue<AbstractEventListener> listeners = (queue == null) ? new PriorityBlockingQueue<>() : queue;
			listeners.add(listener);
			return listeners;
		});
		
		return listener;
	}
	
	/**
	 * Removes a specific listener from the container.<br>
	 * This method identifies the event type of the provided {@code AbstractEventListener}.<br>
	 * It removes that listener from the internal queue associated with its type.
	 * @param listener The {@code AbstractEventListener} to be removed.
	 * @return The same {@code AbstractEventListener} instance that was removed.
	 */
	public AbstractEventListener removeListener(AbstractEventListener listener)
	{
		if (listener == null)
		{
			throw new NullPointerException("Listener cannot be null!");
		}
		
		// Removing a listener that was never added is not a failure, and emptying the queue has to drop it in the same step.
		if (_listeners != null)
		{
			_listeners.computeIfPresent(listener.getType(), (_, queue) ->
			{
				queue.remove(listener);
				return queue.isEmpty() ? null : queue;
			});
		}
		
		return listener;
	}
	
	/**
	 * Removes listeners for a specific event type based on a condition.<br>
	 * This method iterates through all listeners of the given {@code EventType}.<br>
	 * It calls {@code unregisterMe()} on any listener that matches the {@code filter}.
	 * @param type The {@code EventType} to check for listeners.
	 * @param filter A {@code Predicate} used to determine which {@code AbstractEventListener} instances to remove.
	 */
	public void removeListenerIf(EventType type, Predicate<? super AbstractEventListener> filter)
	{
		if (_listeners == null)
		{
			return;
		}
		
		for (AbstractEventListener listener : getListeners(type))
		{
			if (filter.test(listener))
			{
				listener.unregisterMe();
			}
		}
	}
	
	/**
	 * Removes all listeners that match the given criteria.<br>
	 * This method iterates through every listener in the container.<br>
	 * It calls {@code unregisterMe()} on any listener that satisfies the {@code filter}.
	 * @param filter A {@code Predicate} used to determine which {@code AbstractEventListener} instances should be removed.
	 */
	public void removeListenerIf(Predicate<? super AbstractEventListener> filter)
	{
		if (_listeners == null)
		{
			return;
		}
		
		for (Queue<AbstractEventListener> queue : getListeners().values())
		{
			for (AbstractEventListener listener : queue)
			{
				if (filter.test(listener))
				{
					listener.unregisterMe();
				}
			}
		}
	}
	
	/**
	 * Checks if there is at least one listener registered for a specific event type.<br>
	 * This method checks the local container, the entity template, and global containers.
	 * @param type The {@code EventType} to check for listeners.
	 * @return {@code true} if a listener exists, otherwise {@code false}.
	 */
	public boolean hasListener(EventType type)
	{
		if ((_listeners != null) && _listeners.containsKey(type))
		{
			return true;
		}
		
		if (this instanceof Creature)
		{
			final Creature creature = (Creature) this;
			if (creature.getTemplate().hasListener(type))
			{
				return true;
			}
			
			if (creature.isMonster())
			{
				return Containers.Monsters().hasListener(type);
			}
			else if (creature.isNpc())
			{
				return Containers.Npcs().hasListener(type);
			}
			else if (creature.isPlayer())
			{
				return Containers.Players().hasListener(type);
			}
		}
		
		return false;
	}
	
	/**
	 * Retrieves all listeners registered for a specific event type.<br>
	 * Returns an empty list if no listeners are found or if the container is null.
	 * @param type The {@code EventType} to filter by.
	 * @return A {@code Collection} of {@code AbstractEventListener} objects.
	 */
	public Collection<AbstractEventListener> getListeners(EventType type)
	{
		if (_listeners != null)
		{
			final Collection<AbstractEventListener> eventListenerQueue = _listeners.get(type);
			if (eventListenerQueue != null)
			{
				return eventListenerQueue;
			}
		}
		
		return Collections.emptyList();
	}
	
	/**
	 * Retrieves the internal map of event listeners.<br>
	 * This method ensures the {@code _listeners} map is initialized before returning it.
	 * @return a {@code Map} where keys are {@code EventType} and values are queues of {@code AbstractEventListener}.
	 */
	private Map<EventType, Queue<AbstractEventListener>> getListeners()
	{
		if (_listeners == null)
		{
			synchronized (this)
			{
				if (_listeners == null)
				{
					_listeners = new ConcurrentHashMap<>();
				}
			}
		}
		
		return _listeners;
	}
}
