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
 * Represents a product that can be extracted from an item.<br>
 * This class defines the data required for extraction processes within the game.
 * @author JIV, Mobius
 */
public class ExtractableProduct
{
	private final int _id;
	private final long _min;
	private final long _max;
	private final int _chance;
	private final int _minEnchant;
	private final int _maxEnchant;
	
	/**
	 * Creates a new {@code ExtractableProduct} instance.<br>
	 * This constructor initializes the product with specific loot properties.
	 * @param id The unique identifier for the item.
	 * @param min The minimum quantity of the item.
	 * @param max The maximum quantity of the item.
	 * @param chance The probability of the item appearing as a percentage multiplied by {@code 1000}.
	 * @param minEnchant The minimum enchantment level for the item.
	 * @param maxEnchant The maximum enchantment level for the item.
	 */
	public ExtractableProduct(int id, long min, long max, double chance, int minEnchant, int maxEnchant)
	{
		_id = id;
		_min = min;
		_max = max;
		_chance = (int) (chance * 1000);
		_minEnchant = minEnchant;
		_maxEnchant = maxEnchant;
	}
	
	/**
	 * Retrieves the unique identifier for this {@code ExtractableProduct}.<br>
	 * This value is used to identify the specific product in the system.
	 * @return The integer ID of the product.
	 */
	public int getId()
	{
		return _id;
	}
	
	/**
	 * Retrieves the minimum amount for this product.<br>
	 * This value is used to determine the lower bound of a range.
	 * @return the minimum count as a {@code long}
	 */
	public long getMin()
	{
		return _min;
	}
	
	/**
	 * Retrieves the maximum value for this product.<br>
	 * This value is used to determine the upper limit of an extraction.
	 * @return The maximum value as a {@code long}.
	 */
	public long getMax()
	{
		return _max;
	}
	
	/**
	 * Retrieves the success chance for this product.<br>
	 * This value represents the probability of extraction.
	 * @return The current chance as an {@code int}.
	 */
	public int getChance()
	{
		return _chance;
	}
	
	/**
	 * Retrieves the minimum enchantment level.<br>
	 * This value is used to determine the lowest possible enchant for an extracted product.
	 * @return The minimum enchantment level as an {@code int}.
	 */
	public int getMinEnchant()
	{
		return _minEnchant;
	}
	
	/**
	 * Retrieves the maximum enchantment level for this product.<br>
	 * This value represents the upper limit of the enchantment range.
	 * @return The maximum enchantment as an {@code int}.
	 */
	public int getMaxEnchant()
	{
		return _maxEnchant;
	}
}
