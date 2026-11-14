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
package org.l2jmobius.gameserver.entity.item.enchant;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.l2jmobius.gameserver.data.holders.RangeChanceHolder;

/**
 * This class represents a collection of enchantments that are grouped together.<br>
 * It is used to manage multiple {@code Enchant} objects as a single unit.
 * @author UnAfraid
 */
public class EnchantItemGroup
{
	private static final Logger LOGGER = Logger.getLogger(EnchantItemGroup.class.getName());
	private final List<RangeChanceHolder> _chances = new ArrayList<>();
	private final String _name;
	private int _maximumEnchant = -1;
	
	/**
	 * Creates a new {@code EnchantItemGroup} instance.<br>
	 * Sets the unique name for this group.
	 * @param name The name to assign to the group.
	 */
	public EnchantItemGroup(String name)
	{
		_name = name;
	}
	
	/**
	 * Returns the name of this {@code EnchantItemGroup}.<br>
	 * This value was set during object creation.
	 * @return The name as a {@code String}.
	 */
	public String getName()
	{
		return _name;
	}
	
	/**
	 * Adds a new chance to the current group.<br>
	 * This method stores the {@code RangeChanceHolder} in the internal list.
	 * @param holder The {@code RangeChanceHolder} object to add.
	 */
	public void addChance(RangeChanceHolder holder)
	{
		_chances.add(holder);
	}
	
	/**
	 * Retrieves the chance value for a specific enchantment level.<br>
	 * It searches through the list of {@code RangeChanceHolder} objects.<br>
	 * If no matching range is found, it returns the last available chance.<br>
	 * Returns -1 if the group contains no chances.
	 * @param index The enchantment level to check.
	 * @return The chance value as a {@code double}.
	 */
	public double getChance(int index)
	{
		if (!_chances.isEmpty())
		{
			for (RangeChanceHolder holder : _chances)
			{
				if ((holder.getMin() <= index) && (holder.getMax() >= index))
				{
					return holder.getChance();
				}
			}
			// LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Couldn't match proper chance for item group: " + _name);
			return _chances.get(_chances.size() - 1).getChance();
		}
		
		LOGGER.warning(getClass().getSimpleName() + ": item group: " + _name + " doesn't have any chances!");
		return -1;
	}
	
	/**
	 * Retrieves the highest possible enchantment level for this group.<br>
	 * It calculates the maximum value based on all added {@code RangeChanceHolder} objects.<br>
	 * If no chances are defined, it returns a default value.
	 * @return The maximum enchantment level as an {@code int}.
	 */
	public int getMaximumEnchant()
	{
		if (_maximumEnchant == -1)
		{
			for (RangeChanceHolder holder : _chances)
			{
				final int max = holder.getMax();
				if ((holder.getChance() > 0) && (max > _maximumEnchant))
				{
					_maximumEnchant = max;
				}
			}
			
			_maximumEnchant++;
		}
		
		return _maximumEnchant;
	}
}
