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
package org.l2jmobius.gameserver.data.holders;

/**
 * This class acts as a data holder for {@code RelicSummonCategory} information.<br>
 * It provides a structured way to store and access category-specific data for relic summons.
 * @author Brado
 */
public class RelicSummonCategoryHolder
{
	private final int _categoryId;
	private final int _priceId;
	private final long _priceAmount;
	private final int _summonCount;
	
	/**
	 * Creates a new instance of {@code RelicSummonCategoryHolder}.<br>
	 * This constructor initializes the category, price, and count data.
	 * @param categoryId The unique identifier for the relic category.
	 * @param priceId The unique identifier for the currency type.
	 * @param priceAmount The total cost required for the summon.
	 * @param summonCount The number of summons granted per action.
	 */
	public RelicSummonCategoryHolder(int categoryId, int priceId, long priceAmount, int summonCount)
	{
		_categoryId = categoryId;
		_priceId = priceId;
		_priceAmount = priceAmount;
		_summonCount = summonCount;
	}
	
	/**
	 * Retrieves the unique identifier for the category.<br>
	 * This value is used to group different types of relic summons.
	 * @return The {@code int} ID of the category.
	 */
	public int getCategoryId()
	{
		return _categoryId;
	}
	
	/**
	 * Retrieves the unique identifier for the price.<br>
	 * This value is used to link the category to a specific price entry.
	 * @return The {@code int} price identifier.
	 */
	public int getPriceId()
	{
		return _priceId;
	}
	
	/**
	 * Returns the total amount for the price.<br>
	 * This value is retrieved from the {@code _priceAmount} field.
	 * @return The total price count as a {@code long}.
	 */
	public long getPriceCount()
	{
		return _priceAmount;
	}
	
	/**
	 * Retrieves the total number of summons for this category.<br>
	 * This value is stored in the {@code _summonCount} field.
	 * @return The integer count of summons.
	 */
	public int getSummonCount()
	{
		return _summonCount;
	}
}
