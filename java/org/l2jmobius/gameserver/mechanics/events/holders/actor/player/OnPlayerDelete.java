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
 * This class represents an event triggered when a {@code Player} is deleted from the game world.<br>
 * It holds the necessary data for systems to respond to this specific lifecycle change.
 * @author UnAfraid
 */
public class OnPlayerDelete implements IBaseEvent
{
	private final int _objectId;
	private final String _name;
	private final GameClient _client;
	
	/**
	 * This constructor initializes a new {@code OnPlayerDelete} event.<br>
	 * It stores the player's unique ID, their name, and their network client.
	 * @param objectId The unique identifier of the deleted player.
	 * @param name The display name of the player.
	 * @param client The {@code GameClient} associated with the player.
	 */
	public OnPlayerDelete(int objectId, String name, GameClient client)
	{
		_objectId = objectId;
		_name = name;
		_client = client;
	}
	
	/**
	 * Retrieves the unique identifier of the player.<br>
	 * This ID is used to identify the specific object in the game world.
	 * @return The {@code int} value representing the object ID.
	 */
	public int getObjectId()
	{
		return _objectId;
	}
	
	/**
	 * Retrieves the name of the player.<br>
	 * This value is stored when the {@code OnPlayerDelete} event is created.
	 * @return The {@code String} name of the player.
	 */
	public String getName()
	{
		return _name;
	}
	
	/**
	 * Retrieves the {@code GameClient} associated with this event.<br>
	 * This provides access to the player's network connection.
	 * @return the {@code GameClient} instance.
	 */
	public GameClient getClient()
	{
		return _client;
	}
	
	/**
	 * Returns the type of this event.<br>
	 * This method identifies the specific {@code EventType} associated with the player deletion.
	 * @return The {@code EventType} for this event.
	 */
	@Override
	public EventType getType()
	{
		return EventType.ON_PLAYER_DELETE;
	}
}
