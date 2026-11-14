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
package org.l2jmobius.gameserver.data.holders;

/**
 * This class acts as a data holder for range chance values.<br>
 * It stores the probability of an effect occurring within a specific range.<br>
 * Use this class to manage and access {@code RangeChance} data throughout the server.
 * @author UnAfraid
 */
public class RangeChanceHolder
{
	private final int _min;
	private final int _max;
	private final double _chance;
	
	/**
	 * Creates a new {@code RangeChanceHolder} instance.<br>
	 * This constructor initializes the range and probability values.
	 * @param min The minimum value of the range.
	 * @param max The maximum value of the range.
	 * @param chance The probability of occurrence as a double.
	 */
	public RangeChanceHolder(int min, int max, double chance)
	{
		_min = min;
		_max = max;
		_chance = chance;
	}
	
	/**
	 * Retrieves the minimum value of the range.<br>
	 * This value is stored in the {@code _min} field.
	 * @return The minimum integer value.
	 */
	public int getMin()
	{
		return _min;
	}
	
	/**
	 * Returns the maximum value of the range.<br>
	 * This value is stored in the {@code _max} field.
	 * @return The maximum integer value of the range.
	 */
	public int getMax()
	{
		return _max;
	}
	
	/**
	 * Retrieves the probability value for this range.<br>
	 * This value is stored as a {@code double}.
	 * @return The current chance value.
	 */
	public double getChance()
	{
		return _chance;
	}
}
