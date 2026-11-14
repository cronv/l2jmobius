/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2jmobius.gameserver.entity.item.holders;

import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class represents an {@code ItemHolder} that contains specific point values.<br>
 * It is used to manage and store points associated with an item.
 * @author Sdw
 */
public class ItemPointHolder extends ItemHolder
{
	private final int _points;
	
	/**
	 * Creates a new {@code ItemPointHolder} using values from a {@code StatSet}.<br>
	 * This constructor extracts the required data to initialize the object.
	 * @param params The {@code StatSet} containing the item properties.
	 */
	public ItemPointHolder(StatSet params)
	{
		this(params.getInt("id"), params.getLong("count"), params.getInt("points"));
	}
	
	/**
	 * Creates a new {@code ItemPointHolder} instance.<br>
	 * This constructor initializes the item with its ID, quantity, and point value.
	 * @param id The unique identifier for the item.
	 * @param count The total number of items.
	 * @param points The amount of points associated with this item.
	 */
	public ItemPointHolder(int id, long count, int points)
	{
		super(id, count);
		_points = points;
	}
	
	/**
	 * Retrieves the current point value.<br>
	 * This method returns the integer stored in {@code _points}.
	 * @return The number of points associated with this item.
	 */
	public int getPoints()
	{
		return _points;
	}
	
	/**
	 * Returns a string representation of this {@code ItemPointHolder}.<br>
	 * It includes the class name, item ID, count, and point value.
	 * @return A formatted string describing the current state of the holder.
	 */
	@Override
	public String toString()
	{
		return "[" + getClass().getSimpleName() + "] ID: " + getId() + ", count: " + getCount() + ", points: " + _points;
	}
}
