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
 * This class represents an event triggered when a {@code Player} presses a tutorial mark.<br>
 * It is used to handle specific logic related to the tutorial progression system.
 * @author St3eT
 */
public class OnPlayerPressTutorialMark implements IBaseEvent
{
	private final Player _player;
	private final int _markId;
	
	/**
	 * Creates a new {@code OnPlayerPressTutorialMark} event.<br>
	 * This event is triggered when a {@code Player} interacts with a tutorial mark.<br>
	 * It stores the specific player and the unique identifier of the mark.
	 * @param player The {@code Player} who pressed the tutorial mark.
	 * @param markId The unique ID of the tutorial mark that was pressed.
	 */
	public OnPlayerPressTutorialMark(Player player, int markId)
	{
		_player = player;
		_markId = markId;
	}
	
	/**
	 * Retrieves the {@code Player} associated with this event.
	 * @return The {@code Player} object.
	 */
	public Player getPlayer()
	{
		return _player;
	}
	
	/**
	 * Retrieves the unique identifier for the tutorial mark.<br>
	 * This value is used to identify which specific mark was pressed.
	 * @return The {@code int} ID of the tutorial mark.
	 */
	public int getMarkId()
	{
		return _markId;
	}
	
	/**
	 * Retrieves the type of this event.<br>
	 * It returns a constant from the {@code EventType} enum.
	 * @return the {@code EventType} associated with this event.
	 */
	@Override
	public EventType getType()
	{
		return EventType.ON_PLAYER_PRESS_TUTORIAL_MARK;
	}
}
