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

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.instancezone.Instance;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;

/**
 * This class represents an event triggered when a new {@code Instance} is created.<br>
 * It holds the necessary data for systems to respond to instance creation.<br>
 * It implements the {@code IBaseEvent} interface.
 * @author malyelfik
 */
public class OnInstanceCreated implements IBaseEvent
{
	private final Instance _instance;
	private final Player _creator;
	
	/**
	 * This constructor initializes a new {@code OnInstanceCreated} event.<br>
	 * It stores the specific {@code Instance} that was created.<br>
	 * It also records the {@code Player} who performed the creation.
	 * @param instance The {@code Instance} object that was just initialized.
	 * @param creator The {@code Player} responsible for creating the instance.
	 */
	public OnInstanceCreated(Instance instance, Player creator)
	{
		_instance = instance;
		_creator = creator;
	}
	
	/**
	 * Retrieves the {@code Instance} associated with this event.<br>
	 * This method returns the world instance that was created.
	 * @return The {@code Instance} object.
	 */
	public Instance getInstanceWorld()
	{
		return _instance;
	}
	
	/**
	 * Retrieves the player who created this instance.<br>
	 * This method returns the {@code Player} object associated with the creation event.
	 * @return the {@code Player} who created the instance.
	 */
	public Player getCreator()
	{
		return _creator;
	}
	
	/**
	 * Retrieves the type of this event.<br>
	 * This method returns a constant value from {@code EventType}.
	 * @return The {@code EventType} associated with this instance.
	 */
	@Override
	public EventType getType()
	{
		return EventType.ON_INSTANCE_CREATED;
	}
}
