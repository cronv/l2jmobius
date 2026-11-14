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
 * This packet informs the client that a character has started an auto-attack.<br>
 * It is used to synchronize the combat state between the server and the client.
 */
public class AutoAttackStart extends ServerPacket
{
	private final int _targetObjId;
	
	/**
	 * This constructor initializes a new {@code AutoAttackStart} packet.<br>
	 * It sets the ID of the target object to be attacked.
	 * @param targetId The unique identifier of the target object.
	 */
	public AutoAttackStart(int targetId)
	{
		_targetObjId = targetId;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.COMBAT_MODE_START.writeId(this, buffer);
		buffer.writeInt(_targetObjId);
	}
	
	/**
	 * Checks if the packet can be dropped by the server.<br>
	 * This method currently always returns {@code true}.
	 * @param client The {@code GameClient} instance to check.
	 * @return {@code true} if the packet can be dropped, otherwise {@code false}.
	 */
	@Override
	public boolean canBeDropped(GameClient client)
	{
		return true;
	}
}
