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
import org.l2jmobius.gameserver.mechanics.siege.FortSiege;

/**
 * This class represents an event triggered when a {@code FortSiege} begins.<br>
 * It holds the necessary data for systems to react to the start of a siege.
 * @author UnAfraid
 */
public class OnFortSiegeStart implements IBaseEvent
{
	private final FortSiege _siege;
	
	/**
	 * Creates a new instance of {@code OnFortSiegeStart}.<br>
	 * This method initializes the event with a specific {@code FortSiege} object.
	 * @param siege The {@code FortSiege} instance associated with this event.
	 */
	public OnFortSiegeStart(FortSiege siege)
	{
		_siege = siege;
	}
	
	/**
	 * Retrieves the {@code FortSiege} associated with this event.<br>
	 * This method returns the siege object that triggered the start event.
	 * @return the {@code FortSiege} instance.
	 */
	public FortSiege getSiege()
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
		return EventType.ON_FORT_SIEGE_START;
	}
}
