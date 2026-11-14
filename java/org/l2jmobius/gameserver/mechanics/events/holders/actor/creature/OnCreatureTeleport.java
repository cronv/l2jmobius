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
package org.l2jmobius.gameserver.mechanics.events.holders.actor.creature;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.instancezone.Instance;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;

/**
 * This class represents an event triggered when a {@code Creature} is teleported.<br>
 * It holds the necessary data for handling teleportation logic within the game mechanics.
 * @author Nik
 */
public class OnCreatureTeleport implements IBaseEvent
{
	private final Creature _creature;
	private final int _destX;
	private final int _destY;
	private final int _destZ;
	private final int _destHeading;
	private final Instance _destInstance;
	
	/**
	 * Creates a new teleport event for a specific creature.<br>
	 * This method initializes the destination coordinates and instance.
	 * @param creature The {@code Creature} that will be moved.
	 * @param destX The target X coordinate.
	 * @param destY The target Y coordinate.
	 * @param destZ The target Z coordinate.
	 * @param destHeading The heading at the destination.
	 * @param destInstance The {@code Instance} where the creature will land.
	 */
	public OnCreatureTeleport(Creature creature, int destX, int destY, int destZ, int destHeading, Instance destInstance)
	{
		_creature = creature;
		_destX = destX;
		_destY = destY;
		_destZ = destZ;
		_destHeading = destHeading;
		_destInstance = destInstance;
	}
	
	/**
	 * Retrieves the {@code Creature} associated with this teleport event.
	 * @return the {@code Creature} object.
	 */
	public Creature getCreature()
	{
		return _creature;
	}
	
	/**
	 * Retrieves the X coordinate of the destination.<br>
	 * This value is used when a {@code Creature} teleports.
	 * @return The destination X coordinate as an {@code int}.
	 */
	public int getDestX()
	{
		return _destX;
	}
	
	/**
	 * Retrieves the Y coordinate of the destination.<br>
	 * This value is used to determine where a {@code Creature} will teleport.
	 * @return The destination Y coordinate as an {@code int}.
	 */
	public int getDestY()
	{
		return _destY;
	}
	
	/**
	 * Retrieves the destination Z coordinate.<br>
	 * This value represents the vertical position of the teleport target.
	 * @return The integer value of the destination Z coordinate.
	 */
	public int getDestZ()
	{
		return _destZ;
	}
	
	/**
	 * Retrieves the heading of the destination.<br>
	 * This value is used when a {@code Creature} teleports.
	 * @return The destination heading as an {@code int}.
	 */
	public int getDestHeading()
	{
		return _destHeading;
	}
	
	/**
	 * Retrieves the destination instance for the teleport event.<br>
	 * This method returns the {@code Instance} where the creature is moving to.
	 * @return the destination {@code Instance}
	 */
	public Instance getDestInstance()
	{
		return _destInstance;
	}
	
	/**
	 * Retrieves the type of this event.<br>
	 * This method returns a constant value from {@code EventType}.
	 * @return the {@code EventType} associated with this teleport event.
	 */
	@Override
	public EventType getType()
	{
		return EventType.ON_CREATURE_TELEPORT;
	}
}
