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
package org.l2jmobius.gameserver.entity.item.holders;

import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class represents the default equipment set provided to a character upon creation.<br>
 * It serves as a template for initial items held by new players.
 * @author Mobius
 */
public class InitialEquipment extends ItemHolder
{
	private final boolean _equipped;
	
	/**
	 * Creates a new {@code InitialEquipment} instance.<br>
	 * This constructor initializes the item using data from a {@code StatSet}.<br>
	 * It sets the ID and count from the provided set.<br>
	 * It also determines if the item is equipped based on the boolean value in the set.
	 * @param set The {@code StatSet} containing the initial equipment properties.
	 */
	public InitialEquipment(StatSet set)
	{
		super(set.getInt("id"), set.getInt("count"));
		_equipped = set.getBoolean("equipped", false);
	}
	
	/**
	 * Checks if the item is currently equipped.<br>
	 * Returns {@code true} if it is equipped.<br>
	 * Returns {@code false} otherwise.
	 * @return a boolean indicating the equipment status.
	 */
	public boolean isEquipped()
	{
		return _equipped;
	}
}
