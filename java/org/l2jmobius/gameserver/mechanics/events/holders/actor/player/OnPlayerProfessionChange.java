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
import org.l2jmobius.gameserver.entity.actor.templates.PlayerTemplate;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;

/**
 * This class holds data for an event triggered when a {@code Player} changes their profession.<br>
 * It allows the system to react to updates in a player's professional status.
 * @author UnAfraid
 */
public class OnPlayerProfessionChange implements IBaseEvent
{
	private final Player _player;
	private final PlayerTemplate _template;
	private final boolean _isSubClass;
	
	/**
	 * This constructor initializes a new {@code OnPlayerProfessionChange} event.<br>
	 * It stores the player who changed their profession.<br>
	 * It records the new {@code PlayerTemplate} associated with the change.<br>
	 * It tracks whether the new profession is a subclass.
	 * @param player The {@code Player} instance that triggered the event.
	 * @param template The {@code PlayerTemplate} of the new profession.
	 * @param isSubClass A boolean indicating if the new profession is a subclass.
	 */
	public OnPlayerProfessionChange(Player player, PlayerTemplate template, boolean isSubClass)
	{
		_player = player;
		_template = template;
		_isSubClass = isSubClass;
	}
	
	/**
	 * Retrieves the {@code Player} associated with this event.<br>
	 * This method returns the player who triggered the profession change.
	 * @return the {@code Player} object.
	 */
	public Player getPlayer()
	{
		return _player;
	}
	
	/**
	 * Retrieves the {@code PlayerTemplate} associated with this event.<br>
	 * This template contains the data for the player's profession.
	 * @return The {@code PlayerTemplate} object.
	 */
	public PlayerTemplate getTemplate()
	{
		return _template;
	}
	
	/**
	 * Checks if the profession change involves a sub-class.<br>
	 * This method returns the value of the internal {@code _isSubClass} flag.
	 * @return {@code true} if it is a sub-class, {@code false} otherwise.
	 */
	public boolean isSubClass()
	{
		return _isSubClass;
	}
	
	/**
	 * Returns the type of this event.<br>
	 * This method identifies the specific {@code EventType} associated with a profession change.
	 * @return The {@code EventType} for player profession changes.
	 */
	@Override
	public EventType getType()
	{
		return EventType.ON_PLAYER_PROFESSION_CHANGE;
	}
}
