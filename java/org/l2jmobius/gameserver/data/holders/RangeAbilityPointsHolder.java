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
 * This class acts as a data holder for range ability points.<br>
 * It stores and manages the specific point values associated with range-based abilities.
 * @author UnAfraid
 */
public class RangeAbilityPointsHolder
{
	private final int _min;
	private final int _max;
	private final long _sp;
	
	/**
	 * Creates a new instance of {@code RangeAbilityPointsHolder}.<br>
	 * This constructor initializes the minimum, maximum, and SP values.
	 * @param min The minimum value for the range.
	 * @param max The maximum value for the range.
	 * @param sp The skill point value.
	 */
	public RangeAbilityPointsHolder(int min, int max, long sp)
	{
		_min = min;
		_max = max;
		_sp = sp;
	}
	
	/**
	 * Retrieves the minimum value for the ability points.<br>
	 * This method returns the {@code _min} field stored in this {@code RangeAbilityPointsHolder}.
	 * @return The minimum integer value.
	 */
	public int getMin()
	{
		return _min;
	}
	
	/**
	 * Retrieves the maximum value from this holder.<br>
	 * This method returns the {@code _max} field.
	 * @return The maximum integer value.
	 */
	public int getMax()
	{
		return _max;
	}
	
	/**
	 * Retrieves the Skill Point value.<br>
	 * This method returns the current {@code sp} stored in this holder.
	 * @return The skill point value as a {@code long}.
	 */
	public long getSP()
	{
		return _sp;
	}
}
