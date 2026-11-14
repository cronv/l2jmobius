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
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * Handles the activation of a teleport to a specific location.<br>
 * This packet is sent to the client to trigger the movement of a {@code Creature} to a target {@code Location}.
 * @author UnAfraid
 */
public class ExTeleportToLocationActivate extends ServerPacket
{
	private final int _objectId;
	private final Location _loc;
	
	/**
	 * Activates a teleport to a specific location for a creature.<br>
	 * This method initializes the packet with the {@code Creature} data.
	 * @param creature The {@code Creature} that will be teleported.
	 */
	public ExTeleportToLocationActivate(Creature creature)
	{
		_objectId = creature.getObjectId();
		_loc = creature.getLocation();
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_TELEPORT_TO_LOCATION_ACTIVATE.writeId(this, buffer);
		buffer.writeInt(_objectId);
		buffer.writeInt(_loc.getX());
		buffer.writeInt(_loc.getY());
		buffer.writeInt(_loc.getZ());
		buffer.writeInt(0); // Unknown (this isn't instanceId)
		buffer.writeInt(_loc.getHeading());
		buffer.writeInt(0); // Unknown
	}
}
