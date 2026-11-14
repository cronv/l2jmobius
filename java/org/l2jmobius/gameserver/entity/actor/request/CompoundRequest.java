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
package org.l2jmobius.gameserver.entity.actor.request;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.instance.Item;

/**
 * This class represents a request that contains multiple sub-requests.<br>
 * It allows for the grouping and sequential processing of several {@code AbstractRequest} objects.
 * @author UnAfraid
 */
public class CompoundRequest extends AbstractRequest
{
	private int _itemOne;
	private int _itemTwo;
	
	/**
	 * Creates a new {@code CompoundRequest} for a specific player.<br>
	 * This constructor initializes the request with the provided {@code Player} object.
	 * @param player The {@code Player} who is initiating the request.
	 */
	public CompoundRequest(Player player)
	{
		super(player);
	}
	
	/**
	 * Retrieves the first item associated with this request.<br>
	 * It looks up the {@code Item} using the stored object ID.
	 * @return The {@code Item} object if found, or {@code null} otherwise.
	 */
	public Item getItemOne()
	{
		return getPlayer().getInventory().getItemByObjectId(_itemOne);
	}
	
	/**
	 * Sets the ID for {@code itemOne}.<br>
	 * This updates the internal value used by {@code getItemOne}.
	 * @param itemOne The unique identifier for the first item.
	 */
	public void setItemOne(int itemOne)
	{
		_itemOne = itemOne;
	}
	
	/**
	 * Retrieves the second item from the player's inventory.<br>
	 * This method looks up the {@code Item} using the internal ID.
	 * @return The {@code Item} object associated with the second request slot.
	 */
	public Item getItemTwo()
	{
		return getPlayer().getInventory().getItemByObjectId(_itemTwo);
	}
	
	/**
	 * Sets the ID for the second item in the compound request.<br>
	 * This updates the {@code _itemTwo} field.
	 * @param itemTwo The unique identifier of the second item.
	 */
	public void setItemTwo(int itemTwo)
	{
		_itemTwo = itemTwo;
	}
	
	/**
	 * Checks if the current request involves an item.<br>
	 * This method always returns {@code true}.
	 * @return {@code true} if this is an item request.
	 */
	@Override
	public boolean isItemRequest()
	{
		return true;
	}
	
	/**
	 * Checks if this request can be processed with another request.<br>
	 * It returns {@code true} if the other request is not an item request.<br>
	 * This ensures that compound requests are only paired with non-item actions.
	 * @param request The {@code AbstractRequest} to check against.
	 * @return {@code true} if the request is compatible, otherwise {@code false}.
	 */
	@Override
	public boolean canWorkWith(AbstractRequest request)
	{
		return !request.isItemRequest();
	}
	
	/**
	 * Checks if the request involves a specific object.<br>
	 * It returns {@code true} if the {@code objectId} matches either of the items in this request.<br>
	 * The ID must be greater than {@code 0}.
	 * @param objectId The unique identifier of the object to check.
	 * @return {@code true} if the object is part of the request, otherwise {@code false}.
	 */
	@Override
	public boolean isUsing(int objectId)
	{
		return (objectId > 0) && ((objectId == _itemOne) || (objectId == _itemTwo));
	}
}
