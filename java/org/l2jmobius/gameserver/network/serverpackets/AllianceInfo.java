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

import java.util.Collection;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.clan.ClanInfo;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * This packet is sent in response to a {@code RequestAllyInfo} request.<br>
 * It contains the necessary information regarding an alliance for the client.
 * @author afk5min
 */
public class AllianceInfo extends ServerPacket
{
	private final String _name;
	private final int _total;
	private final int _online;
	private final String _leaderC;
	private final String _leaderP;
	private final ClanInfo[] _allies;
	
	/**
	 * Creates a new {@code AllianceInfo} object based on an alliance ID.<br>
	 * This constructor fetches the leader details and all member clans from the database.<br>
	 * It calculates the total and online counts for the entire alliance.
	 * @param allianceId The unique identifier of the alliance to load.
	 */
	public AllianceInfo(int allianceId)
	{
		final Clan leader = ClanTable.getInstance().getClan(allianceId);
		_name = leader.getAllyName();
		_leaderC = leader.getName();
		_leaderP = leader.getLeaderName();
		final Collection<Clan> allies = ClanTable.getInstance().getClanAllies(allianceId);
		_allies = new ClanInfo[allies.size()];
		int idx = 0;
		int total = 0;
		int online = 0;
		for (Clan clan : allies)
		{
			final ClanInfo ci = new ClanInfo(clan);
			_allies[idx++] = ci;
			total += ci.getTotal();
			online += ci.getOnline();
		}
		
		_total = total;
		_online = online;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.ALLIANCE_INFO.writeId(this, buffer);
		buffer.writeString(_name);
		buffer.writeInt(_total);
		buffer.writeInt(_online);
		buffer.writeString(_leaderC);
		buffer.writeString(_leaderP);
		buffer.writeInt(_allies.length);
		for (ClanInfo aci : _allies)
		{
			buffer.writeString(aci.getClan().getName());
			buffer.writeInt(0);
			buffer.writeInt(aci.getClan().getLevel());
			buffer.writeString(aci.getClan().getLeaderName());
			buffer.writeInt(aci.getTotal());
			buffer.writeInt(aci.getOnline());
		}
	}
	
	/**
	 * Retrieves the name of the alliance.
	 * @return The {@code String} name of the alliance.
	 */
	public String getName()
	{
		return _name;
	}
	
	/**
	 * Retrieves the total number of members in the alliance.<br>
	 * This value represents the full count of participants.
	 * @return The total member count as an {@code int}.
	 */
	public int getTotal()
	{
		return _total;
	}
	
	/**
	 * Retrieves the number of online members in the alliance.<br>
	 * This value represents the current active count.
	 * @return The number of online members as an {@code int}.
	 */
	public int getOnline()
	{
		return _online;
	}
	
	/**
	 * Retrieves the character name of the alliance leader.<br>
	 * This value is stored in the {@code _leaderC} field.
	 * @return The name of the leader as a {@code String}.
	 */
	public String getLeaderC()
	{
		return _leaderC;
	}
	
	/**
	 * Retrieves the name of the leader's character.<br>
	 * This value is stored in the {@code _leaderP} field.
	 * @return The name of the leader's character as a {@code String}.
	 */
	public String getLeaderP()
	{
		return _leaderP;
	}
	
	/**
	 * Retrieves the list of all allies in the alliance.<br>
	 * This method returns an array of {@code ClanInfo} objects.
	 * @return An array containing all {@code ClanInfo} for the allies.
	 */
	public ClanInfo[] getAllies()
	{
		return _allies;
	}
}
