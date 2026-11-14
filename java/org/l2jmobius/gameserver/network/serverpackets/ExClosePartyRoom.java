/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * This packet is sent to the client to notify it that a party room has been closed.<br>
 * It handles the synchronization of the UI state when a room is no longer accessible.
 * @author Gnacik
 */
public class ExClosePartyRoom extends ServerPacket
{
	public static final ExClosePartyRoom STATIC_PACKET = new ExClosePartyRoom();
	
	/**
	 * This is a private constructor for the {@code ExClosePartyRoom} class.<br>
	 * It prevents other classes from creating new instances of this packet.<br>
	 * The static instance {@code STATIC_PACKET} should be used instead.
	 */
	private ExClosePartyRoom()
	{
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_DISMISS_PARTY_ROOM.writeId(this, buffer);
	}
}
