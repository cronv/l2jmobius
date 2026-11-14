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
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;

/**
 * This class represents an event triggered when an {@code Npc} performs a teleport.<br>
 * It holds the necessary data for handling teleportation logic within the mechanics system.
 * @author Zealar
 */
public class OnNpcTeleport implements IBaseEvent
{
	private final Npc _npc;
	
	/**
	 * Creates a new {@code OnNpcTeleport} event instance.<br>
	 * This method initializes the event with a specific {@code Npc}.
	 * @param npc The {@code Npc} that triggered the teleport event.
	 */
	public OnNpcTeleport(Npc npc)
	{
		_npc = npc;
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
	 * Retrieves the type of this event.<br>
	 * This method returns a constant from {@code EventType}.
	 * @return The {@code EventType} associated with this event.
	 */
	@Override
	public EventType getType()
	{
		return EventType.ON_NPC_TELEPORT;
	}
}
