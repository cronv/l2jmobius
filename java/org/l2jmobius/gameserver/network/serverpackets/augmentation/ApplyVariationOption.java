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
 * This packet is used to apply a specific augmentation option to an item.<br>
 * It handles the communication between the server and the client for updating item augmentations.
 * @author Index, Mobius
 */
public class ApplyVariationOption extends ServerPacket
{
	private final int _result;
	private final int _enchantedObjectId;
	private final int _option1;
	private final int _option2;
	private final int _option3;
	
	/**
	 * Creates a new {@code ApplyVariationOption} packet.<br>
	 * This packet sends augmentation details to the client.
	 * @param result The final result of the operation.
	 * @param enchantedObjectId The unique identifier for the enchanted object.
	 * @param option1 The first augmentation option value.
	 * @param option2 The second augmentation option value.
	 * @param option3 The third augmentation option value.
	 */
	public ApplyVariationOption(int result, int enchantedObjectId, int option1, int option2, int option3)
	{
		_result = result;
		_enchantedObjectId = enchantedObjectId;
		_option1 = option1;
		_option2 = option2;
		_option3 = option3;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_APPLY_VARIATION_OPTION.writeId(this, buffer);
		buffer.writeByte(_result);
		buffer.writeInt(_enchantedObjectId);
		buffer.writeInt(_option1);
		buffer.writeInt(_option2);
		buffer.writeInt(_option3);
	}
}
