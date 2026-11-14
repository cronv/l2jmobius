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
 * This event is triggered when a new mentee is added to a player.<br>
 * It handles the logic associated with establishing a mentorship relationship.
 * @author UnAfraid
 */
public class OnPlayerMenteeAdd implements IBaseEvent
{
	private final Player _mentor;
	private final Player _mentee;
	
	/**
	 * Creates a new event for when a player is added as a mentee.<br>
	 * This constructor initializes the {@code mentor} and {@code mentee} fields.
	 * @param mentor The {@code Player} who will act as the teacher.
	 * @param mentee The {@code Player} who will be learning from the mentor.
	 */
	public OnPlayerMenteeAdd(Player mentor, Player mentee)
	{
		_mentor = mentor;
		_mentee = mentee;
	}
	
	/**
	 * Retrieves the {@code Player} who is acting as the mentor.<br>
	 * This method returns the mentor associated with this event.
	 * @return the {@code Player} object representing the mentor.
	 */
	public Player getMentor()
	{
		return _mentor;
	}
	
	/**
	 * Retrieves the player who is being mentored.<br>
	 * This method returns the {@code Player} object associated with the mentee role.
	 * @return the {@code Player} instance of the mentee.
	 */
	public Player getMentee()
	{
		return _mentee;
	}
	
	/**
	 * Retrieves the type of this event.<br>
	 * It returns the specific {@code EventType} associated with adding a mentee.
	 * @return The {@code EventType} for this event.
	 */
	@Override
	public EventType getType()
	{
		return EventType.ON_PLAYER_MENTEE_ADD;
	}
}
