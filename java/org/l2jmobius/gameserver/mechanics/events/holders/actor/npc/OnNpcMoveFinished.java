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
 * This class represents an event triggered when an {@code Npc} completes its movement.<br>
 * It holds the necessary data for handling logic after a move action finishes.
 * @author UnAfraid
 */
public class OnNpcMoveFinished implements IBaseEvent
{
	private final Npc _npc;
	
	/**
	 * This constructor initializes a new {@code OnNpcMoveFinished} event.<br>
	 * It stores the specific {@code Npc} that finished moving.
	 * @param npc The {@code Npc} instance associated with this event.
	 */
	public OnNpcMoveFinished(Npc npc)
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
	 * Returns the type of this event.<br>
	 * This method identifies the specific {@code EventType} associated with the move completion.
	 * @return The {@code EventType} for this event.
	 */
	@Override
	public EventType getType()
	{
		return EventType.ON_NPC_MOVE_FINISHED;
	}
}
