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

import java.util.Calendar;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.config.FeatureConfig;
import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.mechanics.siege.Castle;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * This packet provides information regarding the current siege status.<br>
 * It contains details such as the {@code CastleID}, owner clan data, and remaining siege time.
 * @author KenM
 */
public class SiegeInfo extends ServerPacket
{
	private final Castle _castle;
	private final Player _player;
	
	/**
	 * Creates a new {@code SiegeInfo} packet.<br>
	 * This packet provides information about a specific siege to a player.
	 * @param castle The {@code Castle} object containing the siege data.
	 * @param player The {@code Player} who will receive this information.
	 */
	public SiegeInfo(Castle castle, Player player)
	{
		_castle = castle;
		_player = player;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.CASTLE_SIEGE_INFO.writeId(this, buffer);
		if (_castle != null)
		{
			buffer.writeInt(_castle.getResidenceId());
			final int ownerId = _castle.getOwnerId();
			final int clanId = _player.getClanId();
			final boolean isClanLeader = _player.isClanLeader();
			buffer.writeInt((ownerId == clanId) && (isClanLeader));
			buffer.writeInt(ownerId);
			if (ownerId > 0)
			{
				final Clan owner = ClanTable.getInstance().getClan(ownerId);
				if (owner != null)
				{
					buffer.writeString(owner.getName()); // Clan Name
					buffer.writeString(owner.getLeaderName()); // Clan Leader Name
					buffer.writeInt(owner.getAllyId()); // Ally ID
					buffer.writeString(owner.getAllyName()); // Ally Name
				}
				else
				{
					buffer.writeString(""); // Clan Name
					buffer.writeString(""); // Clan Leader Name
					buffer.writeInt(0); // Ally ID
					buffer.writeString(""); // Ally Name
				}
			}
			else
			{
				buffer.writeString(""); // Clan Name
				buffer.writeString(""); // Clan Leader Name
				buffer.writeInt(0); // Ally ID
				buffer.writeString(""); // Ally Name
			}
			
			buffer.writeInt((int) (System.currentTimeMillis() / 1000));
			if (!_castle.isTimeRegistrationOver() && isClanLeader && (clanId == ownerId))
			{
				final Calendar cal = Calendar.getInstance();
				cal.setTimeInMillis(_castle.getSiegeDate().getTimeInMillis());
				cal.set(Calendar.MINUTE, 0);
				cal.set(Calendar.SECOND, 0);
				buffer.writeInt(0);
				buffer.writeInt(FeatureConfig.SIEGE_HOUR_LIST.size());
				for (int hour : FeatureConfig.SIEGE_HOUR_LIST)
				{
					cal.set(Calendar.HOUR_OF_DAY, hour);
					buffer.writeInt((int) (cal.getTimeInMillis() / 1000));
				}
			}
			else
			{
				buffer.writeInt((int) (_castle.getSiegeDate().getTimeInMillis() / 1000));
				buffer.writeInt(0);
			}
		}
	}
}
