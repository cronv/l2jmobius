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
 * This class represents an event triggered when an {@code Instance} is destroyed.<br>
 * It allows the system to handle cleanup or logic specific to instance termination.
 * @author malyelfik
 */
public class OnInstanceDestroy implements IBaseEvent
{
	private final Instance _instance;
	
	/**
	 * Creates a new {@code OnInstanceDestroy} event.<br>
	 * This method initializes the event with a specific {@code Instance}.
	 * @param instance The {@code Instance} that is being destroyed.
	 */
	public OnInstanceDestroy(Instance instance)
	{
		_instance = instance;
	}
	
	/**
	 * Retrieves the {@code Instance} associated with this event.<br>
	 * This method returns the world instance that was destroyed.
	 * @return The {@code Instance} object.
	 */
	public Instance getInstanceWorld()
	{
		return _instance;
	}
	
	/**
	 * Retrieves the type of this event.<br>
	 * This method returns a constant value from {@code EventType}.
	 * @return The {@code EventType} associated with this instance.
	 */
	@Override
	public EventType getType()
	{
		return EventType.ON_INSTANCE_DESTROY;
	}
}
