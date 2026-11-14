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
package org.l2jmobius.gameserver.network.serverpackets.equipmentupgrade;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.AbstractItemPacket;

/**
 * This packet represents the result of an equipment upgrade attempt.<br>
 * It is sent to the client to communicate whether the upgrade succeeded or failed.
 * @author Mobius
 */
public class ExUpgradeSystemResult extends AbstractItemPacket
{
	private final int _objectId;
	private final int _success;
	
	/**
	 * Creates a new {@code ExUpgradeSystemResult} packet.<br>
	 * This packet sends the result of an equipment upgrade to the client.
	 * @param objectId The unique identifier of the item being upgraded.
	 * @param success The status of the upgrade attempt.
	 */
	public ExUpgradeSystemResult(int objectId, int success)
	{
		_objectId = objectId;
		_success = success;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_UPGRADE_SYSTEM_RESULT.writeId(this, buffer);
		buffer.writeShort(_success);
		buffer.writeInt(_objectId);
	}
}
