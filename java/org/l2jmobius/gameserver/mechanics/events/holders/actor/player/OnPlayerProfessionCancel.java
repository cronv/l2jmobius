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
 * This class holds the event data for when a {@code Player} cancels their current profession.<br>
 * It is used to manage the state and logic associated with this specific action.
 * @author Krunchy
 * @since 2.6.0.0
 */
public class OnPlayerProfessionCancel implements IBaseEvent
{
	private final Player _player;
	private final int _classId;
	
	/**
	 * Creates a new {@code OnPlayerProfessionCancel} event.<br>
	 * This event is triggered when a player cancels a profession.
	 * @param player The {@code Player} who performed the action.
	 * @param classId The unique identifier for the profession being cancelled.
	 */
	public OnPlayerProfessionCancel(Player player, int classId)
	{
		_player = player;
		_classId = classId;
	}
	
	/**
	 * Retrieves the {@code Player} associated with this event.<br>
	 * This method returns the player who triggered the profession cancellation.
	 * @return the {@code Player} object.
	 */
	public Player getPlayer()
	{
		return _player;
	}
	
	/**
	 * Retrieves the unique identifier for the profession class.<br>
	 * This value was provided when the {@code OnPlayerProfessionCancel} event was created.
	 * @return The {@code int} ID of the profession class.
	 */
	public int getClassId()
	{
		return _classId;
	}
	
	/**
	 * Retrieves the type of this event.<br>
	 * This method returns a constant from the {@code EventType} enum.
	 * @return The {@code EventType} associated with this event.
	 */
	@Override
	public EventType getType()
	{
		return EventType.ON_PLAYER_PROFESSION_CANCEL;
	}
}
