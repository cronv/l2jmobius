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

import org.l2jmobius.gameserver.entity.actor.enums.player.RelicGrade;

/**
 * Holds the replacement chance configuration for a specific relic grade.<br>
 * This class maps to the {@code <replaceData>} XML element in {@code RelicData.xml}.
 * @author CostyKiller
 */
public class RelicReplaceChanceHolder
{
	private final RelicGrade _grade;
	private final int _failChance;
	private final int _successCommonChance;
	private final int _successShiningChance;
	
	/**
	 * Creates a new {@code RelicReplaceChanceHolder} instance.<br>
	 * This constructor initializes the replacement chances for a specific relic grade.
	 * @param grade The {@code RelicGrade} this holder applies to.
	 * @param failChance The percentage chance of failure.
	 * @param successCommonChance The percentage chance of obtaining a common success.
	 * @param successShiningChance The percentage chance of obtaining a shining success.
	 */
	public RelicReplaceChanceHolder(RelicGrade grade, int failChance, int successCommonChance, int successShiningChance)
	{
		_grade = grade;
		_failChance = failChance;
		_successCommonChance = successCommonChance;
		_successShiningChance = successShiningChance;
	}
	
	/**
	 * Retrieves the {@code RelicGrade} associated with this holder.<br>
	 * This value represents the specific grade for the replacement chance.
	 * @return The {@code RelicGrade} of this configuration.
	 */
	public RelicGrade getGrade()
	{
		return _grade;
	}
	
	/**
	 * Returns the chance of failure for a specific relic grade.<br>
	 * This value is retrieved from the {@code _failChance} field.
	 * @return The integer representing the failure percentage.
	 */
	public int getFailChance()
	{
		return _failChance;
	}
	
	/**
	 * Retrieves the success chance for a common grade relic.<br>
	 * This value represents the probability of obtaining a common item.
	 * @return The success chance as an {@code int}.
	 */
	public int getSuccessCommonChance()
	{
		return _successCommonChance;
	}
	
	/**
	 * Retrieves the chance for a success to result in a shining grade.<br>
	 * This value is specific to the {@code RelicGrade} held by this object.
	 * @return The integer percentage of the success shining chance.
	 */
	public int getSuccessShiningChance()
	{
		return _successShiningChance;
	}
	
	/**
	 * Calculates the total success chance for a relic grade.<br>
	 * This value is the sum of {@code _successCommonChance} and {@code _successShiningChance}.
	 * @return The combined success chance as an {@code int}.
	 */
	public int getTotalSuccessChance()
	{
		return _successCommonChance + _successShiningChance;
	}
}
