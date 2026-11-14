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
package org.l2jmobius.gameserver.geoengine.pathfinding;

import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.geoengine.regions.Region;

/**
 * A pathfinding node holding a geo coordinate and its NSWE movement flags.
 * @author Mobius
 */
public class GeoLocation
{
	private int _x;
	private int _y;
	private int _nswe;
	private int _geoHeight;
	
	/**
	 * Creates a {@code GeoLocation} at the given world coordinate.
	 * @param x The world X coordinate.
	 * @param y The world Y coordinate.
	 * @param z The world Z coordinate.
	 */
	public GeoLocation(int x, int y, int z)
	{
		set(x, y, z);
	}
	
	/**
	 * Moves this location to the given coordinate, recomputing its NSWE flags and geo height from the {@code GeoEngine}.
	 * @param x The world X coordinate.
	 * @param y The world Y coordinate.
	 * @param z The world Z coordinate.
	 */
	public void set(int x, int y, int z)
	{
		_x = x;
		_y = y;
		_nswe = 0; // Reset the bitmask.
		
		// Set the NSWE bitmask based on movement possibilities.
		final GeoEngine geoEngine = GeoEngine.getInstance();
		if (geoEngine.checkNearestNswe(x, y, z, Region.NSWE_NORTH))
		{
			_nswe |= Region.NSWE_NORTH;
		}
		
		if (geoEngine.checkNearestNswe(x, y, z, Region.NSWE_EAST))
		{
			_nswe |= Region.NSWE_EAST;
		}
		
		if (geoEngine.checkNearestNswe(x, y, z, Region.NSWE_SOUTH))
		{
			_nswe |= Region.NSWE_SOUTH;
		}
		
		if (geoEngine.checkNearestNswe(x, y, z, Region.NSWE_WEST))
		{
			_nswe |= Region.NSWE_WEST;
		}
		
		_geoHeight = geoEngine.getNearestZ(x, y, z);
	}
	
	/**
	 * Checks whether movement to the north is allowed from this location.
	 * @return {@code true} if movement north is allowed, {@code false} otherwise.
	 */
	public boolean canGoNorth()
	{
		return (_nswe & Region.NSWE_NORTH) != 0;
	}
	
	/**
	 * Checks whether movement to the east is allowed from this location.
	 * @return {@code true} if movement east is allowed, {@code false} otherwise.
	 */
	public boolean canGoEast()
	{
		return (_nswe & Region.NSWE_EAST) != 0;
	}
	
	/**
	 * Checks whether movement to the south is allowed from this location.
	 * @return {@code true} if movement south is allowed, {@code false} otherwise.
	 */
	public boolean canGoSouth()
	{
		return (_nswe & Region.NSWE_SOUTH) != 0;
	}
	
	/**
	 * Checks whether movement to the west is allowed from this location.
	 * @return {@code true} if movement west is allowed, {@code false} otherwise.
	 */
	public boolean canGoWest()
	{
		return (_nswe & Region.NSWE_WEST) != 0;
	}
	
	/**
	 * Checks whether movement is blocked in every direction.
	 * @return {@code true} if no direction is allowed, {@code false} otherwise.
	 */
	public boolean canGoNone()
	{
		return _nswe == 0;
	}
	
	/**
	 * Checks whether movement is allowed in every direction.
	 * @return {@code true} if all directions are allowed, {@code false} otherwise.
	 */
	public boolean canGoAll()
	{
		return _nswe == Region.NSWE_ALL;
	}
	
	/**
	 * Retrieves the world X coordinate of this location.
	 * @return The world X coordinate as an {@code int}.
	 */
	public int getX()
	{
		return GeoEngine.getWorldX(_x);
	}
	
	/**
	 * Retrieves the world Y coordinate of this location.
	 * @return The world Y coordinate as an {@code int}.
	 */
	public int getY()
	{
		return GeoEngine.getWorldY(_y);
	}
	
	/**
	 * Retrieves the geo height of this location.
	 * @return The geo height as an {@code int}.
	 */
	public int getZ()
	{
		return _geoHeight;
	}
	
	/**
	 * No-op; the height is derived from geodata when the location is set.
	 * @param z The height value (ignored).
	 */
	public void setZ(short z)
	{
	}
	
	/**
	 * Retrieves the geo X coordinate of this node.
	 * @return The geo X coordinate as an {@code int}.
	 */
	public int getNodeX()
	{
		return _x;
	}
	
	/**
	 * Retrieves the geo Y coordinate of this node.
	 * @return The geo Y coordinate as an {@code int}.
	 */
	public int getNodeY()
	{
		return _y;
	}
	
	/**
	 * Returns a hash code derived from the coordinate, geo height and NSWE flags.
	 * @return The hash code as an {@code int}.
	 */
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = (prime * result) + _x;
		result = (prime * result) + _y;
		
		// Combine the geo height and the NSWE bitmask into the hash.
		result = (prime * result) + (((_geoHeight & 0xFFFF) << 1) | _nswe);
		
		return result;
	}
	
	/**
	 * Compares this location with another for equal coordinate, geo height and NSWE flags.
	 * @param obj The object to compare against.
	 * @return {@code true} if the objects are equal, {@code false} otherwise.
	 */
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
		{
			return true;
		}
		
		if ((obj == null) || (getClass() != obj.getClass()))
		{
			return false;
		}
		
		// Compare _x, _y, _geoHeight, and the bitmask _nswe directly.
		final GeoLocation other = (GeoLocation) obj;
		return (_x == other._x) && (_y == other._y) && (_geoHeight == other._geoHeight) && (_nswe == other._nswe);
	}
}
