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
package org.l2jmobius.gameserver.mechanics.events.holders.actor.player;

import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.clan.ClanMember;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;

/**
 * This class represents an event triggered when a player joins a {@code Clan}.<br>
 * It holds the necessary data for handling clan membership changes.<br>
 * Use this event to execute logic related to new member arrivals.
 * @author UnAfraid
 */
public class OnPlayerClanJoin implements IBaseEvent
{
	private final ClanMember _clanMember;
	private final Clan _clan;
	
	/**
	 * This constructor initializes a new {@code OnPlayerClanJoin} event.<br>
	 * It stores the member who joined and the clan they entered.
	 * @param clanMember The {@code ClanMember} who is joining the clan.
	 * @param clan The {@code Clan} that the player is joining.
	 */
	public OnPlayerClanJoin(ClanMember clanMember, Clan clan)
	{
		_clanMember = clanMember;
		_clan = clan;
	}
	
	/**
	 * Retrieves the {@code ClanMember} associated with this event.<br>
	 * This method returns the member who joined the clan.
	 * @return the {@code ClanMember} object.
	 */
	public ClanMember getClanMember()
	{
		return _clanMember;
	}
	
	/**
	 * Retrieves the {@code Clan} associated with this event.<br>
	 * This method returns the clan that the player joined.
	 * @return the {@code Clan} object.
	 */
	public Clan getClan()
	{
		return _clan;
	}
	
	/**
	 * Returns the type of this event.<br>
	 * This method identifies the specific {@code EventType} associated with a player joining a clan.
	 * @return The {@code EventType} for this event.
	 */
	@Override
	public EventType getType()
	{
		return EventType.ON_PLAYER_CLAN_JOIN;
	}
}
