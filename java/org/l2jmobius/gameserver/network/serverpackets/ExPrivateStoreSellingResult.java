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
 * This packet is sent to the client to provide the result of an item sale in a private store.<br>
 * It informs the player whether the transaction was successful or failed.
 */
public class ExPrivateStoreSellingResult extends ServerPacket
{
	/** The client reads the buyer name into a buffer of this many characters. */
	private static final int NAME_FIELD_LENGTH = 24;
	
	private final int _objectId;
	private final long _count;
	private final String _buyer;
	
	/**
	 * This constructor creates a new {@code ExPrivateStoreSellingResult} packet.<br>
	 * It stores the details of a private store sale.
	 * @param objectId The unique identifier for the item sold.
	 * @param count The number of items sold in the transaction.
	 * @param buyer The name of the player who purchased the items.
	 */
	public ExPrivateStoreSellingResult(int objectId, long count, String buyer)
	{
		_objectId = objectId;
		_count = count;
		_buyer = buyer;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_PRIVATE_STORE_SELLING_RESULT.writeId(this, buffer);
		buffer.writeInt(_objectId);
		buffer.writeLong(_count);
		writeFixedString(_buyer, NAME_FIELD_LENGTH, buffer);
	}
}
