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
 * This event is triggered when a player's clan leader status changes.<br>
 * It provides data regarding the {@code Clan} and the involved {@code ClanMember} entities.
 * @author UnAfraid
 */
public class OnPlayerClanLeaderChange implements IBaseEvent
{
	private final ClanMember _oldLeader;
	private final ClanMember _newLeader;
	private final Clan _clan;
	
	/**
	 * This constructor initializes a new {@code OnPlayerClanLeaderChange} event.<br>
	 * It stores the details of the leadership change within a {@code Clan}.
	 * @param oldLeader The {@code ClanMember} who was previously the leader.
	 * @param newLeader The {@code ClanMember} who is now the leader.
	 * @param clan The {@code Clan} where the leadership change occurred.
	 */
	public OnPlayerClanLeaderChange(ClanMember oldLeader, ClanMember newLeader, Clan clan)
	{
		_oldLeader = oldLeader;
		_newLeader = newLeader;
		_clan = clan;
	}
	
	/**
	 * Retrieves the member who was previously the leader of the clan.<br>
	 * This is used during a leadership change event.
	 * @return the {@code ClanMember} who held the leader position before the change.
	 */
	public ClanMember getOldLeader()
	{
		return _oldLeader;
	}
	
	/**
	 * Retrieves the member who became the new leader.<br>
	 * This method returns the {@code ClanMember} object associated with the new leadership role.
	 * @return The {@code ClanMember} who is now the leader of the clan.
	 */
	public ClanMember getNewLeader()
	{
		return _newLeader;
	}
	
	/**
	 * Retrieves the {@code Clan} associated with this event.<br>
	 * This method returns the clan where the leader change occurred.
	 * @return the {@code Clan} object.
	 */
	public Clan getClan()
	{
		return _clan;
	}
	
	/**
	 * Retrieves the type of this event.<br>
	 * It returns a constant value from {@code EventType}.
	 * @return the {@code EventType} associated with this change.
	 */
	@Override
	public EventType getType()
	{
		return EventType.ON_PLAYER_CLAN_LEADER_CHANGE;
	}
}
