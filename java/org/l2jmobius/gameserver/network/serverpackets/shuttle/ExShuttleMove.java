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
package org.l2jmobius.gameserver.network.serverpackets.shuttle;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.instance.Shuttle;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * This packet is sent to the client to update the position of a {@code Shuttle}.<br>
 * It handles the movement synchronization for shuttle entities in the game world.
 * @author UnAfraid
 */
public class ExShuttleMove extends ServerPacket
{
	private final Shuttle _shuttle;
	private final int _x;
	private final int _y;
	private final int _z;
	
	/**
	 * Creates a new {@code ExShuttleMove} packet.<br>
	 * This packet updates the position of a specific {@code Shuttle}.
	 * @param shuttle The {@code Shuttle} instance to move.
	 * @param x The new X coordinate.
	 * @param y The new Y coordinate.
	 * @param z The new Z coordinate.
	 */
	public ExShuttleMove(Shuttle shuttle, int x, int y, int z)
	{
		_shuttle = shuttle;
		_x = x;
		_y = y;
		_z = z;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_SHUTTLE_MOVE.writeId(this, buffer);
		buffer.writeInt(_shuttle.getObjectId());
		buffer.writeInt((int) _shuttle.getStat().getMoveSpeed());
		buffer.writeInt((int) _shuttle.getStat().getRotationSpeed());
		buffer.writeInt(_x);
		buffer.writeInt(_y);
		buffer.writeInt(_z);
	}
}
