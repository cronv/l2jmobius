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
 * This packet is sent to the client to handle recipe shop interactions.<br>
 * It provides information regarding available recipes and their requirements.
 */
public class RecipeShopMsg extends ServerPacket
{
	private final int _objectId;
	private final String _message;
	
	/**
	 * Creates a new {@code RecipeShopMsg} for a specific player.<br>
	 * This constructor initializes the message using the player's object ID and store name.
	 * @param player The {@code Player} instance to retrieve data from.
	 */
	public RecipeShopMsg(Player player)
	{
		_objectId = player.getObjectId();
		_message = player.getStoreName();
	}
	
	/**
	 * Creates a new {@code RecipeShopMsg} packet.<br>
	 * This constructor sets the required data for the shop message.
	 * @param objectId The unique identifier of the shop object.
	 * @param message The text message to be displayed to the player.
	 */
	public RecipeShopMsg(int objectId, String message)
	{
		_objectId = objectId;
		_message = message;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.RECIPE_STORE_MSG.writeId(this, buffer);
		buffer.writeInt(_objectId);
		buffer.writeString(_message);
	}
}
