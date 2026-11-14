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
 * This class serves as a placeholder for an event listener.<br>
 * It provides a callback operation that takes no parameters and returns {@code null}.
 * @author UnAfraid
 */
public class DummyEventListener extends AbstractEventListener
{
	/**
	 * Creates a new instance of {@code DummyEventListener}.<br>
	 * This constructor initializes the listener with the required container and event type.<br>
	 * It allows for basic event handling without specific parameters.
	 * @param container The {@code ListenersContainer} where this listener will be registered.
	 * @param type The {@code EventType} that this listener is designed to handle.
	 * @param owner The object that owns this listener instance.
	 */
	public DummyEventListener(ListenersContainer container, EventType type, Object owner)
	{
		super(container, type, owner);
	}
	
	/**
	 * Executes a specific game event.<br>
	 * This method processes the provided {@code IBaseEvent}.<br>
	 * It returns an object of the specified return type.
	 * @param event The {@code IBaseEvent} to be processed.
	 * @param returnBackClass The {@code Class} of the expected return type.
	 * @return The result of the event execution as a type of {@code R}.
	 */
	@Override
	public <R extends AbstractEventReturn> R executeEvent(IBaseEvent event, Class<R> returnBackClass)
	{
		return null;
	}
}
