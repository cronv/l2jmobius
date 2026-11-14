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
 * This class represents an event triggered when a {@code Player} logs out of the game.<br>
 * It holds the necessary data for handling logout logic within the {@code EventType} system.
 * @author UnAfraid
 */
public class OnPlayerLogout implements IBaseEvent
{
	private final Player _player;
	
	/**
	 * Creates a new {@code OnPlayerLogout} event.<br>
	 * This method is called when a {@code Player} leaves the game.
	 * @param player The {@code Player} object that triggered the logout event.
	 */
	public OnPlayerLogout(Player player)
	{
		_player = player;
	}
	
	/**
	 * Retrieves the {@code Player} associated with this event.<br>
	 * This method returns the player who triggered the logout action.
	 * @return The {@code Player} object.
	 */
	public Player getPlayer()
	{
		return _player;
	}
	
	/**
	 * Retrieves the type of this event.<br>
	 * This method returns a constant value from {@code EventType}.
	 * @return The {@code EventType} associated with this logout event.
	 */
	@Override
	public EventType getType()
	{
		return EventType.ON_PLAYER_LOGOUT;
	}
}
