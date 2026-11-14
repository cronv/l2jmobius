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
package org.l2jmobius.gameserver.mechanics.events.holders;

import org.l2jmobius.gameserver.mechanics.events.EventType;

/**
 * This class represents an event triggered when the world time changes between day and night.<br>
 * It allows for handling specific logic related to these environmental transitions.
 * @author UnAfraid
 */
public class OnDayNightChange implements IBaseEvent
{
	private final boolean _isNight;
	
	/**
	 * Creates a new instance of {@code OnDayNightChange}.<br>
	 * This method sets the current time state.
	 * @param isNight The value to set for whether it is currently night.
	 */
	public OnDayNightChange(boolean isNight)
	{
		_isNight = isNight;
	}
	
	/**
	 * Checks if the current time state is night.<br>
	 * This method returns the value stored in the {@code _isNight} field.
	 * @return {@code true} if it is currently night, {@code false} otherwise.
	 */
	public boolean isNight()
	{
		return _isNight;
	}
	
	/**
	 * Returns the type of this event.<br>
	 * This method identifies the specific {@code EventType} associated with the change.
	 * @return The {@code EventType} for day and night changes.
	 */
	@Override
	public EventType getType()
	{
		return EventType.ON_DAY_NIGHT_CHANGE;
	}
}
