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
 * This class provides a way to return a specific function value based on whether certain conditions are met.<br>
 * It evaluates the requirements defined in {@code Condition} before applying the result.
 * @author Zoey76
 */
public class FuncSet extends AbstractFunction
{
	/**
	 * Creates a new {@code FuncSet} instance.<br>
	 * This function sets a specific value for a stat based on a condition.<br>
	 * It is used to modify statistics during calculations.
	 * @param stat The {@code Stat} type this function applies to.
	 * @param order The priority order of the calculation.
	 * @param owner The object that owns this function.
	 * @param value The numeric value to be applied.
	 * @param applyCond The {@code Condition} that must be met for this value to apply.
	 */
	public FuncSet(Stat stat, int order, Object owner, double value, Condition applyCond)
	{
		super(stat, order, owner, value, applyCond);
	}
	
	/**
	 * Calculates the final value based on specific conditions.<br>
	 * It checks if the {@code Condition} is met for the given action.<br>
	 * If the condition fails, it returns the original value.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the action.
	 * @param skill The skill being used.
	 * @param initVal The initial value to return if conditions are not met.
	 * @return The calculated result as a {@code double}.
	 */
	@Override
	public double calc(Creature effector, Creature effected, Skill skill, double initVal)
	{
		final Condition applyCond = getApplyCond();
		if ((applyCond == null) || applyCond.test(effector, effected, skill))
		{
			return getValue();
		}
		
		return initVal;
	}
}
