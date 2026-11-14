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
 * This class represents an event triggered when a new {@code Player} is created.<br>
 * It holds the necessary data for handling initialization logic during player spawning.
 * @author UnAfraid
 */
public class OnPlayerCreate implements IBaseEvent
{
	private final Player _player;
	private final int _objectId;
	private final String _name;
	private final GameClient _client;
	
	/**
	 * This constructor initializes a new {@code OnPlayerCreate} event.<br>
	 * It stores the data for a newly created player.
	 * @param player The {@code Player} object being created.
	 * @param objectId The unique identifier for the new object.
	 * @param name The display name of the player.
	 * @param client The {@code GameClient} associated with this action.
	 */
	public OnPlayerCreate(Player player, int objectId, String name, GameClient client)
	{
		_player = player;
		_objectId = objectId;
		_name = name;
		_client = client;
	}
	
	/**
	 * Retrieves the {@code Player} associated with this event.<br>
	 * This method returns the player object that triggered the creation.
	 * @return the {@code Player} instance.
	 */
	public Player getPlayer()
	{
		return _player;
	}
	
	/**
	 * Retrieves the unique identifier for the object.<br>
	 * This value is stored during the creation of the {@code OnPlayerCreate} event.
	 * @return The integer ID of the object.
	 */
	public int getObjectId()
	{
		return _objectId;
	}
	
	/**
	 * Retrieves the name of the player.<br>
	 * This value is stored during the creation of the {@code OnPlayerCreate} event.
	 * @return The {@code String} name of the player.
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
	 * This method returns a constant value from {@code EventType}.
	 * @return The {@code EventType} associated with this event.
	 */
	@Override
	public EventType getType()
	{
		return EventType.ON_PLAYER_CREATE;
	}
}
