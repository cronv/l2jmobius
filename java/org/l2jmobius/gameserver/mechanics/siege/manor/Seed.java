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
package org.l2jmobius.gameserver.mechanics.siege.manor;

import org.l2jmobius.gameserver.config.RatesConfig;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Represents a seed used in the manor siege mechanics.<br>
 * This class handles the data and logic for specific seeds within the {@code Seed} entity.
 */
public class Seed
{
	private final int _seedId;
	private final int _cropId; // crop type
	private final int _level; // seed level
	private final int _matureId; // mature crop type
	private final int _reward1;
	private final int _reward2;
	private final int _castleId; // id of manor (castle id) where seed can be farmed
	private final boolean _isAlternative;
	private final int _limitSeeds;
	private final int _limitCrops;
	private final long _seedReferencePrice;
	private final long _cropReferencePrice;
	
	/**
	 * Creates a new {@code Seed} instance using data from a {@code StatSet}.<br>
	 * This constructor initializes all seed properties and reference prices.
	 * @param set The {@code StatSet} containing the required configuration values.
	 */
	public Seed(StatSet set)
	{
		_cropId = set.getInt("id");
		_seedId = set.getInt("seedId");
		_level = set.getInt("level");
		_matureId = set.getInt("mature_Id");
		_reward1 = set.getInt("reward1");
		_reward2 = set.getInt("reward2");
		_castleId = set.getInt("castleId");
		_isAlternative = set.getBoolean("alternative");
		_limitCrops = set.getInt("limit_crops");
		_limitSeeds = set.getInt("limit_seed");
		
		// Set prices
		ItemTemplate item = ItemData.getInstance().getTemplate(_cropId);
		_cropReferencePrice = (item != null) ? item.getReferencePrice() : 1;
		item = ItemData.getInstance().getTemplate(_seedId);
		_seedReferencePrice = (item != null) ? item.getReferencePrice() : 1;
	}
	
	/**
	 * Retrieves the unique identifier for the castle.<br>
	 * This ID determines which manor a seed can be farmed in.
	 * @return the {@code int} value of the castle ID.
	 */
	public int getCastleId()
	{
		return _castleId;
	}
	
	/**
	 * Retrieves the unique identifier for this seed.<br>
	 * This value is used to identify the specific {@code Seed} type.
	 * @return The {@code int} ID of the seed.
	 */
	public int getSeedId()
	{
		return _seedId;
	}
	
	/**
	 * Retrieves the unique identifier for the crop type.<br>
	 * This value represents the specific crop associated with this {@code Seed}.
	 * @return the {@code int} ID of the crop.
	 */
	public int getCropId()
	{
		return _cropId;
	}
	
	/**
	 * Returns the ID of the mature crop.<br>
	 * This value represents what the seed grows into.
	 * @return the {@code int} ID of the mature crop.
	 */
	public int getMatureId()
	{
		return _matureId;
	}
	
	/**
	 * Retrieves the reward amount based on a specific type.<br>
	 * This method checks if {@code type} is equal to {@code 1}.<br>
	 * It returns the first reward if true, otherwise it returns the second reward.
	 * @param type The identifier for the reward type to retrieve.
	 * @return The integer value of the requested reward.
	 */
	public int getReward(int type)
	{
		return (type == 1) ? _reward1 : _reward2;
	}
	
	/**
	 * Retrieves the current level of the {@code Seed}.<br>
	 * This value represents the growth stage or rank.
	 * @return the integer level of the seed.
	 */
	public int getLevel()
	{
		return _level;
	}
	
	/**
	 * Checks if this seed is an alternative type.<br>
	 * Returns {@code true} if it is an alternative, otherwise returns {@code false}.
	 * @return the status of whether the seed is an alternative.
	 */
	public boolean isAlternative()
	{
		return _isAlternative;
	}
	
	/**
	 * Returns the maximum number of seeds that can be obtained.<br>
	 * This value is calculated by multiplying the base limit by the manor drop rate.
	 * @return The total seed limit as an {@code int}.
	 */
	public int getSeedLimit()
	{
		return _limitSeeds * RatesConfig.RATE_DROP_MANOR;
	}
	
	/**
	 * Returns the total limit for crops.<br>
	 * This value is calculated by multiplying {@code _limitCrops} by the manor drop rate.
	 * @return The calculated crop limit as an {@code int}.
	 */
	public int getCropLimit()
	{
		return _limitCrops * RatesConfig.RATE_DROP_MANOR;
	}
	
	/**
	 * Retrieves the base price for a seed.<br>
	 * This value is used as a reference point for calculating market prices.
	 * @return The {@code long} value of the seed reference price.
	 */
	public long getSeedReferencePrice()
	{
		return _seedReferencePrice;
	}
	
	/**
	 * Returns the maximum price for a seed.<br>
	 * This value is calculated by multiplying the {@code _seedReferencePrice} by {@code 10}.
	 * @return The maximum price of the seed as a {@code long}.
	 */
	public long getSeedMaxPrice()
	{
		return _seedReferencePrice * 10;
	}
	
	/**
	 * Retrieves the minimum price for a seed.<br>
	 * This value is calculated as {@code 60%} of the reference price.
	 * @return the minimum price as a {@code long}.
	 */
	public long getSeedMinPrice()
	{
		return (int) (_seedReferencePrice * 0.6);
	}
	
	/**
	 * Retrieves the base price for a mature crop.<br>
	 * This value is used as a reference for calculating market prices.
	 * @return The {@code long} value of the crop reference price.
	 */
	public long getCropReferencePrice()
	{
		return _cropReferencePrice;
	}
	
	/**
	 * Retrieves the maximum price for a mature crop.<br>
	 * This value is calculated by multiplying the reference price by {@code 10}.
	 * @return The maximum price of the crop as a {@code long}.
	 */
	public long getCropMaxPrice()
	{
		return _cropReferencePrice * 10;
	}
	
	/**
	 * Returns the minimum price for a crop.<br>
	 * This value is calculated as {@code 60%} of the reference price.
	 * @return the minimum price as an {@code int}.
	 */
	public int getCropMinPrice()
	{
		return (int) (_cropReferencePrice * 0.6);
	}
	
	/**
	 * Returns a string representation of the {@code Seed} object.<br>
	 * This includes all primary data fields for debugging purposes.
	 * @return A formatted string containing the seed's properties.
	 */
	@Override
	public String toString()
	{
		return "SeedData [_id=" + _seedId + ", _level=" + _level + ", _crop=" + _cropId + ", _mature=" + _matureId + ", _type1=" + _reward1 + ", _type2=" + _reward2 + ", _manorId=" + _castleId + ", _isAlternative=" + _isAlternative + ", _limitSeeds=" + _limitSeeds + ", _limitCrops=" + _limitCrops + "]";
	}
}
