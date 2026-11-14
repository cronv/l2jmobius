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

import java.util.Set;

import org.l2jmobius.gameserver.data.enums.CategoryType;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

/**
 * Represents a specific category type for conditions.<br>
 * This class defines how different types of conditions are grouped and identified within the game mechanics.
 * @author Adry_85
 */
public class ConditionCategoryType extends Condition
{
	private final Set<CategoryType> _categoryTypes;
	
	/**
	 * Creates a new {@code ConditionCategoryType} instance.<br>
	 * This constructor initializes the internal set of categories.
	 * @param categoryTypes The set of {@code CategoryType} values to include.
	 */
	public ConditionCategoryType(Set<CategoryType> categoryTypes)
	{
		_categoryTypes = categoryTypes;
	}
	
	/**
	 * Checks if the {@code effector} belongs to any of the required categories.<br>
	 * This method validates the condition based on the {@code _categoryTypes} set.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the action.
	 * @param skill The skill being used.
	 * @param item The item template involved in the action.
	 * @return {@code true} if the {@code effector} matches a category, otherwise {@code false}.
	 */
	@Override
	public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item)
	{
		for (CategoryType type : _categoryTypes)
		{
			if (effector.isInCategory(type))
			{
				return true;
			}
		}
		
		return false;
	}
}
