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
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * This packet informs the client to remove a specific {@code WorldObject} from its local view.<br>
 * It is used by the server to synchronize object deletions across all connected clients.
 * @author Mobius
 */
public class DeleteObject extends ServerPacket
{
	private final int _objectId;
	private final boolean _disappear;
	
	/**
	 * Creates a new {@code DeleteObject} packet for a specific object.<br>
	 * This method extracts the ID and monster status from the provided {@code WorldObject}.
	 * @param obj The {@code WorldObject} to be removed from the game world.
	 */
	public DeleteObject(WorldObject obj)
	{
		_objectId = obj.getObjectId();
		_disappear = obj.isMonster() && !obj.isFakePlayer();
	}
	
	/**
	 * This packet removes an object from the game world.<br>
	 * It sets the disappearance flag to {@code true}.
	 * @param objectId The unique identifier of the object to delete.
	 */
	public DeleteObject(int objectId)
	{
		_objectId = objectId;
		_disappear = true;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.DELETE_OBJECT.writeId(this, buffer);
		buffer.writeInt(_objectId);
		buffer.writeByte(_disappear); // C2 - If the object is mounted, with 0 it will be dismounted, with 1 it will disappear.
	}
}
