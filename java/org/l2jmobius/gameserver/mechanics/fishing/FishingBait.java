/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2jmobius.gameserver.mechanics.fishing;

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.commons.util.Rnd;

/**
 * Represents an item used to attract fish during the fishing process.<br>
 * This class handles the logic for bait effects and interactions within {@code FishingBait}.
 * @author bit
 */
public class FishingBait
{
	private final int _itemId;
	private final int _level;
	private final int _minPlayerLevel;
	private final int _maxPlayerLevel;
	private final double _chance;
	private final int _timeMin;
	private final int _timeMax;
	private final int _waitMin;
	private final int _waitMax;
	private final boolean _isPremiumOnly;
	private final List<FishingCatch> _rewards = new ArrayList<>();
	
	/**
	 * Creates a new {@code FishingBait} object with specific fishing rules.<br>
	 * This constructor sets the requirements and probabilities for catching fish.
	 * @param itemId The unique identifier for the bait item.
	 * @param level The required level of the bait.
	 * @param minPlayerLevel The minimum player level allowed to use this bait.
	 * @param maxPlayerLevel The maximum player level allowed to use this bait.
	 * @param chance The probability of a successful catch.
	 * @param timeMin The minimum time in seconds for the fishing action.
	 * @param timeMax The maximum time in seconds for the fishing action.
	 * @param waitMin The minimum wait time between attempts.
	 * @param waitMax The maximum wait time between attempts.
	 * @param isPremiumOnly Set to {@code true} if only premium players can use this bait.
	 */
	public FishingBait(int itemId, int level, int minPlayerLevel, int maxPlayerLevel, double chance, int timeMin, int timeMax, int waitMin, int waitMax, boolean isPremiumOnly)
	{
		_itemId = itemId;
		_level = level;
		_minPlayerLevel = minPlayerLevel;
		_maxPlayerLevel = maxPlayerLevel;
		_chance = chance;
		_timeMin = timeMin;
		_timeMax = timeMax;
		_waitMin = waitMin;
		_waitMax = waitMax;
		_isPremiumOnly = isPremiumOnly;
	}
	
	/**
	 * Retrieves the unique identifier for this bait.<br>
	 * This value corresponds to the item ID in the game database.
	 * @return The {@code int} ID of the fishing bait.
	 */
	public int getItemId()
	{
		return _itemId;
	}
	
	/**
	 * Retrieves the level associated with this {@code FishingBait}.<br>
	 * This value represents the difficulty or tier of the bait.
	 * @return The integer level of the bait.
	 */
	public int getLevel()
	{
		return _level;
	}
	
	/**
	 * Retrieves the minimum player level required to use this bait.<br>
	 * This value is used to check if a player meets the requirements.
	 * @return The minimum player level as an {@code int}.
	 */
	public int getMinPlayerLevel()
	{
		return _minPlayerLevel;
	}
	
	/**
	 * Retrieves the maximum player level allowed for this bait.<br>
	 * This value determines the upper limit of a player's level to use it.
	 * @return The maximum player level as an {@code int}.
	 */
	public int getMaxPlayerLevel()
	{
		return _maxPlayerLevel;
	}
	
	/**
	 * Retrieves the success probability for this bait.<br>
	 * The value is returned as a {@code double}.
	 * @return The chance value of the bait.
	 */
	public double getChance()
	{
		return _chance;
	}
	
	/**
	 * Returns the minimum time for fishing.<br>
	 * This value is used to determine the duration of a fishing action.
	 * @return The minimum time as an {@code int}.
	 */
	public int getTimeMin()
	{
		return _timeMin;
	}
	
	/**
	 * Retrieves the maximum time for this bait.<br>
	 * This value represents the upper limit of the fishing duration.
	 * @return The maximum time as an {@code int}.
	 */
	public int getTimeMax()
	{
		return _timeMax;
	}
	
	/**
	 * Gets the minimum wait time for this bait.<br>
	 * This value is used to determine how long a player must wait before fishing again.
	 * @return The minimum wait time as an {@code int}.
	 */
	public int getWaitMin()
	{
		return _waitMin;
	}
	
	/**
	 * Gets the maximum waiting time for this bait.<br>
	 * This value represents the upper limit of the random wait period.
	 * @return The maximum wait time as an {@code int}.
	 */
	public int getWaitMax()
	{
		return _waitMax;
	}
	
	/**
	 * Checks if this bait is restricted to premium players.<br>
	 * Returns {@code true} if the bait requires a premium account.<br>
	 * Returns {@code false} if any player can use it.
	 * @return The premium status of the bait.
	 */
	public boolean isPremiumOnly()
	{
		return _isPremiumOnly;
	}
	
	/**
	 * Retrieves the list of rewards for this bait.<br>
	 * This method returns all {@code FishingCatch} objects associated with the bait.
	 * @return a {@code List} of {@code FishingCatch} objects.
	 */
	public List<FishingCatch> getRewards()
	{
		return _rewards;
	}
	
	/**
	 * Adds a new reward to the bait's reward list.<br>
	 * This method updates the internal collection of {@code FishingCatch} objects.
	 * @param catchData The {@code FishingCatch} object to be added.
	 */
	public void addReward(FishingCatch catchData)
	{
		_rewards.add(catchData);
	}
	
	/**
	 * Selects a random reward from the available list.<br>
	 * It uses a weighted random system based on individual chances.<br>
	 * If no reward is selected, it returns {@code null}.
	 * @return The selected {@code FishingCatch} object or {@code null}.
	 */
	public FishingCatch getRandom()
	{
		float random = Rnd.get(100);
		for (FishingCatch fishingCatchData : _rewards)
		{
			final float chance = fishingCatchData.getChance();
			if (chance > random)
			{
				return fishingCatchData;
			}
			
			random -= chance;
		}
		
		return null;
	}
}
