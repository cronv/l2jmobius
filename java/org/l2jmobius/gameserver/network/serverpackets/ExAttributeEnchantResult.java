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
import org.l2jmobius.gameserver.entity.actor.enums.creature.AttributeType;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * This packet is sent to the client to communicate the result of an extra attribute enchantment.<br>
 * It informs the player whether the enchantment process was successful or failed.
 * @author Mobius
 */
public class ExAttributeEnchantResult extends ServerPacket
{
	private final int _result;
	private final boolean _isWeapon;
	private final int _type;
	private final int _before;
	private final int _after;
	private final int _successCount;
	private final int _failedCount;
	
	/**
	 * Creates a new {@code ExAttributeEnchantResult} packet.<br>
	 * This packet sends the results of an attribute enchantment to the client.
	 * @param result The overall status code of the enchantment.
	 * @param isWeapon A boolean indicating if the item being enchanted is a weapon.
	 * @param type The {@code AttributeType} being modified.
	 * @param before The value of the attribute before the enchantment started.
	 * @param after The value of the attribute after the enchantment finished.
	 * @param successCount The number of successful enchantment attempts.
	 * @param failedCount The number of failed enchantment attempts.
	 */
	public ExAttributeEnchantResult(int result, boolean isWeapon, AttributeType type, int before, int after, int successCount, int failedCount)
	{
		_result = result;
		_isWeapon = isWeapon;
		_type = type.getClientId();
		_before = before;
		_after = after;
		_successCount = successCount;
		_failedCount = failedCount;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_ATTRIBUTE_ENCHANT_RESULT.writeId(this, buffer);
		buffer.writeInt(_result);
		buffer.writeByte(_isWeapon);
		buffer.writeShort(_type);
		buffer.writeShort(_before);
		buffer.writeShort(_after);
		buffer.writeShort(_successCount);
		buffer.writeShort(_failedCount);
	}
}
