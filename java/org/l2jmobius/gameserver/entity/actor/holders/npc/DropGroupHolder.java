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
package org.l2jmobius.gameserver.entity.actor.holders.npc;

import java.util.ArrayList;
import java.util.List;

/**
 * This class acts as a container for a collection of drops associated with an NPC.<br>
 * It helps manage how items are grouped and handled during loot distribution.
 * @author Mobius
 */
public class DropGroupHolder
{
	private final List<DropHolder> _dropList = new ArrayList<>();
	private final double _chance;
	
	/**
	 * Creates a new {@code DropGroupHolder} with a specific drop chance.<br>
	 * This value determines the probability for this group of drops.
	 * @param chance The probability value to assign to this holder.
	 */
	public DropGroupHolder(double chance)
	{
		_chance = chance;
	}
	
	/**
	 * Retrieves the list of drops associated with this group.<br>
	 * This method returns all {@code DropHolder} objects currently stored in the internal list.
	 * @return A {@code List} containing all {@code DropHolder} objects.
	 */
	public List<DropHolder> getDropList()
	{
		return _dropList;
	}
	
	/**
	 * Adds a new {@code DropHolder} to the internal list.<br>
	 * This method updates the collection of drops for this group.
	 * @param holder The {@code DropHolder} object to be added.
	 */
	public void addDrop(DropHolder holder)
	{
		_dropList.add(holder);
	}
	
	/**
	 * Returns the probability of this drop group occurring.<br>
	 * The value is represented as a {@code double}.
	 * @return The chance value.
	 */
	public double getChance()
	{
		return _chance;
	}
	
	/**
	 * Sorts the internal list of {@code DropHolder} objects.<br>
	 * The items are ordered by their chance value in ascending order.
	 */
	public void sortByChance()
	{
		_dropList.sort((d1, d2) -> Double.compare(d1.getChance(), d2.getChance()));
	}
}
