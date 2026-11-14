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

import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;
import org.l2jmobius.gameserver.network.GameClient;

/**
 * This class holds the data for an event triggered when a player is restored.<br>
 * It provides necessary context for handling {@code OnPlayerRestore} logic within the game engine.
 * @author UnAfraid
 */
public class OnPlayerRestore implements IBaseEvent
{
	private final int _objectId;
	private final String _name;
	private final GameClient _client;
	
	/**
	 * Creates a new {@code OnPlayerRestore} event instance.<br>
	 * This constructor initializes the player data for the restoration event.
	 * @param objectId The unique identifier of the player object.
	 * @param name The display name of the player.
	 * @param client The {@code GameClient} associated with the player.
	 */
	public OnPlayerRestore(int objectId, String name, GameClient client)
	{
		_objectId = objectId;
		_name = name;
		_client = client;
	}
	
	/**
	 * Retrieves the unique identifier of the object.<br>
	 * This value is stored during the creation of the {@code OnPlayerRestore} instance.
	 * @return The {@code int} ID of the object.
	 */
	public int getObjectId()
	{
		return _objectId;
	}
	
	/**
	 * Retrieves the name of the player.<br>
	 * This value is stored during object creation.
	 * @return The {@code String} name of the player.
	 */
	public String getName()
	{
		return _name;
	}
	
	/**
	 * Retrieves the {@code GameClient} associated with this event.<br>
	 * This method returns the client instance used during the restoration process.
	 * @return The {@code GameClient} object.
	 */
	public GameClient getClient()
	{
		return _client;
	}
	
	/**
	 * Retrieves the type of this event.<br>
	 * It returns a constant from the {@code EventType} enum.
	 * @return The {@code EventType} associated with this event.
	 */
	@Override
	public EventType getType()
	{
		return EventType.ON_PLAYER_RESTORE;
	}
}
