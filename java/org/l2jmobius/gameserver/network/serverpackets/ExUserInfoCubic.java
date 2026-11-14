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
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.holders.player.CubicList;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * This packet handles the transmission of extended user information for cubic data.<br>
 * It allows the server to send specific player details to the client.
 * @author Sdw, Norvox
 */
public class ExUserInfoCubic extends ServerPacket
{
	private final Player _player;
	
	/**
	 * Creates a new {@code ExUserInfoCubic} packet for a specific player.<br>
	 * This constructor initializes the internal player reference.
	 * @param player The {@code Player} object associated with this packet.
	 */
	public ExUserInfoCubic(Player player)
	{
		_player = player;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_USER_INFO_CUBIC.writeId(this, buffer);
		buffer.writeInt(_player.getObjectId()); // nUserSID
		buffer.writeInt(_player.getAgathionId()); // nMainAgathionNPCID
		
		// subAgathionList
		int secondaryCount = 0;
		for (int i = 1; i < 5; i++)
		{
			if (_player.getAgathionId(i) > 0)
			{
				secondaryCount++;
			}
		}
		
		buffer.writeInt(secondaryCount);
		for (int i = 1; i < 5; i++)
		{
			final int npcId = _player.getAgathionId(i);
			if (npcId > 0)
			{
				buffer.writeInt(i); // nSlot
				buffer.writeInt(npcId); // nNpcID
			}
		}
		
		// cubicList
		final CubicList cubicList = _player.getCubicList();
		buffer.writeInt(cubicList.size());
		cubicList.getCubics().keySet().forEach(buffer::writeShort);
	}
}
