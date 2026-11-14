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
 * This class represents an event triggered when a {@code Player} kills another player in PvP.<br>
 * It holds the necessary data for processing rewards or statistics related to this action.
 * @author UnAfraid
 */
public class OnPlayerPvPKill implements IBaseEvent
{
	private final Player _player;
	private final Player _target;
	
	/**
	 * Creates a new {@code OnPlayerPvPKill} event.<br>
	 * This event is triggered when one {@code Player} kills another {@code Player}.<br>
	 * It stores both the attacker and the victim for processing.
	 * @param player The {@code Player} who performed the kill.
	 * @param target The {@code Player} who was killed.
	 */
	public OnPlayerPvPKill(Player player, Player target)
	{
		_player = player;
		_target = target;
	}
	
	/**
	 * Retrieves the {@code Player} who performed the action.<br>
	 * This method returns the primary actor associated with this event.
	 * @return The {@code Player} object.
	 */
	public Player getPlayer()
	{
		return _player;
	}
	
	/**
	 * Retrieves the {@code Player} who was killed.
	 * @return the {@code Player} object representing the target.
	 */
	public Player getTarget()
	{
		return _target;
	}
	
	/**
	 * Retrieves the type of this event.<br>
	 * This method returns a constant value from {@code EventType}.
	 * @return The {@code EventType} associated with this event.
	 */
	@Override
	public EventType getType()
	{
		return EventType.ON_PLAYER_PVP_KILL;
	}
}
