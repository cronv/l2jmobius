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
package org.l2jmobius.gameserver.network.serverpackets.mentoring;

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.data.enums.CategoryType;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * This packet is sent to a mentor to provide a list of players currently waiting for mentorship.<br>
 * It contains the data necessary to display these pending requests in the game client.
 * @author UnAfraid
 */
public class ListMenteeWaiting extends ServerPacket
{
	private static final int PLAYERS_PER_PAGE = 64;
	
	private final List<Player> _possibleCandiates = new ArrayList<>();
	private final int _page;
	
	/**
	 * Creates a packet to list players waiting for mentors.<br>
	 * It filters players based on their level and status.<br>
	 * The results are organized into pages for easier navigation.
	 * @param page The page number to retrieve.
	 * @param minLevel The minimum level required to be included.
	 * @param maxLevel The maximum level allowed to be included.
	 */
	public ListMenteeWaiting(int page, int minLevel, int maxLevel)
	{
		_page = page;
		for (Player player : World.getPlayers())
		{
			final int level = player.getLevel();
			if ((level >= minLevel) && (level <= maxLevel) && !player.isMentee() && !player.isMentor() && !player.isInCategory(CategoryType.SIXTH_CLASS_GROUP))
			{
				_possibleCandiates.add(player);
			}
		}
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_MENTEE_WAITING_LIST.writeId(this, buffer);
		buffer.writeInt(1); // always 1 in retail
		if (_possibleCandiates.isEmpty())
		{
			buffer.writeInt(0);
			buffer.writeInt(0);
			return;
		}
		
		final int candidatesSize = _possibleCandiates.size();
		buffer.writeInt(candidatesSize);
		buffer.writeInt(candidatesSize % PLAYERS_PER_PAGE);
		for (Player player : _possibleCandiates)
		{
			if ((1 <= (PLAYERS_PER_PAGE * _page)) && (1 > (PLAYERS_PER_PAGE * (_page - 1))))
			{
				buffer.writeString(player.getName());
				buffer.writeInt(player.getActiveClass());
				buffer.writeInt(player.getLevel());
			}
		}
	}
}
