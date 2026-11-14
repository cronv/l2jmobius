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
package org.l2jmobius.gameserver.mechanics.events.holders.sieges;

import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;
import org.l2jmobius.gameserver.mechanics.siege.Siege;

/**
 * This class represents an event triggered when a castle siege begins.<br>
 * It holds the necessary data for {@code Siege} mechanics to initialize during this phase.
 * @author UnAfraid
 */
public class OnCastleSiegeStart implements IBaseEvent
{
	private final Siege _siege;
	
	/**
	 * This constructor initializes a new {@code OnCastleSiegeStart} event.<br>
	 * It stores the specific {@code Siege} instance associated with the start of the siege.
	 * @param siege The {@code Siege} object to be used by this event.
	 */
	public OnCastleSiegeStart(Siege siege)
	{
		_siege = siege;
	}
	
	/**
	 * Retrieves the {@code Siege} object associated with this event.<br>
	 * This method provides access to the current siege data.
	 * @return The {@code Siege} instance.
	 */
	public Siege getSiege()
	{
		return _siege;
	}
	
	/**
	 * Returns the specific type of this event.<br>
	 * This method identifies the event as {@code ON_CASTLE_SIEGE_START}.
	 * @return The {@code EventType} associated with this siege start event.
	 */
	@Override
	public EventType getType()
	{
		return EventType.ON_CASTLE_SIEGE_START;
	}
}
