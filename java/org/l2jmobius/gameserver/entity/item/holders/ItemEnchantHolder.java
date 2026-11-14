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
package org.l2jmobius.gameserver.entity.item.holders;

import java.util.Objects;

import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class manages the enchantment data for an item.<br>
 * It provides a way to store and access specific enchantments associated with an {@code ItemHolder}.
 * @author Index, Mobius
 */
public class ItemEnchantHolder extends ItemHolder
{
	private final int _enchantLevel;
	private final int _index;
	
	/**
	 * Creates a new {@code ItemEnchantHolder} instance.<br>
	 * This constructor initializes the holder with a specific {@code StatSet}.<br>
	 * The enchantment level is set to {@code 0} by default.
	 * @param set The {@code StatSet} to associate with this item holder.
	 */
	public ItemEnchantHolder(StatSet set)
	{
		super(set);
		_enchantLevel = 0;
		_index = 0;
	}
	
	/**
	 * Creates a new {@code ItemEnchantHolder} with a default enchant level.<br>
	 * This constructor initializes the item with an ID and a quantity.<br>
	 * The enchantment level is set to {@code 0}.
	 * @param id The unique identifier for the item.
	 * @param count The number of items in the stack.
	 */
	public ItemEnchantHolder(int id, long count)
	{
		super(id, count);
		_enchantLevel = 0;
		_index = 0;
	}
	
	/**
	 * Creates a new {@code ItemEnchantHolder} instance.<br>
	 * This constructor initializes the item with an ID, quantity, and enchantment level.
	 * @param id The unique identifier for the item.
	 * @param count The number of items in the stack.
	 * @param enchantLevel The current level of the enchantment applied to the item.
	 */
	public ItemEnchantHolder(int id, long count, int enchantLevel)
	{
		super(id, count);
		_enchantLevel = enchantLevel;
		_index = 0;
	}
	
	/**
	 * Creates a new {@code ItemEnchantHolder} with specific enchantment details.<br>
	 * This constructor initializes the item ID, quantity, level, and index.
	 * @param id The unique identifier for the item.
	 * @param count The total amount of the item.
	 * @param enchantLevel The current level of the enchantment.
	 * @param index The specific position or index of the enchantment.
	 */
	public ItemEnchantHolder(int id, long count, int enchantLevel, int index)
	{
		super(id, count);
		_enchantLevel = enchantLevel;
		_index = index;
	}
	
	/**
	 * Retrieves the current enchantment level of the item.<br>
	 * This value is stored in the {@code _enchantLevel} field.
	 * @return The integer value representing the enchantment level.
	 */
	public int getEnchantLevel()
	{
		return _enchantLevel;
	}
	
	/**
	 * Retrieves the unique index of this item holder.<br>
	 * This value is used to identify the specific position in a collection.
	 * @return The current {@code int} index.
	 */
	public int getIndex()
	{
		return _index;
	}
	
	/**
	 * Compares this object with another object for equality.<br>
	 * It checks if the objects are of the same type and have identical values.
	 * @param obj The object to compare this instance against.
	 * @return {@code true} if the objects are equal, {@code false} otherwise.
	 */
	@Override
	public boolean equals(Object obj)
	{
		if (!(obj instanceof ItemEnchantHolder objInstance))
		{
			return false;
		}
		else if (obj == this)
		{
			return true;
		}
		
		return (getId() == objInstance.getId()) && ((getCount() == objInstance.getCount()) && (_enchantLevel == objInstance.getEnchantLevel()));
	}
	
	/**
	 * Returns a hash code value for this object.<br>
	 * This value is used to identify the object in hash-based collections.<br>
	 * It is calculated using the item ID, count, and enchant level.
	 * @return The integer hash code of this object.
	 */
	@Override
	public int hashCode()
	{
		return Objects.hash(getId(), getCount(), _enchantLevel);
	}
	
	/**
	 * Returns a string representation of this {@code ItemEnchantHolder}.<br>
	 * It includes the class name, ID, count, and enchantment level.
	 * @return A formatted string describing the current state.
	 */
	@Override
	public String toString()
	{
		return "[" + getClass().getSimpleName() + "] ID: " + getId() + ", count: " + getCount() + ", enchant level: " + _enchantLevel;
	}
}
