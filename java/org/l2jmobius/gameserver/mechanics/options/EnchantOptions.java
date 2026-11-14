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
package org.l2jmobius.gameserver.mechanics.options;

/**
 * This class defines the configuration options for the enchanting system.<br>
 * It allows developers to adjust various parameters related to how enchantments behave in-game.
 * @author UnAfraid
 */
public class EnchantOptions
{
	private final int _level;
	private final int[] _options;
	
	/**
	 * Creates a new {@code EnchantOptions} object.<br>
	 * Sets the initial enchantment level.<br>
	 * Initializes an empty array of options.
	 * @param level The integer value for the enchantment level.
	 */
	public EnchantOptions(int level)
	{
		_level = level;
		_options = new int[3];
	}
	
	/**
	 * Retrieves the current enchantment level.<br>
	 * This value was set during the creation of the {@code EnchantOptions} object.
	 * @return The integer value representing the level.
	 */
	public int getLevel()
	{
		return _level;
	}
	
	/**
	 * Retrieves the list of available options.<br>
	 * This method returns the internal array of values.
	 * @return an {@code int[]} containing the current options.
	 */
	public int[] getOptions()
	{
		return _options;
	}
	
	/**
	 * Updates a specific enchantment option at the given position.<br>
	 * This method modifies the {@code _options} array.<br>
	 * The change only occurs if the {@code index} is within the valid range.
	 * @param index The position in the options array to update.
	 * @param option The new value to assign to the specified index.
	 */
	public void setOption(byte index, int option)
	{
		if (_options.length > index)
		{
			_options[index] = option;
		}
	}
}
