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
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * This packet informs the client to move a character to a specific {@code WorldObject}.<br>
 * It is used to synchronize entity positions when an actor interacts with a pawn.
 */
public class MoveToPawn extends ServerPacket
{
	private final int _objectId;
	private final int _targetId;
	private final int _distance;
	private final int _x;
	private final int _y;
	private final int _z;
	private final int _tx;
	private final int _ty;
	private final int _tz;
	
	/**
	 * This packet tells a client to move a {@code Creature} toward a {@code WorldObject}.<br>
	 * It provides the current position and the destination coordinates.<br>
	 * The movement is restricted by a specific distance.
	 * @param creature The {@code Creature} that will perform the movement.
	 * @param target The {@code WorldObject} that serves as the destination.
	 * @param distance The maximum distance the {@code Creature} can travel.
	 */
	public MoveToPawn(Creature creature, WorldObject target, int distance)
	{
		_objectId = creature.getObjectId();
		_targetId = target.getObjectId();
		_distance = distance;
		_x = creature.getX();
		_y = creature.getY();
		_z = creature.getZ();
		_tx = target.getX();
		_ty = target.getY();
		_tz = target.getZ();
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.MOVE_TO_PAWN.writeId(this, buffer);
		buffer.writeInt(_objectId);
		buffer.writeInt(_targetId);
		buffer.writeInt(_distance);
		buffer.writeInt(_x);
		buffer.writeInt(_y);
		buffer.writeInt(_z);
		buffer.writeInt(_tx);
		buffer.writeInt(_ty);
		buffer.writeInt(_tz);
	}
	
	/**
	 * Checks if the packet can be dropped by the client.<br>
	 * This method currently always returns {@code true}.
	 * @param client The {@code GameClient} instance to check.
	 * @return {@code true} if the packet can be dropped, otherwise {@code false}.
	 */
	@Override
	public boolean canBeDropped(GameClient client)
	{
		return true;
	}
}
