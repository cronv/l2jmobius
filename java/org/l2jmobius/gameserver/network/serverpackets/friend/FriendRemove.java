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
package org.l2jmobius.gameserver.network.serverpackets.friend;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * This packet is sent to the client to notify them that a friend has been removed.<br>
 * It handles the removal of a specific friend from the player's social list.
 * @author UnAfraid
 */
public class FriendRemove extends ServerPacket
{
	private final int _responce;
	private final String _charName;
	
	/**
	 * This packet handles the removal of a friend from a player's list.<br>
	 * It sends the result of the operation to the client.
	 * @param charName The name of the character being removed.
	 * @param responce The status code of the removal operation.
	 */
	public FriendRemove(String charName, int responce)
	{
		_responce = responce;
		_charName = charName;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.FRIEND_REMOVE.writeId(this, buffer);
		buffer.writeInt(_responce);
		buffer.writeString(_charName);
	}
}
