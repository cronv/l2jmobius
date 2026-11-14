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

import java.util.Collections;
import java.util.List;

import org.l2jmobius.gameserver.entity.item.holders.ItemChanceHolder;

/**
 * This class serves as a data container for crystallization-related information.<br>
 * It stores configuration and state data used by the game server to manage crystallization mechanics.
 * @author UnAfraid
 */
public class CrystallizationDataHolder
{
	private final int _id;
	private final List<ItemChanceHolder> _items;
	
	/**
	 * Creates a new instance of {@code CrystallizationDataHolder}.<br>
	 * This constructor initializes the unique identifier and the list of items.<br>
	 * The provided list is stored as an unmodifiable collection.
	 * @param id The unique identification number for this data holder.
	 * @param items A list of {@code ItemChanceHolder} objects associated with this ID.
	 */
	public CrystallizationDataHolder(int id, List<ItemChanceHolder> items)
	{
		_id = id;
		_items = Collections.unmodifiableList(items);
	}
	
	/**
	 * Retrieves the unique identifier for this {@code CrystallizationDataHolder}.<br>
	 * This value is used to identify specific data entries.
	 * @return The integer {@code id} of the holder.
	 */
	public int getId()
	{
		return _id;
	}
	
	/**
	 * Retrieves the list of items associated with this data holder.<br>
	 * This method returns all {@code ItemChanceHolder} objects.
	 * @return a {@code List} of {@code ItemChanceHolder} objects.
	 */
	public List<ItemChanceHolder> getItems()
	{
		return _items;
	}
}
