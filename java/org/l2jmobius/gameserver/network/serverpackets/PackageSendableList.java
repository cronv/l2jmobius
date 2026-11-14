/*
 * Copyright (c) 2013 L2jMobius
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.l2jmobius.gameserver.network.serverpackets;

import java.util.Collection;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * This class represents a collection of items that can be sent to the client.<br>
 * It facilitates the transmission of multiple {@code Item} objects within a single network packet.
 * @author Mobius
 */
public class PackageSendableList extends AbstractItemPacket
{
	private final Collection<Item> _items;
	private final int _objectId;
	private final long _adena;
	private final int _sendType;
	
	/**
	 * Creates a new {@code PackageSendableList} for a specific player.<br>
	 * This constructor populates the list with available items from the player's inventory.
	 * @param sendType The type of packet to be sent.
	 * @param player The {@code Player} whose inventory will be accessed.
	 * @param objectId The unique identifier for the object.
	 */
	public PackageSendableList(int sendType, Player player, int objectId)
	{
		_sendType = sendType;
		_items = player.getInventory().getAvailableItems(true, true, true);
		_objectId = objectId;
		_adena = player.getAdena();
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.PACKAGE_SENDABLE_LIST.writeId(this, buffer);
		buffer.writeByte(_sendType);
		if (_sendType == 2)
		{
			final int itemsSize = _items.size();
			buffer.writeInt(itemsSize);
			buffer.writeInt(itemsSize);
			for (Item item : _items)
			{
				writeItem(item, buffer);
				buffer.writeInt(item.getObjectId());
			}
		}
		else
		{
			buffer.writeInt(_objectId);
			buffer.writeLong(_adena);
			buffer.writeInt(_items.size());
		}
	}
}
