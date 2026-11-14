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

import java.util.HashSet;
import java.util.Set;

import org.l2jmobius.gameserver.entity.item.ItemTemplate;

/**
 * Represents an item that modifies the success rate of enchanting.<br>
 * This class handles the logic for applying specific enchantment bonuses to players or items.
 * @author UnAfraid, Mobius
 */
public class EnchantRateItem
{
	private final String _name;
	private final Set<Integer> _items = new HashSet<>();
	private long _slot;
	private Boolean _isMagicWeapon = null;
	
	/**
	 * Creates a new {@code EnchantRateItem} instance.<br>
	 * This constructor initializes the object with a specific name.
	 * @param name The name of the enchantment rate item.
	 */
	public EnchantRateItem(String name)
	{
		_name = name;
	}
	
	/**
	 * Retrieves the name of the {@code EnchantRateItem}.<br>
	 * This value is used to identify the item in the system.
	 * @return The name of the item as a {@code String}.
	 */
	public String getName()
	{
		return _name;
	}
	
	/**
	 * Adds a new item ID to the internal collection.<br>
	 * This method updates the set of items associated with this {@code EnchantRateItem}.
	 * @param id The unique identifier of the item to add.
	 */
	public void addItemId(int id)
	{
		_items.add(id);
	}
	
	/**
	 * Adds a specific bitmask to the current {@code _slot} value.<br>
	 * This method uses a bitwise OR operation to combine the new value.
	 * @param slot The {@code long} value representing the bitmask to add.
	 */
	public void addSlot(long slot)
	{
		_slot |= slot;
	}
	
	/**
	 * Sets whether the item is considered a magic weapon.<br>
	 * This updates the {@code _isMagicWeapon} field.
	 * @param magicWeapon The value to set for the magic weapon status.
	 */
	public void setMagicWeapon(boolean magicWeapon)
	{
		_isMagicWeapon = Boolean.valueOf(magicWeapon);
	}
	
	/**
	 * Checks if an {@code ItemTemplate} is valid for this enchantment.<br>
	 * It verifies the item ID, body part slot, and magic weapon status.
	 * @param item The {@code ItemTemplate} to check.
	 * @return {@code true} if the item meets all requirements, otherwise {@code false}.
	 */
	public boolean validate(ItemTemplate item)
	{
		if (!_items.isEmpty() && !_items.contains(item.getId()))
		{
			return false;
		}
		else if ((_slot != 0) && ((item.getBodyPart().getMask() & _slot) == 0))
		{
			return false;
		}
		else if ((_isMagicWeapon != null) && (item.isMagicWeapon() != _isMagicWeapon.booleanValue()))
		{
			return false;
		}
		
		return true;
	}
}
