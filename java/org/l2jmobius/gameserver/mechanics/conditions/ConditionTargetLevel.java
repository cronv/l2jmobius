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
 * This class checks if a target entity meets a specific level requirement.<br>
 * It is used to validate the level of a {@code Creature} or other targets within mechanics.
 * @author mkizub
 */
public class ConditionTargetLevel extends Condition
{
	private final int _level;
	
	/**
	 * Creates a new {@code ConditionTargetLevel} instance.<br>
	 * This sets the required level for the condition.
	 * @param level The minimum level required to satisfy this condition.
	 */
	public ConditionTargetLevel(int level)
	{
		_level = level;
	}
	
	/**
	 * Checks if the target creature meets the required level.<br>
	 * It compares the level of the {@code effected} creature against the internal level requirement.<br>
	 * Returns {@code false} if the {@code effected} parameter is {@code null}.
	 * @param effector The creature performing the action.
	 * @param effected The creature being checked for the condition.
	 * @param skill The skill being used.
	 * @param item The item template involved in the action.
	 * @return {@code true} if the target level is met, otherwise {@code false}.
	 */
	@Override
	public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item)
	{
		if (effected == null)
		{
			return false;
		}
		
		return effected.getLevel() >= _level;
	}
}
