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
package org.l2jmobius.gameserver.network.serverpackets.gacha;

import java.util.List;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.item.holders.GachaItemTimeStampHolder;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * This packet sends the history of unique items obtained from a gacha system to the client.<br>
 * It allows players to view their previous successful pulls in the user interface.
 */
public class UniqueGachaHistory extends ServerPacket
{
	private static final int SHORT_PACKET_SIZE = 2 + 1 + 4 + 8;
	
	private final List<GachaItemTimeStampHolder> _items;
	
	/**
	 * Creates a new {@code UniqueGachaHistory} packet.<br>
	 * This constructor initializes the history with a list of items.
	 * @param items The list of {@code GachaItemTimeStampHolder} objects to include in the history.
	 */
	public UniqueGachaHistory(List<GachaItemTimeStampHolder> items)
	{
		_items = items;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_UNIQUE_GACHA_HISTORY.writeId(this, buffer);
		buffer.writeInt(_items.size());
		for (GachaItemTimeStampHolder item : _items)
		{
			buffer.writeShort(SHORT_PACKET_SIZE);
			buffer.writeByte(item.getRank().getClientId());
			buffer.writeInt(item.getId());
			buffer.writeLong(item.getCount());
			buffer.writeInt(item.getTimeStampFromNow());
		}
	}
}
