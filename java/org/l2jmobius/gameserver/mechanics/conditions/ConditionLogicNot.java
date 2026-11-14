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
package org.l2jmobius.gameserver.mechanics.conditions;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

/**
 * This class represents a logical NOT operation for conditions.<br>
 * It returns {@code false} if the underlying condition is met, and {@code true} otherwise.
 * @author mkizub
 */
public class ConditionLogicNot extends Condition
{
	private final Condition _condition;
	
	/**
	 * Creates a new {@code ConditionLogicNot} instance.<br>
	 * This class negates the result of the provided {@code condition}.<br>
	 * If the original condition is true, this one returns false.
	 * @param condition The {@code Condition} to be negated.
	 */
	public ConditionLogicNot(Condition condition)
	{
		_condition = condition;
		if (getListener() != null)
		{
			_condition.setListener(this);
		}
	}
	
	/**
	 * Sets the listener for this condition.<br>
	 * This method updates the {@code ConditionListener} for both this object and its internal condition.
	 * @param listener The {@code ConditionListener} to set.
	 */
	@Override
	void setListener(ConditionListener listener)
	{
		if (listener != null)
		{
			_condition.setListener(this);
		}
		else
		{
			_condition.setListener(null);
		}
		
		super.setListener(listener);
	}
	
	/**
	 * Checks if the nested condition is false.<br>
	 * This method returns {@code true} only if the underlying condition fails.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the action.
	 * @param skill The skill being used.
	 * @param item The item involved in the action.
	 * @return {@code true} if the condition is not met, otherwise {@code false}.
	 */
	@Override
	public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item)
	{
		return !_condition.test(effector, effected, skill, item);
	}
}
