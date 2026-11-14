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
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * This packet is sent to the client to instruct a {@code Creature} to stop moving.<br>
 * It is used by the server to synchronize movement states and prevent continuous walking.
 */
public class StopMove extends ServerPacket
{
	private final int _objectId;
	private final int _x;
	private final int _y;
	private final int _z;
	private final int _heading;
	
	/**
	 * This constructor creates a {@code StopMove} packet for a specific entity.<br>
	 * It extracts the position and orientation data from the provided {@code Creature}.
	 * @param creature The {@code Creature} whose movement should be stopped.
	 */
	public StopMove(Creature creature)
	{
		this(creature.getObjectId(), creature.getX(), creature.getY(), creature.getZ(), creature.getHeading());
	}
	
	/**
	 * This constructor initializes a {@code StopMove} packet.<br>
	 * It sets the position and orientation for an object to stop moving.
	 * @param objectId The unique identifier of the object.
	 * @param x The X coordinate of the object.
	 * @param y The Y coordinate of the object.
	 * @param z The Z coordinate of the object.
	 * @param heading The rotation angle of the object.
	 */
	public StopMove(int objectId, int x, int y, int z, int heading)
	{
		_objectId = objectId;
		_x = x;
		_y = y;
		_z = z;
		_heading = heading;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.STOP_MOVE.writeId(this, buffer);
		buffer.writeInt(_objectId);
		buffer.writeInt(_x);
		buffer.writeInt(_y);
		buffer.writeInt(_z);
		buffer.writeInt(_heading);
	}
}
