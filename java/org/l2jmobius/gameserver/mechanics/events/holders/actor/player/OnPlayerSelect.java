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
import org.l2jmobius.gameserver.network.GameClient;

/**
 * This class represents an event triggered when a {@code Player} is selected.<br>
 * It holds the necessary data for handling selection logic within the game engine.
 * @author UnAfraid
 */
public class OnPlayerSelect implements IBaseEvent
{
	private final Player _player;
	private final int _objectId;
	private final String _name;
	private final GameClient _client;
	
	/**
	 * Creates a new {@code OnPlayerSelect} event.<br>
	 * This constructor initializes the event with all required data.
	 * @param player The {@code Player} who triggered the selection.
	 * @param objectId The unique identifier of the selected object.
	 * @param name The display name of the selected object.
	 * @param client The {@code GameClient} associated with this action.
	 */
	public OnPlayerSelect(Player player, int objectId, String name, GameClient client)
	{
		_player = player;
		_objectId = objectId;
		_name = name;
		_client = client;
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
	 * Retrieves the unique identifier of the selected object.<br>
	 * This value is stored when the {@code OnPlayerSelect} event is created.
	 * @return The {@code int} ID of the object.
	 */
	public int getObjectId()
	{
		return _objectId;
	}
	
	/**
	 * Returns the name of the player.<br>
	 * This value is retrieved from the {@code OnPlayerSelect} instance.
	 * @return The name as a {@code String}.
	 */
	public String getName()
	{
		return _name;
	}
	
	/**
	 * Retrieves the {@code GameClient} associated with this event.<br>
	 * This provides access to the network client of the player.
	 * @return the {@code GameClient} instance.
	 */
	public GameClient getClient()
	{
		return _client;
	}
	
	/**
	 * Retrieves the type of this event.<br>
	 * It returns a constant value from the {@code EventType} enum.
	 * @return the {@code EventType} associated with this event.
	 */
	@Override
	public EventType getType()
	{
		return EventType.ON_PLAYER_SELECT;
	}
}
