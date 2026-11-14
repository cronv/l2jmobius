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
package org.l2jmobius.gameserver.entity.item.combination;

import java.util.EnumMap;
import java.util.Map;

import org.l2jmobius.gameserver.util.StatSet;

/**
 * Represents an item used in the combination system.<br>
 * This class stores data related to items that can be combined or crafted.<br>
 * It helps manage the requirements and results of specific combinations.
 * @author UnAfraid, Mobius
 */
public class CombinationItem
{
	private final int _itemOne;
	private final int _enchantOne;
	private final int _itemTwo;
	private final int _enchantTwo;
	private final long _commission;
	private final float _chance;
	private final boolean _announce;
	private final Map<CombinationItemType, CombinationItemReward> _rewards = new EnumMap<>(CombinationItemType.class);
	
	/**
	 * Creates a new {@code CombinationItem} using data from a {@code StatSet}.<br>
	 * This constructor initializes the required items and success chance.
	 * @param set The {@code StatSet} containing the configuration values.
	 */
	public CombinationItem(StatSet set)
	{
		_itemOne = set.getInt("one");
		_enchantOne = set.getInt("enchantOne", 0);
		_itemTwo = set.getInt("two");
		_enchantTwo = set.getInt("enchantTwo", 0);
		_commission = set.getLong("commission", 0);
		_chance = set.getFloat("chance", 33);
		_announce = set.getBoolean("announce", false);
	}
	
	/**
	 * Retrieves the ID of the first item required for a combination.<br>
	 * This value is stored in the {@code _itemOne} field.
	 * @return The unique identifier for the first item as an {@code int}.
	 */
	public int getItemOne()
	{
		return _itemOne;
	}
	
	/**
	 * Retrieves the enchantment level of the first item.<br>
	 * This value is used during the combination process.
	 * @return The {@code int} value representing the first item's enchantment.
	 */
	public int getEnchantOne()
	{
		return _enchantOne;
	}
	
	/**
	 * Retrieves the ID of the second item required for a combination.<br>
	 * This value is stored in the {@code _itemTwo} field.
	 * @return The integer ID of the second item.
	 */
	public int getItemTwo()
	{
		return _itemTwo;
	}
	
	/**
	 * Retrieves the enchantment level of the second item.<br>
	 * This value is used during the combination process.
	 * @return The integer value of {@code _enchantTwo}.
	 */
	public int getEnchantTwo()
	{
		return _enchantTwo;
	}
	
	/**
	 * Retrieves the commission fee for this combination.<br>
	 * The value is returned as a {@code long}.
	 * @return The commission amount.
	 */
	public long getCommission()
	{
		return _commission;
	}
	
	/**
	 * Retrieves the success probability for this combination.<br>
	 * The value is represented as a {@code float}.
	 * @return The chance of the combination succeeding.
	 */
	public float getChance()
	{
		return _chance;
	}
	
	/**
	 * Checks if the combination result should be announced to other players.<br>
	 * Returns {@code true} if an announcement is enabled.<br>
	 * Returns {@code false} if no announcement should occur.
	 * @return The announcement status as a {@code boolean}.
	 */
	public boolean isAnnounce()
	{
		return _announce;
	}
	
	/**
	 * Adds a new reward to the combination list.<br>
	 * This method maps the {@code CombinationItemReward} to its specific type.<br>
	 * It updates the internal rewards map used for processing combinations.
	 * @param item The {@code CombinationItemReward} object to be added.
	 */
	public void addReward(CombinationItemReward item)
	{
		_rewards.put(item.getType(), item);
	}
	
	/**
	 * Retrieves the reward associated with a specific combination type.<br>
	 * This method looks up the result in the internal rewards map.
	 * @param type The {@code CombinationItemType} to look up.
	 * @return The corresponding {@code CombinationItemReward} or {@code null}.
	 */
	public CombinationItemReward getReward(CombinationItemType type)
	{
		return _rewards.get(type);
	}
}
