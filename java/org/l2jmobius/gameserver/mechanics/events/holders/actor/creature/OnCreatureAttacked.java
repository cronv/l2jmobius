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
import org.l2jmobius.gameserver.mechanics.skill.Skill;

/**
 * This class represents an instantly executed event triggered when a {@code Creature} is attacked by another {@code Creature}.<br>
 * It provides the necessary context for handling combat interactions between two entities.
 * @author UnAfraid
 */
public class OnCreatureAttacked implements IBaseEvent
{
	private Creature _attacker;
	private Creature _target;
	private Skill _skill;
	
	/**
	 * Creates a new instance of the {@code OnCreatureAttacked} event.<br>
	 * This event is triggered when one {@code Creature} attacks another.
	 */
	public OnCreatureAttacked()
	{
	}
	
	/**
	 * Retrieves the {@code Creature} that initiated the attack.<br>
	 * This method returns the attacker associated with this event.
	 * @return the {@code Creature} who performed the attack, or {@code null} if no attacker exists.
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
	 * Retrieves the {@code Creature} that was attacked.<br>
	 * This method returns the target of the event.
	 * @return the {@code Creature} being targeted, or {@code null} if no target exists.
	 */
	public Creature getTarget()
	{
		return _target;
	}
	
	/**
	 * Sets the {@code Creature} that is being attacked.<br>
	 * This method updates the internal target of the event.
	 * @param target The {@code Creature} to set as the target.
	 */
	public synchronized void setTarget(Creature target)
	{
		_target = target;
	}
	
	/**
	 * Retrieves the {@code Skill} used during this attack event.<br>
	 * Returns the {@code Skill} object associated with the action.
	 * @return the {@code Skill} instance.
	 */
	public Skill getSkill()
	{
		return _skill;
	}
	
	/**
	 * Sets the {@code Skill} associated with this event.<br>
	 * This method is thread-safe because it is synchronized.
	 * @param skill The {@code Skill} to be assigned.
	 */
	public synchronized void setSkill(Skill skill)
	{
		_skill = skill;
	}
	
	/**
	 * Returns the type of this event.<br>
	 * This method identifies the specific {@code EventType} associated with the attack.
	 * @return The {@code EventType} for this event.
	 */
	@Override
	public EventType getType()
	{
		return EventType.ON_CREATURE_ATTACKED;
	}
}
