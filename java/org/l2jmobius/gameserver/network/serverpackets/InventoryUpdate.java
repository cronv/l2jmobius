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
 * This class handles the synchronization of inventory changes between the server and the client.<br>
 * It ensures that the player's inventory is updated correctly when items are added, removed, or moved.
 * @author Advi
 */
public class InventoryUpdate extends AbstractInventoryUpdate
{
	/**
	 * Creates a new instance of {@code InventoryUpdate}.<br>
	 * This constructor initializes the packet with default values.
	 */
	public InventoryUpdate()
	{
	}
	
	/**
	 * Creates a new {@code InventoryUpdate} for a specific item.<br>
	 * This constructor initializes the update using the provided {@code Item}.
	 * @param item The {@code Item} to be updated in the inventory.
	 */
	public InventoryUpdate(Item item)
	{
		super(item);
	}
	
	/**
	 * This constructor creates a new {@code InventoryUpdate} object.<br>
	 * It initializes the update with a list of items.<br>
	 * Use this when multiple items need to be updated at once.
	 * @param items The list of {@code ItemInfo} objects to include in the update.
	 */
	public InventoryUpdate(List<ItemInfo> items)
	{
		super(items);
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.INVENTORY_UPDATE.writeId(this, buffer);
		writeItems(buffer);
	}
}
