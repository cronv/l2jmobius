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
 * This class holds the event data for when a {@code Player}'s ability points change.<br>
 * It allows other systems to react to changes in the player's skill point totals.
 * @author St3eT
 */
public class OnPlayerAbilityPointsChanged implements IBaseEvent
{
	private final Player _player;
	private final int _newAbilityPoints;
	private final int _oldAbilityPoints;
	
	/**
	 * This constructor initializes a new {@code OnPlayerAbilityPointsChanged} event.<br>
	 * It stores the player who changed their points.<br>
	 * It also records both the previous and current ability point values.
	 * @param player The {@code Player} instance that triggered the change.
	 * @param newAbilityPoints The value of the ability points after the change.
	 * @param oldAbilityPoints The value of the ability points before the change.
	 */
	public OnPlayerAbilityPointsChanged(Player player, int newAbilityPoints, int oldAbilityPoints)
	{
		_player = player;
		_newAbilityPoints = newAbilityPoints;
		_oldAbilityPoints = oldAbilityPoints;
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
	 * Retrieves the new ability points for the player.<br>
	 * This value is used when a change event occurs.
	 * @return The number of new ability points as a {@code long}.
	 */
	public long getNewAbilityPoints()
	{
		return _newAbilityPoints;
	}
	
	/**
	 * Retrieves the ability points before they were changed.<br>
	 * This value represents the previous state of the player's stats.
	 * @return The old amount of ability points as a {@code long}.
	 */
	public long getOldAbilityPoints()
	{
		return _oldAbilityPoints;
	}
	
	/**
	 * Retrieves the type of this event.<br>
	 * It returns a constant from the {@code EventType} enum.
	 * @return The {@code EventType} associated with this event.
	 */
	@Override
	public EventType getType()
	{
		return EventType.ON_PLAYER_ABILITY_POINTS_CHANGED;
	}
}
