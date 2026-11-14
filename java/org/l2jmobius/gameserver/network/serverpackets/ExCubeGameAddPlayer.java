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
 * This packet is used to notify the client that a new player has joined the game world.<br>
 * It handles the synchronization of {@code Player} data for the connected clients.
 * @author mrTJO
 */
public class ExCubeGameAddPlayer extends ServerPacket
{
	private final Player _player;
	private final boolean _isRedTeam;
	
	/**
	 * Adds a {@code Player} to the minigame waiting list.<br>
	 * This method identifies which team the player belongs to.
	 * @param player The {@code Player} instance to add.
	 * @param isRedTeam Set to {@code true} if the player is on the red team, or {@code false} otherwise.
	 */
	public ExCubeGameAddPlayer(Player player, boolean isRedTeam)
	{
		_player = player;
		_isRedTeam = isRedTeam;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_BLOCK_UPSET_LIST.writeId(this, buffer);
		buffer.writeInt(1);
		buffer.writeInt(0xffffffff);
		buffer.writeInt(_isRedTeam);
		buffer.writeInt(_player.getObjectId());
		buffer.writeString(_player.getName());
	}
}
