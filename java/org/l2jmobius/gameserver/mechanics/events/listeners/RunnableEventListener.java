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

import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.ListenersContainer;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;
import org.l2jmobius.gameserver.mechanics.events.returns.AbstractEventReturn;

/**
 * This class provides a simple way to register an event listener that performs a callback action.<br>
 * It is used for operations that do not require any input parameters or return objects.
 * @author UnAfraid
 */
public class RunnableEventListener extends AbstractEventListener
{
	private final Runnable _callback;
	
	/**
	 * Creates a new {@code RunnableEventListener}.<br>
	 * This listener executes a simple task when an event occurs.<br>
	 * It does not require any input parameters or return values.
	 * @param container The {@code ListenersContainer} where this listener will be stored.
	 * @param type The {@code EventType} that triggers this listener.
	 * @param callback The {@code Runnable} task to execute when the event is fired.
	 * @param owner The object that owns this listener instance.
	 */
	public RunnableEventListener(ListenersContainer container, EventType type, Runnable callback, Object owner)
	{
		super(container, type, owner);
		_callback = callback;
	}
	
	/**
	 * Executes the logic associated with a specific event.<br>
	 * This method runs the internal callback and returns a result.
	 * @param event The {@code IBaseEvent} instance to be processed.
	 * @param returnBackClass The {@code Class} type of the expected return object.
	 * @return The result of the execution, which is always {@code null}.
	 */
	@Override
	public <R extends AbstractEventReturn> R executeEvent(IBaseEvent event, Class<R> returnBackClass)
	{
		_callback.run();
		return null;
	}
}
