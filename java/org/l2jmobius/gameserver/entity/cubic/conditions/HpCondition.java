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
 * This class handles conditions related to the {@code hp} of a {@link org.l2jmobius.gameserver.entity.actor.Creature}.<br>
 * It allows for checking and evaluating health-based requirements within the {@code Cubic} system.
 * @author UnAfraid
 */
public class HpCondition implements ICubicCondition
{
	private final HpConditionType _type;
	private final int _hpPer;
	
	/**
	 * Creates a new {@code HpCondition} object.<br>
	 * This constructor initializes the condition with a specific type and health value.
	 * @param type The {@code HpConditionType} to be used for this condition.
	 * @param hpPer The amount of health points per unit for this condition.
	 */
	public HpCondition(HpConditionType type, int hpPer)
	{
		_type = type;
		_hpPer = hpPer;
	}
	
	/**
	 * Checks if the target meets a specific health percentage requirement.<br>
	 * This method evaluates whether the current HP of a {@code Creature} or door is greater than or less than a defined value.<br>
	 * It returns {@code false} if the target is not a valid entity.
	 * @param cubic The {@code Cubic} object being processed.
	 * @param owner The {@code Creature} that owns the condition.
	 * @param target The {@code WorldObject} to check against the health requirement.
	 * @return {@code true} if the health condition is met, otherwise {@code false}.
	 */
	@Override
	public boolean test(Cubic cubic, Creature owner, WorldObject target)
	{
		if (target.isCreature() || target.isDoor())
		{
			final double hpPer = (target.isDoor() ? target.asDoor() : target.asCreature()).getCurrentHpPercent();
			switch (_type)
			{
				case GREATER:
				{
					return hpPer > _hpPer;
				}
				case LESSER:
				{
					return hpPer < _hpPer;
				}
			}
		}
		
		return false;
	}
	
	/**
	 * Returns a human-readable string representation of this condition.<br>
	 * It includes the class name and the {@code _hpPer} value.
	 * @return A string describing the condition.
	 */
	@Override
	public String toString()
	{
		return getClass().getSimpleName() + " chance: " + _hpPer;
	}
	
	public enum HpConditionType
	{
		GREATER,
		LESSER;
	}
}
