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
 * This packet is sent to the client to provide a list of items.<br>
 * It is used as a response when an item list is requested or updated.
 * @author Mobius
 */
public class ExReplyPostItemList extends AbstractItemPacket
{
	private final int _sendType;
	private final Player _player;
	private final Collection<Item> _itemList;
	
	/**
	 * Creates a new {@code ExReplyPostItemList} packet.<br>
	 * This constructor initializes the packet with the specified send type and player.<br>
	 * It automatically populates the item list from the {@code Player} inventory.
	 * @param sendType The type of message being sent.
	 * @param player The {@code Player} instance who owns the items.
	 */
	public ExReplyPostItemList(int sendType, Player player)
	{
		_sendType = sendType;
		_player = player;
		_itemList = _player.getInventory().getAvailableItems(true, false, false);
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_REPLY_POST_ITEM_LIST.writeId(this, buffer);
		buffer.writeByte(_sendType);
		final int itemListSize = _itemList.size();
		buffer.writeInt(itemListSize);
		if (_sendType == 2)
		{
			buffer.writeInt(itemListSize);
			for (Item item : _itemList)
			{
				writeItem(item, buffer);
			}
		}
	}
}
