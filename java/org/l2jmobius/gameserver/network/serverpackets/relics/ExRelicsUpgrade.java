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
package org.l2jmobius.gameserver.network.serverpackets.relics;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * This packet handles the upgrade process for {@code ExRelics}.<br>
 * It informs the client about the results of an upgrade attempt.
 * @author Brado
 */
public class ExRelicsUpgrade extends ServerPacket
{
	private final boolean _success;
	private final int _relicId;
	private final int _relicLevel;
	
	/**
	 * Creates a new {@code ExRelicsUpgrade} packet.<br>
	 * This packet informs the client about the result of a relic upgrade.
	 * @param success The status of the upgrade attempt. Use {@code true} for success and {@code false} for failure.
	 * @param relicId The unique identifier of the relic being upgraded.
	 * @param relicLevel The new level reached by the relic after the upgrade.
	 */
	public ExRelicsUpgrade(boolean success, int relicId, int relicLevel)
	{
		_success = success;
		_relicId = relicId;
		_relicLevel = relicLevel;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_RELICS_UPGRADE.writeId(this, buffer);
		buffer.writeByte(_success);
		buffer.writeInt(_relicId);
		buffer.writeInt(_relicLevel);
	}
}
