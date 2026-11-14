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

import java.util.function.Consumer;

import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.ListenersContainer;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;
import org.l2jmobius.gameserver.mechanics.events.returns.AbstractEventReturn;

/**
 * This class handles event listeners that perform a callback operation.<br>
 * It is used when an action needs to be executed without returning any value.<br>
 * It provides a way to process {@code IBaseEvent} objects using a {@code Consumer}.
 * @author UnAfraid
 */
public class ConsumerEventListener extends AbstractEventListener
{
	private final Consumer<IBaseEvent> _callback;
	
	/**
	 * Creates a new {@code ConsumerEventListener} instance.<br>
	 * This constructor registers a callback for a specific event type.<br>
	 * It allows the system to execute logic without returning a value.
	 * @param container The {@code ListenersContainer} where this listener will be stored.
	 * @param type The {@code EventType} that this listener should respond to.
	 * @param callback The {@code Consumer} function to execute when the event occurs.
	 * @param owner The object that owns this listener instance.
	 */
	@SuppressWarnings("unchecked")
	public ConsumerEventListener(ListenersContainer container, EventType type, Consumer<? extends IBaseEvent> callback, Object owner)
	{
		super(container, type, owner);
		_callback = (Consumer<IBaseEvent>) callback;
	}
	
	/**
	 * Executes the logic associated with a specific event.<br>
	 * This method triggers the internal callback for the provided {@code IBaseEvent}.<br>
	 * It returns {@code null} regardless of the operation outcome.
	 * @param event The {@code IBaseEvent} instance to be processed.
	 * @param returnBackClass The {@code Class} type expected as a return value.
	 * @return Always returns {@code null}.
	 */
	@Override
	public <R extends AbstractEventReturn> R executeEvent(IBaseEvent event, Class<R> returnBackClass)
	{
		_callback.accept(event);
		return null;
	}
}
