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
import java.util.Collections;
import java.util.List;

import org.l2jmobius.gameserver.entity.item.ItemTemplate;

/**
 * This class manages a collection of {@code ItemTemplate} objects that represent enchantment scrolls.<br>
 * It provides utility methods to group and handle these items together within the game logic.
 * @author UnAfraid
 */
public class EnchantScrollGroup
{
	private final int _id;
	private List<EnchantRateItem> _rateGroups;
	
	/**
	 * Creates a new {@code EnchantScrollGroup} instance.<br>
	 * This constructor initializes the group with a specific unique identifier.
	 * @param id The unique identification number for this group.
	 */
	public EnchantScrollGroup(int id)
	{
		_id = id;
	}
	
	/**
	 * Retrieves the unique identifier for this {@code EnchantScrollGroup}.<br>
	 * This value is assigned when the group is created.
	 * @return The integer ID of the group.
	 */
	public int getId()
	{
		return _id;
	}
	
	/**
	 * Adds a new {@code EnchantRateItem} to the group.<br>
	 * This method initializes the internal list if it is currently {@code null}.<br>
	 * The provided {@code group} is added to the collection of rate groups.
	 * @param group The {@code EnchantRateItem} to be added to the group.
	 */
	public void addRateGroup(EnchantRateItem group)
	{
		if (_rateGroups == null)
		{
			_rateGroups = new ArrayList<>();
		}
		
		_rateGroups.add(group);
	}
	
	/**
	 * Retrieves the list of enchantment rate groups.<br>
	 * Returns an empty {@code List} if no groups exist.
	 * @return a {@code List} of {@code EnchantRateItem} objects.
	 */
	public List<EnchantRateItem> getRateGroups()
	{
		return _rateGroups != null ? _rateGroups : Collections.emptyList();
	}
	
	/**
	 * Finds the specific {@code EnchantRateItem} for a given item.<br>
	 * It checks all available groups to see if they match the provided template.<br>
	 * Returns {@code null} if no matching group is found.
	 * @param item The {@code ItemTemplate} to check against the rate groups.
	 * @return The matching {@code EnchantRateItem} or {@code null}.
	 */
	public EnchantRateItem getRateGroup(ItemTemplate item)
	{
		for (EnchantRateItem group : getRateGroups())
		{
			if (group.validate(item))
			{
				return group;
			}
		}
		
		return null;
	}
}
