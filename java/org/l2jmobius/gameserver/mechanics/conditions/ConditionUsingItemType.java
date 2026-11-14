/*
 * Copyright (c) 2013 L2jMobius
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.l2jmobius.gameserver.mechanics.conditions;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.entity.item.enums.BodyPart;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.item.type.ArmorType;
import org.l2jmobius.gameserver.entity.itemcontainer.Inventory;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

/**
 * Checks if a creature is currently using an item of a specific type.<br>
 * This condition validates the {@code ArmorType} or other relevant categories associated with an item.
 * @author mkizub, naker
 */
public class ConditionUsingItemType extends Condition
{
	private final boolean _armor;
	private final int _mask;
	
	/**
	 * Creates a new {@code ConditionUsingItemType} instance.<br>
	 * This constructor initializes the condition based on a bitmask.<br>
	 * It determines if the item is armor by checking specific bits.
	 * @param mask The bitmask used to define the allowed item types.
	 */
	public ConditionUsingItemType(int mask)
	{
		_mask = mask;
		_armor = (_mask & (ArmorType.MAGIC.mask() | ArmorType.LIGHT.mask() | ArmorType.HEAVY.mask())) != 0;
	}
	
	/**
	 * Checks if the effector meets the required item type conditions.<br>
	 * This method validates the equipment or inventory of the {@code Creature}.<br>
	 * It considers specific armor slots and masks based on the internal configuration.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the action.
	 * @param skill The skill being used.
	 * @param item The template of the item involved.
	 * @return {@code true} if the conditions are met, {@code false} otherwise.
	 */
	@Override
	public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item)
	{
		if (effector == null)
		{
			return false;
		}
		
		if (!effector.isPlayer())
		{
			return !_armor && ((_mask & effector.getAttackType().mask()) != 0);
		}
		
		final Inventory inv = effector.getInventory();
		if (_armor)
		{
			final Item chest = inv.getPaperdollItem(Inventory.PAPERDOLL_CHEST);
			final Item legs = inv.getPaperdollItem(Inventory.PAPERDOLL_LEGS);
			if ((chest == null) && (legs == null))
			{
				return (ArmorType.NONE.mask() & _mask) == ArmorType.NONE.mask();
			}
			
			if (chest != null)
			{
				if (legs == null)
				{
					final BodyPart chestBodyPart = chest.getTemplate().getBodyPart();
					if (chestBodyPart == BodyPart.FULL_ARMOR)
					{
						final int chestMask = chest.getTemplate().getItemMask();
						return (_mask & chestMask) != 0;
					}
					
					return (ArmorType.NONE.mask() & _mask) == ArmorType.NONE.mask();
				}
				
				final int chestMask = chest.getTemplate().getItemMask();
				final int legMask = legs.getTemplate().getItemMask();
				if (chestMask == legMask)
				{
					return (_mask & chestMask) != 0;
				}
				
				return (ArmorType.NONE.mask() & _mask) == ArmorType.NONE.mask();
			}
		}
		
		return (_mask & inv.getWearedMask()) != 0;
	}
}
