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

import java.util.Map;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.itemcontainer.Inventory;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * This packet provides information about the items displayed in a shop preview.<br>
 * It is used to update the client's view of available goods.
 * @author Gnacik
 */
public class ShopPreviewInfo extends ServerPacket
{
	private final Map<Integer, Integer> _itemlist;
	
	/**
	 * Creates a new {@code ShopPreviewInfo} object.<br>
	 * This constructor initializes the internal list of items.
	 * @param itemlist A {@code Map<Integer, Integer>} containing the shop items.
	 */
	public ShopPreviewInfo(Map<Integer, Integer> itemlist)
	{
		_itemlist = itemlist;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.BUY_PREVIEW_INFO.writeId(this, buffer);
		buffer.writeInt(Inventory.PAPERDOLL_TOTALSLOTS);
		
		// Slots
		buffer.writeInt(getFromList(Inventory.PAPERDOLL_UNDER));
		buffer.writeInt(getFromList(Inventory.PAPERDOLL_REAR));
		buffer.writeInt(getFromList(Inventory.PAPERDOLL_LEAR));
		buffer.writeInt(getFromList(Inventory.PAPERDOLL_NECK));
		buffer.writeInt(getFromList(Inventory.PAPERDOLL_RFINGER));
		buffer.writeInt(getFromList(Inventory.PAPERDOLL_LFINGER));
		buffer.writeInt(getFromList(Inventory.PAPERDOLL_HEAD));
		buffer.writeInt(getFromList(Inventory.PAPERDOLL_RHAND));
		buffer.writeInt(getFromList(Inventory.PAPERDOLL_LHAND));
		buffer.writeInt(getFromList(Inventory.PAPERDOLL_GLOVES));
		buffer.writeInt(getFromList(Inventory.PAPERDOLL_CHEST));
		buffer.writeInt(getFromList(Inventory.PAPERDOLL_LEGS));
		buffer.writeInt(getFromList(Inventory.PAPERDOLL_FEET));
		buffer.writeInt(getFromList(Inventory.PAPERDOLL_CLOAK));
		buffer.writeInt(getFromList(Inventory.PAPERDOLL_RHAND));
		buffer.writeInt(getFromList(Inventory.PAPERDOLL_HAIR));
		buffer.writeInt(getFromList(Inventory.PAPERDOLL_HAIR2));
		buffer.writeInt(getFromList(Inventory.PAPERDOLL_RBRACELET));
		buffer.writeInt(getFromList(Inventory.PAPERDOLL_LBRACELET));
	}
	
	/**
	 * Retrieves a value from the internal item list.<br>
	 * It uses the provided {@code key} to find the corresponding integer.<br>
	 * If the {@code key} is not found, it returns {@code 0}.
	 * @param key The unique identifier for the item to look up.
	 * @return The integer value associated with the key or {@code 0} if missing.
	 */
	private int getFromList(int key)
	{
		return (_itemlist.getOrDefault(key, 0));
	}
}
