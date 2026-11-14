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
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * This packet handles social actions performed by players in the game world.<br>
 * It informs the client about specific interactions such as emotes or group activities.
 */
public class SocialAction extends ServerPacket
{
	// TODO: Enum
	public static final int LEVEL_UP = 2122;
	
	private final int _objectId;
	private final int _actionId;
	
	/**
	 * Creates a new {@code SocialAction} packet.<br>
	 * This constructor initializes the required IDs for a social interaction.
	 * @param objectId The unique identifier of the target object.
	 * @param actionId The specific ID representing the type of social action.
	 */
	public SocialAction(int objectId, int actionId)
	{
		_objectId = objectId;
		_actionId = actionId;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.SOCIAL_ACTION.writeId(this, buffer);
		buffer.writeInt(_objectId);
		buffer.writeInt(_actionId);
		buffer.writeInt(0); // TODO: Find me!
	}
	
	/**
	 * Checks if the social action can be dropped by a specific client.<br>
	 * This method currently always returns {@code true}.
	 * @param client The {@code GameClient} instance to check.
	 * @return {@code true} if the action can be dropped, otherwise {@code false}.
	 */
	@Override
	public boolean canBeDropped(GameClient client)
	{
		return true;
	}
}
