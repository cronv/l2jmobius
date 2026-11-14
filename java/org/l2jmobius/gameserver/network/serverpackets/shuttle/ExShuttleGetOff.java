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
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.instance.Shuttle;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * This packet is sent to the client when a player successfully exits a {@code Shuttle}.<br>
 * It handles the synchronization of the player's state after they disembark.
 * @author UnAfraid
 */
public class ExShuttleGetOff extends ServerPacket
{
	private final int _playerObjectId;
	private final int _shuttleObjectId;
	private final int _x;
	private final int _y;
	private final int _z;
	
	/**
	 * This packet handles a player getting off a {@code Shuttle}.<br>
	 * It records the location where the player exits.
	 * @param player The {@code Player} who is exiting the shuttle.
	 * @param shuttle The {@code Shuttle} being exited.
	 * @param x The X coordinate of the exit point.
	 * @param y The Y coordinate of the exit point.
	 * @param z The Z coordinate of the exit point.
	 */
	public ExShuttleGetOff(Player player, Shuttle shuttle, int x, int y, int z)
	{
		_playerObjectId = player.getObjectId();
		_shuttleObjectId = shuttle.getObjectId();
		_x = x;
		_y = y;
		_z = z;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_GETOFF_SHUTTLE.writeId(this, buffer);
		buffer.writeInt(_playerObjectId);
		buffer.writeInt(_shuttleObjectId);
		buffer.writeInt(_x);
		buffer.writeInt(_y);
		buffer.writeInt(_z);
	}
}
