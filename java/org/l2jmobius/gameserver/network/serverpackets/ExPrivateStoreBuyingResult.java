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

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * This packet is sent to the client to communicate the result of a private store purchase.<br>
 * It informs the player whether their attempt to buy an item from a {@code ExPrivateStore} was successful.
 */
public class ExPrivateStoreBuyingResult extends ServerPacket
{
	/** The client reads the seller name into a buffer of this many characters. */
	private static final int NAME_FIELD_LENGTH = 24;
	
	private final int _objectId;
	private final long _count;
	private final String _seller;
	
	/**
	 * This constructor creates a new {@code ExPrivateStoreBuyingResult} packet.<br>
	 * It stores the details of a private store purchase.
	 * @param objectId The unique identifier for the item.
	 * @param count The number of items purchased.
	 * @param seller The name of the player who owns the store.
	 */
	public ExPrivateStoreBuyingResult(int objectId, long count, String seller)
	{
		_objectId = objectId;
		_count = count;
		_seller = seller;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_PRIVATE_STORE_BUYING_RESULT.writeId(this, buffer);
		buffer.writeInt(_objectId);
		buffer.writeLong(_count);
		writeFixedString(_seller, NAME_FIELD_LENGTH, buffer);
	}
}
