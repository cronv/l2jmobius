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
 * This class calculates a stat value by subtracting the result of a function from an initial value.<br>
 * The calculation is only performed if the specified {@code Condition} is met.
 * @author Zoey76
 */
public class FuncSub extends AbstractFunction
{
	/**
	 * Creates a new {@code FuncSub} instance.<br>
	 * This function subtracts a specific value from the initial stat value.<br>
	 * The calculation only occurs if the provided condition is met.
	 * @param stat The {@code Stat} type this function applies to.
	 * @param order The priority order of this function.
	 * @param owner The object that owns this function.
	 * @param value The amount to subtract from the initial value.
	 * @param applyCond The {@code Condition} required for this function to trigger.
	 */
	public FuncSub(Stat stat, int order, Object owner, double value, Condition applyCond)
	{
		super(stat, order, owner, value, applyCond);
	}
	
	/**
	 * Calculates the final value of a stat based on a subtraction function.<br>
	 * It checks if the {@code Condition} is met before applying the change.<br>
	 * If the condition fails, it returns the original value.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the action.
	 * @param skill The skill being used.
	 * @param initVal The initial value to start from.
	 * @return The resulting double value after calculation.
	 */
	@Override
	public double calc(Creature effector, Creature effected, Skill skill, double initVal)
	{
		final Condition applyCond = getApplyCond();
		if ((applyCond == null) || applyCond.test(effector, effected, skill))
		{
			return initVal - getValue();
		}
		
		return initVal;
	}
}
