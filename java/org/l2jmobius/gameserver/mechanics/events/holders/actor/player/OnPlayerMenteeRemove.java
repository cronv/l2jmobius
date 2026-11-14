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
 * This event is triggered when a {@code Mentee} is removed from a {@code Player}.<br>
 * It handles the logic required to clean up mentee-related data or states.
 * @author UnAfraid
 */
public class OnPlayerMenteeRemove implements IBaseEvent
{
	private final Player _mentor;
	private final Mentee _mentee;
	
	/**
	 * Creates a new {@code OnPlayerMenteeRemove} event.<br>
	 * This method initializes the event with specific players.<br>
	 * It stores the mentor and the mentee who was removed.
	 * @param mentor The {@code Player} acting as the mentor.
	 * @param mentee The {@code Mentee} being removed from the mentor.
	 */
	public OnPlayerMenteeRemove(Player mentor, Mentee mentee)
	{
		_mentor = mentor;
		_mentee = mentee;
	}
	
	/**
	 * Retrieves the {@code Player} who is acting as a mentor.<br>
	 * This method returns the mentor associated with this event.
	 * @return the {@code Player} object representing the mentor.
	 */
	public Player getMentor()
	{
		return _mentor;
	}
	
	/**
	 * Retrieves the {@code Mentee} associated with this event.<br>
	 * This method returns the player who was removed from the mentorship.
	 * @return the {@code Mentee} object.
	 */
	public Mentee getMentee()
	{
		return _mentee;
	}
	
	/**
	 * Retrieves the type of this event.<br>
	 * This method returns a constant from the {@code EventType} enum.
	 * @return The {@code EventType} associated with this event.
	 */
	@Override
	public EventType getType()
	{
		return EventType.ON_PLAYER_MENTEE_REMOVE;
	}
}
