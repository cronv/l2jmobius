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

import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class holds the data for random rewards granted from a limited shop craft.<br>
 * It manages the reward configuration and its associated properties.
 * @author Norvox
 */
public class LimitShopRandomCraftReward
{
	private final int _itemId;
	private final AtomicInteger _count;
	private final int _rewardIndex;
	
	/**
	 * Creates a new {@code LimitShopRandomCraftReward} object.<br>
	 * This constructor initializes the item data for a random craft reward.
	 * @param itemId The unique identifier for the item.
	 * @param count The initial quantity of the item.
	 * @param rewardIndex The specific index assigned to this reward.
	 */
	public LimitShopRandomCraftReward(int itemId, int count, int rewardIndex)
	{
		_itemId = itemId;
		_count = new AtomicInteger(count);
		_rewardIndex = rewardIndex;
	}
	
	/**
	 * Retrieves the unique identifier for the item.<br>
	 * This value is used to identify which item is associated with this reward.
	 * @return The {@code int} ID of the item.
	 */
	public int getItemId()
	{
		return _itemId;
	}
	
	/**
	 * Retrieves the current count of the reward.<br>
	 * This value is stored in an {@code AtomicInteger}.
	 * @return the {@code AtomicInteger} containing the count.
	 */
	public AtomicInteger getCount()
	{
		return _count;
	}
	
	/**
	 * Retrieves the index of the reward.<br>
	 * This value identifies which specific reward is assigned to this object.
	 * @return The {@code int} representing the reward index.
	 */
	public int getRewardIndex()
	{
		return _rewardIndex;
	}
}
