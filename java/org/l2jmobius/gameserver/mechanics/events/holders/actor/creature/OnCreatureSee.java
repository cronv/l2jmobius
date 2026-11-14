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
package org.l2jmobius.gameserver.mechanics.events.holders.actor.creature;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;

/**
 * This class represents an event triggered when a {@code Creature} perceives another entity.<br>
 * It holds the necessary data for handling "see" mechanics between actors.
 * @author UnAfraid
 */
public class OnCreatureSee implements IBaseEvent
{
	private final Creature _creature;
	private final Creature _seen;
	
	/**
	 * Creates a new {@code OnCreatureSee} event.<br>
	 * This method initializes the event with two specific creatures.
	 * @param creature The creature that is performing the action.
	 * @param seen The creature that has been spotted.
	 */
	public OnCreatureSee(Creature creature, Creature seen)
	{
		_creature = creature;
		_seen = seen;
	}
	
	/**
	 * Retrieves the {@code Creature} associated with this event.<br>
	 * This method returns the primary actor involved in the "see" action.
	 * @return the {@code Creature} object.
	 */
	public Creature getCreature()
	{
		return _creature;
	}
	
	/**
	 * Returns the {@code Creature} that was spotted.<br>
	 * This is the target of the see event.
	 * @return the {@code Creature} that was seen
	 */
	public Creature getSeen()
	{
		return _seen;
	}
	
	/**
	 * Returns the type of this event.<br>
	 * This method identifies the specific {@code EventType} associated with this instance.
	 * @return The {@code EventType} for this event.
	 */
	@Override
	public EventType getType()
	{
		return EventType.ON_CREATURE_SEE;
	}
}
