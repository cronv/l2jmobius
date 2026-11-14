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
 * This packet is sent to the client to register an item in the world exchange.<br>
 * It provides the necessary data for the client to display a specific item within the exchange interface.
 * @author Index
 */
public class WorldExchangeRegisterItem extends ServerPacket
{
	public static final WorldExchangeRegisterItem FAIL = new WorldExchangeRegisterItem(-1, -1L, 0, 0);
	
	private final int _itemObjectId;
	private final long _itemAmount;
	private final int _type;
	private final int _weType;
	
	/**
	 * Creates a new {@code WorldExchangeRegisterItem} packet.<br>
	 * This packet registers an item for the world exchange system.
	 * @param itemObjectId The unique identifier of the item object.
	 * @param itemAmount The total quantity of the item.
	 * @param type The category or classification of the item.
	 * @param weType The specific exchange type associated with this registration.
	 */
	public WorldExchangeRegisterItem(int itemObjectId, long itemAmount, int type, int weType)
	{
		_itemObjectId = itemObjectId;
		_itemAmount = itemAmount;
		_type = type;
		_weType = weType;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_WORLD_EXCHANGE_REGI_ITEM.writeId(this, buffer);
		buffer.writeByte(_weType);
		buffer.writeInt(_itemObjectId);
		buffer.writeLong(_itemAmount);
		buffer.writeByte(_type);
		buffer.writeInt(0); // 563 nCondValue
	}
}
