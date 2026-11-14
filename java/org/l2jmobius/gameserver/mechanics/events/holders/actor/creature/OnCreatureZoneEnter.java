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
import org.l2jmobius.gameserver.entity.zone.ZoneType;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;

/**
 * This class represents an event triggered when a {@code Creature} enters a new zone.<br>
 * It holds the necessary data for handling logic related to zone transitions.
 * @author UnAfraid
 */
public class OnCreatureZoneEnter implements IBaseEvent
{
	private final Creature _creature;
	private final ZoneType _zone;
	
	/**
	 * Creates a new {@code OnCreatureZoneEnter} event.<br>
	 * This method initializes the event with a specific creature and zone type.
	 * @param creature The {@code Creature} that entered the zone.
	 * @param zone The {@code ZoneType} of the area entered.
	 */
	public OnCreatureZoneEnter(Creature creature, ZoneType zone)
	{
		_creature = creature;
		_zone = zone;
	}
	
	/**
	 * Retrieves the {@code Creature} associated with this event.<br>
	 * This method returns the entity that triggered the zone entry.
	 * @return the {@code Creature} object.
	 */
	public Creature getCreature()
	{
		return _creature;
	}
	
	/**
	 * Retrieves the {@code ZoneType} associated with this event.<br>
	 * This identifies which zone the creature entered.
	 * @return the {@code ZoneType} of the current event.
	 */
	public ZoneType getZone()
	{
		return _zone;
	}
	
	/**
	 * Retrieves the type of this event.<br>
	 * This method returns a constant value from {@code EventType}.
	 * @return the {@code EventType} associated with this event.
	 */
	@Override
	public EventType getType()
	{
		return EventType.ON_CREATURE_ZONE_ENTER;
	}
}
