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
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * This packet is sent to a player to notify them of the result of a friend request.<br>
 * It informs the client whether the {@code Player} successfully added another user as a friend.
 * @author UnAfraid
 */
public class FriendAddRequestResult extends ServerPacket
{
	private final int _result;
	private final int _charId;
	private final String _charName;
	private final int _isOnline;
	private final int _charObjectId;
	private final int _charLevel;
	private final int _charClassId;
	
	/**
	 * Creates a new {@code FriendAddRequestResult} packet.<br>
	 * This constructor initializes the result with data from the provided {@code Player}.
	 * @param player The {@code Player} object used to populate character details.
	 * @param result The status code of the friend request operation.
	 */
	public FriendAddRequestResult(Player player, int result)
	{
		_result = result;
		_charId = player.getObjectId();
		_charName = player.getName();
		_isOnline = player.isOnlineInt();
		_charObjectId = _charId;
		_charLevel = player.getLevel();
		_charClassId = player.getActiveClass();
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.FRIEND_ADD_REQUEST_RESULT.writeId(this, buffer);
		buffer.writeInt(_result);
		buffer.writeInt(_charId);
		buffer.writeString(_charName);
		buffer.writeInt(_isOnline);
		buffer.writeInt(_charObjectId);
		buffer.writeInt(_charLevel);
		buffer.writeInt(_charClassId);
		buffer.writeShort(0); // Always 0 on retail
	}
}
