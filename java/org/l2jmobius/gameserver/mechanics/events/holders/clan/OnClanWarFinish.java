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
package org.l2jmobius.gameserver.mechanics.events.holders.clan;

import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;

/**
 * This class represents an event triggered when a {@code Clan} war has concluded.<br>
 * It holds the necessary data to handle logic following the end of a clan warfare.<br>
 * It implements the {@code IBaseEvent} interface.
 * @author UnAfraid
 */
public class OnClanWarFinish implements IBaseEvent
{
	private final Clan _clan1;
	private final Clan _clan2;
	
	/**
	 * This constructor initializes a new {@code OnClanWarFinish} event.<br>
	 * It stores the two clans involved in the finished war.
	 * @param clan1 The first {@code Clan} involved in the war.
	 * @param clan2 The second {@code Clan} involved in the war.
	 */
	public OnClanWarFinish(Clan clan1, Clan clan2)
	{
		_clan1 = clan1;
		_clan2 = clan2;
	}
	
	/**
	 * Retrieves the first clan involved in the war.<br>
	 * This method returns the {@code Clan} object for the first participant.
	 * @return The first {@code Clan} associated with this event.
	 */
	public Clan getClan1()
	{
		return _clan1;
	}
	
	/**
	 * Retrieves the second clan involved in the war.<br>
	 * This method returns the {@code Clan} object stored as {@code _clan2}.
	 * @return the second {@code Clan} associated with this event.
	 */
	public Clan getClan2()
	{
		return _clan2;
	}
	
	/**
	 * Retrieves the type of this event.<br>
	 * This method returns a constant from the {@code EventType} enum.
	 * @return the {@code EventType} associated with this event.
	 */
	@Override
	public EventType getType()
	{
		return EventType.ON_CLAN_WAR_FINISH;
	}
}
