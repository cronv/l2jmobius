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
 * This class holds the data for an event triggered when a {@code Player} mentor status changes.<br>
 * It provides necessary context for handling mentorship updates within the game mechanics.
 * @author UnAfraid
 */
public class OnPlayerMentorStatus implements IBaseEvent
{
	private final Player _mentor;
	private final boolean _isOnline;
	
	/**
	 * Creates a new {@code OnPlayerMentorStatus} event.<br>
	 * This constructor initializes the status of a player's mentor.
	 * @param mentor The {@code Player} object representing the mentor.
	 * @param isOnline A boolean indicating if the mentor is currently online.
	 */
	public OnPlayerMentorStatus(Player mentor, boolean isOnline)
	{
		_mentor = mentor;
		_isOnline = isOnline;
	}
	
	/**
	 * Retrieves the {@code Player} who is acting as a mentor.<br>
	 * This method returns the mentor associated with this event status.
	 * @return the {@code Player} object representing the mentor
	 */
	public Player getMentor()
	{
		return _mentor;
	}
	
	/**
	 * Checks if the mentor is currently online.<br>
	 * This method returns the status stored in this event.
	 * @return {@code true} if the mentor is online, {@code false} otherwise.
	 */
	public boolean isMentorOnline()
	{
		return _isOnline;
	}
	
	/**
	 * Retrieves the type of this event.<br>
	 * It returns a constant from the {@code EventType} enum.
	 * @return the {@code EventType} associated with this status change.
	 */
	@Override
	public EventType getType()
	{
		return EventType.ON_PLAYER_MENTOR_STATUS;
	}
}
