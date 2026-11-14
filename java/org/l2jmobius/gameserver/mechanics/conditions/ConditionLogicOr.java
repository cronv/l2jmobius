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
 * This class represents a logical OR condition for game mechanics.<br>
 * It evaluates to {@code true} if any of its child conditions are met.<br>
 * Use this to group multiple requirements where only one needs to be satisfied.
 * @author mkizub
 */
public class ConditionLogicOr extends Condition
{
	private static Condition[] _emptyConditions = new Condition[0];
	public Condition[] conditions = _emptyConditions;
	
	/**
	 * Adds a new {@code Condition} to the list of conditions.<br>
	 * This method ignores the request if the provided {@code condition} is {@code null}.<br>
	 * It also links the current listener to the new {@code Condition}.
	 * @param condition The {@code Condition} to be added.
	 */
	public void add(Condition condition)
	{
		if (condition == null)
		{
			return;
		}
		
		if (getListener() != null)
		{
			condition.setListener(this);
		}
		
		final int len = conditions.length;
		final Condition[] tmp = new Condition[len + 1];
		System.arraycopy(conditions, 0, tmp, 0, len);
		tmp[len] = condition;
		conditions = tmp;
	}
	
	/**
	 * Sets the listener for this condition.<br>
	 * This method also updates the listeners for all nested conditions.<br>
	 * If {@code null} is passed, it removes the listener from all components.
	 * @param listener The {@code ConditionListener} to assign.
	 */
	@Override
	void setListener(ConditionListener listener)
	{
		if (listener != null)
		{
			for (Condition c : conditions)
			{
				c.setListener(this);
			}
		}
		else
		{
			for (Condition c : conditions)
			{
				c.setListener(null);
			}
		}
		
		super.setListener(listener);
	}
	
	/**
	 * Checks if any of the registered conditions are met.<br>
	 * It iterates through all conditions in the list.<br>
	 * If at least one condition returns {@code true}, the method returns {@code true}.<br>
	 * Otherwise, it returns {@code false}.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the action.
	 * @param skill The skill being used.
	 * @param item The item template involved in the action.
	 * @return {@code true} if any condition is satisfied, otherwise {@code false}.
	 */
	@Override
	public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item)
	{
		for (Condition c : conditions)
		{
			if (c.test(effector, effected, skill, item))
			{
				return true;
			}
		}
		
		return false;
	}
}
