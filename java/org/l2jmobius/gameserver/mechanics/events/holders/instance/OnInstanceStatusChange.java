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
package org.l2jmobius.gameserver.mechanics.events.holders.instance;

import org.l2jmobius.gameserver.entity.instancezone.Instance;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;

/**
 * This class represents an event triggered when the status of an {@code Instance} changes.<br>
 * It holds the necessary data for systems to react to these state transitions.
 * @author malyelfik
 */
public class OnInstanceStatusChange implements IBaseEvent
{
	private final Instance _world;
	private final int _status;
	
	/**
	 * Creates a new {@code OnInstanceStatusChange} event.<br>
	 * This method initializes the event with a specific instance and its new status.
	 * @param world The {@code Instance} that experienced the status change.
	 * @param status The new status value of the instance.
	 */
	public OnInstanceStatusChange(Instance world, int status)
	{
		_world = world;
		_status = status;
	}
	
	/**
	 * Retrieves the {@code Instance} associated with this event.<br>
	 * This method returns the world where the status change occurred.
	 * @return the {@code Instance} object representing the world.
	 */
	public Instance getWorld()
	{
		return _world;
	}
	
	/**
	 * Retrieves the current status of the instance.<br>
	 * This value represents the state change recorded in this event.
	 * @return The integer status code.
	 */
	public int getStatus()
	{
		return _status;
	}
	
	/**
	 * Returns the type of this event.<br>
	 * This method identifies the specific {@code EventType} associated with the instance status change.
	 * @return The {@code EventType} for this event.
	 */
	@Override
	public EventType getType()
	{
		return EventType.ON_INSTANCE_STATUS_CHANGE;
	}
}
