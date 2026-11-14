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

import java.util.List;
import java.util.function.Function;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.DailyMissionStatus;
import org.l2jmobius.gameserver.entity.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.entity.item.holders.ItemHolder;
import org.l2jmobius.gameserver.handler.AbstractDailyMissionHandler;
import org.l2jmobius.gameserver.handler.DailyMissionHandler;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class stores the progress and state of daily missions for a {@code Player}.<br>
 * It manages data such as completion status, current progress, and reset information.<br>
 * Use this holder to access or update mission-related statistics during gameplay.
 * @author Sdw, CostyKiller
 */
public class DailyMissionDataHolder
{
	private final int _id;
	private final String _name;
	private final int _category;
	private final List<ItemHolder> _rewardsItems;
	private final List<PlayerClass> _classRestriction;
	private final int _requiredMissionCompleteId;
	private final int _requiredCompletions;
	private final StatSet _params;
	private final boolean _dailyReset;
	private final boolean _isOneTime;
	private final boolean _isMainClassOnly;
	private final boolean _isDualClassOnly;
	private final boolean _isDisplayedWhenNotAvailable;
	private final AbstractDailyMissionHandler _handler;
	
	/**
	 * Creates a new instance of {@code DailyMissionDataHolder}.<br>
	 * This constructor initializes the mission data using a {@code StatSet}.<br>
	 * It maps values from the set to internal fields like rewards and restrictions.
	 * @param set The {@code StatSet} containing all configuration data for the mission.
	 */
	public DailyMissionDataHolder(StatSet set)
	{
		final Function<DailyMissionDataHolder, AbstractDailyMissionHandler> handler = DailyMissionHandler.getInstance().getHandler(set.getString("handler"));
		_id = set.getInt("id");
		_name = set.getString("name");
		_category = set.getInt("category", 0);
		_requiredMissionCompleteId = set.getInt("requiredMissionCompleteId", 0);
		_requiredCompletions = set.getInt("requiredCompletion", 0);
		_rewardsItems = set.getList("items", ItemHolder.class);
		_classRestriction = set.getList("classRestriction", PlayerClass.class);
		_params = set.getObject("params", StatSet.class);
		_dailyReset = set.getBoolean("dailyReset", true);
		_isOneTime = set.getBoolean("isOneTime", true);
		_isMainClassOnly = set.getBoolean("isMainClassOnly", false);
		_isDualClassOnly = set.getBoolean("isDualClassOnly", false);
		_isDisplayedWhenNotAvailable = set.getBoolean("isDisplayedWhenNotAvailable", true);
		_handler = handler != null ? handler.apply(this) : null;
	}
	
	/**
	 * Retrieves the unique identifier for this mission.<br>
	 * This value is used to distinguish between different missions.
	 * @return The {@code int} ID of the daily mission.
	 */
	public int getId()
	{
		return _id;
	}
	
	/**
	 * Retrieves the display name of the daily mission.<br>
	 * This name is used to identify the mission in the user interface.
	 * @return The {@code String} name of the mission.
	 */
	public String getName()
	{
		return _name;
	}
	
	/**
	 * Retrieves the category of this daily mission.<br>
	 * The value represents a specific group for organizing missions.
	 * @return the {@code int} category identifier.
	 */
	public int getCategory()
	{
		return _category;
	}
	
	/**
	 * Retrieves the list of classes restricted for this mission.<br>
	 * This method returns all {@code PlayerClass} types that are allowed or excluded.
	 * @return a {@code List} of {@code PlayerClass} objects.
	 */
	public List<PlayerClass> getClassRestriction()
	{
		return _classRestriction;
	}
	
	/**
	 * Retrieves the list of rewards for this mission.<br>
	 * Each reward is stored as an {@code ItemHolder}.
	 * @return a {@code List} of {@code ItemHolder} objects.
	 */
	public List<ItemHolder> getRewards()
	{
		return _rewardsItems;
	}
	
	/**
	 * Retrieves the ID of the mission that must be completed first.<br>
	 * This value determines the prerequisite for this specific daily mission.
	 * @return The {@code int} identifier of the required mission.
	 */
	public int getRequiredMissionCompleteId()
	{
		return _requiredMissionCompleteId;
	}
	
	/**
	 * Retrieves the number of times a mission must be completed.<br>
	 * This value defines the goal for the daily mission.
	 * @return The total number of required completions.
	 */
	public int getRequiredCompletions()
	{
		return _requiredCompletions;
	}
	
	/**
	 * Retrieves the configuration parameters for this mission.<br>
	 * These values are stored in a {@code StatSet}.
	 * @return the {@code StatSet} containing the mission parameters.
	 */
	public StatSet getParams()
	{
		return _params;
	}
	
	/**
	 * Checks if the daily mission should be reset every day.<br>
	 * Returns {@code true} if it is a recurring daily mission.<br>
	 * Returns {@code false} if it is not a daily reset mission.
	 * @return the reset status of this mission.
	 */
	public boolean dailyReset()
	{
		return _dailyReset;
	}
	
	/**
	 * Checks if the mission is a one-time task.<br>
	 * Returns {@code true} if the mission can only be completed once.<br>
	 * Returns {@code false} if the mission repeats.
	 * @return The completion status of the one-time flag.
	 */
	public boolean isOneTime()
	{
		return _isOneTime;
	}
	
	/**
	 * Checks if the mission is restricted to the main class.<br>
	 * Returns {@code true} if it only applies to the main class.<br>
	 * Returns {@code false} otherwise.
	 * @return a boolean indicating if the mission is for the main class only.
	 */
	public boolean isMainClassOnly()
	{
		return _isMainClassOnly;
	}
	
	/**
	 * Checks if the mission is restricted to dual classes.<br>
	 * Returns {@code true} if it is only available for dual class players.<br>
	 * Returns {@code false} otherwise.
	 * @return a boolean indicating if the mission is dual class only.
	 */
	public boolean isDualClassOnly()
	{
		return _isDualClassOnly;
	}
	
	/**
	 * Checks if the mission should be shown even when it is not available.<br>
	 * This determines the visibility of the mission in the user interface.
	 * @return {@code true} if the mission is displayed when unavailable, {@code false} otherwise.
	 */
	public boolean isDisplayedWhenNotAvailable()
	{
		return _isDisplayedWhenNotAvailable;
	}
	
	/**
	 * Checks if the mission can be shown to a specific player.<br>
	 * It verifies if the mission is not completed or is repeatable.<br>
	 * It also checks if the player's class meets the requirements.
	 * @param player The {@code Player} to check against.
	 * @return {@code true} if the mission should be displayed, {@code false} otherwise.
	 */
	public boolean isDisplayable(Player player)
	{
		// Check if it is main class only.
		if (_isMainClassOnly && (player.isSubClassActive() || player.isDualClassActive()))
		{
			return false;
		}
		
		// Check if it is dual class only.
		if (_isDualClassOnly && !player.isDualClassActive())
		{
			return false;
		}
		
		// Check for specific class restrictions.
		if (!_classRestriction.isEmpty() && !_classRestriction.contains(player.getPlayerClass()))
		{
			return false;
		}
		
		final int status = getStatus(player);
		if (!_isDisplayedWhenNotAvailable && (status == DailyMissionStatus.NOT_AVAILABLE.getClientId()))
		{
			return false;
		}
		
		// Show only if it is repeatable, recently completed or incompleted that has met the checks above.
		return (!_isOneTime || isRecentlyCompleted(player) || (status != DailyMissionStatus.COMPLETED.getClientId()));
	}
	
	/**
	 * This method handles the request for a mission reward.<br>
	 * It checks if the {@code _handler} exists and if the player can see it.<br>
	 * If both conditions are met, it calls the handler's reward logic.
	 * @param player The {@code Player} who is requesting the reward.
	 */
	public void requestReward(Player player)
	{
		if ((_handler != null) && isDisplayable(player))
		{
			_handler.requestReward(player);
		}
	}
	
	/**
	 * Retrieves the current status of a daily mission for a specific player.<br>
	 * It uses the internal handler to determine the progress state.<br>
	 * If no handler exists, it returns the client ID for {@code DailyMissionStatus.NOT_AVAILABLE}.
	 * @param player The {@code Player} whose mission status is being checked.
	 * @return The integer client ID representing the current mission status.
	 */
	public int getStatus(Player player)
	{
		return _handler != null ? _handler.getStatus(player) : DailyMissionStatus.NOT_AVAILABLE.getClientId();
	}
	
	/**
	 * Retrieves the current progress of a daily mission for a specific player.<br>
	 * It uses the internal handler to calculate the value.<br>
	 * If no handler exists, it returns the client ID for {@code DailyMissionStatus.NOT_AVAILABLE}.
	 * @param player The {@code Player} whose progress is being checked.
	 * @return The current progress as an {@code int}.
	 */
	public int getProgress(Player player)
	{
		return _handler != null ? _handler.getProgress(player) : DailyMissionStatus.NOT_AVAILABLE.getClientId();
	}
	
	/**
	 * Checks if the player has recently finished this mission.<br>
	 * This method delegates the check to the internal {@code AbstractDailyMissionHandler}.
	 * @param player The {@code Player} object to check.
	 * @return {@code true} if the mission was completed recently, {@code false} otherwise.
	 */
	public boolean isRecentlyCompleted(Player player)
	{
		return (_handler != null) && _handler.isRecentlyCompleted(player);
	}
	
	/**
	 * Checks if this mission is a level up mission.<br>
	 * It delegates the check to the associated {@code AbstractDailyMissionHandler}.
	 * @return {@code true} if it is a level up mission, {@code false} otherwise.
	 */
	public boolean isLevelUpMission()
	{
		return (_handler != null) && _handler.isLevelUpMission();
	}
}
