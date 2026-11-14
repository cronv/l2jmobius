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
package org.l2jmobius.gameserver.entity.item.combination;

import org.l2jmobius.gameserver.entity.item.holders.ItemEnchantHolder;

/**
 * Represents an item received as a reward from a combination process.<br>
 * This class extends {@code ItemHolder} to store the specific result of a successful craft.
 * @author UnAfraid
 */
public class CombinationItemReward extends ItemEnchantHolder
{
	private final CombinationItemType _type;
	
	/**
	 * Creates a new {@code CombinationItemReward} object.<br>
	 * This constructor initializes the item properties and its specific type.
	 * @param id The unique identifier for the item.
	 * @param count The quantity of the item to be awarded.
	 * @param type The {@code CombinationItemType} category of the reward.
	 * @param enchant The enchantment level of the rewarded item.
	 */
	public CombinationItemReward(int id, int count, CombinationItemType type, int enchant)
	{
		super(id, count, enchant);
		_type = type;
	}
	
	/**
	 * Retrieves the type of the combination item.<br>
	 * This method returns the {@code CombinationItemType} associated with this reward.
	 * @return the {@code CombinationItemType} of the reward.
	 */
	public CombinationItemType getType()
	{
		return _type;
	}
}
