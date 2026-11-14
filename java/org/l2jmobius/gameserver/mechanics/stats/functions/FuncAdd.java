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
package org.l2jmobius.gameserver.mechanics.stats.functions;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.mechanics.conditions.Condition;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.stats.Stat;

/**
 * This class calculates a stat value by adding a functional modifier to the base value.<br>
 * It only applies this addition if all associated {@code Condition} objects are met.
 * @author Zoey76
 */
public class FuncAdd extends AbstractFunction
{
	/**
	 * Adds a specific value to a {@code Stat}.<br>
	 * This calculation only occurs if the provided {@code Condition} is met.
	 * @param stat The {@code Stat} type that this function modifies.
	 * @param order The priority order of this calculation.
	 * @param owner The object that owns this function.
	 * @param value The numeric amount to add to the stat.
	 * @param applyCond The {@code Condition} required for this addition to trigger.
	 */
	public FuncAdd(Stat stat, int order, Object owner, double value, Condition applyCond)
	{
		super(stat, order, owner, value, applyCond);
	}
	
	/**
	 * Calculates the final value of a stat based on a condition.<br>
	 * It adds the function value to {@code initVal} if the condition is met.<br>
	 * If the condition fails, it returns {@code initVal} unchanged.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the action.
	 * @param skill The skill being used.
	 * @param initVal The base value before applying this function.
	 * @return The final calculated double value.
	 */
	@Override
	public double calc(Creature effector, Creature effected, Skill skill, double initVal)
	{
		final Condition applyCond = getApplyCond();
		if ((applyCond == null) || applyCond.test(effector, effected, skill))
		{
			return initVal + getValue();
		}
		
		return initVal;
	}
}
