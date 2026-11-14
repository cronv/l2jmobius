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
 * This class represents an instantly executed event that occurs when a {@code Creature} is killed by another {@code Creature}.<br>
 * It serves as a data holder for processing logic related to creature-on-creature deaths.
 * @author UnAfraid
 */
public class OnCreatureDeath implements IBaseEvent
{
	private final Creature _attacker;
	private final Creature _target;
	
	/**
	 * This constructor initializes a new {@code OnCreatureDeath} event.<br>
	 * It sets the creature that performed the kill and the creature that died.
	 * @param attacker The {@code Creature} that dealt the killing blow.
	 * @param target The {@code Creature} that was killed.
	 */
	public OnCreatureDeath(Creature attacker, Creature target)
	{
		_attacker = attacker;
		_target = target;
	}
	
	/**
	 * Retrieves the {@code Creature} that initiated the attack.<br>
	 * This method returns the attacker associated with this death event.
	 * @return the {@code Creature} who attacked the target.
	 */
	public Creature getAttacker()
	{
		return _attacker;
	}
	
	/**
	 * Retrieves the creature that was killed.<br>
	 * This method returns the {@code target} of the death event.
	 * @return the {@code Creature} that died
	 */
	public Creature getTarget()
	{
		return _target;
	}
	
	/**
	 * Retrieves the type of this event.<br>
	 * This method returns a constant value from {@code EventType}.
	 * @return The {@code EventType} associated with this event.
	 */
	@Override
	public EventType getType()
	{
		return EventType.ON_CREATURE_DEATH;
	}
}
