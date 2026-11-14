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
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * This packet is sent to the client when a vehicle has successfully started.<br>
 * It informs the player that they can now operate the vehicle.
 * @author Kerberos
 */
public class VehicleStarted extends ServerPacket
{
	private final int _objectId;
	private final int _state;
	
	/**
	 * This constructor initializes a new {@code VehicleStarted} packet.<br>
	 * It sets the object ID from the provided {@code Creature}.<br>
	 * It also stores the current vehicle state.
	 * @param boat The {@code Creature} representing the boat.
	 * @param state The integer value representing the current state.
	 */
	public VehicleStarted(Creature boat, int state)
	{
		_objectId = boat.getObjectId();
		_state = state;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.VEHICLE_START_PACKET.writeId(this, buffer);
		buffer.writeInt(_objectId);
		buffer.writeInt(_state);
	}
}
