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
package org.l2jmobius.gameserver.entity.actor.stat;

import org.l2jmobius.gameserver.entity.actor.instance.StaticObject;

/**
 * Represents the statistics for a {@code StaticObject}.<br>
 * This class handles data specific to non-living entities in the game world.<br>
 * It extends {@code CreatureStat} to provide base functionality for static objects.
 */
public class StaticObjectStat extends CreatureStat
{
	/**
	 * Creates a new {@code StaticObjectStat} instance.<br>
	 * This constructor initializes the statistics for a specific object.
	 * @param activeChar The {@code StaticObject} associated with these stats.
	 */
	public StaticObjectStat(StaticObject activeChar)
	{
		super(activeChar);
	}
	
	/**
	 * Retrieves the {@code StaticObject} associated with this statistic.<br>
	 * This method returns the character currently being modified by these stats.
	 * @return the current {@code StaticObject} instance.
	 */
	@Override
	public StaticObject getActiveChar()
	{
		return (StaticObject) super.getActiveChar();
	}
	
	/**
	 * Retrieves the current level of the associated character.<br>
	 * This method calls {@code getActiveChar()} to fetch the level.
	 * @return The integer level of the active character.
	 */
	@Override
	public int getLevel()
	{
		return getActiveChar().getLevel();
	}
}
