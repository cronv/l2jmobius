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
 * This class represents an event triggered when a {@code FortSiege} has concluded.<br>
 * It holds the necessary data to handle logic following the end of a siege.
 * @author UnAfraid
 */
public class OnFortSiegeFinish implements IBaseEvent
{
	private final FortSiege _siege;
	
	/**
	 * This constructor initializes a new {@code OnFortSiegeFinish} event.<br>
	 * It stores the specific {@code FortSiege} instance that has finished.
	 * @param siege The {@code FortSiege} object associated with this event.
	 */
	public OnFortSiegeFinish(FortSiege siege)
	{
		_siege = siege;
	}
	
	/**
	 * Retrieves the {@code FortSiege} associated with this event.<br>
	 * This method returns the siege object that triggered the finish event.
	 * @return The {@code FortSiege} instance.
	 */
	public FortSiege getSiege()
	{
		return _siege;
	}
	
	/**
	 * Returns the type of this event.<br>
	 * This method identifies the specific {@code EventType} associated with the siege finish.
	 * @return The {@code EventType} for this event.
	 */
	@Override
	public EventType getType()
	{
		return EventType.ON_FORT_SIEGE_FINISH;
	}
}
