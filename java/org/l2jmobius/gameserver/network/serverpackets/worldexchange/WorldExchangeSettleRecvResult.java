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
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * This packet informs the client of the result of a world exchange settlement.<br>
 * It is sent to the player after the server processes their trade request.
 * @author Index
 */
public class WorldExchangeSettleRecvResult extends ServerPacket
{
	public static final WorldExchangeSettleRecvResult FAIL = new WorldExchangeSettleRecvResult(-1, -1L, 0, 0);
	
	private final int _itemObjectId;
	private final long _itemAmount;
	private final int _type;
	private final int _wetype;
	
	/**
	 * Creates a new {@code WorldExchangeSettleRecvResult} packet.<br>
	 * This result is sent to the client after an exchange settlement.
	 * @param itemObjectId The unique ID of the item involved in the transaction.
	 * @param itemAmount The quantity of the item being exchanged.
	 * @param type The category or classification of the item.
	 * @param wetype The specific subtype identifier for the item.
	 */
	public WorldExchangeSettleRecvResult(int itemObjectId, long itemAmount, int type, int wetype)
	{
		_itemObjectId = itemObjectId;
		_itemAmount = itemAmount;
		_type = type;
		_wetype = wetype;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_WORLD_EXCHANGE_SETTLE_RECV_RESULT.writeId(this, buffer);
		buffer.writeByte(_wetype); // 557
		buffer.writeInt(_itemObjectId);
		buffer.writeLong(_itemAmount);
		buffer.writeByte(_type);
	}
}
