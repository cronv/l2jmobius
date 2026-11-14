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

/**
 * This class manages items that provide restoration effects to entities.<br>
 * It handles the logic for applying healing or mana recovery when these items are used.
 * @author Mobius
 */
public class RestorationItemHolder
{
	private final int _id;
	private final long _count;
	private final int _minEnchant;
	private final int _maxEnchant;
	
	/**
	 * Creates a new instance of {@code RestorationItemHolder}.<br>
	 * This constructor initializes the item properties.
	 * @param id The unique identifier for the item.
	 * @param count The total quantity of the item.
	 * @param minEnchant The minimum enchantment level allowed.
	 * @param maxEnchant The maximum enchantment level allowed.
	 */
	public RestorationItemHolder(int id, long count, int minEnchant, int maxEnchant)
	{
		_id = id;
		_count = count;
		_minEnchant = minEnchant;
		_maxEnchant = maxEnchant;
	}
	
	/**
	 * Retrieves the unique identifier for this restoration item.<br>
	 * This value is used to identify the specific item type.
	 * @return The {@code int} ID of the item.
	 */
	public int getId()
	{
		return _id;
	}
	
	/**
	 * Retrieves the total number of items held.<br>
	 * This value represents the quantity of the item in the {@code RestorationItemHolder}.
	 * @return The current count as a {@code long}.
	 */
	public long getCount()
	{
		return _count;
	}
	
	/**
	 * Retrieves the minimum enchantment level for this item.<br>
	 * This value is stored in the {@code _minEnchant} field.
	 * @return The minimum enchantment level as an {@code int}.
	 */
	public int getMinEnchant()
	{
		return _minEnchant;
	}
	
	/**
	 * Retrieves the maximum enchantment level for this item.<br>
	 * This value is stored in the {@code _maxEnchant} field.
	 * @return The maximum enchantment level as an {@code int}.
	 */
	public int getMaxEnchant()
	{
		return _maxEnchant;
	}
}
