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
 * This class represents an event triggered when a {@code Player}'s reputation value changes.<br>
 * It holds the necessary data for systems to react to reputation updates.
 * @author UnAfraid
 */
public class OnPlayerReputationChanged implements IBaseEvent
{
	private final Player _player;
	private final int _oldReputation;
	private final int _newReputation;
	
	/**
	 * This constructor initializes a new {@code OnPlayerReputationChanged} event.<br>
	 * It stores the player who changed and their reputation values.
	 * @param player The {@code Player} object that experienced the change.
	 * @param oldReputation The reputation value before the change.
	 * @param newReputation The reputation value after the change.
	 */
	public OnPlayerReputationChanged(Player player, int oldReputation, int newReputation)
	{
		_player = player;
		_oldReputation = oldReputation;
		_newReputation = newReputation;
	}
	
	/**
	 * Retrieves the {@code Player} associated with this event.<br>
	 * This method returns the player whose reputation was changed.
	 * @return the {@code Player} object.
	 */
	public Player getPlayer()
	{
		return _player;
	}
	
	/**
	 * Retrieves the reputation value before it was changed.<br>
	 * This is useful for comparing differences in player status.
	 * @return The previous reputation as an {@code int}.
	 */
	public int getOldReputation()
	{
		return _oldReputation;
	}
	
	/**
	 * Retrieves the reputation value after it was changed.<br>
	 * This returns the updated integer value for the {@code Player}.
	 * @return The new reputation value.
	 */
	public int getNewReputation()
	{
		return _newReputation;
	}
	
	/**
	 * Returns the type of this event.<br>
	 * This identifies the specific action that occurred.
	 * @return the {@code EventType} associated with this event.
	 */
	@Override
	public EventType getType()
	{
		return EventType.ON_PLAYER_REPUTATION_CHANGED;
	}
}
