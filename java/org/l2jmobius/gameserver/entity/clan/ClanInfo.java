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
package org.l2jmobius.gameserver.entity.clan;

/**
 * This class stores and manages the general information for a {@code Clan}.<br>
 * It serves as a data container for clan-related attributes and properties.
 * @author UnAfraid
 */
public class ClanInfo
{
	private final Clan _clan;
	private final int _total;
	private final int _online;
	
	/**
	 * Creates a new {@code ClanInfo} object from a {@code Clan}.<br>
	 * This constructor initializes the total and online member counts.
	 * @param clan The {@code Clan} object to use for initialization.
	 */
	public ClanInfo(Clan clan)
	{
		_clan = clan;
		_total = clan.getMembersCount();
		_online = clan.getOnlineMembersCount();
	}
	
	/**
	 * Retrieves the {@code Clan} associated with this information.<br>
	 * This method returns the internal clan object.
	 * @return The {@code Clan} object.
	 */
	public Clan getClan()
	{
		return _clan;
	}
	
	/**
	 * Retrieves the total number of members in the clan.<br>
	 * This value is stored in the {@code _total} field.
	 * @return The total count of clan members as an {@code int}.
	 */
	public int getTotal()
	{
		return _total;
	}
	
	/**
	 * Retrieves the current number of online members.<br>
	 * This value represents the count of players currently active in the clan.
	 * @return The total number of online members as an {@code int}.
	 */
	public int getOnline()
	{
		return _online;
	}
}
