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
 * Holds the compound chance configuration for a specific relic grade.<br>
 * This class maps to the {@code <combineData>} XML element in {@code RelicData.xml}.
 * @author CostyKiller
 */
public class RelicCompoundChanceHolder
{
	private final RelicGrade _grade;
	private final int _failCommonChance;
	private final int _failShiningChance;
	private final int _successCommonChance;
	private final int _successShiningChance;
	
	/**
	 * Creates a new {@code RelicCompoundChanceHolder} instance.<br>
	 * This constructor initializes the chances for different relic outcomes based on the provided grade.
	 * @param grade The {@code RelicGrade} this holder applies to.
	 * @param failCommonChance The chance to fail and receive a common item.
	 * @param failShiningChance The chance to fail and receive a shining item.
	 * @param successCommonChance The chance to succeed and receive a common item.
	 * @param successShiningChance The chance to succeed and receive a shining item.
	 */
	public RelicCompoundChanceHolder(RelicGrade grade, int failCommonChance, int failShiningChance, int successCommonChance, int successShiningChance)
	{
		_grade = grade;
		_failCommonChance = failCommonChance;
		_failShiningChance = failShiningChance;
		_successCommonChance = successCommonChance;
		_successShiningChance = successShiningChance;
	}
	
	/**
	 * Retrieves the {@code RelicGrade} associated with this holder.<br>
	 * This value identifies which grade these chances belong to.
	 * @return The {@code RelicGrade} of this configuration.
	 */
	public RelicGrade getGrade()
	{
		return _grade;
	}
	
	/**
	 * Retrieves the failure chance for a common grade relic.<br>
	 * This value is used during the compound chance calculation.
	 * @return The integer value of the fail common chance.
	 */
	public int getFailCommonChance()
	{
		return _failCommonChance;
	}
	
	/**
	 * Retrieves the chance for a failed shining result.<br>
	 * This value is specific to the current {@code RelicGrade}.
	 * @return The integer percentage of the fail shining chance.
	 */
	public int getFailShiningChance()
	{
		return _failShiningChance;
	}
	
	/**
	 * Retrieves the success chance for a common grade relic.<br>
	 * This value is used during the compound chance calculation.
	 * @return The {@code int} value representing the success chance.
	 */
	public int getSuccessCommonChance()
	{
		return _successCommonChance;
	}
	
	/**
	 * Retrieves the chance for a successful shining result.<br>
	 * This value is specific to the current {@code RelicGrade}.
	 * @return The success shining chance as an {@code int}.
	 */
	public int getSuccessShiningChance()
	{
		return _successShiningChance;
	}
	
	/**
	 * Calculates the total success chance for this relic grade.<br>
	 * It adds the common and shining success chances together.
	 * @return The sum of {@code _successCommonChance} and {@code _successShiningChance}.
	 */
	public int getTotalSuccessChance()
	{
		return _successCommonChance + _successShiningChance;
	}
}
