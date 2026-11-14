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

import java.util.Calendar;

import org.l2jmobius.gameserver.config.AttendanceRewardsConfig;
import org.l2jmobius.gameserver.data.xml.AttendanceRewardData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.variables.AccountVariables;
import org.l2jmobius.gameserver.mechanics.variables.PlayerVariables;

/**
 * Manages the attendance reward state of a {@code Player}.<br>
 * It tracks the current reward index and the delay before the next claim.<br>
 * The state lives in player and account variables rather than its own table.
 * @author Mobius
 */
public class Attendance
{
	private final Player _player;
	private long _delay;
	
	/**
	 * Creates a new {@code Attendance} for a specific player.
	 * @param player The {@code Player} object associated with this instance.
	 */
	public Attendance(Player player)
	{
		_player = player;
	}
	
	/**
	 * Retrieves the current attendance information for the player.<br>
	 * This method calculates if a reward is available based on the last claim time.<br>
	 * It also determines the next reward index to be granted.
	 * @return an {@code AttendanceProgress} containing the reward index and eligibility status.
	 */
	public AttendanceProgress getProgress()
	{
		if (AttendanceRewardsConfig.ATTENDANCE_REWARDS_MATCH_REAL_DAYS)
		{
			return getProgressRealDays();
		}
		
		return getProgressNormal();
	}
	
	/**
	 * Retrieves the current attendance information for the player.<br>
	 * This method calculates if a reward is available based on the current time and saved variables.<br>
	 * It handles both account-wide and character-specific rewards depending on the configuration.
	 * @return an {@code AttendanceProgress} containing the current reward index and eligibility status.
	 */
	private AttendanceProgress getProgressNormal()
	{
		// Get reset time.
		final Calendar calendar = Calendar.getInstance();
		if ((calendar.get(Calendar.HOUR_OF_DAY) < 6) && (calendar.get(Calendar.MINUTE) < 30))
		{
			calendar.add(Calendar.DAY_OF_MONTH, -1);
		}
		calendar.set(Calendar.HOUR_OF_DAY, 6);
		calendar.set(Calendar.MINUTE, 30);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		
		// Get last player reward time.
		final long receiveDate;
		int rewardIndex;
		if (AttendanceRewardsConfig.ATTENDANCE_REWARDS_SHARE_ACCOUNT)
		{
			receiveDate = _player.getAccountVariables().getLong(PlayerVariables.ATTENDANCE_DATE, 0);
			rewardIndex = _player.getAccountVariables().getInt(PlayerVariables.ATTENDANCE_INDEX, 0);
		}
		else
		{
			receiveDate = _player.getVariables().getLong(PlayerVariables.ATTENDANCE_DATE, 0);
			rewardIndex = _player.getVariables().getInt(PlayerVariables.ATTENDANCE_INDEX, 0);
		}
		
		// Check if player can receive reward today.
		boolean canBeRewarded = false;
		if (calendar.getTimeInMillis() > receiveDate)
		{
			if (rewardIndex >= AttendanceRewardData.getInstance().getRewardsCount())
			{
				if (AttendanceRewardsConfig.ATTENDANCE_REWARDS_LOOP)
				{
					// Reset to day 1 and persist immediately so the client renders all slots as unclaimed.
					rewardIndex = 0;
					if (AttendanceRewardsConfig.ATTENDANCE_REWARDS_SHARE_ACCOUNT)
					{
						_player.getAccountVariables().set(PlayerVariables.ATTENDANCE_INDEX, 0);
						_player.getAccountVariables().set(PlayerVariables.ATTENDANCE_DATE, 0L);
						_player.getAccountVariables().set(PlayerVariables.ATTENDANCE_LOOP_RESET, true);
					}
					else
					{
						_player.getVariables().set(PlayerVariables.ATTENDANCE_INDEX, 0);
						_player.getVariables().set(PlayerVariables.ATTENDANCE_DATE, 0L);
						_player.getVariables().set(PlayerVariables.ATTENDANCE_LOOP_RESET, true);
					}
					canBeRewarded = true;
				}
				// else: all rewards claimed and loop disabled -> stay false, no popup.
			}
			else
			{
				canBeRewarded = true;
			}
		}
		
		return new AttendanceProgress(rewardIndex, canBeRewarded);
	}
	
	/**
	 * Calculates the attendance information based on real-time days.<br>
	 * This method determines the current reward index and whether a free claim is available.<br>
	 * It handles both shared account variables and individual player variables.
	 * @return an {@code AttendanceProgress} containing the calculated progress and status.
	 */
	private AttendanceProgress getProgressRealDays()
	{
		final int rewardsCount = AttendanceRewardData.getInstance().getRewardsCount();
		
		// Calculate current cycle start: this month's AttendanceCycleStartDay at 00:00.
		final Calendar cycleStart = Calendar.getInstance();
		cycleStart.set(Calendar.DAY_OF_MONTH, AttendanceRewardsConfig.ATTENDANCE_CYCLE_START_DAY);
		cycleStart.set(Calendar.HOUR_OF_DAY, 0);
		cycleStart.set(Calendar.MINUTE, 0);
		cycleStart.set(Calendar.SECOND, 0);
		cycleStart.set(Calendar.MILLISECOND, 0);
		
		// If today is before the cycle start day of this month, use last month's cycle.
		final Calendar now = Calendar.getInstance();
		if (now.before(cycleStart))
		{
			cycleStart.add(Calendar.MONTH, -1);
		}
		
		// Calculate which day of the cycle today is (1-based, capped at rewardsCount).
		final long millisPerDay = 24L * 60 * 60 * 1000;
		final long elapsed = now.getTimeInMillis() - cycleStart.getTimeInMillis();
		final int currentCycleDay = (int) Math.min((elapsed / millisPerDay) + 1, rewardsCount);
		
		// Get stored values - ATTENDANCE_DATE used same as normal mode (last free claim time).
		int rewardIndex;
		long lastFreeClaim;
		if (AttendanceRewardsConfig.ATTENDANCE_REWARDS_SHARE_ACCOUNT)
		{
			rewardIndex = _player.getAccountVariables().getInt(PlayerVariables.ATTENDANCE_INDEX, 0);
			lastFreeClaim = _player.getAccountVariables().getLong(PlayerVariables.ATTENDANCE_DATE, 0);
		}
		else
		{
			rewardIndex = _player.getVariables().getInt(PlayerVariables.ATTENDANCE_INDEX, 0);
			lastFreeClaim = _player.getVariables().getLong(PlayerVariables.ATTENDANCE_DATE, 0);
		}
		
		// If cycle has reset (last free claim is before cycle start), clear all progress.
		if (lastFreeClaim < cycleStart.getTimeInMillis())
		{
			rewardIndex = 0;
			if (AttendanceRewardsConfig.ATTENDANCE_REWARDS_SHARE_ACCOUNT)
			{
				_player.getAccountVariables().set(PlayerVariables.ATTENDANCE_INDEX, 0);
				_player.getAccountVariables().set(PlayerVariables.ATTENDANCE_DATE, 0L);
				_player.getAccountVariables().set(PlayerVariables.ATTENDANCE_LOOP_RESET, true);
			}
			else
			{
				_player.getVariables().set(PlayerVariables.ATTENDANCE_INDEX, 0);
				_player.getVariables().set(PlayerVariables.ATTENDANCE_DATE, 0L);
				_player.getVariables().set(PlayerVariables.ATTENDANCE_LOOP_RESET, true);
			}
		}
		
		// All days in this cycle already claimed/purchased.
		if (rewardIndex >= currentCycleDay)
		{
			return new AttendanceProgress(rewardIndex, false, 0, currentCycleDay);
		}
		
		// Calculate today's midnight for last-claim comparison.
		final Calendar todayMidnight = Calendar.getInstance();
		todayMidnight.set(Calendar.HOUR_OF_DAY, 0);
		todayMidnight.set(Calendar.MINUTE, 0);
		todayMidnight.set(Calendar.SECOND, 0);
		todayMidnight.set(Calendar.MILLISECOND, 0);
		
		// Free claim available if timer done AND not already claimed today.
		final boolean timerDone = getDelay() == 0;
		final boolean notClaimedToday = lastFreeClaim < todayMidnight.getTimeInMillis();
		final boolean canBeRewarded = timerDone && notClaimedToday;
		
		// missedDays = purchasable days after the first unclaimed up to today.
		final int missedDays = canBeRewarded ? (currentCycleDay - rewardIndex - 1) : (currentCycleDay - rewardIndex);
		
		return new AttendanceProgress(rewardIndex, canBeRewarded, Math.max(missedDays, 0), currentCycleDay);
	}
	
	/**
	 * Updates the attendance information for the player.<br>
	 * This method calculates the time for the next available reward.<br>
	 * It saves the new date and index to either account or character variables.
	 * @param rewardIndex The index of the reward to be granted.
	 */
	public void setProgress(int rewardIndex)
	{
		if (AttendanceRewardsConfig.ATTENDANCE_REWARDS_MATCH_REAL_DAYS)
		{
			// Real-days mode: update index and record free claim time in ATTENDANCE_DATE,
			// same variable as normal mode, so next login won't offer free reward again today.
			if (AttendanceRewardsConfig.ATTENDANCE_REWARDS_SHARE_ACCOUNT)
			{
				final AccountVariables accountVars = _player.getAccountVariables();
				accountVars.set(PlayerVariables.ATTENDANCE_INDEX, rewardIndex);
				accountVars.set(PlayerVariables.ATTENDANCE_DATE, System.currentTimeMillis());
				accountVars.set(PlayerVariables.ATTENDANCE_LOOP_RESET, false);
			}
			else
			{
				final PlayerVariables variables = _player.getVariables();
				variables.set(PlayerVariables.ATTENDANCE_INDEX, rewardIndex);
				variables.set(PlayerVariables.ATTENDANCE_DATE, System.currentTimeMillis());
				variables.set(PlayerVariables.ATTENDANCE_LOOP_RESET, false);
			}
			return;
		}
		
		// Normal mode: set next claimable time to tomorrow at 06:30.
		final Calendar nextReward = Calendar.getInstance();
		nextReward.set(Calendar.MINUTE, 30);
		if (nextReward.get(Calendar.HOUR_OF_DAY) >= 6)
		{
			nextReward.add(Calendar.DATE, 1);
		}
		nextReward.set(Calendar.HOUR_OF_DAY, 6);
		if (AttendanceRewardsConfig.ATTENDANCE_REWARDS_SHARE_ACCOUNT)
		{
			final AccountVariables accountVars = _player.getAccountVariables();
			accountVars.set(PlayerVariables.ATTENDANCE_DATE, nextReward.getTimeInMillis());
			accountVars.set(PlayerVariables.ATTENDANCE_INDEX, rewardIndex);
			accountVars.set(PlayerVariables.ATTENDANCE_LOOP_RESET, false);
		}
		else
		{
			final PlayerVariables variables = _player.getVariables();
			variables.set(PlayerVariables.ATTENDANCE_DATE, nextReward.getTimeInMillis());
			variables.set(PlayerVariables.ATTENDANCE_INDEX, rewardIndex);
			variables.set(PlayerVariables.ATTENDANCE_LOOP_RESET, false);
		}
	}
	
	/**
	 * Calculates the time remaining until the next attendance reward.<br>
	 * It converts the difference between the current system time and {@code _delay} into seconds.<br>
	 * If the delay has already passed, it returns 0.
	 * @return The number of seconds remaining as an {@code int}.
	 */
	public int getDelay()
	{
		final long currentTime = System.currentTimeMillis();
		final long remainingTime = _delay - currentTime;
		final int remainingSeconds = (int) (remainingTime / 1000);
		return Math.max(remainingSeconds, 0);
	}
	
	/**
	 * Sets the time delay for attendance.<br>
	 * This method calculates a future timestamp based on the provided minutes.
	 * @param timeInMinutes The number of minutes to add to the current system time.
	 */
	public void setDelay(int timeInMinutes)
	{
		final long currentTime = System.currentTimeMillis();
		_delay = currentTime + (timeInMinutes * 60 * 1000);
	}
}
