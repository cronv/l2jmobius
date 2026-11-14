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
package org.l2jmobius.gameserver.network.serverpackets.sayune;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.interfaces.ILocational;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.enums.SayuneType;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * Broadcasts the movement of a {@code Player} that is currently in an exaggerated flying state.<br>
 * This packet ensures that other clients are synchronized with the entity's position and animation.<br>
 * It is used specifically for handling movements related to the {@code SayuneType} mechanics.
 * @author UnAfraid
 */
public class ExFlyMoveBroadcast extends ServerPacket
{
	private final int _objectId;
	private final int _mapId;
	private final ILocational _currentLoc;
	private final ILocational _targetLoc;
	private final SayuneType _type;
	
	/**
	 * Creates a broadcast packet for an entity's flying movement.<br>
	 * This is used to sync the movement of specific entities across the network.
	 * @param player The {@code Player} who initiated the action.
	 * @param type The {@code SayuneType} associated with this move.
	 * @param mapId The unique identifier for the current map.
	 * @param targetLoc The destination location of the movement.
	 */
	public ExFlyMoveBroadcast(Player player, SayuneType type, int mapId, ILocational targetLoc)
	{
		_objectId = player.getObjectId();
		_type = type;
		_mapId = mapId;
		_currentLoc = player;
		_targetLoc = targetLoc;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_FLY_MOVE_BROADCAST.writeId(this, buffer);
		buffer.writeInt(_objectId);
		buffer.writeInt(_type.ordinal());
		buffer.writeInt(_mapId);
		buffer.writeInt(_targetLoc.getX());
		buffer.writeInt(_targetLoc.getY());
		buffer.writeInt(_targetLoc.getZ());
		buffer.writeInt(0); // ?
		buffer.writeInt(_currentLoc.getX());
		buffer.writeInt(_currentLoc.getY());
		buffer.writeInt(_currentLoc.getZ());
	}
}
