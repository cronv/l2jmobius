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
 * This packet informs the client to move a character to a specific location.<br>
 * It is used by {@code TeleportToLocation} to synchronize position data between the server and the client.
 */
public class TeleportToLocation extends ServerPacket
{
	private final int _targetObjId;
	private final int _x;
	private final int _y;
	private final int _z;
	private final int _heading;
	
	/**
	 * Teleports a {@code WorldObject} to a specific location.<br>
	 * This packet updates the position and rotation of an entity.
	 * @param obj The {@code WorldObject} to be moved.
	 * @param x The target X coordinate.
	 * @param y The target Y coordinate.
	 * @param z The target Z coordinate.
	 * @param heading The new facing direction for the object.
	 */
	public TeleportToLocation(WorldObject obj, int x, int y, int z, int heading)
	{
		_targetObjId = obj.getObjectId();
		_x = x;
		_y = y;
		_z = z;
		_heading = heading;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.TELEPORT_TO_LOCATION.writeId(this, buffer);
		buffer.writeInt(_targetObjId);
		buffer.writeInt(_x);
		buffer.writeInt(_y);
		buffer.writeInt(_z);
		buffer.writeInt(0); // Fade 0, Instant 1.
		buffer.writeInt(_heading);
		buffer.writeInt(0); // Unknown.
	}
}
