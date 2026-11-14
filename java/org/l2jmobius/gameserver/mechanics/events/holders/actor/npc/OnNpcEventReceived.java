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
package org.l2jmobius.gameserver.mechanics.events.holders.actor.npc;

import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;

/**
 * This class represents an event triggered when an {@code Npc} receives a specific game event.<br>
 * It serves as a data holder for processing logic related to NPC interactions.
 * @author UnAfraid
 */
public class OnNpcEventReceived implements IBaseEvent
{
	private final String _eventName;
	private final Npc _sender;
	private final Npc _receiver;
	private final WorldObject _reference;
	
	/**
	 * Creates a new instance of an NPC event.<br>
	 * This constructor initializes the event with all required data.
	 * @param eventName The name of the specific event being triggered.
	 * @param sender The {@code Npc} that initiated the event.
	 * @param receiver The {@code Npc} that is receiving the event.
	 * @param reference The {@code WorldObject} associated with this event.
	 */
	public OnNpcEventReceived(String eventName, Npc sender, Npc receiver, WorldObject reference)
	{
		_eventName = eventName;
		_sender = sender;
		_receiver = receiver;
		_reference = reference;
	}
	
	/**
	 * Retrieves the name of the event.<br>
	 * This value is stored in the {@code _eventName} field.
	 * @return The name of the event as a {@code String}.
	 */
	public String getEventName()
	{
		return _eventName;
	}
	
	/**
	 * Retrieves the {@code Npc} that sent this event.<br>
	 * This method returns the sender object stored in the event holder.
	 * @return The {@code Npc} who initiated the event.
	 */
	public Npc getSender()
	{
		return _sender;
	}
	
	/**
	 * Retrieves the {@code Npc} that received this event.<br>
	 * This method returns the target of the event action.
	 * @return the {@code Npc} receiver
	 */
	public Npc getReceiver()
	{
		return _receiver;
	}
	
	/**
	 * Retrieves the {@code WorldObject} associated with this event.<br>
	 * This object serves as a reference for the event's context.
	 * @return The {@code WorldObject} reference.
	 */
	public WorldObject getReference()
	{
		return _reference;
	}
	
	/**
	 * Retrieves the type of this event.<br>
	 * It returns a constant value from {@code EventType}.
	 * @return the {@code EventType} associated with this event.
	 */
	@Override
	public EventType getType()
	{
		return EventType.ON_NPC_EVENT_RECEIVED;
	}
}
