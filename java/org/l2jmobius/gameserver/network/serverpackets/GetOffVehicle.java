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
package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * This packet is sent to the client to notify it that a player has dismounted from a vehicle.<br>
 * It handles the synchronization of the character's position and state after leaving a mount.
 * @author Maktakien
 */
public class GetOffVehicle extends ServerPacket
{
	private final int _objectId;
	private final int _boatObjId;
	private final int _x;
	private final int _y;
	private final int _z;
	
	/**
	 * This packet handles the action of a character getting off a vehicle.<br>
	 * It updates the position of the character to the specified coordinates.
	 * @param charObjId The unique identifier for the character.
	 * @param boatObjId The unique identifier for the boat.
	 * @param x The X coordinate for the new position.
	 * @param y The Y coordinate for the new position.
	 * @param z The Z coordinate for the new position.
	 */
	public GetOffVehicle(int charObjId, int boatObjId, int x, int y, int z)
	{
		_objectId = charObjId;
		_boatObjId = boatObjId;
		_x = x;
		_y = y;
		_z = z;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.GETOFF_VEHICLE.writeId(this, buffer);
		buffer.writeInt(_objectId);
		buffer.writeInt(_boatObjId);
		buffer.writeInt(_x);
		buffer.writeInt(_y);
		buffer.writeInt(_z);
	}
}
