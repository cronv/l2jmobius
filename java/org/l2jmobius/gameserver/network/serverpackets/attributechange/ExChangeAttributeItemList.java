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
package org.l2jmobius.gameserver.network.serverpackets.attributechange;

import java.util.List;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.item.holders.ItemInfo;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.AbstractItemPacket;

/**
 * This packet is used to notify the client about a change in an item list's attributes.<br>
 * It allows for updating specific properties of items during an exchange process.
 * @author Mobius
 */
public class ExChangeAttributeItemList extends AbstractItemPacket
{
	private final int _type;
	private final int _itemId;
	private final List<ItemInfo> _itemsList;
	
	/**
	 * Creates a new {@code ExChangeAttributeItemList} packet.<br>
	 * This packet contains information about an item exchange.<br>
	 * It stores the type, ID, and list of items involved.
	 * @param type The type of the exchange.
	 * @param itemId The unique identifier for the item.
	 * @param itemList The list of {@code ItemInfo} objects to be included in the packet.
	 */
	public ExChangeAttributeItemList(int type, int itemId, List<ItemInfo> itemList)
	{
		_type = type;
		_itemId = itemId;
		_itemsList = itemList;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_CHANGE_ATTRIBUTE_ITEM_LIST.writeId(this, buffer);
		buffer.writeByte(_type);
		buffer.writeInt(_itemId);
		buffer.writeInt(_itemsList.size());
		for (ItemInfo item : _itemsList)
		{
			writeItem(item, buffer);
		}
	}
}
