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
import org.l2jmobius.gameserver.entity.clan.ClanMember;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * This packet sends power information related to a {@code ClanMember} to the client.<br>
 * It is used to update the UI with specific pledge statistics.
 * @author -Wooden-
 */
public class PledgeReceivePowerInfo extends ServerPacket
{
	private final ClanMember _member;
	
	/**
	 * Creates a new {@code PledgeReceivePowerInfo} packet.<br>
	 * This constructor initializes the packet with specific clan member data.
	 * @param member The {@code ClanMember} object to be included in the packet.
	 */
	public PledgeReceivePowerInfo(ClanMember member)
	{
		_member = member;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_VIEW_PLEDGE_POWER.writeId(this, buffer);
		final int powerGrade = _member.getPowerGrade();
		buffer.writeInt(powerGrade); // power grade
		buffer.writeString(_member.getName());
		buffer.writeInt(_member.getClan().getRankPrivs(powerGrade).getMask()); // privileges
	}
}
