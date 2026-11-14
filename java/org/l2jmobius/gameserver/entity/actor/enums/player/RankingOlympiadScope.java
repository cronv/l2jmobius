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
package org.l2jmobius.gameserver.entity.actor.enums.player;

/**
 * Defines the scope for ranking systems within the Olympiad event.<br>
 * This enum determines which players or groups are included in specific rankings.
 * @author Berezkin Nikolay
 */
public enum RankingOlympiadScope
{
	TOP_100(0),
	TOP_50(0),
	ALL(0),
	SELF(1);
	
	final private int _id;
	
	/**
	 * Creates a new instance of {@code RankingOlympiadScope}.<br>
	 * This constructor initializes the internal ID.
	 * @param id The unique identifier for the scope.
	 */
	private RankingOlympiadScope(int id)
	{
		_id = id;
	}
	
	/**
	 * Retrieves the unique identifier for this {@code RankingOlympiadScope}.<br>
	 * This value is used to distinguish between different scope types.
	 * @return The integer ID of the scope.
	 */
	public int getId()
	{
		return _id;
	}
}
