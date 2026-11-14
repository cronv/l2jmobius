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
package org.l2jmobius.gameserver.entity.actor.holders.npc;

import org.l2jmobius.gameserver.entity.actor.enums.npc.DropType;

/**
 * This class manages the loot drops associated with an {@code NPC}.<br>
 * It handles the logic for determining which items are dropped and their quantities.
 * @author Mobius
 */
public class DropHolder
{
	private final DropType _dropType;
	private final int _itemId;
	private final long _min;
	private final long _max;
	private final double _chance;
	
	/**
	 * Creates a new {@code DropHolder} instance.<br>
	 * This object stores the drop rules for an item.
	 * @param dropType The category of the drop.
	 * @param itemId The unique identifier for the item.
	 * @param min The minimum quantity of the item.
	 * @param max The maximum quantity of the item.
	 * @param chance The probability of the item dropping.
	 */
	public DropHolder(DropType dropType, int itemId, long min, long max, double chance)
	{
		_dropType = dropType;
		_itemId = itemId;
		_min = min;
		_max = max;
		_chance = chance;
	}
	
	/**
	 * Retrieves the drop type associated with this holder.<br>
	 * This value determines how the item is dropped in the game.
	 * @return the {@code DropType} of this holder.
	 */
	public DropType getDropType()
	{
		return _dropType;
	}
	
	/**
	 * Retrieves the unique identifier for the item.<br>
	 * This value is used to identify which item is dropped.
	 * @return The {@code int} ID of the item.
	 */
	public int getItemId()
	{
		return _itemId;
	}
	
	/**
	 * Retrieves the minimum value for this drop.<br>
	 * This value is used to determine the lower bound of a range.
	 * @return The minimum value as a {@code long}.
	 */
	public long getMin()
	{
		return _min;
	}
	
	/**
	 * Retrieves the maximum value for this drop.<br>
	 * This value is used to determine the upper limit of a range.
	 * @return The maximum {@code long} value.
	 */
	public long getMax()
	{
		return _max;
	}
	
	/**
	 * Returns the drop probability.<br>
	 * The value is represented as a {@code double}.
	 * @return The chance of this drop occurring.
	 */
	public double getChance()
	{
		return _chance;
	}
}
