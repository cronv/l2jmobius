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

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;

/**
 * This class represents an event triggered when a {@code Player} changes their mentee status.<br>
 * It holds the necessary data for handling mentor-related state updates.
 * @author UnAfraid
 */
public class OnPlayerMenteeStatus implements IBaseEvent
{
	private final Player _mentee;
	private final boolean _isOnline;
	
	/**
	 * Creates a new {@code OnPlayerMenteeStatus} event.<br>
	 * This constructor initializes the status of a specific player.
	 * @param mentee The {@code Player} object representing the mentee.
	 * @param isOnline A boolean indicating if the mentee is currently online.
	 */
	public OnPlayerMenteeStatus(Player mentee, boolean isOnline)
	{
		_mentee = mentee;
		_isOnline = isOnline;
	}
	
	/**
	 * Retrieves the {@code Player} who is currently being mentored.
	 * @return the {@code Player} object representing the mentee.
	 */
	public Player getMentee()
	{
		return _mentee;
	}
	
	/**
	 * Checks if the mentee player is currently online.<br>
	 * This method returns the status stored in this event holder.
	 * @return {@code true} if the mentee is online, {@code false} otherwise.
	 */
	public boolean isMenteeOnline()
	{
		return _isOnline;
	}
	
	/**
	 * Retrieves the type of this event.<br>
	 * It returns a constant from the {@code EventType} enum.
	 * @return the {@code EventType} associated with this event.
	 */
	@Override
	public EventType getType()
	{
		return EventType.ON_PLAYER_MENTEE_STATUS;
	}
}
