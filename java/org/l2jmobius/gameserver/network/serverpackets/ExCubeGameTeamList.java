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

import java.util.List;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * This packet handles the transmission of the game team list to the client.<br>
 * It contains information about all teams currently active in the {@code ExCube} game mode.
 * @author mrTJO
 */
public class ExCubeGameTeamList extends ServerPacket
{
	// Players Lists
	private final List<Player> _bluePlayers;
	private final List<Player> _redPlayers;
	
	// Common Values
	private final int _roomNumber;
	
	/**
	 * Creates a new {@code ExCubeGameTeamList} packet.<br>
	 * This packet displays the waiting list for a minigame.<br>
	 * It stores the players for both teams and the room identifier.
	 * @param redPlayers The list of players on the red team.
	 * @param bluePlayers The list of players on the blue team.
	 * @param roomNumber The unique ID of the arena or room.
	 */
	public ExCubeGameTeamList(List<Player> redPlayers, List<Player> bluePlayers, int roomNumber)
	{
		_redPlayers = redPlayers;
		_bluePlayers = bluePlayers;
		_roomNumber = roomNumber - 1;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_BLOCK_UPSET_LIST.writeId(this, buffer);
		buffer.writeInt(0);
		buffer.writeInt(_roomNumber);
		buffer.writeInt(0xffffffff);
		buffer.writeInt(_bluePlayers.size());
		for (Player player : _bluePlayers)
		{
			buffer.writeInt(player.getObjectId());
			buffer.writeString(player.getName());
		}
		
		buffer.writeInt(_redPlayers.size());
		for (Player player : _redPlayers)
		{
			buffer.writeInt(player.getObjectId());
			buffer.writeString(player.getName());
		}
	}
}
