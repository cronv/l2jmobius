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
package org.l2jmobius.gameserver.network.serverpackets;

import java.util.List;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.item.holders.ItemInfo;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * This packet updates the client with the current contents of a pet's inventory.<br>
 * It is used to synchronize item data between the server and the player's UI.
 * @author Yme, Advi
 */
public class PetInventoryUpdate extends AbstractInventoryUpdate
{
	/**
	 * Creates a new instance of {@code PetInventoryUpdate}.<br>
	 * This constructor initializes the packet with default values.
	 */
	public PetInventoryUpdate()
	{
	}
	
	/**
	 * Updates the inventory of a pet with a specific item.<br>
	 * This constructor initializes the update using a single {@code Item}.
	 * @param item The {@code Item} to be added or updated in the pet's inventory.
	 */
	public PetInventoryUpdate(Item item)
	{
		super(item);
	}
	
	/**
	 * Updates the inventory of a pet with a list of items.<br>
	 * This constructor passes the provided items to the parent class.
	 * @param items The list of {@code ItemInfo} objects to update.
	 */
	public PetInventoryUpdate(List<ItemInfo> items)
	{
		super(items);
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.PET_INVENTORY_UPDATE.writeId(this, buffer);
		writeItems(buffer);
	}
}
