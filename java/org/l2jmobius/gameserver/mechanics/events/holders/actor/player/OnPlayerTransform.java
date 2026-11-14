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
 * This class represents an event triggered when a {@code Player} undergoes a transformation.<br>
 * It holds the necessary data for handling transformation-related mechanics in the game engine.
 * @author UnAfraid
 */
public class OnPlayerTransform implements IBaseEvent
{
	private final Player _player;
	private final int _transformId;
	
	/**
	 * Creates a new {@code OnPlayerTransform} event.<br>
	 * This method initializes the event with a specific player and transformation ID.
	 * @param player The {@code Player} who underwent the transformation.
	 * @param transformId The unique identifier for the transformation type.
	 */
	public OnPlayerTransform(Player player, int transformId)
	{
		_player = player;
		_transformId = transformId;
	}
	
	/**
	 * Retrieves the {@code Player} associated with this event.<br>
	 * This method returns the player who triggered the transformation.
	 * @return the {@code Player} object.
	 */
	public Player getPlayer()
	{
		return _player;
	}
	
	/**
	 * Retrieves the unique identifier for the player's transformation.<br>
	 * This value is used to determine which form the {@code Player} has taken.
	 * @return The integer ID of the current transformation.
	 */
	public int getTransformId()
	{
		return _transformId;
	}
	
	/**
	 * Retrieves the type of this event.<br>
	 * This method returns a constant value from {@code EventType}.
	 * @return the {@code EventType} associated with this transformation.
	 */
	@Override
	public EventType getType()
	{
		return EventType.ON_PLAYER_TRANSFORM;
	}
}
