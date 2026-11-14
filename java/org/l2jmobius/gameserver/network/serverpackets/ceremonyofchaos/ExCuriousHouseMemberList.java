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
package org.l2jmobius.gameserver.network.serverpackets.ceremonyofchaos;

import java.util.Collection;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * This packet is used to send the list of members in a house to a client.<br>
 * It is specifically utilized during the Ceremony of Chaos event.<br>
 * The {@code ExCuriousHouseMemberList} class handles the serialization of this data.
 * @author UnAfraid
 */
public class ExCuriousHouseMemberList extends ServerPacket
{
	private final int _id;
	private final int _maxPlayers;
	private final Collection<Player> _players;
	
	/**
	 * Creates a new {@code ExCuriousHouseMemberList} packet.<br>
	 * This packet contains information about the house members.
	 * @param id The unique identifier for the house.
	 * @param maxPlayers The maximum number of players allowed in the house.
	 * @param players A collection of {@code Player} objects currently in the house.
	 */
	public ExCuriousHouseMemberList(int id, int maxPlayers, Collection<Player> players)
	{
		_id = id;
		_maxPlayers = maxPlayers;
		_players = players;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_CURIOUS_HOUSE_MEMBER_LIST.writeId(this, buffer);
		buffer.writeInt(_id);
		buffer.writeInt(_maxPlayers);
		buffer.writeInt(_players.size());
		int pos = 0;
		for (Player player : _players)
		{
			buffer.writeInt(player.getObjectId());
			buffer.writeInt(pos++);
			buffer.writeInt((int) player.getMaxHp());
			buffer.writeInt(player.getMaxCp());
			buffer.writeInt((int) player.getCurrentHp());
			buffer.writeInt((int) player.getCurrentCp());
		}
	}
}
