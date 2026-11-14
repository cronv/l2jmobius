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
package org.l2jmobius.gameserver.mechanics.undergroundColiseum;

/**
 * Represents a reward granted to players within the Underground Coliseum.<br>
 * This class defines the items or benefits received upon completing specific objectives.
 */
public class UCReward
{
	private final int _itemId;
	private final long _amount;
	private final boolean _allowMidifier;
	
	/**
	 * Creates a new {@code UCReward} object.<br>
	 * This constructor initializes the reward properties.
	 * @param itemId The unique identifier for the item.
	 * @param amount The quantity of the item to give.
	 * @param allowMidifier Determines if modifiers can be applied. Set to {@code true} to allow or {@code false} to block.
	 */
	public UCReward(int itemId, long amount, boolean allowMidifier)
	{
		_itemId = itemId;
		_amount = amount;
		_allowMidifier = allowMidifier;
	}
	
	/**
	 * Retrieves the unique identifier for this reward.<br>
	 * This value corresponds to the {@code itemId}.
	 * @return The unique identifier of the reward.
	 */
	public int getId()
	{
		return _itemId;
	}
	
	/**
	 * Retrieves the quantity of the reward.<br>
	 * This value represents how many items are given.
	 * @return The total amount as a {@code long}.
	 */
	public long getAmount()
	{
		return _amount;
	}
	
	/**
	 * Checks if the reward allows modifiers.<br>
	 * This method returns the value of the {@code _allowMidifier} flag.
	 * @return {@code true} if modifiers are allowed, {@code false} otherwise.
	 */
	public boolean isAllowMidifier()
	{
		return _allowMidifier;
	}
}
