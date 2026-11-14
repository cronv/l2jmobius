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

import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.ListenersContainer;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;
import org.l2jmobius.gameserver.mechanics.events.returns.AbstractEventReturn;

/**
 * This class provides a mechanism for handling events that require a return value.<br>
 * It allows for callback operations where the result can be captured as an {@code AbstractEventReturn} object.
 * @author UnAfraid
 */
public class FunctionEventListener extends AbstractEventListener
{
	private static final Logger LOGGER = Logger.getLogger(FunctionEventListener.class.getName());
	private final Function<IBaseEvent, ? extends AbstractEventReturn> _callback;
	
	/**
	 * Creates a new {@code FunctionEventListener} instance.<br>
	 * This constructor registers the listener with a specific container and event type.<br>
	 * It also assigns the logic to be executed when an event occurs.
	 * @param container The {@code ListenersContainer} where this listener will be stored.
	 * @param type The {@code EventType} that this listener should respond to.
	 * @param callback A {@code Function} that defines the action to take and the result to return.
	 * @param owner The object that owns this listener instance.
	 */
	@SuppressWarnings("unchecked")
	public FunctionEventListener(ListenersContainer container, EventType type, Function<? extends IBaseEvent, ? extends AbstractEventReturn> callback, Object owner)
	{
		super(container, type, owner);
		_callback = (Function<IBaseEvent, ? extends AbstractEventReturn>) callback;
	}
	
	/**
	 * Executes the callback associated with a specific event.<br>
	 * It casts the result to the requested return type.<br>
	 * If an error occurs during execution, it logs a warning and returns {@code null}.
	 * @param event The {@code IBaseEvent} instance to process.
	 * @param returnBackClass The {@code Class} of the expected return type.
	 * @return The result of the callback cast to {@code R}, or {@code null} if an error occurs.
	 */
	@Override
	public <R extends AbstractEventReturn> R executeEvent(IBaseEvent event, Class<R> returnBackClass)
	{
		try
		{
			return returnBackClass.cast(_callback.apply(event));
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Error while invoking " + event + " on " + getOwner(), e);
		}
		
		return null;
	}
}
