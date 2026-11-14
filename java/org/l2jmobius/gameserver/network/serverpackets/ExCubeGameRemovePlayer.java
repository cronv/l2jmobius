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
 * This packet is sent to the client to remove a {@code Player} from the game world.<br>
 * It is typically used when a player disconnects or is removed by the server.
 * @author mrTJO
 */
public class ExCubeGameRemovePlayer extends ServerPacket
{
	private final Player _player;
	private final boolean _isRedTeam;
	
	/**
	 * Removes a specific {@code Player} from the minigame waiting list.<br>
	 * This method identifies which team the player belongs to during removal.
	 * @param player The {@code Player} object to be removed from the list.
	 * @param isRedTeam A boolean indicating if the {@code Player} is on the red team.
	 */
	public ExCubeGameRemovePlayer(Player player, boolean isRedTeam)
	{
		_player = player;
		_isRedTeam = isRedTeam;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_BLOCK_UPSET_LIST.writeId(this, buffer);
		buffer.writeInt(2);
		buffer.writeInt(0xffffffff);
		buffer.writeInt(_isRedTeam);
		buffer.writeInt(_player.getObjectId());
	}
}
