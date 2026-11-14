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
 * This class calculates a result by dividing an initial value by a function value.<br>
 * The calculation is only performed if the specified {@code Condition} is met.
 * @author Zoey76
 */
public class FuncDiv extends AbstractFunction
{
	/**
	 * Creates a new {@code FuncDiv} instance.<br>
	 * This function divides the initial value by a specific amount.<br>
	 * The calculation only occurs if the provided condition is met.
	 * @param stat The {@code Stat} type associated with this function.
	 * @param order The priority order of the calculation.
	 * @param owner The object that owns this function.
	 * @param value The divisor used in the calculation.
	 * @param applyCond The {@code Condition} required to trigger the division.
	 */
	public FuncDiv(Stat stat, int order, Object owner, double value, Condition applyCond)
	{
		super(stat, order, owner, value, applyCond);
	}
	
	/**
	 * Calculates the final value based on a division operation.<br>
	 * This method checks if the required conditions are met before applying the math.<br>
	 * If the condition fails or a division by zero occurs, it returns the original value.
	 * @param effector The {@code Creature} performing the action.
	 * @param effected The {@code Creature} receiving the action.
	 * @param skill The {@code Skill} being used.
	 * @param initVal The initial value to be processed.
	 * @return The result of the division or the original {@code initVal}.
	 */
	@Override
	public double calc(Creature effector, Creature effected, Skill skill, double initVal)
	{
		if ((getApplyCond() == null) || getApplyCond().test(effector, effected, skill))
		{
			try
			{
				return initVal / getValue();
			}
			catch (Exception e)
			{
				LOG.warning(FuncDiv.class.getSimpleName() + ": Division by zero: " + getValue() + "!");
			}
		}
		
		return initVal;
	}
}
