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
import java.util.Collection;
import java.util.List;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.mechanics.siege.Castle;
import org.l2jmobius.gameserver.mechanics.siege.Siege;
import org.l2jmobius.gameserver.mechanics.siege.SiegeClan;
import org.l2jmobius.gameserver.mechanics.siege.SiegeClanType;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * This packet populates the defender list within the {@code SiegeInfo} window.<br>
 * It contains details about defending clans, including their names, leaders, and status.
 * @author Atronic
 */
public class SiegeDefenderList extends ServerPacket
{
	private final Castle _castle;
	private final Clan _owner;
	final List<Clan> _defenders = new ArrayList<>();
	
	/**
	 * Creates a new {@code SiegeDefenderList} for a specific castle.<br>
	 * This constructor populates the list of defending clans.<br>
	 * It includes the owner and all confirmed or waiting defenders.
	 * @param castle The {@code Castle} object used to retrieve siege data.
	 */
	public SiegeDefenderList(Castle castle)
	{
		_castle = castle;
		_owner = castle.getOwner();
		
		// Add owners.
		if (_owner != null)
		{
			_defenders.add(_owner);
		}
		
		// List of confirmed defenders.
		final Siege siege = _castle.getSiege();
		for (SiegeClan siegeClan : siege.getDefenderClans())
		{
			final Clan clan = ClanTable.getInstance().getClan(siegeClan.getClanId());
			if ((clan != null) && (clan != _owner))
			{
				_defenders.add(clan);
			}
		}
		
		// List of not confirmed defenders.
		for (SiegeClan siegeClan : siege.getDefenderWaitingClans())
		{
			final Clan clan = ClanTable.getInstance().getClan(siegeClan.getClanId());
			if (clan != null)
			{
				_defenders.add(clan);
			}
		}
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.CASTLE_SIEGE_DEFENDER_LIST.writeId(this, buffer);
		buffer.writeInt(_castle.getResidenceId());
		buffer.writeInt(0); // Unknown.
		
		buffer.writeInt((_owner != null) && _castle.isTimeRegistrationOver()); // Valid registration.
		buffer.writeInt(0); // Unknown.
		
		final int size = _defenders.size();
		buffer.writeInt(size);
		buffer.writeInt(size);
		final Collection<SiegeClan> defenderClans = _castle.getSiege().getDefenderClans();
		for (Clan clan : _defenders)
		{
			final int clanId = clan.getId();
			buffer.writeInt(clanId);
			buffer.writeString(clan.getName());
			buffer.writeString(clan.getLeaderName());
			buffer.writeInt(clan.getCrestId());
			buffer.writeInt(0); // Signed time in seconds.
			if (clan == _owner)
			{
				buffer.writeInt(SiegeClanType.OWNER.ordinal() + 1);
			}
			else if (defenderClans.stream().anyMatch(defender -> defender.getClanId() == clanId))
			{
				buffer.writeInt(SiegeClanType.DEFENDER.ordinal() + 1);
			}
			else
			{
				buffer.writeInt(SiegeClanType.DEFENDER_PENDING.ordinal() + 1);
			}
			
			final int clanAllyId = clan.getAllyId();
			buffer.writeInt(clanAllyId);
			if (clanAllyId != 0)
			{
				final AllianceInfo info = new AllianceInfo(clanAllyId);
				buffer.writeString(info.getName());
				buffer.writeString(info.getLeaderP()); // Ally leader name.
				buffer.writeInt(clan.getAllyCrestId());
			}
			else
			{
				buffer.writeString("");
				buffer.writeString(""); // Ally leader name.
				buffer.writeInt(0);
			}
		}
	}
}
