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
 * Checks if a target entity is within a specific distance from a source point.<br>
 * This {@code RangeCondition} is used to validate spatial proximity in cubic logic.
 * @author Sdw
 */
public class RangeCondition implements ICubicCondition
{
	private final int _range;
	
	/**
	 * Creates a new {@code RangeCondition} with a specific distance.<br>
	 * This condition checks if an object is within the defined range.
	 * @param range The maximum distance allowed for the condition.
	 */
	public RangeCondition(int range)
	{
		_range = range;
	}
	
	/**
	 * Checks if the {@code target} is within a specific range of the {@code owner}.<br>
	 * This method calculates the 2D distance between the two entities.<br>
	 * It returns {@code true} if the distance is less than or equal to the defined range.
	 * @param cubic The current {@code Cubic} instance being processed.
	 * @param owner The {@code Creature} that owns the effect.
	 * @param target The {@code WorldObject} being checked for proximity.
	 * @return {@code true} if the target is within range, otherwise {@code false}.
	 */
	@Override
	public boolean test(Cubic cubic, Creature owner, WorldObject target)
	{
		return owner.calculateDistance2D(target) <= _range;
	}
}
