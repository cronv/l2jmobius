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
import org.l2jmobius.gameserver.entity.groups.matching.CommandChannelMatchingRoom;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * This packet provides information about a specific matching room to the client.<br>
 * It is used to synchronize room details during the matchmaking process.
 * @author Sdw
 */
public class ExMPCCRoomInfo extends ServerPacket
{
	private final CommandChannelMatchingRoom _room;
	
	/**
	 * Creates a new {@code ExMPCCRoomInfo} packet.<br>
	 * This constructor initializes the packet with specific room data.
	 * @param room The {@code CommandChannelMatchingRoom} to be included in the packet.
	 */
	public ExMPCCRoomInfo(CommandChannelMatchingRoom room)
	{
		_room = room;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_MPCC_ROOM_INFO.writeId(this, buffer);
		buffer.writeInt(_room.getId());
		buffer.writeInt(_room.getMaxMembers());
		buffer.writeInt(_room.getMinLevel());
		buffer.writeInt(_room.getMaxLevel());
		buffer.writeInt(_room.getLootType());
		buffer.writeInt(_room.getLocation());
		buffer.writeString(_room.getTitle());
	}
}
