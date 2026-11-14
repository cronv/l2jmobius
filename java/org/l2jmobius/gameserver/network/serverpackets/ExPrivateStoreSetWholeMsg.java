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
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * This packet is sent to the client to update the entire contents of a private store.<br>
 * It synchronizes all items within the {@code ExPrivateStore} with the player's interface.
 * @author KenM
 */
public class ExPrivateStoreSetWholeMsg extends ServerPacket
{
	private final int _objectId;
	private final String _message;
	
	/**
	 * Creates a new {@code ExPrivateStoreSetWholeMsg} packet for a specific player.<br>
	 * This method sets the object ID from the {@code Player} and assigns the provided message.
	 * @param player The {@code Player} instance to retrieve the object ID from.
	 * @param msg The message string to be included in the packet.
	 */
	public ExPrivateStoreSetWholeMsg(Player player, String msg)
	{
		_objectId = player.getObjectId();
		_message = msg;
	}
	
	/**
	 * This constructor initializes a new {@code ExPrivateStoreSetWholeMsg} packet.<br>
	 * It sets the specific object identifier and the associated message text.
	 * @param objectId The unique ID of the object to update.
	 * @param message The full message string to be sent.
	 */
	public ExPrivateStoreSetWholeMsg(int objectId, String message)
	{
		_objectId = objectId;
		_message = message;
	}
	
	/**
	 * Creates a new {@code ExPrivateStoreSetWholeMsg} packet for a specific player.<br>
	 * This method automatically retrieves the title from the player's sell list.<br>
	 * It uses the default message associated with that list.
	 * @param player The {@code Player} object to generate the packet for.
	 */
	public ExPrivateStoreSetWholeMsg(Player player)
	{
		this(player, player.getSellList().getTitle());
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_PRIVATE_STORE_WHOLE_MSG.writeId(this, buffer);
		buffer.writeInt(_objectId);
		buffer.writeString(_message);
	}
}
