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
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * This packet is used to inform the client about the current number of pledges.<br>
 * It updates the UI to reflect the correct count for a {@code Clan}.
 * @author UnAfraid
 */
public class ExPledgeCount extends ServerPacket
{
	private final int _count;
	
	/**
	 * Creates a new {@code ExPledgeCount} packet for a specific clan.<br>
	 * This constructor retrieves the current online member count from the {@code Clan}.
	 * @param clan The {@code Clan} object used to determine the member count.
	 */
	public ExPledgeCount(Clan clan)
	{
		_count = clan.getOnlineMembersCount();
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_PLEDGE_COUNT.writeId(this, buffer);
		buffer.writeInt(_count);
	}
}
