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
import org.l2jmobius.gameserver.entity.item.holders.ItemHolder;

/**
 * This class holds the data for a relic compound fee.<br>
 * It extends {@code ItemHolder} to manage specific properties related to relic crafting costs.
 * @author Brado
 */
public class RelicCompoundFeeHolder extends ItemHolder
{
	private final RelicGrade _grade;
	
	/**
	 * Creates a new instance of {@code RelicCompoundFeeHolder}.<br>
	 * This constructor initializes the item data and its specific relic grade.
	 * @param grade The {@code RelicGrade} assigned to this fee.
	 * @param id The unique identifier for the item.
	 * @param count The quantity of the item.
	 */
	public RelicCompoundFeeHolder(RelicGrade grade, int id, long count)
	{
		super(id, count);
		_grade = grade;
	}
	
	/**
	 * Retrieves the {@code RelicGrade} associated with this fee holder.<br>
	 * This value determines the specific grade of the relic.
	 * @return The {@code RelicGrade} of the item.
	 */
	public RelicGrade getFeeGrade()
	{
		return _grade;
	}
}
