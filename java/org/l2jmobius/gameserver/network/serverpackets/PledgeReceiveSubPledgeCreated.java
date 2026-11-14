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
import org.l2jmobius.gameserver.entity.clan.Clan.SubPledge;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.PacketLogger;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * This packet is sent to the client when a new {@code SubPledge} is created within a {@code Clan}.<br>
 * It informs the player that a new sub-pledge has been successfully initialized.
 * @author -Wooden-
 */
public class PledgeReceiveSubPledgeCreated extends ServerPacket
{
	private final SubPledge _subPledge;
	private final Clan _clan;
	
	/**
	 * This packet is sent to the client when a new {@code SubPledge} is created.<br>
	 * It contains information about the new pledge and its parent {@code Clan}.
	 * @param subPledge The newly created {@code SubPledge} object.
	 * @param clan The {@code Clan} that owns the new pledge.
	 */
	public PledgeReceiveSubPledgeCreated(SubPledge subPledge, Clan clan)
	{
		_subPledge = subPledge;
		_clan = clan;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_SUBPLEDGE_UPDATED.writeId(this, buffer);
		buffer.writeInt(1);
		buffer.writeInt(_subPledge.getId());
		buffer.writeString(_subPledge.getName());
		buffer.writeString(getLeaderName());
	}
	
	/**
	 * Retrieves the name of the leader for the current {@code SubPledge}.<br>
	 * It returns an empty string if the leader is missing or invalid.<br>
	 * A warning is logged if a valid ID exists but no member is found.
	 * @return The name of the leader as a {@code String}.
	 */
	private String getLeaderName()
	{
		final int LeaderId = _subPledge.getLeaderId();
		if ((_subPledge.getId() == -1) || (LeaderId == 0))
		{
			return "";
		}
		else if (_clan.getClanMember(LeaderId) == null)
		{
			PacketLogger.warning("SubPledgeLeader: " + LeaderId + " is missing from clan: " + _clan.getName() + "[" + _clan.getId() + "]");
			return "";
		}
		else
		{
			return _clan.getClanMember(LeaderId).getName();
		}
	}
}
