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

import java.util.Map;
import java.util.Map.Entry;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * This class handles the transmission of a list of items to the client.<br>
 * It is used to synchronize data such as inventory contents or shop listings.
 * @author -Wooden-, UnAfraid, mrTJO
 */
public class PackageToList extends ServerPacket
{
	private final Map<Integer, String> _players;
	
	/**
	 * This constructor initializes a new {@code PackageToList} object.<br>
	 * It stores the provided map of player data.
	 * @param chars A {@code Map<Integer, String>} containing the character information.
	 */
	public PackageToList(Map<Integer, String> chars)
	{
		_players = chars;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.PACKAGE_TO_LIST.writeId(this, buffer);
		buffer.writeInt(_players.size());
		for (Entry<Integer, String> entry : _players.entrySet())
		{
			buffer.writeInt(entry.getKey());
			buffer.writeString(entry.getValue());
		}
	}
}
