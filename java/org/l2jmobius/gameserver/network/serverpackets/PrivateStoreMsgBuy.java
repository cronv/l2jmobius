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
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * This packet is sent to the client when a player successfully purchases an item from a private store.<br>
 * It provides the necessary data for the client to update the inventory and display the transaction result.
 */
public class PrivateStoreMsgBuy extends ServerPacket
{
	private final int _objectId;
	private String _message;
	
	/**
	 * Creates a new {@code PrivateStoreMsgBuy} packet for a specific player.<br>
	 * This constructor automatically retrieves the object ID from the {@code Player}.<br>
	 * It also sets the message based on the title of the player's buy list if it exists.
	 * @param player The {@code Player} instance to use for creating the packet.
	 */
	public PrivateStoreMsgBuy(Player player)
	{
		_objectId = player.getObjectId();
		if (player.getBuyList() != null)
		{
			_message = player.getBuyList().getTitle();
		}
	}
	
	/**
	 * Creates a new {@code PrivateStoreMsgBuy} packet.<br>
	 * This packet is used to notify the client about a purchase in a private store.
	 * @param objectId The unique identifier of the item being purchased.
	 * @param message A descriptive message to display to the player.
	 */
	public PrivateStoreMsgBuy(int objectId, String message)
	{
		_objectId = objectId;
		_message = message;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.PRIVATE_STORE_BUY_MSG.writeId(this, buffer);
		buffer.writeInt(_objectId);
		buffer.writeString(_message);
	}
}
