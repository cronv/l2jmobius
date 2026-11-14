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
import org.l2jmobius.gameserver.entity.actor.holders.player.Mentee;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;

/**
 * This event is triggered when a {@code Player} leaves their current {@code Mentee} status.<br>
 * It handles the necessary cleanup and logic associated with ending a mentorship.
 * @author UnAfraid
 */
public class OnPlayerMenteeLeft implements IBaseEvent
{
	private final Mentee _mentor;
	private final Player _mentee;
	
	/**
	 * This constructor initializes the event when a {@code Player} leaves their mentor.<br>
	 * It stores the references for both the {@code Mentee} and the {@code Player}.
	 * @param mentor The {@code Mentee} object representing the mentor.
	 * @param mentee The {@code Player} object representing the student who left.
	 */
	public OnPlayerMenteeLeft(Mentee mentor, Player mentee)
	{
		_mentor = mentor;
		_mentee = mentee;
	}
	
	/**
	 * Retrieves the {@code Mentee} object associated with this event.<br>
	 * This method returns the mentor who was involved in the action.
	 * @return The {@code Mentee} object representing the mentor.
	 */
	public Mentee getMentor()
	{
		return _mentor;
	}
	
	/**
	 * Retrieves the {@code Player} who was a mentee.<br>
	 * This method returns the specific player associated with this event.
	 * @return the {@code Player} object representing the mentee.
	 */
	public Player getMentee()
	{
		return _mentee;
	}
	
	/**
	 * Retrieves the type of this event.<br>
	 * This method returns a constant from the {@code EventType} enum.
	 * @return the {@code EventType} associated with this event.
	 */
	@Override
	public EventType getType()
	{
		return EventType.ON_PLAYER_MENTEE_LEFT;
	}
}
