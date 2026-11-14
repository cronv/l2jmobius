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
 * This class represents an event triggered instantly when one {@code Creature} kills another.<br>
 * It is used to handle logic that occurs specifically at the moment of a creature's death.
 * @author UnAfraid
 */
public class OnCreatureKilled implements IBaseEvent
{
	private final Creature _attacker;
	private final Creature _target;
	
	/**
	 * This constructor initializes a new {@code OnCreatureKilled} event.<br>
	 * It stores the creature that performed the kill and the creature that died.
	 * @param attacker The {@code Creature} that dealt the killing blow.
	 * @param target The {@code Creature} that was killed.
	 */
	public OnCreatureKilled(Creature attacker, Creature target)
	{
		_attacker = attacker;
		_target = target;
	}
	
	/**
	 * Retrieves the {@code Creature} that initiated the attack.<br>
	 * This method returns the attacker associated with this event.
	 * @return the {@code Creature} who performed the kill
	 */
	public Creature getAttacker()
	{
		return _attacker;
	}
	
	/**
	 * Retrieves the creature that was killed.<br>
	 * This method returns the {@code target} of the event.
	 * @return the {@code Creature} that died
	 */
	public Creature getTarget()
	{
		return _target;
	}
	
	/**
	 * Retrieves the type of this event.<br>
	 * This method returns the specific {@code EventType} associated with this instance.
	 * @return The {@code EventType} for this event.
	 */
	@Override
	public EventType getType()
	{
		return EventType.ON_CREATURE_KILLED;
	}
}
