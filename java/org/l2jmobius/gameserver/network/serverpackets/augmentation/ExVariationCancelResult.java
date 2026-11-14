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
package org.l2jmobius.gameserver.network.serverpackets.augmentation;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * This packet is sent to the client to notify it of the result of an {@code ExVariation} cancellation.<br>
 * It informs the player whether their attempt to cancel a specific action was successful.
 */
public class ExVariationCancelResult extends ServerPacket
{
	public static final ExVariationCancelResult STATIC_PACKET_SUCCESS = new ExVariationCancelResult(1);
	public static final ExVariationCancelResult STATIC_PACKET_FAILURE = new ExVariationCancelResult(0);
	
	private final int _result;
	
	/**
	 * Creates a new instance of {@code ExVariationCancelResult}.<br>
	 * This constructor sets the internal result value.
	 * @param result The integer code representing the outcome.
	 */
	private ExVariationCancelResult(int result)
	{
		_result = result;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_VARIATION_CANCEL_RESULT.writeId(this, buffer);
		buffer.writeInt(_result);
	}
}
