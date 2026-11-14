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
 * Checks if a player's level falls within a specific range.<br>
 * This {@code Condition} is used to validate the character level of an actor.
 * @author Zoey76
 */
public class ConditionPlayerLevelRange extends Condition
{
	private final int[] _levels;
	
	/**
	 * Creates a new {@code ConditionPlayerLevelRange} object.<br>
	 * This condition checks if a player's level falls within a specific range.
	 * @param levels An array of integers representing the minimum and maximum levels.
	 */
	public ConditionPlayerLevelRange(int[] levels)
	{
		_levels = levels;
	}
	
	/**
	 * Checks if the {@code effector} level is within a specific range.<br>
	 * This method validates the level of the creature performing the action.
	 * @param effector The {@code Creature} performing the action.
	 * @param effected The {@code Creature} receiving the action.
	 * @param skill The {@code Skill} being used.
	 * @param item The {@code ItemTemplate} involved in the action.
	 * @return {@code true} if the level is within range, otherwise {@code false}.
	 */
	@Override
	public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item)
	{
		final int level = effector.getLevel();
		return (level >= _levels[0]) && (level <= _levels[1]);
	}
}
