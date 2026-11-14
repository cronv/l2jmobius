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
 * This class represents an event triggered when the owner of a {@code Siege} changes.<br>
 * It is used to notify other systems about ownership transitions during castle sieges.
 * @author UnAfraid
 */
public class OnCastleSiegeOwnerChange implements IBaseEvent
{
	private final Siege _siege;
	
	/**
	 * This constructor initializes a new {@code OnCastleSiegeOwnerChange} event.<br>
	 * It stores the specific {@code Siege} instance associated with this change.
	 * @param siege The {@code Siege} object that triggered the owner change.
	 */
	public OnCastleSiegeOwnerChange(Siege siege)
	{
		_siege = siege;
	}
	
	/**
	 * Retrieves the {@code Siege} object associated with this event.<br>
	 * This method returns the siege data used during the owner change.
	 * @return the {@code Siege} instance.
	 */
	public Siege getSiege()
	{
		return _siege;
	}
	
	/**
	 * Returns the type of this event.<br>
	 * This identifies the specific action that occurred.
	 * @return the {@code EventType} associated with this event.
	 */
	@Override
	public EventType getType()
	{
		return EventType.ON_CASTLE_SIEGE_OWNER_CHANGE;
	}
}
