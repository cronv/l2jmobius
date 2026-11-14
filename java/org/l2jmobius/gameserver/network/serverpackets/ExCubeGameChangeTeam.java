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
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * This packet is sent to the client to notify it of a team change for an {@code ExCube} game.<br>
 * It updates the local client state regarding which team a player belongs to.
 * @author mrTJO
 */
public class ExCubeGameChangeTeam extends ServerPacket
{
	private final Player _player;
	private final boolean _fromRedTeam;
	
	/**
	 * This packet handles changing a player's team.<br>
	 * It identifies which team the {@code Player} is moving from.
	 * @param player The {@code Player} instance to update.
	 * @param fromRedTeam Set to {@code true} if the player is leaving the red team, or {@code false} otherwise.
	 */
	public ExCubeGameChangeTeam(Player player, boolean fromRedTeam)
	{
		_player = player;
		_fromRedTeam = fromRedTeam;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_BLOCK_UPSET_LIST.writeId(this, buffer);
		buffer.writeInt(5);
		buffer.writeInt(_player.getObjectId());
		buffer.writeInt(_fromRedTeam);
		buffer.writeInt(!_fromRedTeam);
	}
}
