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
 * This class holds event data for when a {@code Creature}'s health changes.<br>
 * It is used to trigger mechanics or effects based on HP fluctuations.
 * @author UnAfraid
 */
public class OnCreatureHpChange implements IBaseEvent
{
	private final Creature _creature;
	private final double _newHp;
	private final double _oldHp;
	
	/**
	 * Creates a new {@code OnCreatureHpChange} event.<br>
	 * This event is triggered when a {@code Creature} changes its health points.
	 * @param creature The {@code Creature} that experienced the health change.
	 * @param oldHp The health value before the change.
	 * @param newHp The health value after the change.
	 */
	public OnCreatureHpChange(Creature creature, double oldHp, double newHp)
	{
		_creature = creature;
		_oldHp = oldHp;
		_newHp = newHp;
	}
	
	/**
	 * Retrieves the {@code Creature} associated with this event.<br>
	 * This method returns the entity that experienced a health change.
	 * @return the {@code Creature} object.
	 */
	public Creature getCreature()
	{
		return _creature;
	}
	
	/**
	 * Retrieves the health value of the creature before it changed.<br>
	 * This value is stored during the creation of the {@code OnCreatureHpChange} event.
	 * @return The previous health amount as a {@code double}.
	 */
	public double getOldHp()
	{
		return _oldHp;
	}
	
	/**
	 * Returns the new health value of the creature.<br>
	 * This value is used during a {@code OnCreatureHpChange} event.
	 * @return The current health as a {@code double}.
	 */
	public double getNewHp()
	{
		return _newHp;
	}
	
	/**
	 * Retrieves the type of this event.<br>
	 * It returns the {@code EventType} associated with creature health changes.
	 * @return the {@code EventType} for this specific event.
	 */
	@Override
	public EventType getType()
	{
		return EventType.ON_CREATURE_HP_CHANGE;
	}
}
