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
 * This class checks if a specific item exists in an actor's inventory.<br>
 * It is used to verify the presence of an {@code ItemTemplate} within a designated slot.
 * @author mkizub
 */
public class ConditionSlotItemId extends ConditionInventory
{
	private final int _itemId;
	private final int _enchantLevel;
	
	/**
	 * Creates a new {@code ConditionSlotItemId} instance.<br>
	 * This constructor initializes the required slot, item ID, and enchantment level.
	 * @param slot The inventory slot index to check.
	 * @param itemId The unique identifier of the item.
	 * @param enchantLevel The required enchantment level for the item.
	 */
	public ConditionSlotItemId(int slot, int itemId, int enchantLevel)
	{
		super(slot);
		_itemId = itemId;
		_enchantLevel = enchantLevel;
	}
	
	/**
	 * Checks if the effector meets specific inventory requirements.<br>
	 * It verifies if a player is wearing an item in a specific slot.<br>
	 * The check ensures the item ID and enchant level match the required values.
	 * @param effector The {@code Creature} performing the action.
	 * @param effected The {@code Creature} receiving the action.
	 * @param skill The {@code Skill} being used.
	 * @param item The {@code ItemTemplate} associated with the action.
	 * @return {@code true} if the condition is met, otherwise {@code false}.
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
			return _itemId == 0;
		}
		
		return (itemSlot.getId() == _itemId) && (itemSlot.getEnchantLevel() >= _enchantLevel);
	}
}
