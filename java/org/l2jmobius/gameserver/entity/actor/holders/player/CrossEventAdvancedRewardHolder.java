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
package org.l2jmobius.gameserver.entity.actor.holders.player;

/**
 * This class manages advanced rewards for players participating in cross-server events.<br>
 * It stores specific data required to process these rewards within the {@code CrossEventAdvancedRewardHolder} context.
 * @author Smoto
 */
public class CrossEventAdvancedRewardHolder
{
	private final int _itemId;
	private final long _itemCount;
	private final int _itemChance;
	
	/**
	 * Creates a new instance of {@code CrossEventAdvancedRewardHolder}.<br>
	 * This constructor initializes the reward data for an event.<br>
	 * It converts the chance value into a scaled integer format.
	 * @param itemId The unique identifier for the item to be rewarded.
	 * @param itemCount The total number of items to give to the player.
	 * @param itemChance The probability of receiving the item as a double between 0.0 and 1.0.
	 */
	public CrossEventAdvancedRewardHolder(int itemId, int itemCount, double itemChance)
	{
		_itemId = itemId;
		_itemCount = itemCount;
		_itemChance = (int) (itemChance * 1000);
	}
	
	/**
	 * Retrieves the unique identifier for the reward item.<br>
	 * This value corresponds to the {@code itemId} provided during initialization.
	 * @return The {@code int} ID of the item.
	 */
	public int getItemId()
	{
		return _itemId;
	}
	
	/**
	 * Retrieves the total number of items for this reward.<br>
	 * This value is stored in the {@code _itemCount} field.
	 * @return The count of items as a {@code long}.
	 */
	public long getCount()
	{
		return _itemCount;
	}
	
	/**
	 * Retrieves the chance for a specific reward.<br>
	 * This value is used to determine if an item is granted.
	 * @return The chance as an {@code int}.
	 */
	public int getChance()
	{
		return _itemChance;
	}
}
