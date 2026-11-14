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
package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.instance.AirShip;
import org.l2jmobius.gameserver.entity.item.type.WeaponType;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.ExMoveToLocationInAirShip;
import org.l2jmobius.gameserver.network.serverpackets.StopMoveInVehicle;

/**
 * Handles the client request to move a character to a specific location within an {@code AirShip}.<br>
 * It processes coordinates for both the target destination and the origin point.<br>
 * This packet is used to synchronize movement between the player and the vehicle.
 * @author GodKratos
 */
public class MoveToLocationInAirShip extends ClientPacket
{
	private int _shipId;
	private int _targetX;
	private int _targetY;
	private int _targetZ;
	private int _originX;
	private int _originY;
	private int _originZ;
	
	@Override
	protected void readImpl()
	{
		_shipId = readInt();
		_targetX = readInt();
		_targetY = readInt();
		_targetZ = readInt();
		_originX = readInt();
		_originY = readInt();
		_originZ = readInt();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		if ((_targetX == _originX) && (_targetY == _originY) && (_targetZ == _originZ))
		{
			player.sendPacket(new StopMoveInVehicle(player, _shipId));
			return;
		}
		
		if (player.isAttackingNow() && (player.getActiveWeaponItem() != null) && (player.getActiveWeaponItem().getItemType() == WeaponType.BOW))
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (player.isSitting() || player.isMovementDisabled())
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (!player.isInAirShip())
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		final AirShip airShip = player.getAirShip();
		if (airShip.getObjectId() != _shipId)
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		player.setInVehiclePosition(new Location(_targetX, _targetY, _targetZ));
		player.broadcastPacket(new ExMoveToLocationInAirShip(player));
	}
}
