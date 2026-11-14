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
package org.l2jmobius.gameserver.mechanics.events.listeners;

import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.ListenersContainer;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;
import org.l2jmobius.gameserver.mechanics.events.returns.AbstractEventReturn;

/**
 * This class provides a mechanism for dynamically attaching callbacks to any method operation.<br>
 * It supports methods both with and without return objects using annotations.<br>
 * {@code AnnotationEventListener} allows for flexible event handling across the game server.
 * @author UnAfraid
 */
public class AnnotationEventListener extends AbstractEventListener
{
	private static final Logger LOGGER = Logger.getLogger(AnnotationEventListener.class.getName());
	private final Method _callback;
	
	/**
	 * Creates a new {@code AnnotationEventListener} instance.<br>
	 * This constructor registers the listener with a specific container and event type.<br>
	 * It sets the method to be executed and its execution priority.
	 * @param container The {@code ListenersContainer} where this listener will be stored.
	 * @param type The {@code EventType} that triggers this listener.
	 * @param callback The {@code Method} to be called when the event occurs.
	 * @param owner The object instance that owns the callback method.
	 * @param priority The numerical order in which this listener should be executed.
	 */
	public AnnotationEventListener(ListenersContainer container, EventType type, Method callback, Object owner, int priority)
	{
		super(container, type, owner);
		_callback = callback;
		setPriority(priority);
	}
	
	/**
	 * Executes the callback method associated with an event.<br>
	 * It handles the invocation and casting of the return value.<br>
	 * If an error occurs during execution, it logs a warning and returns {@code null}.
	 * @param event The {@code IBaseEvent} instance to process.
	 * @param returnBackClass The {@code Class} type expected for the return value.
	 * @return The result of the callback cast to {@code R}, or {@code null} if an error occurs.
	 */
	@Override
	public <R extends AbstractEventReturn> R executeEvent(IBaseEvent event, Class<R> returnBackClass)
	{
		try
		{
			final Object result = _callback.invoke(getOwner(), event);
			if (_callback.getReturnType() == returnBackClass)
			{
				return returnBackClass.cast(result);
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Error while invoking " + _callback.getName() + " on " + getOwner(), e);
		}
		
		return null;
	}
}
