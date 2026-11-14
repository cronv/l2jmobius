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
 * Serves as the base class for all event listeners in the mechanics system.<br>
 * It provides a foundation for handling different {@code EventType} occurrences.<br>
 * Subclasses should implement specific logic to respond to game events.
 * @author UnAfraid
 */
public abstract class AbstractEventListener implements Comparable<AbstractEventListener>
{
	private int _priority = 0;
	private final ListenersContainer _container;
	private final EventType _type;
	private final Object _owner;
	
	/**
	 * Creates a new instance of an event listener.<br>
	 * This constructor initializes the required fields for the listener.
	 * @param container The {@code ListenersContainer} where this listener will be stored.
	 * @param type The {@code EventType} that this listener handles.
	 * @param owner The {@code Object} that owns this specific listener.
	 */
	public AbstractEventListener(ListenersContainer container, EventType type, Object owner)
	{
		_container = container;
		_type = type;
		_owner = owner;
	}
	
	/**
	 * Retrieves the container associated with this listener.<br>
	 * This is used to manage the collection of event listeners.
	 * @return the {@code ListenersContainer} instance.
	 */
	public ListenersContainer getContainer()
	{
		return _container;
	}
	
	/**
	 * Retrieves the type of event this listener handles.<br>
	 * This is used to determine which events should trigger this listener.
	 * @return the {@code EventType} associated with this listener.
	 */
	public EventType getType()
	{
		return _type;
	}
	
	/**
	 * Retrieves the object that owns this listener.<br>
	 * This is useful for identifying which entity or system registered the event.
	 * @return The {@code Object} owner of this listener.
	 */
	public Object getOwner()
	{
		return _owner;
	}
	
	/**
	 * Retrieves the current priority of this listener.<br>
	 * Higher values indicate a higher execution order.
	 * @return The integer value representing the priority.
	 */
	public int getPriority()
	{
		return _priority;
	}
	
	/**
	 * Sets the execution order for this listener.<br>
	 * A higher {@code priority} value means it runs earlier.
	 * @param priority The new priority level to assign.
	 */
	public void setPriority(int priority)
	{
		_priority = priority;
	}
	
	/**
	 * Method invoked by EventDispatcher that will use the callback.
	 * @param <R>
	 * @param event
	 * @param returnBackClass
	 * @return
	 */
	public abstract <R extends AbstractEventReturn> R executeEvent(IBaseEvent event, Class<R> returnBackClass);
	
	/**
	 * Removes this listener from the {@code ListenersContainer}.<br>
	 * This method stops the current instance from receiving any more events.
	 */
	public void unregisterMe()
	{
		_container.removeListener(this);
	}
	
	/**
	 * Compares this listener to another {@code AbstractEventListener}.<br>
	 * This method uses the priority value of the listeners.<br>
	 * A higher priority results in a lower comparison value.
	 * @param o The other {@code AbstractEventListener} to compare against.
	 * @return The integer result of the comparison between priorities.
	 */
	@Override
	public int compareTo(AbstractEventListener o)
	{
		return Integer.compare(o.getPriority(), getPriority());
	}
}
