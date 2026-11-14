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
package org.l2jmobius.gameserver.network.serverpackets.worldexchange;

import java.util.Collection;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.managers.WorldExchangeManager;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * This packet contains the complete list of items available in the world exchange.<br>
 * It is sent to the client to populate the exchange interface.
 * @author Mobius
 */
public class WorldExchangeTotalList extends ServerPacket
{
	private final int _weType;
	private final Collection<Integer> _itemIds;
	
	/**
	 * Creates a new {@code WorldExchangeTotalList} packet.<br>
	 * This packet contains the type and list of items for a world exchange.
	 * @param weType The type of the world exchange.
	 * @param itemIds A collection of unique identifiers for the items.
	 */
	public WorldExchangeTotalList(int weType, Collection<Integer> itemIds)
	{
		_weType = weType;
		_itemIds = itemIds;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_WORLD_EXCHANGE_TOTAL_LIST.writeId(this, buffer);
		buffer.writeByte(_weType); // cWEType - 557
		buffer.writeInt(_itemIds.size());
		for (int id : _itemIds)
		{
			final long minPrice = WorldExchangeManager.getInstance().getMinPriceForItem(id, _weType);
			final long amount = WorldExchangeManager.getInstance().getTotalAmountForItem(id, _weType);
			buffer.writeInt(id); // ItemClassID
			buffer.writeLong(minPrice); // MinPricePerPiece
			buffer.writeLong(minPrice); // Price
			buffer.writeLong(Math.max(1, amount)); // Amount
		}
	}
}
