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
 * This class holds the event data for when a {@code Player} changes their PvP status.<br>
 * It is used to notify other systems of transitions between different PvP states.
 * @author UnAfraid
 */
public class OnPlayerPvPChanged implements IBaseEvent
{
	private final Player _player;
	private final int _oldPoints;
	private final int _newPoints;
	
	/**
	 * Creates a new {@code OnPlayerPvPChanged} event.<br>
	 * This event tracks changes to a player's PvP points.<br>
	 * It stores the previous and current point values.
	 * @param player The {@code Player} who experienced the change.
	 * @param oldPoints The value of the points before the change.
	 * @param newPoints The value of the points after the change.
	 */
	public OnPlayerPvPChanged(Player player, int oldPoints, int newPoints)
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
	 * Retrieves the previous PvP points of the player.<br>
	 * This value is captured before any changes occur.
	 * @return The integer value of the old points.
	 */
	public int getOldPoints()
	{
		return _oldPoints;
	}
	
	/**
	 * Retrieves the new PvP points for the player.<br>
	 * This value is updated when a PvP change occurs.
	 * @return the current number of {@code int} points.
	 */
	public int getNewPoints()
	{
		return _newPoints;
	}
	
	/**
	 * Returns the type of this event.<br>
	 * This method identifies the specific {@code EventType} associated with the change.
	 * @return The {@code EventType} for this event.
	 */
	@Override
	public EventType getType()
	{
		return EventType.ON_PLAYER_PVP_CHANGED;
	}
}
