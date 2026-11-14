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
 * This request handles the process of enchanting an item's attributes.<br>
 * It allows a {@code Player} to modify the properties of an {@code Item}.
 * @author UnAfraid
 */
public class EnchantItemAttributeRequest extends AbstractRequest
{
	private volatile int _enchantingItemObjectId;
	private volatile int _enchantingStoneObjectId;
	
	/**
	 * Creates a new {@code EnchantItemAttributeRequest}.<br>
	 * This request is used when a player attempts to enchant an item.<br>
	 * It initializes the request with the target player and the stone ID.
	 * @param player The {@code Player} who is performing the action.
	 * @param enchantingStoneObjectId The unique identifier for the enchanting stone.
	 */
	public EnchantItemAttributeRequest(Player player, int enchantingStoneObjectId)
	{
		super(player);
		_enchantingStoneObjectId = enchantingStoneObjectId;
	}
	
	/**
	 * Retrieves the item being enchanted.<br>
	 * This method looks up the {@code Item} using the stored object ID.
	 * @return the {@code Item} instance or {@code null} if not found.
	 */
	public Item getEnchantingItem()
	{
		return getPlayer().getInventory().getItemByObjectId(_enchantingItemObjectId);
	}
	
	/**
	 * Sets the unique identifier for the item being enchanted.<br>
	 * This value is stored in the {@code _enchantingItemObjectId} field.
	 * @param objectId The unique ID of the item to be enchanted.
	 */
	public void setEnchantingItem(int objectId)
	{
		_enchantingItemObjectId = objectId;
	}
	
	/**
	 * Retrieves the {@code Item} used as an enchanting stone.<br>
	 * This method looks up the item in the player's inventory.<br>
	 * It uses the internal ID stored during request creation.
	 * @return The {@code Item} object representing the enchanting stone, or {@code null} if not found.
	 */
	public Item getEnchantingStone()
	{
		return getPlayer().getInventory().getItemByObjectId(_enchantingStoneObjectId);
	}
	
	/**
	 * Sets the unique identifier for the enchanting stone.<br>
	 * This value is used to identify which stone is being applied to an item.
	 * @param objectId The {@code int} ID of the enchanting stone.
	 */
	public void setEnchantingStone(int objectId)
	{
		_enchantingStoneObjectId = objectId;
	}
	
	/**
	 * Checks if this request involves an {@code Item}.<br>
	 * This method always returns {@code true} for this specific request type.
	 * @return {@code true} if the request is related to an item.
	 */
	@Override
	public boolean isItemRequest()
	{
		return true;
	}
	
	/**
	 * Checks if this request can be processed with another request.<br>
	 * It returns {@code true} if the provided request is not an item request.
	 * @param request The {@code AbstractRequest} to check.
	 * @return {@code true} if compatible, otherwise {@code false}.
	 */
	@Override
	public boolean canWorkWith(AbstractRequest request)
	{
		return !request.isItemRequest();
	}
	
	/**
	 * Checks if the provided object ID belongs to this request.<br>
	 * It returns {@code true} if the ID matches either the enchanting item or the stone.<br>
	 * The ID must be greater than {@code 0}.
	 * @param objectId The unique identifier of the object to check.
	 * @return {@code true} if the ID is valid and belongs to this request, otherwise {@code false}.
	 */
	@Override
	public boolean isUsing(int objectId)
	{
		return (objectId > 0) && ((objectId == _enchantingItemObjectId) || (objectId == _enchantingStoneObjectId));
	}
}
