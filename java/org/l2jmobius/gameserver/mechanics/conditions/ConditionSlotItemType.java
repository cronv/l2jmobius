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
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

/**
 * Represents a specific type of item required within an inventory slot to satisfy a condition.<br>
 * This class is used to define requirements for {@code Skill} mechanics based on held items.
 * @author mkizub
 */
public class ConditionSlotItemType extends ConditionInventory
{
	private final int _mask;
	
	/**
	 * Creates a new instance of {@code ConditionSlotItemType}.<br>
	 * This constructor initializes the slot and the bitmask.
	 * @param slot The index of the inventory slot.
	 * @param mask The bitmask used to filter items.
	 */
	public ConditionSlotItemType(int slot, int mask)
	{
		super(slot);
		_mask = mask;
	}
	
	/**
	 * Checks if the effector meets specific item requirements.<br>
	 * This method verifies that the {@code effector} is a player.<br>
	 * It also checks if an item exists in the correct slot.<br>
	 * Finally, it validates the item mask against the required bitmask.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the action.
	 * @param skill The skill being used.
	 * @param item The template of the item involved.
	 * @return {@code true} if all conditions are met, otherwise {@code false}.
	 */
	@Override
	public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item)
	{
		if ((effector == null) || !effector.isPlayer())
		{
			return false;
		}
		
		final Item itemSlot = effector.getInventory().getPaperdollItem(_slot);
		if (itemSlot == null)
		{
			return false;
		}
		
		return (itemSlot.getTemplate().getItemMask() & _mask) != 0;
	}
}
