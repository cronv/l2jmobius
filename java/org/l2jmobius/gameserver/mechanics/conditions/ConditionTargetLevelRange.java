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
 * Checks if the target's level falls within a specific range.<br>
 * This {@code Condition} is used to validate requirements based on character levels.
 * @author UnAfraid
 */
public class ConditionTargetLevelRange extends Condition
{
	private final int[] _levels;
	
	/**
	 * Creates a new {@code ConditionTargetLevelRange} object.<br>
	 * This defines the allowed level range for a target.
	 * @param levels an array of integers representing the minimum and maximum levels
	 */
	public ConditionTargetLevelRange(int[] levels)
	{
		_levels = levels;
	}
	
	/**
	 * Checks if the target creature's level falls within a specific range.<br>
	 * This method returns {@code false} if the target is {@code null}.<br>
	 * It compares the level of the {@code effected} creature against the defined levels.
	 * @param effector The creature performing the action.
	 * @param effected The creature being targeted by the action.
	 * @param skill The skill being used.
	 * @param item The item template associated with the action.
	 * @return {@code true} if the level is within range, otherwise {@code false}.
	 */
	@Override
	public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item)
	{
		if (effected == null)
		{
			return false;
		}
		
		final int level = effected.getLevel();
		return (level >= _levels[0]) && (level <= _levels[1]);
	}
}
