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
 * This class holds the event data for when a {@code Player} changes their PK status.<br>
 * It is used to notify systems that a player's PvP status has been updated.
 * @author UnAfraid
 */
public class OnPlayerPKChanged implements IBaseEvent
{
	private final Player _player;
	private final int _oldPoints;
	private final int _newPoints;
	
	/**
	 * This constructor initializes a new {@code OnPlayerPKChanged} event.<br>
	 * It stores the player who changed and their previous and current PK points.
	 * @param player The {@code Player} object that experienced the change.
	 * @param oldPoints The number of PK points before the change.
	 * @param newPoints The number of PK points after the change.
	 */
	public OnPlayerPKChanged(Player player, int oldPoints, int newPoints)
	{
		_player = player;
		_oldPoints = oldPoints;
		_newPoints = newPoints;
	}
	
	/**
	 * Retrieves the {@code Player} associated with this event.
	 * @return the {@code Player} object.
	 */
	public Player getPlayer()
	{
		return _player;
	}
	
	/**
	 * Retrieves the previous point value.<br>
	 * This value is used before the change occurred.
	 * @return The old points as an {@code int}.
	 */
	public int getOldPoints()
	{
		return _oldPoints;
	}
	
	/**
	 * Retrieves the new points value.<br>
	 * This value is assigned when a player's PK status changes.
	 * @return The current number of new points as an {@code int}.
	 */
	public int getNewPoints()
	{
		return _newPoints;
	}
	
	/**
	 * Retrieves the type of this event.<br>
	 * It returns a constant from the {@code EventType} enum.
	 * @return The {@code EventType} associated with this event.
	 */
	@Override
	public EventType getType()
	{
		return EventType.ON_PLAYER_PK_CHANGED;
	}
}
