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

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.managers.WorldExchangeManager;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * This packet sends the average price of items to the client.<br>
 * It is used by {@code WorldExchangeManager} to update market data.
 * @author Mobius
 */
public class WorldExchangeAveragePrice extends ServerPacket
{
	private final int _weType;
	private final int _itemId;
	private final long _averagePrice;
	
	/**
	 * Creates a new {@code WorldExchangeAveragePrice} packet.<br>
	 * This constructor initializes the exchange type and item ID.<br>
	 * It also fetches the current average price from the {@code WorldExchangeManager}.
	 * @param weType The type of world exchange.
	 * @param itemId The unique identifier for the item.
	 */
	public WorldExchangeAveragePrice(int weType, int itemId)
	{
		_weType = weType;
		_itemId = itemId;
		_averagePrice = WorldExchangeManager.getInstance().getAveragePriceOfItem(itemId);
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_WORLD_EXCHANGE_AVERAGE_PRICE.writeId(this, buffer);
		buffer.writeByte(_weType); // 557
		buffer.writeInt(_itemId);
		buffer.writeLong(_averagePrice);
	}
}
