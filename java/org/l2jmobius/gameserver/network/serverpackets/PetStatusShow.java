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
import org.l2jmobius.gameserver.entity.actor.Summon;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * This packet updates the client on the current status of a {@code Summon}.<br>
 * It is used to synchronize pet states between the server and the player.
 * @author Mobius
 */
public class PetStatusShow extends ServerPacket
{
	private final int _summonType;
	private final int _summonObjectId;
	
	/**
	 * Creates a new {@code PetStatusShow} packet.<br>
	 * This constructor extracts data from the provided {@code Summon} object.<br>
	 * It sets the internal type and ID for the pet status display.
	 * @param summon The {@code Summon} entity to extract information from.
	 */
	public PetStatusShow(Summon summon)
	{
		_summonType = summon.getSummonType();
		_summonObjectId = summon.getObjectId();
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.PET_STATUS_SHOW.writeId(this, buffer);
		buffer.writeInt(_summonType);
		buffer.writeInt(_summonObjectId);
	}
}
