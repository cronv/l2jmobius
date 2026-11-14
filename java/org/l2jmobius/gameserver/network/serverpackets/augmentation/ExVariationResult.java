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
 * This packet represents the result of an {@code ExVariation} action.<br>
 * It is sent from the server to the client to communicate the outcome of a specific variation request.
 * @author Mobius
 */
public class ExVariationResult extends ServerPacket
{
	public static final ExVariationResult FAIL = new ExVariationResult(0, 0, 0, false);
	
	private final int _option1;
	private final int _option2;
	private final int _option3;
	private final boolean _success;
	
	/**
	 * Creates a new {@code ExVariationResult} object.<br>
	 * This constructor initializes the variation options and the success status.
	 * @param option1 The first variation option value.
	 * @param option2 The second variation option value.
	 * @param option3 The third variation option value.
	 * @param success The result of the operation, either {@code true} or {@code false}.
	 */
	public ExVariationResult(int option1, int option2, int option3, boolean success)
	{
		_option1 = option1;
		_option2 = option2;
		_option3 = option3;
		_success = success;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_VARIATION_RESULT.writeId(this, buffer);
		buffer.writeInt(_option1);
		buffer.writeInt(_option2);
		buffer.writeInt(_option3);
		buffer.writeLong(0); // GemStoneCount
		buffer.writeLong(0); // NecessaryGemStoneCount
		buffer.writeInt(_success);
	}
}
