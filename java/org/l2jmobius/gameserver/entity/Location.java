/*
 * Copyright (c) 2013 L2jMobius
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.l2jmobius.gameserver.entity;

import java.util.Objects;

import org.l2jmobius.gameserver.interfaces.ILocational;
import org.l2jmobius.gameserver.interfaces.IPositionable;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Represents a 3D coordinate in the game world.<br>
 * It stores {@code x}, {@code y}, and {@code z} values along with an optional heading.
 */
public class Location implements IPositionable
{
	private static final int MAX_HEADING = 65535; // Max unsigned 16-bit value.
	
	protected volatile int _x;
	protected volatile int _y;
	protected volatile int _z;
	protected volatile int _heading;
	
	/**
	 * Creates a new {@code Location} with the given coordinates.<br>
	 * The heading is initialized to {@code 0}.
	 * @param x The x coordinate.
	 * @param y The y coordinate.
	 * @param z The z coordinate.
	 */
	public Location(int x, int y, int z)
	{
		_x = x;
		_y = y;
		_z = z;
		_heading = 0;
	}
	
	/**
	 * Creates a new {@code Location} with the given coordinates and heading.<br>
	 * This constructor sets all spatial properties of the location.
	 * @param x The x coordinate.
	 * @param y The y coordinate.
	 * @param z The z coordinate.
	 * @param heading The heading of the location.
	 */
	public Location(int x, int y, int z, int heading)
	{
		_x = x;
		_y = y;
		_z = z;
		_heading = heading;
	}
	
	/**
	 * Creates a new {@code Location} from the coordinates of a {@code WorldObject}.<br>
	 * This copies the {@code x}, {@code y}, {@code z}, and heading from the provided object.
	 * @param obj The {@code WorldObject} to extract location data from.
	 */
	public Location(WorldObject obj)
	{
		this(obj.getX(), obj.getY(), obj.getZ(), obj.getHeading());
	}
	
	/**
	 * Creates a new {@code Location} using values from a {@code StatSet}.<br>
	 * It extracts the {@code x}, {@code y}, {@code z}, and heading, using {@code 0} for any missing keys.
	 * @param set The {@code StatSet} containing the location data.
	 */
	public Location(StatSet set)
	{
		_x = set.getInt("x", 0);
		_y = set.getInt("y", 0);
		_z = set.getInt("z", 0);
		_heading = set.getInt("heading", 0);
	}
	
	/**
	 * Retrieves the x coordinate of this {@code Location}.<br>
	 * This value represents the horizontal position on the map.
	 * @return The x coordinate as an {@code int}.
	 */
	@Override
	public int getX()
	{
		return _x;
	}
	
	/**
	 * Retrieves the y coordinate of this {@code Location}.<br>
	 * This value represents the vertical position on the map.
	 * @return The y coordinate as an {@code int}.
	 */
	@Override
	public int getY()
	{
		return _y;
	}
	
	/**
	 * Retrieves the z coordinate of this {@code Location}.<br>
	 * This value represents the height on the map.
	 * @return The z coordinate as an {@code int}.
	 */
	@Override
	public int getZ()
	{
		return _z;
	}
	
	/**
	 * Updates the 3D coordinates of this {@code Location}.<br>
	 * This method sets the new {@code x}, {@code y}, and {@code z} values.
	 * @param x The new x coordinate.
	 * @param y The new y coordinate.
	 * @param z The new z coordinate.
	 */
	@Override
	public void setXYZ(int x, int y, int z)
	{
		_x = x;
		_y = y;
		_z = z;
	}
	
	/**
	 * Updates the 3D coordinates of this {@code Location} using another location.<br>
	 * This method copies the {@code x}, {@code y}, and {@code z} values from the provided object.
	 * @param loc The {@code ILocational} object containing the new coordinates.
	 */
	@Override
	public void setXYZ(ILocational loc)
	{
		setXYZ(loc.getX(), loc.getY(), loc.getZ());
	}
	
	/**
	 * Retrieves the heading of this {@code Location}.<br>
	 * This value represents the orientation in 3D space.
	 * @return The heading as an {@code int}.
	 */
	@Override
	public int getHeading()
	{
		return _heading;
	}
	
	/**
	 * Sets the heading of this {@code Location}.<br>
	 * The value is clamped between {@code 0} and {@code 65535}.
	 * @param heading The new heading value to set.
	 */
	@Override
	public void setHeading(int heading)
	{
		_heading = Math.clamp(heading, 0, MAX_HEADING);
	}
	
	/**
	 * Retrieves the current location of this object.<br>
	 * This method returns the {@code Location} instance itself.
	 * @return The current {@code IPositionable} location.
	 */
	@Override
	public IPositionable getLocation()
	{
		return this;
	}
	
	/**
	 * Updates this {@code Location} using another location.<br>
	 * This method copies the {@code x}, {@code y}, {@code z}, and heading from the provided object.
	 * @param loc The {@code Location} to copy from.
	 */
	@Override
	public void setLocation(Location loc)
	{
		_x = loc.getX();
		_y = loc.getY();
		_z = loc.getZ();
		_heading = loc.getHeading();
	}
	
	/**
	 * Creates a new copy of this {@code Location}.<br>
	 * The new object has the same coordinates and heading as the original.<br>
	 * This is useful for creating a separate instance that does not affect the current one.
	 * @return A new {@code Location} instance with identical values.
	 */
	@Override
	public Location clone()
	{
		return new Location(_x, _y, _z, _heading);
	}
	
	/**
	 * Returns a hash code value for this {@code Location}.<br>
	 * This value is used to compare objects in collections.<br>
	 * It is calculated based on the {@code x}, {@code y}, and {@code z} coordinates.
	 * @return The hash code of this object.
	 */
	@Override
	public int hashCode()
	{
		return (31 * Objects.hash(_x, _y)) + Objects.hash(_z);
	}
	
	/**
	 * Compares this {@code Location} to the specified object.<br>
	 * It checks whether both objects have the same {@code x}, {@code y}, {@code z}, and heading.
	 * @param obj The object to compare with this instance.
	 * @return {@code true} if the objects are equal, {@code false} otherwise.
	 */
	@Override
	public boolean equals(Object obj)
	{
		if (obj instanceof Location)
		{
			final Location loc = (Location) obj;
			return (getX() == loc.getX()) && (getY() == loc.getY()) && (getZ() == loc.getZ()) && (getHeading() == loc.getHeading());
		}
		
		return false;
	}
	
	/**
	 * Returns a string representation of this {@code Location}.<br>
	 * This includes the class name, coordinates, and heading.
	 * @return A formatted string containing the x, y, z, and heading values.
	 */
	@Override
	public String toString()
	{
		return "[" + getClass().getSimpleName() + "] X: " + _x + " Y: " + _y + " Z: " + _z + " Heading: " + _heading;
	}
}
