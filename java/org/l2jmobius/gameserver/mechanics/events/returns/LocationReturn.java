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
package org.l2jmobius.gameserver.mechanics.events.returns;

import org.l2jmobius.gameserver.entity.instancezone.Instance;
import org.l2jmobius.gameserver.interfaces.ILocational;

/**
 * Handles the logic for returning an entity to a specific location.<br>
 * This class is used when a character or object needs to be moved back to a predefined coordinate.
 * @author Nik
 */
public class LocationReturn extends TerminateReturn
{
	private final boolean _overrideLocation;
	private int _x;
	private int _y;
	private int _z;
	private int _heading;
	private Instance _instance;
	
	/**
	 * Creates a new {@code LocationReturn} object.<br>
	 * This constructor initializes the return behavior and location settings.
	 * @param terminate Sets whether to terminate the process when returning.
	 * @param overrideLocation Sets whether to ignore the default location and use a custom one.
	 */
	public LocationReturn(boolean terminate, boolean overrideLocation)
	{
		super(terminate, false, false);
		_overrideLocation = overrideLocation;
	}
	
	/**
	 * Creates a new {@code LocationReturn} with specific coordinates.<br>
	 * This constructor sets the location data from an {@code ILocational} object.<br>
	 * It also assigns the target instance to this return event.
	 * @param terminate Determines if the action should end upon completion.
	 * @param overrideLocation Sets whether to ignore the default location logic.
	 * @param targetLocation The source for the new coordinates and heading.
	 * @param instance The {@code Instance} where this return occurs.
	 */
	public LocationReturn(boolean terminate, boolean overrideLocation, ILocational targetLocation, Instance instance)
	{
		super(terminate, false, false);
		_overrideLocation = overrideLocation;
		if (targetLocation != null)
		{
			setX(targetLocation.getX());
			setY(targetLocation.getY());
			setZ(targetLocation.getZ());
			setHeading(targetLocation.getHeading());
			setInstance(instance);
		}
	}
	
	/**
	 * Sets the X coordinate for this location.<br>
	 * This updates the internal {@code _x} field.
	 * @param x The new X coordinate value.
	 */
	public void setX(int x)
	{
		_x = x;
	}
	
	/**
	 * Sets the vertical coordinate of the location.<br>
	 * This updates the {@code _y} field with the provided value.
	 * @param y The new vertical coordinate to set.
	 */
	public void setY(int y)
	{
		_y = y;
	}
	
	/**
	 * Sets the vertical coordinate of the location.<br>
	 * This updates the {@code _z} field.
	 * @param z The new vertical coordinate value.
	 */
	public void setZ(int z)
	{
		_z = z;
	}
	
	/**
	 * Sets the rotation direction for the return location.<br>
	 * This updates the {@code _heading} field of the {@code LocationReturn} object.
	 * @param heading The new heading value to set.
	 */
	public void setHeading(int heading)
	{
		_heading = heading;
	}
	
	/**
	 * Sets the {@code Instance} associated with this return.<br>
	 * This method updates the internal {@code _instance} field.
	 * @param instance The {@code Instance} to set.
	 */
	public void setInstance(Instance instance)
	{
		_instance = instance;
	}
	
	/**
	 * Checks if the location should be overridden.<br>
	 * This method returns the value of the {@code _overrideLocation} flag.
	 * @return {@code true} if the location is overridden, {@code false} otherwise.
	 */
	public boolean overrideLocation()
	{
		return _overrideLocation;
	}
	
	/**
	 * Retrieves the X coordinate of the location.<br>
	 * This value represents the horizontal position on the map.
	 * @return The current {@code int} value of the X coordinate.
	 */
	public int getX()
	{
		return _x;
	}
	
	/**
	 * Retrieves the vertical coordinate of the location.<br>
	 * This value represents the {@code y} axis position.
	 * @return The current {@code y} coordinate as an {@code int}.
	 */
	public int getY()
	{
		return _y;
	}
	
	/**
	 * Retrieves the Z coordinate of the location.<br>
	 * This value represents the vertical position.
	 * @return The current {@code int} value for the Z coordinate.
	 */
	public int getZ()
	{
		return _z;
	}
	
	/**
	 * Retrieves the current heading value.<br>
	 * This value represents the orientation of the return point.
	 * @return The integer value of the {@code _heading}.
	 */
	public int getHeading()
	{
		return _heading;
	}
	
	/**
	 * Retrieves the {@code Instance} associated with this return.
	 * @return The {@code Instance} object or {@code null} if no instance is set.
	 */
	public Instance getInstance()
	{
		return _instance;
	}
}
