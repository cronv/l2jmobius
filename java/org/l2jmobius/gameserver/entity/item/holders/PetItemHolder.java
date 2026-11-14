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

import org.l2jmobius.gameserver.entity.item.instance.Item;

/**
 * This class manages the items associated with a pet entity.<br>
 * It acts as a container for {@code Item} instances linked to specific pets.
 * @author UnAfraid
 */
public class PetItemHolder
{
	private final Item _item;
	
	/**
	 * Creates a new {@code PetItemHolder} instance.<br>
	 * This constructor assigns the provided {@code Item} to the holder.
	 * @param item The {@code Item} to be stored in this holder.
	 */
	public PetItemHolder(Item item)
	{
		_item = item;
	}
	
	/**
	 * Retrieves the {@code Item} associated with this holder.<br>
	 * This method returns the internal item instance.
	 * @return The {@code Item} object.
	 */
	public Item getItem()
	{
		return _item;
	}
}
