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
 * This class holds the reward data for regular cross-event rewards.<br>
 * It is used to manage and store information related to these specific player rewards.
 * @author Smoto
 */
public class CrossEventRegularRewardHolder
{
	private final int _cellId;
	private final int _rewardId;
	private final int _rewardAmount;
	
	/**
	 * Creates a new instance of {@code CrossEventRegularRewardHolder}.<br>
	 * This constructor initializes the reward data for a specific event cell.
	 * @param cellId The unique identifier for the event cell.
	 * @param rewardId The unique identifier for the reward type.
	 * @param rewardAmount The quantity of the reward to be given.
	 */
	public CrossEventRegularRewardHolder(int cellId, int rewardId, int rewardAmount)
	{
		_cellId = cellId;
		_rewardId = rewardId;
		_rewardAmount = rewardAmount;
	}
	
	/**
	 * Retrieves the unique identifier for the cell.<br>
	 * This value is used to identify specific reward locations.
	 * @return The {@code int} value of the cell ID.
	 */
	public int cellId()
	{
		return _cellId;
	}
	
	/**
	 * Retrieves the unique identifier for the reward associated with a specific cell.<br>
	 * This value corresponds to the {@code _rewardId} field.
	 * @return The integer ID of the reward.
	 */
	public int cellReward()
	{
		return _rewardId;
	}
	
	/**
	 * Gets the total amount of the reward.<br>
	 * This value represents how many items are given for a specific cell.
	 * @return The number of rewards as an {@code int}.
	 */
	public int cellAmount()
	{
		return _rewardAmount;
	}
}
