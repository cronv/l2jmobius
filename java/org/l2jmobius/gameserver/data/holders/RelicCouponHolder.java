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
package org.l2jmobius.gameserver.data.holders;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.l2jmobius.gameserver.entity.actor.enums.player.RelicGrade;

/**
 * This class acts as a data container for {@code RelicCoupon} objects.<br>
 * It manages the collection of coupons associated with a specific entity.
 * @author Brado
 */
public class RelicCouponHolder
{
	private final int _itemId;
	private final int _relicId;
	private final int _summonCount;
	private final Set<Integer> _disabledIds = new HashSet<>();
	private final Map<RelicGrade, Integer> _grades = new LinkedHashMap<>();
	private final Map<Integer, Integer> _chanceRolls = new HashMap<>();
	
	/**
	 * Creates a new {@code RelicCouponHolder} instance.<br>
	 * This constructor initializes the basic properties of the coupon.
	 * @param itemId The unique identifier for the item.
	 * @param relicId The unique identifier for the relic.
	 * @param summonCount The number of summons allowed by this coupon.
	 */
	public RelicCouponHolder(int itemId, int relicId, int summonCount)
	{
		_itemId = itemId;
		_relicId = relicId;
		_summonCount = summonCount;
	}
	
	/**
	 * Creates a new {@code RelicCouponHolder} with specific grade and exclusion rules.<br>
	 * This constructor initializes the coupon with custom rewards and restricted IDs.
	 * @param itemId The unique identifier for the item.
	 * @param summonCount The number of summons allowed by this coupon.
	 * @param grades A map defining the {@code RelicGrade} distribution.
	 * @param disabledIds A set of IDs that are excluded from being awarded.
	 */
	public RelicCouponHolder(int itemId, int summonCount, Map<RelicGrade, Integer> grades, Set<Integer> disabledIds)
	{
		_itemId = itemId;
		_relicId = 0;
		_summonCount = summonCount;
		_grades.putAll(grades);
		_disabledIds.addAll(disabledIds);
	}
	
	/**
	 * Creates a new {@code RelicCouponHolder} using specific roll chances.<br>
	 * This constructor sets the default {@code relicId} to {@code 0}.<br>
	 * It populates the internal chance map with provided values.
	 * @param itemId The unique identifier for the item.
	 * @param summonCount The number of summons allowed.
	 * @param chanceRolls A map containing the roll chances.
	 */
	public RelicCouponHolder(int itemId, int summonCount, Map<Integer, Integer> chanceRolls)
	{
		_itemId = itemId;
		_relicId = 0;
		_summonCount = summonCount;
		_chanceRolls.putAll(chanceRolls);
	}
	
	/**
	 * Retrieves the unique identifier for the item.<br>
	 * This value is stored in the {@code _itemId} field.
	 * @return The {@code int} ID of the item.
	 */
	public int getItemId()
	{
		return _itemId;
	}
	
	/**
	 * Retrieves the unique identifier for the relic.<br>
	 * This value is stored within the {@code RelicCouponHolder}.
	 * @return The {@code int} ID of the relic.
	 */
	public int getRelicId()
	{
		return _relicId;
	}
	
	/**
	 * Retrieves the total number of summons allowed by this coupon.<br>
	 * This value is stored in the {@code _summonCount} field.
	 * @return The integer count of available relic summons.
	 */
	public int getRelicSummonCount()
	{
		return _summonCount;
	}
	
	/**
	 * Retrieves the map of relic grades and their associated values.<br>
	 * This method returns the internal grade data for this coupon.
	 * @return a {@code Map<RelicGrade, Integer>} containing the relic grades.
	 */
	public Map<RelicGrade, Integer> getCouponRelicGrades()
	{
		return _grades;
	}
	
	/**
	 * Retrieves the list of disabled IDs.<br>
	 * These IDs are excluded from the current coupon logic.
	 * @return a {@code Collection<Integer>} containing all disabled IDs.
	 */
	public Collection<Integer> getDisabledIds()
	{
		return _disabledIds;
	}
	
	/**
	 * Retrieves the map of chance rolls for this coupon.<br>
	 * This map contains the specific probabilities used during a roll.
	 * @return A {@code Map<Integer, Integer>} containing the chance rolls.
	 */
	public Map<Integer, Integer> getChanceRolls()
	{
		return _chanceRolls;
	}
}