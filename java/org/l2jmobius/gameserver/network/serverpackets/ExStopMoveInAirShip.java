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
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * This packet informs the client to stop moving while in an airship.<br>
 * It is used to synchronize movement states for players on specific transport types.
 * @author Kerberos, JIV
 */
public class ExStopMoveInAirShip extends ServerPacket
{
	private final Player _player;
	private final int _shipObjId;
	private final int _h;
	private final Location _loc;
	
	/**
	 * This packet stops a {@code Player} from moving while inside an airship.<br>
	 * It captures the current heading and position of the player.
	 * @param player The {@code Player} who is currently in the vehicle.
	 * @param shipObjId The unique identifier for the airship object.
	 */
	public ExStopMoveInAirShip(Player player, int shipObjId)
	{
		_player = player;
		_shipObjId = shipObjId;
		_h = player.getHeading();
		_loc = player.getInVehiclePosition();
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_STOP_MOVE_IN_AIRSHIP.writeId(this, buffer);
		buffer.writeInt(_player.getObjectId());
		buffer.writeInt(_shipObjId);
		buffer.writeInt(_loc.getX());
		buffer.writeInt(_loc.getY());
		buffer.writeInt(_loc.getZ());
		buffer.writeInt(_h);
	}
}
