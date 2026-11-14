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
 * This condition checks if the target of a mechanic is {@code null}.<br>
 * It ensures that no specific entity or object is targeted for the action.
 * @author mkizub
 */
public class ConditionTargetNone extends Condition
{
	/**
	 * Creates a new instance of {@code ConditionTargetNone}.<br>
	 * This condition is used when no specific target is required.
	 */
	public ConditionTargetNone()
	{
		//
	}
	
	/**
	 * Checks if the target of an action is missing.<br>
	 * This method returns {@code true} if the {@code effected} creature is {@code null}.<br>
	 * It is used to validate conditions where no target should be present.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the action.
	 * @param skill The skill being used.
	 * @param item The item template involved in the action.
	 * @return {@code true} if the target is null, otherwise {@code false}.
	 */
	@Override
	public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item)
	{
		return effected == null;
	}
}
