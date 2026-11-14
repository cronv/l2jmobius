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
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * This packet is sent to the client to notify it that an entity has mounted a vehicle.<br>
 * It provides the necessary data for the client to update the visual state of the character.
 * @author Maktakien
 */
public class GetOnVehicle extends ServerPacket
{
	private final int _objectId;
	private final int _boatObjId;
	private final Location _pos;
	
	/**
	 * This packet handles a character boarding a vehicle.<br>
	 * It sends the necessary data to the client to update the scene.
	 * @param charObjId The unique identifier for the character.
	 * @param boatObjId The unique identifier for the boat or vehicle.
	 * @param pos The location where the action takes place.
	 */
	public GetOnVehicle(int charObjId, int boatObjId, Location pos)
	{
		_objectId = charObjId;
		_boatObjId = boatObjId;
		_pos = pos;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.GETON_VEHICLE.writeId(this, buffer);
		buffer.writeInt(_objectId);
		buffer.writeInt(_boatObjId);
		buffer.writeInt(_pos.getX());
		buffer.writeInt(_pos.getY());
		buffer.writeInt(_pos.getZ());
	}
}
