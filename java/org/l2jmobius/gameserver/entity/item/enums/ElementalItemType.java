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
package org.l2jmobius.gameserver.entity.item.enums;

/**
 * Defines the different types of elemental properties that items can possess.<br>
 * This enumeration is used to categorize items based on their elemental affinities.
 * @author Mobius
 */
public enum ElementalItemType
{
	STONE(3),
	STONE_SUPER(3),
	CRYSTAL(6),
	CRYSTAL_SUPER(6),
	JEWEL(9),
	ENERGY(12);
	
	private final int _maxLevel;
	
	/**
	 * Creates a new {@code ElementalItemType} with a specific level limit.<br>
	 * This constructor sets the maximum level for the item type.
	 * @param maxLevel The maximum level allowed for this item type.
	 */
	ElementalItemType(int maxLevel)
	{
		_maxLevel = maxLevel;
	}
	
	/**
	 * Returns the maximum level allowed for this {@code ElementalItemType}.<br>
	 * This value is defined during the initialization of the enum.
	 * @return The integer value representing the maximum level.
	 */
	public int getMaxLevel()
	{
		return _maxLevel;
	}
}
