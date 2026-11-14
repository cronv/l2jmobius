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

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * This packet is sent to the client to retrieve a specific item from the unique gacha inventory.<br>
 * It allows the game client to display or process items obtained through the unique gacha system.
 */
public class UniqueGachaInvenGetItem extends ServerPacket
{
	private final int _result;
	
	/**
	 * Creates a new {@code UniqueGachaInvenGetItem} packet.<br>
	 * This constructor sets the internal result based on the provided boolean.<br>
	 * If the input is {@code true}, the result is set to {@code 1}.<br>
	 * If the input is {@code false}, the result is set to {@code 0}.
	 * @param result The success status of the operation.
	 */
	public UniqueGachaInvenGetItem(boolean result)
	{
		_result = result ? 1 : 0;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_UNIQUE_GACHA_INVEN_GET_ITEM.writeId(this, buffer);
		buffer.writeByte(_result);
	}
}
