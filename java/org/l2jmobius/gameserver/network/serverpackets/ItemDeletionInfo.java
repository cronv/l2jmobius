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

import java.util.Map;
import java.util.Map.Entry;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.managers.events.ItemDeletionInfoManager;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * This packet informs the client about an item that has been deleted from the world.<br>
 * It contains details necessary to synchronize the removal of the object for the player.
 * @author Index, Mobius
 */
public class ItemDeletionInfo extends ServerPacket
{
	private final Map<Integer, Integer> _itemDates;
	
	/**
	 * Constructs a new {@code ItemDeletionInfo} object.<br>
	 * This constructor initializes the internal map of item dates.<br>
	 * It retrieves the data from the {@code ItemDeletionInfoManager}.
	 */
	public ItemDeletionInfo()
	{
		_itemDates = ItemDeletionInfoManager.getInstance().getItemDates();
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_ITEM_DELETION_INFO.writeId(this, buffer);
		
		// Items.
		buffer.writeInt(_itemDates.size());
		for (Entry<Integer, Integer> info : _itemDates.entrySet())
		{
			buffer.writeInt(info.getKey()); // item
			buffer.writeInt(info.getValue()); // date
		}
		
		// Skills.
		buffer.writeInt(0);
	}
}
