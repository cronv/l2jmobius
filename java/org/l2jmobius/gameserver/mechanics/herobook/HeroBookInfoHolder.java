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
package org.l2jmobius.gameserver.mechanics.herobook;

/**
 * This class acts as a data container for {@code HeroBook} information.<br>
 * It stores the necessary properties and state required by the {@code HeroBook} mechanic.
 * @author Index
 */
public class HeroBookInfoHolder
{
	private int _currentLevel;
	private int _currentExp;
	
	/**
	 * Creates a new instance of {@code HeroBookInfoHolder}.<br>
	 * This constructor initializes the holder with default values.
	 */
	public HeroBookInfoHolder()
	{
	}
	
	/**
	 * Retrieves the current level of the hero book.<br>
	 * This value is stored in the {@code _currentLevel} field.
	 * @return The current level as an {@code int}.
	 */
	public int getCurrentLevel()
	{
		return _currentLevel;
	}
	
	/**
	 * Updates the current level of the hero book.<br>
	 * This method sets the {@code _currentLevel} field to a new value.
	 * @param currentLevel The new level to set for the hero book.
	 */
	public void setCurrentLevel(int currentLevel)
	{
		_currentLevel = currentLevel;
	}
	
	/**
	 * Retrieves the current experience points.<br>
	 * This value represents the progress made toward the next level.
	 * @return The current amount of experience as an {@code int}.
	 */
	public int getCurrentExp()
	{
		return _currentExp;
	}
	
	/**
	 * Updates the current experience points.<br>
	 * This method sets the {@code _currentExp} field to a new value.
	 * @param currentExp The new amount of experience to set.
	 */
	public void setCurrentExp(int currentExp)
	{
		_currentExp = currentExp;
	}
}
