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
 * This packet handles the delivery of private messages between friends.<br>
 * It allows a player to send a direct message to another specific user.
 * @author Tempy
 */
public class L2FriendSay extends ServerPacket
{
	private final String _sender;
	private final String _receiver;
	private final String _message;
	
	/**
	 * Creates a new {@code L2FriendSay} packet.<br>
	 * This packet is used to send a private message between two players.<br>
	 * It stores the sender, the receiver, and the content of the message.
	 * @param sender The name of the player sending the message.
	 * @param receiver The name of the player receiving the message.
	 * @param message The text content of the friend message.
	 */
	public L2FriendSay(String sender, String receiver, String message)
	{
		_sender = sender;
		_receiver = receiver;
		_message = message;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.L2_FRIEND_SAY.writeId(this, buffer);
		buffer.writeInt(0); // ??
		buffer.writeString(_receiver);
		buffer.writeString(_sender);
		buffer.writeString(_message);
	}
}
