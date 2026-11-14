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
package org.l2jmobius.gameserver.mechanics.fishing;

/**
 * Represents the result of a successful fishing action.<br>
 * This class stores data about the item caught during the fishing process.
 * @author Zarcos
 */
public class FishingCatch
{
	private final int _itemId;
	private final float _chance;
	private final float _multiplier;
	
	/**
	 * Creates a new {@code FishingCatch} object.<br>
	 * This constructor initializes the fishing loot properties.
	 * @param itemId The unique identifier for the item to be caught.
	 * @param chance The probability of catching this specific item.
	 * @param multiplier A value used to adjust the final catch calculation.
	 */
	public FishingCatch(int itemId, float chance, float multiplier)
	{
		_itemId = itemId;
		_chance = chance;
		_multiplier = multiplier;
	}
	
	/**
	 * Retrieves the unique identifier for the item.<br>
	 * This value is used to identify which item was caught.
	 * @return The {@code int} ID of the item.
	 */
	public int getItemId()
	{
		return _itemId;
	}
	
	/**
	 * Retrieves the success probability for this catch.<br>
	 * This value is used to determine if a fish is caught.
	 * @return The {@code float} chance value.
	 */
	public float getChance()
	{
		return _chance;
	}
	
	/**
	 * Retrieves the current multiplier for this catch.<br>
	 * This value is used to adjust the final result of a fishing action.
	 * @return The {@code float} multiplier value.
	 */
	public float getMultiplier()
	{
		return _multiplier;
	}
}
