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

import java.util.List;

import org.l2jmobius.gameserver.entity.actor.enums.player.RelicGrade;
import org.l2jmobius.gameserver.entity.item.holders.ItemHolder;

/**
 * Holds the replacement attempt configuration for a specific relic grade.<br>
 * This class maps to the {@code <replaceCategory>} XML element in {@code RelicData.xml}.
 * @author CostyKiller
 */
public class RelicReplaceCategoryHolder
{
	private final RelicGrade _grade;
	private final int _replaceAttempts;
	private final List<ItemHolder> _fees;
	
	/**
	 * Creates a new {@code RelicReplaceCategoryHolder} instance.<br>
	 * This constructor initializes the relic grade, attempt limit, and required fees.
	 * @param grade The {@code RelicGrade} associated with this category.
	 * @param replaceAttempts The maximum number of allowed replacement attempts.
	 * @param fees A {@code List} of {@code ItemHolder} objects required as payment.
	 */
	public RelicReplaceCategoryHolder(RelicGrade grade, int replaceAttempts, List<ItemHolder> fees)
	{
		_grade = grade;
		_replaceAttempts = replaceAttempts;
		_fees = fees;
	}
	
	/**
	 * Retrieves the {@code RelicGrade} associated with this holder.<br>
	 * This value represents the specific grade for the replacement configuration.
	 * @return The {@code RelicGrade} of this category.
	 */
	public RelicGrade getGrade()
	{
		return _grade;
	}
	
	/**
	 * Returns the number of replacement attempts allowed.<br>
	 * This value is specific to the current relic grade.
	 * @return The total number of allowed attempts as an {@code int}.
	 */
	public int getReplaceAttempts()
	{
		return _replaceAttempts;
	}
	
	/**
	 * Retrieves the list of items required as fees.<br>
	 * These items are associated with a specific relic replacement category.
	 * @return A {@code List} of {@code ItemHolder} objects representing the fees.
	 */
	public List<ItemHolder> getFees()
	{
		return _fees;
	}
}
