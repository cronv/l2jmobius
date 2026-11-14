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
package org.l2jmobius.gameserver.entity.cubic.conditions;

import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.cubic.Cubic;

/**
 * This class handles conditions related to the health of a {@code Creature}.<br>
 * It allows for checking and evaluating life points within the {@code Cubic} system.
 * @author UnAfraid
 */
public class HealthCondition implements ICubicCondition
{
	private final int _min;
	private final int _max;
	
	/**
	 * Creates a new {@code HealthCondition} object.<br>
	 * This sets the range for health values that satisfy the condition.
	 * @param min The minimum health value required.
	 * @param max The maximum health value allowed.
	 */
	public HealthCondition(int min, int max)
	{
		_min = min;
		_max = max;
	}
	
	/**
	 * Checks if the target's health percentage is within a specific range.<br>
	 * This method returns {@code true} if the target is a {@code Creature} or a door and its current HP percent is between {@code _min} and {@code _max}.<br>
	 * It returns {@code false} if the target does not meet these criteria.
	 * @param cubic The {@code Cubic} object associated with this condition.
	 * @param owner The {@code Creature} that owns the {@code Cubic}.
	 * @param target The {@code WorldObject} being checked for health requirements.
	 * @return {@code true} if the target's health is within the required range, otherwise {@code false}.
	 */
	@Override
	public boolean test(Cubic cubic, Creature owner, WorldObject target)
	{
		if (target.isCreature() || target.isDoor())
		{
			final double hpPer = (target.isDoor() ? target.asDoor() : target.asCreature()).getCurrentHpPercent();
			return (hpPer > _min) && (hpPer < _max);
		}
		
		return false;
	}
	
	/**
	 * Returns a string representation of this condition.<br>
	 * It includes the class name and the min and max values.
	 * @return A string describing the {@code HealthCondition}.
	 */
	@Override
	public String toString()
	{
		return getClass().getSimpleName() + " min: " + _min + " max: " + _max;
	}
}
