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
 * This event is triggered instantly when a {@code Creature} fails to hit another {@code Creature}.<br>
 * It allows for custom logic to be executed specifically during an attack miss.
 * @author Zealar
 */
public class OnCreatureAttackAvoid implements IBaseEvent
{
	private Creature _attacker;
	private Creature _target;
	private boolean _damageOverTime;
	
	/**
	 * Creates a new instance of the {@code OnCreatureAttackAvoid} event.<br>
	 * This event is triggered when an attack from one {@code Creature} misses another.<br>
	 * Use this to handle logic for missed attacks in the game.
	 */
	public OnCreatureAttackAvoid()
	{
	}
	
	/**
	 * Retrieves the {@code Creature} that initiated the attack.<br>
	 * This method returns the attacker associated with this event.
	 * @return the {@code Creature} who performed the attack.
	 */
	public Creature getAttacker()
	{
		return _attacker;
	}
	
	/**
	 * Sets the {@code Creature} that initiated the attack.<br>
	 * This method updates the internal {@code _attacker} field.
	 * @param attacker The {@code Creature} performing the attack.
	 */
	public synchronized void setAttacker(Creature attacker)
	{
		_attacker = attacker;
	}
	
	/**
	 * Retrieves the creature that was targeted by the attack.<br>
	 * Returns the {@code Creature} object associated with this event.
	 * @return The target {@code Creature}.
	 */
	public Creature getTarget()
	{
		return _target;
	}
	
	/**
	 * Sets the {@code Creature} that is being targeted by this event.<br>
	 * This method updates the internal {@code _target} field.
	 * @param target The {@code Creature} to set as the target.
	 */
	public synchronized void setTarget(Creature target)
	{
		_target = target;
	}
	
	/**
	 * Checks if the event represents a damage over time effect.<br>
	 * Returns {@code true} if it is a damage over time effect.<br>
	 * Returns {@code false} otherwise.
	 * @return The status of the damage over time flag.
	 */
	public boolean isDamageOverTime()
	{
		return _damageOverTime;
	}
	
	/**
	 * Sets whether the attack event deals damage over time.<br>
	 * This updates the {@code _damageOverTime} field.
	 * @param damageOverTime The boolean value to set for damage over time.
	 */
	public synchronized void setDamageOverTime(boolean damageOverTime)
	{
		_damageOverTime = damageOverTime;
	}
	
	/**
	 * Retrieves the type of this event.<br>
	 * It returns the {@code EventType} associated with an attack being avoided.
	 * @return the {@code EventType} for this specific event.
	 */
	@Override
	public EventType getType()
	{
		return EventType.ON_CREATURE_ATTACK_AVOID;
	}
}
