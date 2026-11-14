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
package org.l2jmobius.gameserver.mechanics.events.holders.actor.npc;

import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;

/**
 * This class represents an event triggered when an {@code Npc} becomes visible to a {@code Player}.<br>
 * It holds the necessary data for processing visibility logic between these two entities.
 * @author UnAfraid
 */
public class OnNpcCanBeSeen implements IBaseEvent
{
	private final Npc _npc;
	private final Player _player;
	
	/**
	 * Creates a new event for when an {@code Npc} becomes visible to a {@code Player}.<br>
	 * This constructor initializes the required actors for the event.
	 * @param npc The {@code Npc} that can be seen.
	 * @param player The {@code Player} who sees the NPC.
	 */
	public OnNpcCanBeSeen(Npc npc, Player player)
	{
		_npc = npc;
		_player = player;
	}
	
	/**
	 * Retrieves the {@code Npc} associated with this event.
	 * @return the {@code Npc} object.
	 */
	public Npc getNpc()
	{
		return _npc;
	}
	
	/**
	 * Retrieves the {@code Player} associated with this event.<br>
	 * This method returns the player who triggered the visibility check.
	 * @return the {@code Player} object.
	 */
	public Player getPlayer()
	{
		return _player;
	}
	
	/**
	 * Retrieves the type of this event.<br>
	 * This method returns a constant value from {@code EventType}.
	 * @return The {@code EventType} associated with this event.
	 */
	@Override
	public EventType getType()
	{
		return EventType.ON_NPC_CAN_BE_SEEN;
	}
}
