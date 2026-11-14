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

/**
 * This class manages items that have a limited drop quantity.<br>
 * It ensures that specific loot limits are respected during the spawning process.
 * @author Mobius
 */
public class LimitedDropHolder
{
	private final int _itemId;
	private final int _minDropAmount;
	private final int _maxDropAmount;
	private final int _minPlayerLevel;
	private final int _maxPlayerLevel;
	private final int _dailyLimit;
	private final double _chance;
	
	/**
	 * Creates a new {@code LimitedDropHolder} instance.<br>
	 * This constructor initializes all drop constraints and probabilities.
	 * @param itemId The unique identifier for the item.
	 * @param minDropAmount The minimum quantity of the item that can drop.
	 * @param maxDropAmount The maximum quantity of the item that can drop.
	 * @param minPlayerLevel The minimum level required for a player to receive this drop.
	 * @param maxPlayerLevel The maximum level allowed for a player to receive this drop.
	 * @param dailyLimit The maximum number of times this item can drop per day.
	 * @param chance The probability percentage for the item to drop.
	 */
	public LimitedDropHolder(int itemId, int minDropAmount, int maxDropAmount, int minPlayerLevel, int maxPlayerLevel, int dailyLimit, double chance)
	{
		_itemId = itemId;
		_minDropAmount = minDropAmount;
		_maxDropAmount = maxDropAmount;
		_minPlayerLevel = minPlayerLevel;
		_maxPlayerLevel = maxPlayerLevel;
		_dailyLimit = dailyLimit;
		_chance = chance;
	}
	
	/**
	 * Retrieves the unique identifier for the item.<br>
	 * This value is used to identify which item is being handled by this {@code LimitedDropHolder}.
	 * @return The {@code int} ID of the item.
	 */
	public int getItemId()
	{
		return _itemId;
	}
	
	/**
	 * Returns the minimum amount of an item that can drop.<br>
	 * This value is used to calculate the quantity for {@code LimitedDropHolder}.
	 * @return The minimum drop amount as an {@code int}.
	 */
	public int getMinDropAmount()
	{
		return _minDropAmount;
	}
	
	/**
	 * Returns the maximum amount of an item that can drop.<br>
	 * This value is retrieved from the internal {@code _maxDropAmount} field.
	 * @return The maximum drop quantity as an {@code int}.
	 */
	public int getMaxDropAmount()
	{
		return _maxDropAmount;
	}
	
	/**
	 * Returns the minimum level required for a player to receive this drop.<br>
	 * This value is used to filter eligible players.
	 * @return the minimum player level as an {@code int}.
	 */
	public int getMinPlayerLevel()
	{
		return _minPlayerLevel;
	}
	
	/**
	 * Retrieves the maximum player level allowed for this drop.<br>
	 * This value is used to check if a player meets the requirements.
	 * @return The maximum player level as an {@code int}.
	 */
	public int getMaxPlayerLevel()
	{
		return _maxPlayerLevel;
	}
	
	/**
	 * Retrieves the maximum number of items allowed to drop per day.<br>
	 * This value is used to restrict item distribution.
	 * @return The daily limit as an {@code int}.
	 */
	public int getDailyLimit()
	{
		return _dailyLimit;
	}
	
	/**
	 * Retrieves the drop chance for this item.<br>
	 * The value is returned as a {@code double}.
	 * @return the current drop chance.
	 */
	public double getChance()
	{
		return _chance;
	}
}
