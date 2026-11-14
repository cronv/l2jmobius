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

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * This class represents a network packet used to send a list of {@code Item} objects to the client.<br>
 * It is used by the server to synchronize inventory data or other item collections with the player.
 * @author Mobius
 */
public class ItemList extends AbstractItemPacket
{
	private final int _sendType;
	private final Player _player;
	private final List<Item> _items = new ArrayList<>();
	
	/**
	 * Creates a new {@code ItemList} packet for a specific player.<br>
	 * This constructor populates the list with non-quest items from the player's inventory.
	 * @param sendType The type of packet being sent.
	 * @param player The {@code Player} whose inventory will be processed.
	 */
	public ItemList(int sendType, Player player)
	{
		_sendType = sendType;
		_player = player;
		for (Item item : player.getInventory().getItems())
		{
			if (!item.isQuestItem())
			{
				_items.add(item);
			}
		}
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.ITEM_LIST.writeId(this, buffer);
		if (_sendType == 2)
		{
			buffer.writeByte(_sendType);
			final int itemsSize = _items.size();
			buffer.writeInt(itemsSize);
			buffer.writeInt(itemsSize);
			for (Item item : _items)
			{
				writeItem(item, buffer);
			}
		}
		else
		{
			buffer.writeByte(1); // _showWindow
			buffer.writeInt(0);
			buffer.writeInt(_items.size());
		}
		
		writeInventoryBlock(_player.getInventory(), buffer);
	}
}
