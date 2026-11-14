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
package org.l2jmobius.gameserver.entity.item.enchant;

import org.l2jmobius.gameserver.entity.item.type.EtcItemType;
import org.l2jmobius.gameserver.entity.item.type.ItemType;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Represents an item that provides specific support for the enchantment system.<br>
 * This class handles logic for items used to enhance or modify enchantments.
 * @author UnAfraid, Mobius
 */
public class EnchantSupportItem extends AbstractEnchantItem
{
	private final boolean _isWeapon;
	private final boolean _isBlessed;
	private final boolean _isDown;
	private final boolean _isGiant;
	private final int _failDecrease;
	private final ItemType type;
	
	/**
	 * Creates a new {@code EnchantSupportItem} using the provided stats.<br>
	 * This constructor initializes the item type and its specific properties.<br>
	 * It determines if the item is a weapon, blessed, or giant based on its {@code ItemType}.
	 * @param set The {@code StatSet} containing the statistics for this item.
	 */
	public EnchantSupportItem(StatSet set)
	{
		super(set);
		type = getItem().getItemType();
		_failDecrease = set.getInt("failDecrease", 1);
		_isWeapon = (type == EtcItemType.ENCHT_ATTR_INC_PROP_ENCHT_WP) || (type == EtcItemType.ENCHT_WP_DOWN) || (type == EtcItemType.BLESSED_ENCHT_ATTR_INC_PROP_ENCHT_WP) || (type == EtcItemType.GIANT_ENCHT_ATTR_INC_PROP_ENCHT_WP) || (type == EtcItemType.BLESSED_GIANT_ENCHT_ATTR_INC_PROP_ENCHT_WP);
		_isBlessed = (type == EtcItemType.BLESSED_ENCHT_ATTR_INC_PROP_ENCHT_AM) || (type == EtcItemType.BLESSED_ENCHT_ATTR_INC_PROP_ENCHT_WP) || (type == EtcItemType.BLESSED_GIANT_ENCHT_ATTR_INC_PROP_ENCHT_AM) || (type == EtcItemType.BLESSED_GIANT_ENCHT_ATTR_INC_PROP_ENCHT_WP);
		_isDown = (type == EtcItemType.ENCHT_AM_DOWN) || (type == EtcItemType.ENCHT_WP_DOWN);
		_isGiant = (type == EtcItemType.GIANT_ENCHT_ATTR_INC_PROP_ENCHT_AM) || (type == EtcItemType.GIANT_ENCHT_ATTR_INC_PROP_ENCHT_WP) || (type == EtcItemType.BLESSED_GIANT_ENCHT_ATTR_INC_PROP_ENCHT_AM) || (type == EtcItemType.BLESSED_GIANT_ENCHT_ATTR_INC_PROP_ENCHT_WP);
	}
	
	/**
	 * Checks if the item is a weapon.<br>
	 * Returns {@code true} if it is a weapon.<br>
	 * Returns {@code false} otherwise.
	 * @return The weapon status of this item.
	 */
	@Override
	public boolean isWeapon()
	{
		return _isWeapon;
	}
	
	/**
	 * Checks if the item has the blessed property.<br>
	 * Returns {@code true} if it is blessed.<br>
	 * Returns {@code false} otherwise.
	 * @return The blessing status of the item.
	 */
	public boolean isBlessed()
	{
		return _isBlessed;
	}
	
	/**
	 * Checks if the support item has the down status.<br>
	 * This method returns {@code true} if the item is marked as a down type.<br>
	 * Otherwise, it returns {@code false}.
	 * @return {@code true} if the item is down, {@code false} otherwise.
	 */
	public boolean isDown()
	{
		return _isDown;
	}
	
	/**
	 * Checks if the item has the giant property.<br>
	 * Returns {@code true} if it is a giant item.<br>
	 * Returns {@code false} otherwise.
	 * @return The status of the giant property.
	 */
	public boolean isGiant()
	{
		return _isGiant;
	}
	
	/**
	 * @return how many enchant levels a down type support takes away when the enchant fails.
	 */
	public int getFailDecrease()
	{
		return _failDecrease;
	}
}
