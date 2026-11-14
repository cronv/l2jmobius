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
 * Defines the different scopes available for player rankings.<br>
 * This enum determines how ranking data is filtered and displayed to users.
 * @author Berezkin Nikolay
 */
public enum RankingScope
{
	TOP_100(0),
	TOP_150(0),
	ALL(0),
	SELF(1);
	
	final private int _id;
	
	/**
	 * Creates a new instance of {@code RankingScope}.<br>
	 * This constructor assigns an internal identifier to the scope.
	 * @param id The unique integer value for this scope.
	 */
	private RankingScope(int id)
	{
		_id = id;
	}
	
	/**
	 * Returns the unique identifier for this {@code RankingScope}.<br>
	 * This value is used to distinguish between different ranking types.
	 * @return The integer ID of the scope.
	 */
	public int getId()
	{
		return _id;
	}
}
