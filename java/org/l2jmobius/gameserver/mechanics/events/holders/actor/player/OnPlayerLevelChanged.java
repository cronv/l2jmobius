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
 * This class holds the data for an event triggered when a {@code Player} changes their level.<br>
 * It allows other systems to react to level updates within the game engine.
 * @author UnAfraid
 */
public class OnPlayerLevelChanged implements IBaseEvent
{
	private final Player _player;
	private final int _oldLevel;
	private final int _newLevel;
	
	/**
	 * This constructor initializes a new {@code OnPlayerLevelChanged} event.<br>
	 * It stores the player who leveled up and their level change details.
	 * @param player The {@code Player} object that experienced the level change.
	 * @param oldLevel The previous level of the {@code Player}.
	 * @param newLevel The current level of the {@code Player}.
	 */
	public OnPlayerLevelChanged(Player player, int oldLevel, int newLevel)
	{
		_player = player;
		_oldLevel = oldLevel;
		_newLevel = newLevel;
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
	 * Retrieves the level of the {@code Player} before it was changed.<br>
	 * This value is stored during the creation of the {@code OnPlayerLevelChanged} event.
	 * @return The previous level as an {@code int}.
	 */
	public int getOldLevel()
	{
		return _oldLevel;
	}
	
	/**
	 * Retrieves the level of the player after it changed.<br>
	 * This value is stored in the {@code _newLevel} field.
	 * @return The new level as an {@code int}.
	 */
	public int getNewLevel()
	{
		return _newLevel;
	}
	
	/**
	 * Returns the type of this event.<br>
	 * This method identifies the specific {@code EventType} associated with the level change.
	 * @return The {@code EventType} for player level changes.
	 */
	@Override
	public EventType getType()
	{
		return EventType.ON_PLAYER_LEVEL_CHANGED;
	}
}
