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
package org.l2jmobius.gameserver.network.serverpackets.commission;

import java.util.Collection;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.AbstractItemPacket;

/**
 * This packet is sent to the client to provide a list of items associated with a commission.<br>
 * It allows the game client to display the required or rewarded items for a specific task.
 * @author Mobius
 */
public class ExResponseCommissionItemList extends AbstractItemPacket
{
	private final int _sendType;
	private final Collection<Item> _items;
	
	/**
	 * Creates a new {@code ExResponseCommissionItemList} packet.<br>
	 * This packet contains a list of items for a commission response.
	 * @param sendType The type of message being sent.
	 * @param items A collection of {@code Item} objects to include in the packet.
	 */
	public ExResponseCommissionItemList(int sendType, Collection<Item> items)
	{
		_sendType = sendType;
		_items = items;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_RESPONSE_COMMISSION_ITEM_LIST.writeId(this, buffer);
		buffer.writeByte(_sendType);
		if (_sendType == 2)
		{
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
			buffer.writeInt(0);
			buffer.writeInt(0);
		}
	}
}
