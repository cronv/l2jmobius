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
package org.l2jmobius.gameserver.entity.shuttle;

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.gameserver.entity.Location;

/**
 * Represents a specific destination point for the shuttle system.<br>
 * It stores the {@code Location} where the shuttle will stop to pick up or drop off players.
 * @author UnAfraid
 */
public class ShuttleStop
{
	private final int _id;
	private boolean _isOpen = true;
	private final List<Location> _dimensions = new ArrayList<>(3);
	private long _lastDoorStatusChanges = System.currentTimeMillis();
	
	/**
	 * Creates a new instance of {@code ShuttleStop}.<br>
	 * This constructor initializes the stop with a unique identifier.
	 * @param id The unique identification number for this shuttle stop.
	 */
	public ShuttleStop(int id)
	{
		_id = id;
	}
	
	/**
	 * Returns the unique identifier for this {@code ShuttleStop}.<br>
	 * This value is assigned when the object is created.
	 * @return The integer ID of the shuttle stop.
	 */
	public int getId()
	{
		return _id;
	}
	
	/**
	 * Checks if the shuttle door is currently open.<br>
	 * Returns {@code true} if the door is open.<br>
	 * Returns {@code false} if the door is closed.
	 * @return The current state of the door.
	 */
	public boolean isDoorOpen()
	{
		return _isOpen;
	}
	
	/**
	 * Adds a new coordinate to the shuttle's dimensions.<br>
	 * This updates the list of locations associated with this {@code ShuttleStop}.
	 * @param loc The {@code Location} to be added.
	 */
	public void addDimension(Location loc)
	{
		_dimensions.add(loc);
	}
	
	/**
	 * Retrieves the list of locations associated with this shuttle stop.<br>
	 * This method returns all dimensions currently stored in the {@code _dimensions} list.
	 * @return a {@code List} of {@code Location} objects.
	 */
	public List<Location> getDimensions()
	{
		return _dimensions;
	}
	
	/**
	 * Opens the door of the {@code ShuttleStop}.<br>
	 * This method sets the internal state to {@code true}.<br>
	 * It updates the timestamp for the last status change.
	 */
	public void openDoor()
	{
		if (_isOpen)
		{
			return;
		}
		
		_isOpen = true;
		_lastDoorStatusChanges = System.currentTimeMillis();
	}
	
	/**
	 * Closes the door of the {@code ShuttleStop}.<br>
	 * This method updates the {@code _isOpen} status to {@code false}.<br>
	 * It also updates the timestamp for the last status change.
	 */
	public void closeDoor()
	{
		if (!_isOpen)
		{
			return;
		}
		
		_isOpen = false;
		_lastDoorStatusChanges = System.currentTimeMillis();
	}
	
	/**
	 * Checks if the door status was updated recently.<br>
	 * It returns {@code true} if the change happened within the last 1000 milliseconds.<br>
	 * Otherwise, it returns {@code false}.
	 * @return {@code true} if the door changed recently, {@code false} otherwise.
	 */
	public boolean hasDoorChanged()
	{
		return (System.currentTimeMillis() - _lastDoorStatusChanges) <= 1000;
	}
}
