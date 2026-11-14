/*
 * Copyright (c) 2013 L2jMobius
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.l2jmobius.gameserver.network.serverpackets;

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.mechanics.siege.Castle;
import org.l2jmobius.gameserver.mechanics.siege.SiegeClan;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * This packet populates the attacker list within the {@code SiegeInfo} window.<br>
 * It contains data regarding clans, leaders, and allies participating in a siege.
 * @author Mobius
 */
public class SiegeAttackerList extends ServerPacket
{
	private final Castle _castle;
	private final List<Clan> _attackers = new ArrayList<>();
	
	/**
	 * Creates a new {@code SiegeAttackerList} for a specific castle.<br>
	 * This constructor populates the list of attacking clans.<br>
	 * It retrieves each clan from the {@code ClanTable} based on the siege data.
	 * @param castle The {@code Castle} object associated with the siege.
	 */
	public SiegeAttackerList(Castle castle)
	{
		_castle = castle;
		for (SiegeClan siegeClan : _castle.getSiege().getAttackerClans())
		{
			final Clan clan = ClanTable.getInstance().getClan(siegeClan.getClanId());
			if (clan != null)
			{
				_attackers.add(clan);
			}
		}
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.CASTLE_SIEGE_ATTACKER_LIST.writeId(this, buffer);
		buffer.writeInt(_castle.getResidenceId());
		buffer.writeInt(0); // 0
		buffer.writeInt(1); // 1
		buffer.writeInt(0); // 0
		final int size = _attackers.size();
		if (size > 0)
		{
			buffer.writeInt(size);
			buffer.writeInt(size);
			for (Clan clan : _attackers)
			{
				buffer.writeInt(clan.getId());
				buffer.writeString(clan.getName());
				buffer.writeString(clan.getLeaderName());
				buffer.writeInt(clan.getCrestId());
				buffer.writeInt(0); // signed time (seconds) (not storated by L2J)
				buffer.writeInt(clan.getAllyId());
				buffer.writeString(clan.getAllyName());
				buffer.writeString(""); // AllyLeaderName
				buffer.writeInt(clan.getAllyCrestId());
			}
		}
		else
		{
			buffer.writeInt(0);
			buffer.writeInt(0);
		}
	}
}
