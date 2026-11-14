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
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;
import org.l2jmobius.gameserver.mechanics.events.listeners.AbstractEventListener;
import org.l2jmobius.gameserver.mechanics.events.returns.AbstractEventReturn;

/**
 * Dispatches game events to the registered listeners of one or more containers.
 * @author UnAfraid, Mobius
 */
public class EventDispatcher
{
	private static final Logger LOGGER = Logger.getLogger(EventDispatcher.class.getName());
	
	/**
	 * Creates the {@code EventDispatcher} singleton.
	 */
	protected EventDispatcher()
	{
	}
	
	/**
	 * Checks whether any listener is registered globally for the event type.
	 * @param type The {@code EventType} to check.
	 * @return {@code true} if a listener exists, {@code false} otherwise.
	 */
	public boolean hasListener(EventType type)
	{
		return Containers.Global().hasListener(type);
	}
	
	/**
	 * Checks whether a listener for the event type exists globally or in the container.
	 * @param type The {@code EventType} to check.
	 * @param container The {@code ListenersContainer} to search.
	 * @return {@code true} if a listener exists, {@code false} otherwise.
	 */
	public boolean hasListener(EventType type, ListenersContainer container)
	{
		return Containers.Global().hasListener(type) || ((container != null) && container.hasListener(type));
	}
	
	/**
	 * Checks whether a listener for the event type exists globally or in any container.
	 * @param type The {@code EventType} to check.
	 * @param containers The {@code ListenersContainer} objects to search.
	 * @return {@code true} if a listener exists, {@code false} otherwise.
	 */
	public boolean hasListener(EventType type, ListenersContainer... containers)
	{
		boolean hasListeners = Containers.Global().hasListener(type);
		if (!hasListeners)
		{
			for (ListenersContainer container : containers)
			{
				if (container.hasListener(type))
				{
					hasListeners = true;
					break;
				}
			}
		}
		
		return hasListeners;
	}
	
	/**
	 * Notifies all global listeners of the event and returns the result.
	 * @param <T> The {@code AbstractEventReturn} subtype returned.
	 * @param event The event to process.
	 * @return The notification result as a {@code T}.
	 */
	public <T extends AbstractEventReturn> T notifyEvent(IBaseEvent event)
	{
		return notifyEvent(event, null, null);
	}
	
	/**
	 * Notifies all global listeners and casts the result to the callback type.
	 * @param <T> The {@code AbstractEventReturn} subtype returned.
	 * @param event The event to process.
	 * @param callbackClass The expected return type.
	 * @return The result as a {@code T}.
	 */
	public <T extends AbstractEventReturn> T notifyEvent(IBaseEvent event, Class<T> callbackClass)
	{
		return notifyEvent(event, null, callbackClass);
	}
	
	/**
	 * Notifies all listeners in the container and returns the result.
	 * @param <T> The {@code AbstractEventReturn} subtype returned.
	 * @param event The event to process.
	 * @param container The {@code ListenersContainer} holding the listeners.
	 * @return The result as a {@code T}.
	 */
	public <T extends AbstractEventReturn> T notifyEvent(IBaseEvent event, ListenersContainer container)
	{
		return notifyEvent(event, container, null);
	}
	
	/**
	 * Notifies container then global listeners, returning the result or {@code null} on error.
	 * @param <T> The {@code AbstractEventReturn} subtype returned.
	 * @param event The event to process.
	 * @param container The {@code ListenersContainer} holding the listeners.
	 * @param callbackClass The expected return type.
	 * @return The result, or {@code null} if an error occurs.
	 */
	public <T extends AbstractEventReturn> T notifyEvent(IBaseEvent event, ListenersContainer container, Class<T> callbackClass)
	{
		try
		{
			return notifyEventImpl(event, container, callbackClass);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Couldn't notify event " + event.getClass().getSimpleName(), e);
		}
		
		return null;
	}
	
	/**
	 * Asynchronously notifies all listeners in the container.
	 * @param event The event to process.
	 * @param container The {@code ListenersContainer} to notify.
	 * @throws NullPointerException If {@code event} is {@code null}.
	 */
	public void notifyEventAsync(IBaseEvent event, ListenersContainer container)
	{
		if (event == null)
		{
			throw new NullPointerException("Event cannot be null!");
		}
		
		ThreadPool.execute(() -> notifyEventToSingleContainer(event, container, null));
	}
	
	/**
	 * Asynchronously notifies all listeners in the containers.
	 * @param event The event to process.
	 * @param containers The {@code ListenersContainer} objects to notify.
	 * @throws NullPointerException If {@code event} is {@code null}.
	 */
	public void notifyEventAsync(IBaseEvent event, ListenersContainer... containers)
	{
		if (event == null)
		{
			throw new NullPointerException("Event cannot be null!");
		}
		
		ThreadPool.execute(() -> notifyEventToMultipleContainers(event, containers, null));
	}
	
	/**
	 * Notifies container then global listeners, returning the result or {@code null} on error.
	 * @param <T> The {@code AbstractEventReturn} subtype returned.
	 * @param event The event to process.
	 * @param container The {@code ListenersContainer} to notify.
	 * @param callbackClass The expected return type.
	 * @return The result, or {@code null}.
	 * @throws NullPointerException If {@code event} is {@code null}.
	 */
	private <T extends AbstractEventReturn> T notifyEventToSingleContainer(IBaseEvent event, ListenersContainer container, Class<T> callbackClass)
	{
		if (event == null)
		{
			throw new NullPointerException("Event cannot be null!");
		}
		
		try
		{
			// Local listener container.
			T callback = null;
			if (container != null)
			{
				callback = notifyToListeners(container.getListeners(event.getType()), event, callbackClass, callback);
			}
			
			// Global listener container.
			if ((callback == null) || !callback.abort())
			{
				callback = notifyToListeners(Containers.Global().getListeners(event.getType()), event, callbackClass, callback);
			}
			
			return callback;
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Couldn't notify event " + event.getClass().getSimpleName(), e);
		}
		
		return null;
	}
	
	/**
	 * Notifies the containers then global listeners, returning the result or {@code null} on error.
	 * @param <T> The {@code AbstractEventReturn} subtype returned.
	 * @param event The event to process.
	 * @param containers The {@code ListenersContainer} array to notify.
	 * @param callbackClass The expected return type.
	 * @return The result, or {@code null}.
	 * @throws NullPointerException If {@code event} is {@code null}.
	 */
	private <T extends AbstractEventReturn> T notifyEventToMultipleContainers(IBaseEvent event, ListenersContainer[] containers, Class<T> callbackClass)
	{
		if (event == null)
		{
			throw new NullPointerException("Event cannot be null!");
		}
		
		try
		{
			T callback = null;
			if (containers != null)
			{
				// Local listener containers.
				for (ListenersContainer container : containers)
				{
					if ((callback == null) || !callback.abort())
					{
						callback = notifyToListeners(container.getListeners(event.getType()), event, callbackClass, callback);
					}
				}
			}
			
			// Global listener container.
			if ((callback == null) || !callback.abort())
			{
				callback = notifyToListeners(Containers.Global().getListeners(event.getType()), event, callbackClass, callback);
			}
			
			return callback;
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Couldn't notify event " + event.getClass().getSimpleName(), e);
		}
		
		return null;
	}
	
	/**
	 * Notifies container then global listeners, continuing only while not aborted.
	 * @param <T> The {@code AbstractEventReturn} subtype returned.
	 * @param event The event to process.
	 * @param container The {@code ListenersContainer} to check first.
	 * @param callbackClass The expected return type.
	 * @return The result as a {@code T}.
	 * @throws NullPointerException If {@code event} is {@code null}.
	 */
	private <T extends AbstractEventReturn> T notifyEventImpl(IBaseEvent event, ListenersContainer container, Class<T> callbackClass)
	{
		if (event == null)
		{
			throw new NullPointerException("Event cannot be null!");
		}
		
		// Local listener container.
		T callback = null;
		if (container != null)
		{
			callback = notifyToListeners(container.getListeners(event.getType()), event, callbackClass, callback);
		}
		
		// Global listener container.
		if ((callback == null) || !callback.abort())
		{
			callback = notifyToListeners(Containers.Global().getListeners(event.getType()), event, callbackClass, callback);
		}
		
		return callback;
	}
	
	/**
	 * Notifies a collection of listeners, handling return overrides and abort signals while isolating per-listener exceptions.
	 * @param <T> The {@code AbstractEventReturn} subtype returned.
	 * @param listeners The {@code AbstractEventListener} objects to notify.
	 * @param event The event being processed.
	 * @param returnBackClass The {@code Class} used to cast the result.
	 * @param callbackValue The initial return value.
	 * @return The final result as a {@code T}.
	 */
	private <T extends AbstractEventReturn> T notifyToListeners(Collection<AbstractEventListener> listeners, IBaseEvent event, Class<T> returnBackClass, T callbackValue)
	{
		T callback = callbackValue;
		for (AbstractEventListener listener : listeners)
		{
			try
			{
				final T rb = listener.executeEvent(event, returnBackClass);
				if (rb == null)
				{
					continue;
				}
				
				if ((callback == null) || rb.override()) // Let's check if this listener wants to override previous return object or we simply don't have one.
				{
					callback = rb;
				}
				else if (rb.abort()) // This listener wants to abort the notification to others.
				{
					break;
				}
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Exception during notification of event: " + event.getClass().getSimpleName() + " listener: " + listener.getClass().getSimpleName(), e);
			}
		}
		
		return callback;
	}
	
	/**
	 * Retrieves the singleton {@code EventDispatcher} instance.
	 * @return The single {@code EventDispatcher} instance.
	 */
	public static EventDispatcher getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final EventDispatcher INSTANCE = new EventDispatcher();
	}
}
