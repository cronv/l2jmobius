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
 * Handles the request for a player to use a shape-shifting item.<br>
 * This class processes the logic required to transform an actor based on an {@code Item} instance.
 * @author Sdw
 */
public class ShapeShiftingItemRequest extends AbstractRequest
{
	private Item _appearanceStone;
	private Item _appearanceExtractItem;
	
	/**
	 * Creates a new {@code ShapeShiftingItemRequest} for a specific player.<br>
	 * This request handles the logic for using an appearance stone.
	 * @param player The {@code Player} who is initiating the request.
	 * @param appearanceStone The {@code Item} being used as the appearance stone.
	 */
	public ShapeShiftingItemRequest(Player player, Item appearanceStone)
	{
		super(player);
		_appearanceStone = appearanceStone;
	}
	
	/**
	 * Retrieves the {@code Item} used as an appearance stone.<br>
	 * This method returns the stone associated with this request.
	 * @return the {@code Item} representing the appearance stone.
	 */
	public Item getAppearanceStone()
	{
		return _appearanceStone;
	}
	
	/**
	 * Sets the {@code Item} used for the appearance stone.<br>
	 * This value is stored in the {@code ShapeShiftingItemRequest} object.
	 * @param appearanceStone The {@code Item} to be set as the appearance stone.
	 */
	public void setAppearanceStone(Item appearanceStone)
	{
		_appearanceStone = appearanceStone;
	}
	
	/**
	 * Retrieves the item used for extracting an appearance.<br>
	 * This method returns the {@code Item} stored in this request.
	 * @return the {@code Item} representing the extraction source.
	 */
	public Item getAppearanceExtractItem()
	{
		return _appearanceExtractItem;
	}
	
	/**
	 * Sets the item used to extract an appearance.<br>
	 * This updates the {@code _appearanceExtractItem} field.
	 * @param appearanceExtractItem The {@code Item} to be used for extraction.
	 */
	public void setAppearanceExtractItem(Item appearanceExtractItem)
	{
		_appearanceExtractItem = appearanceExtractItem;
	}
	
	/**
	 * Checks if this request involves an {@code Item}.<br>
	 * This method always returns {@code true} for shape shifting requests.
	 * @return {@code true} if the request is related to an item.
	 */
	@Override
	public boolean isItemRequest()
	{
		return true;
	}
	
	/**
	 * Checks if this request can be processed alongside another request.<br>
	 * It returns {@code true} only if the other request is not an item request.
	 * @param request The {@code AbstractRequest} to check.
	 * @return {@code true} if the requests are compatible, {@code false} otherwise.
	 */
	@Override
	public boolean canWorkWith(AbstractRequest request)
	{
		return !request.isItemRequest();
	}
	
	/**
	 * Checks if the provided object ID matches the required items.<br>
	 * It verifies that both the appearance stone and extract item are not {@code null}.<br>
	 * The check returns {@code true} if the ID belongs to either of these two items.
	 * @param objectId The unique identifier of the object to check.
	 * @return {@code true} if the ID matches a valid required item, otherwise {@code false}.
	 */
	@Override
	public boolean isUsing(int objectId)
	{
		if ((_appearanceStone == null) || (_appearanceExtractItem == null))
		{
			return false;
		}
		
		return (objectId > 0) && ((objectId == _appearanceStone.getObjectId()) || (objectId == _appearanceExtractItem.getObjectId()));
	}
}
