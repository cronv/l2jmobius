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
 * This class represents an event triggered when a {@code Player} bypasses a specific mechanic.<br>
 * It holds the necessary data for handling bypass logic within the game engine.
 * @author UnAfraid
 */
public class OnPlayerBypass implements IBaseEvent
{
	private final Player _player;
	private final String _command;
	
	/**
	 * Creates a new {@code OnPlayerBypass} event.<br>
	 * This method stores the player who performed the action.<br>
	 * It also saves the specific command used for the bypass.
	 * @param player The {@code Player} instance that triggered the event.
	 * @param command The {@code String} command provided by the player.
	 */
	public OnPlayerBypass(Player player, String command)
	{
		_player = player;
		_command = command;
	}
	
	/**
	 * Retrieves the {@code Player} associated with this event.<br>
	 * This method returns the player who triggered the bypass action.
	 * @return the {@code Player} object.
	 */
	public Player getPlayer()
	{
		return _player;
	}
	
	/**
	 * Retrieves the command string associated with this event.<br>
	 * This value was provided when the {@code OnPlayerBypass} object was created.
	 * @return The {@code String} representation of the command.
	 */
	public String getCommand()
	{
		return _command;
	}
	
	/**
	 * Retrieves the type of this event.<br>
	 * This method returns a constant value from {@code EventType}.
	 * @return The {@code EventType} associated with this event.
	 */
	@Override
	public EventType getType()
	{
		return EventType.ON_PLAYER_BYPASS;
	}
}
