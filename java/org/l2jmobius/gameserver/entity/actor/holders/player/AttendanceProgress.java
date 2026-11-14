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
 * This class holds the attendance data for a specific player.<br>
 * It tracks information related to daily login rewards and streaks.<br>
 * Use this holder to manage {@code AttendanceInfo} associated with an actor.
 * @author Mobius, CostyKiller
 */
public class AttendanceProgress
{
	private final int _rewardIndex;
	private final boolean _rewardAvailable;
	private final int _missedDays;
	private final int _currentCycleDay;
	
	/**
	 * Creates a new instance of {@code AttendanceProgress}.<br>
	 * This constructor initializes the reward data for a player.
	 * @param rewardIndex The index identifying the specific reward.
	 * @param rewardAvailable A boolean indicating if the reward is currently available.
	 */
	public AttendanceProgress(int rewardIndex, boolean rewardAvailable)
	{
		_rewardIndex = rewardIndex;
		_rewardAvailable = rewardAvailable;
		_missedDays = 0;
		_currentCycleDay = rewardIndex;
	}
	
	/**
	 * Creates a new {@code AttendanceProgress} with specific progress data.<br>
	 * This constructor is used for tracking attendance cycles and rewards.
	 * @param rewardIndex The index of the reward to be granted.
	 * @param rewardAvailable Whether the reward is currently available to claim.
	 * @param missedDays The total number of days missed by the player.
	 * @param currentCycleDay The current day within the active attendance cycle.
	 */
	public AttendanceProgress(int rewardIndex, boolean rewardAvailable, int missedDays, int currentCycleDay)
	{
		_rewardIndex = rewardIndex;
		_rewardAvailable = rewardAvailable;
		_missedDays = missedDays;
		_currentCycleDay = currentCycleDay;
	}
	
	/**
	 * Retrieves the current index of the reward.<br>
	 * This value identifies which specific reward is assigned to the player.
	 * @return The {@code int} representing the reward index.
	 */
	public int getRewardIndex()
	{
		return _rewardIndex;
	}
	
	/**
	 * Checks if a reward is currently available for the player.<br>
	 * This method returns the status of the reward availability.
	 * @return {@code true} if the reward is available, {@code false} otherwise.
	 */
	public boolean isRewardAvailable()
	{
		return _rewardAvailable;
	}
	
	/**
	 * Retrieves the total number of days missed.<br>
	 * This value is stored in the {@code _missedDays} field.
	 * @return The count of missed days as an {@code int}.
	 */
	public int getMissedDays()
	{
		return _missedDays;
	}
	
	/**
	 * Retrieves the current day of the attendance cycle.<br>
	 * This value represents the progress within the current reward period.
	 * @return The current cycle day as an {@code int}.
	 */
	public int getCurrentCycleDay()
	{
		return _currentCycleDay;
	}
	
	/**
	 * Checks if the player has missed any days.<br>
	 * It returns {@code true} if the count of missed days is greater than {@code 0}.<br>
	 * Otherwise, it returns {@code false}.
	 * @return {@code true} if there are missed days, {@code false} otherwise.
	 */
	public boolean hasMissedDays()
	{
		return _missedDays > 0;
	}
}