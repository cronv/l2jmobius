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
import org.l2jmobius.gameserver.entity.teleporter.TeleportLocation;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;

/**
 * This class acts as a listener for player teleport requests.<br>
 * It is triggered by the {@code Npc, int)} method.
 * @author malyelfik
 */
public class OnNpcTeleportRequest implements IBaseEvent
{
	private final Player _player;
	private final Npc _npc;
	private final TeleportLocation _loc;
	
	/**
	 * Creates a new request for an {@code Npc} teleport.<br>
	 * This event is triggered when a {@code Player} interacts with a teleporter.
	 * @param player The {@code Player} who initiated the teleport.
	 * @param npc The {@code Npc} acting as the teleporter.
	 * @param loc The destination {@code TeleportLocation}.
	 */
	public OnNpcTeleportRequest(Player player, Npc npc, TeleportLocation loc)
	{
		_player = player;
		_npc = npc;
		_loc = loc;
	}
	
	/**
	 * Retrieves the {@code Player} associated with this event.<br>
	 * This method returns the player who initiated the teleport request.
	 * @return the {@code Player} object.
	 */
	public Player getPlayer()
	{
		return _player;
	}
	
	/**
	 * Retrieves the {@code Npc} associated with this teleport request.
	 * @return the {@code Npc} object.
	 */
	public Npc getNpc()
	{
		return _npc;
	}
	
	/**
	 * Retrieves the destination of the teleport.<br>
	 * This returns the {@code TeleportLocation} object associated with this event.
	 * @return the {@code TeleportLocation} for the teleport request.
	 */
	public TeleportLocation getLocation()
	{
		return _loc;
	}
	
	/**
	 * Returns the type of this event.<br>
	 * This identifies the event as {@code ON_NPC_TELEPORT_REQUEST}.
	 * @return the {@code EventType} associated with this request.
	 */
	@Override
	public EventType getType()
	{
		return EventType.ON_NPC_TELEPORT_REQUEST;
	}
}
