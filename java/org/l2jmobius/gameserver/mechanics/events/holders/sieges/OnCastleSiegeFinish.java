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
 * This class represents an event triggered when a castle siege has concluded.<br>
 * It holds the necessary data for systems to respond to the end of a {@code Siege}.
 * @author UnAfraid
 */
public class OnCastleSiegeFinish implements IBaseEvent
{
	private final Siege _siege;
	
	/**
	 * This constructor initializes the event with a specific {@code Siege}.<br>
	 * It stores the {@code Siege} object for use during the event processing.
	 * @param siege The {@code Siege} instance that has finished.
	 */
	public OnCastleSiegeFinish(Siege siege)
	{
		_siege = siege;
	}
	
	/**
	 * Retrieves the {@code Siege} object associated with this event.<br>
	 * This method returns the siege data used during the castle siege.
	 * @return the {@code Siege} instance.
	 */
	public Siege getSiege()
	{
		return _siege;
	}
	
	/**
	 * Retrieves the type of this event.<br>
	 * This method returns a constant from {@code EventType}.
	 * @return The {@code EventType} associated with this event.
	 */
	@Override
	public EventType getType()
	{
		return EventType.ON_CASTLE_SIEGE_FINISH;
	}
}
