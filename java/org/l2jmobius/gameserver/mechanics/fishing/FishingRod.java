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
 * Represents a fishing rod used by players to perform the fishing mechanic.<br>
 * This class handles the logic and properties associated with the fishing tool.
 * @author Zarcos
 */
public class FishingRod
{
	private final int _itemId;
	private final int _reduceFishingTime;
	private final float _xpMultiplier;
	private final float _spMultiplier;
	
	/**
	 * Creates a new {@code FishingRod} instance.<br>
	 * This constructor initializes the rod with specific stats.
	 * @param itemId The unique identifier for the item.
	 * @param reduceFishingTime The amount of time reduced during fishing.
	 * @param xpMultiplier The multiplier applied to experience points.
	 * @param spMultiplier The multiplier applied to skill points.
	 */
	public FishingRod(int itemId, int reduceFishingTime, float xpMultiplier, float spMultiplier)
	{
		_itemId = itemId;
		_reduceFishingTime = reduceFishingTime;
		_xpMultiplier = xpMultiplier;
		_spMultiplier = spMultiplier;
	}
	
	/**
	 * Retrieves the unique identifier for this fishing rod.<br>
	 * This value corresponds to the item ID in the game database.
	 * @return The {@code int} ID of the item.
	 */
	public int getItemId()
	{
		return _itemId;
	}
	
	/**
	 * Gets the amount of time reduced for fishing.<br>
	 * This value is used to speed up the fishing process.
	 * @return the reduction time as an {@code int}.
	 */
	public int getReduceFishingTime()
	{
		return _reduceFishingTime;
	}
	
	/**
	 * Gets the experience point multiplier for this fishing rod.<br>
	 * This value determines how much extra {@code xp} is earned.
	 * @return The current {@code float} multiplier.
	 */
	public float getXpMultiplier()
	{
		return _xpMultiplier;
	}
	
	/**
	 * Gets the skill point multiplier for this fishing rod.<br>
	 * This value determines how much extra skill points are earned.
	 * @return the {@code float} multiplier for skill points.
	 */
	public float getSpMultiplier()
	{
		return _spMultiplier;
	}
}
