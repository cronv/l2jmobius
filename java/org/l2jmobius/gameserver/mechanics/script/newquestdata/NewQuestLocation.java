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
package org.l2jmobius.gameserver.mechanics.script.newquestdata;

/**
 * Holds the start, end and quest location ids for a quest.
 * @author Magik
 */
public class NewQuestLocation
{
	private final int _startLocationId;
	private final int _endLocationId;
	private final int _questLocationId;
	
	/**
	 * Creates a location holder for the start, end and quest location ids.
	 * @param startLocationId The start location id.
	 * @param endLocationId The end location id.
	 * @param questLocationId The quest location id.
	 */
	public NewQuestLocation(int startLocationId, int endLocationId, int questLocationId)
	{
		_startLocationId = startLocationId;
		_endLocationId = endLocationId;
		_questLocationId = questLocationId;
	}
	
	/**
	 * Retrieves the start location id.
	 * @return The start location id as an {@code int}.
	 */
	public int getStartLocationId()
	{
		return _startLocationId;
	}
	
	/**
	 * Retrieves the end location id.
	 * @return The end location id as an {@code int}.
	 */
	public int getEndLocationId()
	{
		return _endLocationId;
	}
	
	/**
	 * Retrieves the quest location id.
	 * @return The quest location id as an {@code int}.
	 */
	public int getQuestLocationId()
	{
		return _questLocationId;
	}
}
