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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.config.HuntPassConfig;
import org.l2jmobius.gameserver.data.xml.HuntPassData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.huntpass.HuntPassSimpleInfo;

/**
 * Manages the logic and state for a player's hunt pass.<br>
 * This class handles the activation, expiration, and data retrieval of {@code HuntPassData}.
 * @author Serenitty
 */
public class HuntPass
{
	private static final Logger LOGGER = Logger.getLogger(HuntPass.class.getName());
	
	private static final String INSERT_SEASONPASS = "REPLACE INTO huntpass (`account_name`, `current_step`, `points`, `reward_step`, `is_premium`, `premium_reward_step`, `sayha_points_available`, `sayha_points_used`, `unclaimed_reward`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
	private static final String RESTORE_SEASONPASS = "SELECT * FROM huntpass WHERE account_name=?";
	
	private final Player _user;
	private int _availableSayhaTime;
	private int _points;
	private boolean _isPremium = false;
	private boolean _rewardAlert = false;
	
	private int _rewardStep;
	private int _currentStep;
	private int _premiumRewardStep;
	
	private boolean _toggleSayha = false;
	private ScheduledFuture<?> _sayhasSustentionTask = null;
	private int _toggleStartTime = 0;
	private int _usedSayhaTime;
	
	private static int _dayEnd = 0;
	
	/**
	 * Creates a new {@code HuntPass} instance for a specific player.<br>
	 * This constructor initializes the pass by restoring data from the database.<br>
	 * It also calculates the day end and saves the initial state.
	 * @param user The {@code Player} who owns this hunt pass.
	 */
	public HuntPass(Player user)
	{
		_user = user;
		restoreHuntPass();
		huntPassDayEnd();
		store();
	}
	
	/**
	 * Loads the hunt pass data from the database.<br>
	 * This method updates all local fields using the account name.<br>
	 * It handles errors by logging a severe message if the operation fails.
	 */
	public void restoreHuntPass()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(RESTORE_SEASONPASS))
		{
			statement.setString(1, getAccountName());
			try (ResultSet rset = statement.executeQuery())
			{
				if (rset.next())
				{
					setPoints(rset.getInt("points"));
					setCurrentStep(rset.getInt("current_step"));
					setRewardStep(rset.getInt("reward_step"));
					setPremium(rset.getBoolean("is_premium"));
					setPremiumRewardStep(rset.getInt("premium_reward_step"));
					setAvailableSayhaTime(rset.getInt("sayha_points_available"));
					setUsedSayhaTime(rset.getInt("sayha_points_used"));
					setRewardAlert(rset.getBoolean("unclaimed_reward"));
				}
				
				rset.close();
				statement.close();
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Could not restore Season Pass for playerId: " + _user.getAccountName());
		}
	}
	
	/**
	 * Resets all hunt pass data to their default values.<br>
	 * This method clears points, steps, and premium status.<br>
	 * It also updates the database via {@code store()}.
	 */
	public void resetHuntPass()
	{
		setPoints(0);
		setCurrentStep(0);
		setRewardStep(0);
		setPremium(false);
		setPremiumRewardStep(0);
		setAvailableSayhaTime(0);
		setUsedSayhaTime(0);
		setRewardAlert(false);
		store();
	}
	
	/**
	 * Retrieves the name of the account associated with this {@code HuntPass}.<br>
	 * This value is obtained from the linked {@code Player} object.
	 * @return The account name as a {@code String}.
	 */
	public String getAccountName()
	{
		return _user.getAccountName();
	}
	
	/**
	 * Saves the current {@code HuntPass} data to the database.<br>
	 * It uses an {@code INSERT} statement to update all player progress fields.<br>
	 * If a {@code SQLException} occurs, it logs a severe error message.
	 */
	public void store()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(INSERT_SEASONPASS))
		{
			statement.setString(1, getAccountName());
			statement.setInt(2, getCurrentStep());
			statement.setInt(3, getPoints());
			statement.setInt(4, getRewardStep());
			statement.setBoolean(5, isPremium());
			statement.setInt(6, getPremiumRewardStep());
			statement.setInt(7, getAvailableSayhaTime());
			statement.setInt(8, getUsedSayhaTime());
			statement.setBoolean(9, rewardAlert());
			statement.execute();
			statement.close();
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.SEVERE, "Could not store Season-Pass data for Account " + _user.getAccountName() + ": ", e);
		}
	}
	
	/**
	 * Retrieves the end time for the current hunt pass day.
	 * @return The end time as an {@code int}.
	 */
	public int getHuntPassDayEnd()
	{
		return _dayEnd;
	}
	
	/**
	 * Calculates the end of the current hunt pass day.<br>
	 * This method determines the timestamp for the next reset period.<br>
	 * It uses the configuration from {@code HuntPassConfig}.
	 */
	public void huntPassDayEnd()
	{
		final Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(System.currentTimeMillis());
		calendar.set(Calendar.DAY_OF_MONTH, HuntPassConfig.HUNT_PASS_PERIOD);
		calendar.set(Calendar.HOUR_OF_DAY, 6);
		calendar.set(Calendar.MINUTE, 30);
		if (calendar.getTimeInMillis() < System.currentTimeMillis())
		{
			calendar.add(Calendar.MONTH, 1);
		}
		
		_dayEnd = (int) (calendar.getTimeInMillis() / 1000);
	}
	
	/**
	 * Checks the current state of the Sayha toggle.<br>
	 * This method returns whether the feature is currently enabled or disabled.
	 * @return {@code true} if the toggle is active, {@code false} otherwise.
	 */
	public boolean toggleSayha()
	{
		return _toggleSayha;
	}
	
	/**
	 * Retrieves the current number of points for the hunt pass.
	 * @return the total amount of {@code int} points.
	 */
	public int getPoints()
	{
		return _points;
	}
	
	/**
	 * Increments the player's hunt pass points.<br>
	 * This method checks if the user is in a timed hunting zone to grant extra points.<br>
	 * It also updates the current step if enough points are earned.<br>
	 * If a new level is reached, it sends a notification packet to the {@code Player}.
	 */
	public void addPassPoint()
	{
		if (!HuntPassConfig.ENABLE_HUNT_PASS)
		{
			return;
		}
		
		// Add points.
		int points = getPoints() + 1;
		if (_user.isInTimedHuntingZone())
		{
			points++;
		}
		
		// Check current step.
		boolean hasNewLevel = false;
		while (points >= HuntPassConfig.HUNT_PASS_POINTS_FOR_STEP)
		{
			points -= HuntPassConfig.HUNT_PASS_POINTS_FOR_STEP;
			setCurrentStep(getCurrentStep() + 1);
			hasNewLevel = true;
		}
		
		// Save the current point count.
		setPoints(points);
		
		// Send info when needed.
		if (hasNewLevel)
		{
			setRewardAlert(true);
			_user.sendPacket(new HuntPassSimpleInfo(_user));
		}
	}
	
	/**
	 * Updates the current point balance.<br>
	 * This method sets the internal {@code _points} value to the provided amount.
	 * @param points The new number of points to set.
	 */
	public void setPoints(int points)
	{
		_points = points;
	}
	
	/**
	 * Retrieves the current progress step of the hunt pass.
	 * @return The current step as an {@code int}.
	 */
	public int getCurrentStep()
	{
		return _currentStep;
	}
	
	/**
	 * Updates the current progress step of the hunt pass.<br>
	 * The value is clamped between 0 and the maximum rewards count.
	 * @param step The new step value to set.
	 */
	public void setCurrentStep(int step)
	{
		_currentStep = Math.max(0, Math.min(step, HuntPassData.getInstance().getRewardsCount()));
	}
	
	/**
	 * Retrieves the current reward step for the hunt pass.<br>
	 * This value indicates which reward is currently available to the player.
	 * @return The current {@code int} value of the reward step.
	 */
	public int getRewardStep()
	{
		return _rewardStep;
	}
	
	/**
	 * Sets the current reward step for the hunt pass.<br>
	 * This method updates the {@code _rewardStep} value.<br>
	 * It ensures the value stays within the valid range defined in {@code HuntPassData}.<br>
	 * Premium accounts may have restrictions based on their premium reward step.
	 * @param step The new reward step to set.
	 */
	public void setRewardStep(int step)
	{
		if (_isPremium && (_premiumRewardStep <= _rewardStep))
		{
			return;
		}
		
		_rewardStep = Math.max(0, Math.min(step, HuntPassData.getInstance().getRewardsCount()));
	}
	
	/**
	 * Checks if the current hunt pass is premium.
	 * @return {@code true} if the status is premium, {@code false} otherwise.
	 */
	public boolean isPremium()
	{
		return _isPremium;
	}
	
	/**
	 * Updates the premium status of the hunt pass.<br>
	 * Sets the {@code _isPremium} field to the provided value.
	 * @param premium The new premium status to set.
	 */
	public void setPremium(boolean premium)
	{
		_isPremium = premium;
	}
	
	/**
	 * Retrieves the current step for premium rewards.<br>
	 * This value is used to track progress specifically for premium users.
	 * @return The current {@code int} value of the premium reward step.
	 */
	public int getPremiumRewardStep()
	{
		return _premiumRewardStep;
	}
	
	/**
	 * Sets the current premium reward step for the player.<br>
	 * The value is clamped between 0 and the maximum count defined in {@code HuntPassData}.
	 * @param step The new premium reward step to set.
	 */
	public void setPremiumRewardStep(int step)
	{
		_premiumRewardStep = Math.max(0, Math.min(step, HuntPassData.getInstance().getPremiumRewardsCount()));
	}
	
	/**
	 * Checks if the reward alert is currently enabled.
	 * @return {@code true} if the alert is active, {@code false} otherwise.
	 */
	public boolean rewardAlert()
	{
		return _rewardAlert;
	}
	
	/**
	 * Enables or disables the reward alert notification.<br>
	 * This method updates the {@code _rewardAlert} status.
	 * @param enable The boolean value to set for the alert.
	 */
	public void setRewardAlert(boolean enable)
	{
		_rewardAlert = enable;
	}
	
	/**
	 * Retrieves the amount of available Sayha time.<br>
	 * This value represents the remaining time for the current session.
	 * @return The number of seconds of available Sayha time.
	 */
	public int getAvailableSayhaTime()
	{
		return _availableSayhaTime;
	}
	
	/**
	 * Sets the amount of available Sayha time.<br>
	 * This updates the {@code _availableSayhaTime} field for the player.
	 * @param time The new amount of time to set.
	 */
	public void setAvailableSayhaTime(int time)
	{
		_availableSayhaTime = time;
	}
	
	/**
	 * Adds a specific amount of time to the available {@code Sayha} duration.<br>
	 * The input value is treated as minutes and converted to seconds for storage.
	 * @param time The number of minutes to add to the current total.
	 */
	public void addSayhaTime(int time)
	{
		// Microsec to sec to database. 1 hour 3600 sec.
		_availableSayhaTime += time * 60;
	}
	
	/**
	 * Retrieves the total amount of {@code sayha} time that has been used.
	 * @return The amount of used {@code sayha} time as an {@code int}.
	 */
	public int getUsedSayhaTime()
	{
		return _usedSayhaTime;
	}
	
	/**
	 * Handles the logic when the {@code _availableSayhaTime} reaches zero.<br>
	 * This method disables the Sayha sustention status for the player.
	 */
	private void onSayhaEndTime()
	{
		setSayhasSustention(false);
	}
	
	/**
	 * Sets the amount of used {@code Sayha} time.<br>
	 * This updates the internal value for the player's consumed duration.
	 * @param time The amount of time to set as used.
	 */
	public void setUsedSayhaTime(int time)
	{
		_usedSayhaTime = time;
	}
	
	/**
	 * Increases the total amount of used sustention time.<br>
	 * This method updates the internal counter for spent time.
	 * @param time The amount of time to add to the total.
	 */
	public void addSayhasSustentionTimeUsed(int time)
	{
		_usedSayhaTime += time;
	}
	
	/**
	 * Retrieves the start time of the toggle.
	 * @return The {@code int} value representing the start time.
	 */
	public int getToggleStartTime()
	{
		return _toggleStartTime;
	}
	
	/**
	 * Enables or disables the vitality sustention effect for the season pass.<br>
	 * If {@code true}, it starts consuming available time and schedules a task to end the effect.<br>
	 * If {@code false}, it stops the consumption and calculates the time used during activation.
	 * @param active The state to set for the sustention effect.
	 */
	public void setSayhasSustention(boolean active)
	{
		_toggleSayha = active;
		if (active)
		{
			_toggleStartTime = (int) (System.currentTimeMillis() / 1000);
			if (_sayhasSustentionTask != null)
			{
				_sayhasSustentionTask.cancel(true);
				_sayhasSustentionTask = null;
			}
			
			_user.sendPacket(SystemMessageId.VITALITY_SUSTENTION_EFFECT_OF_THE_SEASON_PASS_IS_ACTIVATED_AVAILABLE_VITALITY_SUSTENTION_TIME_IS_BEING_CONSUMED);
			_sayhasSustentionTask = ThreadPool.schedule(this::onSayhaEndTime, Math.max(0, getAvailableSayhaTime() - getUsedSayhaTime()) * 1000L);
		}
		else
		{
			if (_sayhasSustentionTask != null)
			{
				addSayhasSustentionTimeUsed((int) ((System.currentTimeMillis() / 1000) - _toggleStartTime));
				_toggleStartTime = 0;
				_sayhasSustentionTask.cancel(true);
				_sayhasSustentionTask = null;
				_user.sendPacket(SystemMessageId.VITALITY_SUSTENTION_EFFECT_OF_THE_SEASON_PASS_HAS_BEEN_DEACTIVATED_THE_SUSTENTION_TIME_YOU_HAVE_DOES_NOT_DECREASE);
			}
		}
	}
}
