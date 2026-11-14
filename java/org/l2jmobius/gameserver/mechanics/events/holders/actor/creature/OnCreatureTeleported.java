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
 * This class represents an event triggered when a {@code Creature} is teleported.<br>
 * It holds the necessary data for handling teleportation logic within the game mechanics.
 * @author UnAfraid
 */
public class OnCreatureTeleported implements IBaseEvent
{
	private final Creature _creature;
	
	/**
	 * Creates a new {@code OnCreatureTeleported} event instance.<br>
	 * This method initializes the event with a specific {@code Creature}.
	 * @param creature The {@code Creature} that was teleported.
	 */
	public OnCreatureTeleported(Creature creature)
	{
		_creature = creature;
	}
	
	/**
	 * Retrieves the {@code Creature} associated with this event.<br>
	 * This method returns the entity that was teleported.
	 * @return the {@code Creature} object.
	 */
	public Creature getCreature()
	{
		return _creature;
	}
	
	/**
	 * Retrieves the type of this event.<br>
	 * This method returns a constant value from {@code EventType}.
	 * @return The {@code EventType} associated with this event.
	 */
	@Override
	public EventType getType()
	{
		return EventType.ON_CREATURE_TELEPORTED;
	}
}
