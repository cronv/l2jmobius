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
package org.l2jmobius.gameserver.mechanics.ensoul;

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.gameserver.entity.item.holders.ItemHolder;

/**
 * This class handles the calculation of fees associated with the {@code ensoul} mechanic.<br>
 * It determines the costs required for players to perform specific actions within this system.
 * @author UnAfraid
 */
public class EnsoulFee
{
	private final Integer _stoneId;
	
	private final ItemHolder[] _ensoulFee = new ItemHolder[3];
	private final ItemHolder[] _resoulFees = new ItemHolder[3];
	private final List<ItemHolder> _removalFee = new ArrayList<>();
	
	/**
	 * Creates a new {@code EnsoulFee} object.<br>
	 * This constructor initializes the fee with a specific stone identifier.
	 * @param stoneId The unique identifier for the stone.
	 */
	public EnsoulFee(Integer stoneId)
	{
		_stoneId = stoneId;
	}
	
	/**
	 * Retrieves the unique identifier for the stone.<br>
	 * This value is used to identify which specific stone type is associated with this fee.
	 * @return the {@code Integer} ID of the stone.
	 */
	public Integer getStoneId()
	{
		return _stoneId;
	}
	
	/**
	 * Sets the required item for a specific ensoul fee.<br>
	 * This updates the {@code _ensoulFee} array at the given position.
	 * @param index The position in the array to update.
	 * @param item The {@code ItemHolder} object to assign.
	 */
	public void setEnsoul(int index, ItemHolder item)
	{
		_ensoulFee[index] = item;
	}
	
	/**
	 * Sets a specific {@code ItemHolder} for the resoul fee.<br>
	 * This updates the array at the given position.
	 * @param index The position in the array to update.
	 * @param item The {@code ItemHolder} to store at the specified index.
	 */
	public void setResoul(int index, ItemHolder item)
	{
		_resoulFees[index] = item;
	}
	
	/**
	 * Adds a new item to the removal fee list.<br>
	 * This method stores the provided {@code ItemHolder} in the internal collection.
	 * @param itemHolder The {@code ItemHolder} object to be added.
	 */
	public void addRemovalFee(ItemHolder itemHolder)
	{
		_removalFee.add(itemHolder);
	}
	
	/**
	 * Retrieves an {@code ItemHolder} from the ensoul fee list.<br>
	 * The item is located at the specified position in the array.
	 * @param index The position of the item to retrieve.
	 * @return The {@code ItemHolder} at the given index.
	 */
	public ItemHolder getEnsoul(int index)
	{
		return _ensoulFee[index];
	}
	
	/**
	 * Retrieves a specific {@code ItemHolder} from the resoul fees list.<br>
	 * This method uses the provided {@code index} to find the item.
	 * @param index The position of the item in the array.
	 * @return The {@code ItemHolder} at the specified position.
	 */
	public ItemHolder getResoul(int index)
	{
		return _resoulFees[index];
	}
	
	/**
	 * Retrieves the list of items required for a removal fee.<br>
	 * This method returns all {@code ItemHolder} objects added to the removal fee list.
	 * @return A {@code List} containing the {@code ItemHolder} objects.
	 */
	public List<ItemHolder> getRemovalFee()
	{
		return _removalFee;
	}
}
