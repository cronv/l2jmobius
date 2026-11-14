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
 * This class represents a condition based on an item ID.<br>
 * It is used to check if a specific {@code ItemTemplate} exists or meets certain criteria.
 * @author mkizub
 */
public class ConditionItemId extends Condition
{
	private final int _itemId;
	
	/**
	 * Creates a new instance of {@code ConditionItemId}.<br>
	 * This constructor sets the required item ID for the condition.
	 * @param itemId The unique identifier for the item.
	 */
	public ConditionItemId(int itemId)
	{
		_itemId = itemId;
	}
	
	/**
	 * Checks if the provided {@code ItemTemplate} matches a specific ID.<br>
	 * This method returns {@code true} if the item is not {@code null} and its ID matches the stored value.<br>
	 * Otherwise, it returns {@code false}.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the action.
	 * @param skill The skill being used.
	 * @param item The item template to check.
	 * @return {@code true} if the item ID matches, otherwise {@code false}.
	 */
	@Override
	public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item)
	{
		return (item != null) && (item.getId() == _itemId);
	}
}
