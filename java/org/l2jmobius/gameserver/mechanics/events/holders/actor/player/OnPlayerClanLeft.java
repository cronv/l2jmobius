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
 * This event is triggered when a player leaves a {@code Clan}.<br>
 * It holds the necessary data for handling logic related to this action.
 * @author UnAfraid
 */
public class OnPlayerClanLeft implements IBaseEvent
{
	private final ClanMember _clanMember;
	private final Clan _clan;
	
	/**
	 * This constructor initializes a new {@code OnPlayerClanLeft} event.<br>
	 * It stores the player who left and the clan they departed from.
	 * @param clanMember The {@code ClanMember} who is leaving the clan.
	 * @param clan The {@code Clan} that the member is leaving.
	 */
	public OnPlayerClanLeft(ClanMember clanMember, Clan clan)
	{
		_clanMember = clanMember;
		_clan = clan;
	}
	
	/**
	 * Retrieves the {@code ClanMember} associated with this event.<br>
	 * This method returns the member who left the clan.
	 * @return the {@code ClanMember} object.
	 */
	public ClanMember getClanMember()
	{
		return _clanMember;
	}
	
	/**
	 * Retrieves the {@code Clan} associated with this event.<br>
	 * This method returns the clan that was left by the player.
	 * @return The {@code Clan} object.
	 */
	public Clan getClan()
	{
		return _clan;
	}
	
	/**
	 * Returns the type of this event.<br>
	 * This method identifies the specific {@code EventType} associated with a player leaving a clan.
	 * @return The {@code EventType} for this event.
	 */
	@Override
	public EventType getType()
	{
		return EventType.ON_PLAYER_CLAN_LEFT;
	}
}
