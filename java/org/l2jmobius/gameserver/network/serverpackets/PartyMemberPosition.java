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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.groups.Party;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * This packet updates the client on the current positions of all members within a {@code Party}.<br>
 * It is used to synchronize movement data for party members in real-time.
 * @author zabbix
 */
public class PartyMemberPosition extends ServerPacket
{
	private final Map<Integer, Location> locations = new HashMap<>();
	
	/**
	 * Creates a new {@code PartyMemberPosition} packet.<br>
	 * This constructor initializes the packet using the provided {@code Party} object.<br>
	 * It calls the {@code reuse} method to set up the internal data.
	 * @param party The {@code Party} containing the member locations.
	 */
	public PartyMemberPosition(Party party)
	{
		reuse(party);
	}
	
	/**
	 * Updates the internal location map for a {@code Party}.<br>
	 * This method clears existing data and populates it with current member positions.
	 * @param party The {@code Party} object to refresh.
	 */
	public void reuse(Party party)
	{
		locations.clear();
		for (Player member : party.getMembers())
		{
			if (member == null)
			{
				continue;
			}
			
			locations.put(member.getObjectId(), member.getLocation());
		}
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.PARTY_MEMBER_POSITION.writeId(this, buffer);
		buffer.writeInt(locations.size());
		for (Entry<Integer, Location> entry : locations.entrySet())
		{
			final Location loc = entry.getValue();
			buffer.writeInt(entry.getKey());
			buffer.writeInt(loc.getX());
			buffer.writeInt(loc.getY());
			buffer.writeInt(loc.getZ());
		}
	}
}
