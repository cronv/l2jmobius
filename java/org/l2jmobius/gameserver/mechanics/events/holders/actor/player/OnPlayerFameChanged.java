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
 * This class holds the event data for when a {@code Player}'s fame value changes.<br>
 * It is used to notify other systems of updates to player reputation.
 * @author UnAfraid
 */
public class OnPlayerFameChanged implements IBaseEvent
{
	private final Player _player;
	private final int _oldFame;
	private final int _newFame;
	
	/**
	 * This constructor initializes a new {@code OnPlayerFameChanged} event.<br>
	 * It stores the player who experienced the change.<br>
	 * It also records the previous and current fame values.
	 * @param player The {@code Player} object associated with this event.
	 * @param oldFame The fame value before the change occurred.
	 * @param newFame The fame value after the change occurred.
	 */
	public OnPlayerFameChanged(Player player, int oldFame, int newFame)
	{
		_player = player;
		_oldFame = oldFame;
		_newFame = newFame;
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
	 * Retrieves the fame value before it was changed.<br>
	 * This is useful for comparing changes in player stats.
	 * @return the previous {@code int} fame value.
	 */
	public int getOldFame()
	{
		return _oldFame;
	}
	
	/**
	 * Retrieves the new fame value.<br>
	 * This value is used after a change occurs.
	 * @return The updated fame as an {@code int}.
	 */
	public int getNewFame()
	{
		return _newFame;
	}
	
	/**
	 * Retrieves the type of this event.<br>
	 * It returns the specific {@code EventType} associated with fame changes.
	 * @return The {@code EventType} for this event.
	 */
	@Override
	public EventType getType()
	{
		return EventType.ON_PLAYER_FAME_CHANGED;
	}
}
