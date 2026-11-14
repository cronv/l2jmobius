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
package org.l2jmobius.gameserver.entity.zone.form;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.itemcontainer.Inventory;
import org.l2jmobius.gameserver.entity.zone.ZoneForm;
import org.l2jmobius.gameserver.geoengine.GeoEngine;

/**
 * Represents a primitive circular zone in the game world.<br>
 * This {@code ZoneCylinder} defines a cylindrical area for spatial logic.
 * @author durgus, Mobius
 */
public class ZoneCylinder extends ZoneForm
{
	private final int _x;
	private final int _y;
	private final int _z1;
	private final int _z2;
	private final int _rad;
	private final int _radS;
	private final Location _centerPoint;
	
	/**
	 * Creates a new {@code ZoneCylinder} instance.<br>
	 * This constructor defines a cylindrical zone based on its center and height.
	 * @param x The X coordinate of the cylinder center.
	 * @param y The Y coordinate of the cylinder center.
	 * @param z1 The minimum Z coordinate of the cylinder.
	 * @param z2 The maximum Z coordinate of the cylinder.
	 * @param rad The radius of the cylinder.
	 */
	public ZoneCylinder(int x, int y, int z1, int z2, int rad)
	{
		_x = x;
		_y = y;
		_z1 = z1;
		_z2 = z2;
		_rad = rad;
		_radS = rad * rad;
		_centerPoint = new Location(_x, _y, (_z1 + _z2) / 2);
	}
	
	/**
	 * Checks if a specific point is located within this zone.<br>
	 * It uses the {@code x}, {@code y}, and {@code z} coordinates to perform the check.
	 * @param x The x-coordinate of the point.
	 * @param y The y-coordinate of the point.
	 * @param z The z-coordinate of the point.
	 * @return {@code true} if the point is inside the zone, otherwise {@code false}.
	 */
	@Override
	public boolean isInsideZone(int x, int y, int z)
	{
		final double dx = _x - x;
		final double dy = _y - y;
		return (((dx * dx) + (dy * dy)) <= _radS) && (z >= _z1) && (z <= _z2);
	}
	
	/**
	 * Checks if this zone overlaps with a given rectangle.<br>
	 * It evaluates the position of the center point and all four corners.<br>
	 * It also checks for collisions along the sides of the rectangle.
	 * @param ax1 The minimum x-coordinate of the rectangle.
	 * @param ax2 The maximum x-coordinate of the rectangle.
	 * @param ay1 The minimum y-coordinate of the rectangle.
	 * @param ay2 The maximum y-coordinate of the rectangle.
	 * @return {@code true} if the zone and rectangle intersect, otherwise {@code false}.
	 */
	@Override
	public boolean intersectsRectangle(int ax1, int ax2, int ay1, int ay2)
	{
		// Circles point inside the rectangle?
		if ((_x > ax1) && (_x < ax2) && (_y > ay1) && (_y < ay2))
		{
			return true;
		}
		
		// Any point of the rectangle intersecting the Circle?
		final double dx1 = ax1 - _x;
		final double dy1 = ay1 - _y;
		final double dx1Sq = dx1 * dx1;
		final double dy1Sq = dy1 * dy1;
		if ((dx1Sq + dy1Sq) < _radS)
		{
			return true;
		}
		
		final double dy2 = ay2 - _y;
		final double dy2Sq = dy2 * dy2;
		if ((dx1Sq + dy2Sq) < _radS)
		{
			return true;
		}
		
		final double dx2 = ax2 - _x;
		final double dx2Sq = dx2 * dx2;
		if ((dx2Sq + dy1Sq) < _radS)
		{
			return true;
		}
		
		if ((dx2Sq + dy2Sq) < _radS)
		{
			return true;
		}
		
		// Collision on any side of the rectangle?
		if ((_x > ax1) && (_x < ax2))
		{
			if (Math.abs(_y - ay2) < _rad)
			{
				return true;
			}
			
			if (Math.abs(_y - ay1) < _rad)
			{
				return true;
			}
		}
		
		if ((_y > ay1) && (_y < ay2))
		{
			if (Math.abs(_x - ax2) < _rad)
			{
				return true;
			}
			
			if (Math.abs(_x - ax1) < _rad)
			{
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Calculates the distance from a point to the edge of this zone.<br>
	 * It uses the {@code x} and {@code y} coordinates to find the shortest path.<br>
	 * The result is the distance to the boundary, not the center.
	 * @param x The horizontal coordinate of the point.
	 * @param y The vertical coordinate of the point.
	 * @return The distance from the point to the zone as a {@code double}.
	 */
	@Override
	public double getDistanceToZone(int x, int y)
	{
		final double dx = _x - x;
		final double dy = _y - y;
		return Math.sqrt((dx * dx) + (dy * dy)) - _rad;
	}
	
	// getLowZ() / getHighZ() - These two functions were added to cope with the demand of the new fishing algorithms, wich are now able to correctly place the hook in the water, thanks to getHighZ(). getLowZ() was added, considering potential future modifications.
	/**
	 * Retrieves the lowest vertical coordinate of the zone.<br>
	 * This value corresponds to the {@code _z1} field.
	 * @return The minimum {@code int} height of the cylinder.
	 */
	@Override
	public int getLowZ()
	{
		return _z1;
	}
	
	/**
	 * Returns the maximum height of the cylinder.<br>
	 * This value corresponds to the {@code _z2} coordinate.
	 * @return The highest {@code int} Z-coordinate of the zone.
	 */
	@Override
	public int getHighZ()
	{
		return _z2;
	}
	
	/**
	 * This method draws a visual representation of the zone.<br>
	 * It drops {@code Inventory.ADENA_ID} items to mark the boundary.<br>
	 * The visualization occurs at the specific height provided.
	 * @param z The vertical coordinate where the zone should be visualized.
	 */
	@Override
	public void visualizeZone(int z)
	{
		final int count = (int) ((2 * Math.PI * _rad) / STEP);
		final double angle = (2 * Math.PI) / count;
		for (int i = 0; i < count; i++)
		{
			dropDebugItem(Inventory.ADENA_ID, 1, _x + (int) (Math.cos(angle * i) * _rad), _y + (int) (Math.sin(angle * i) * _rad), z);
		}
	}
	
	/**
	 * Generates a random coordinate within the boundaries of this zone.<br>
	 * It calculates a point that satisfies the circular radius requirements.<br>
	 * The height is determined by the average of the zone's vertical limits.
	 * @return A new {@code Location} object representing the random point.
	 */
	@Override
	public Location getRandomPoint()
	{
		int x = 0;
		int y = 0;
		final int x2 = _x - _rad;
		final int y2 = _y - _rad;
		final int x3 = _x + _rad;
		final int y3 = _y + _rad;
		double dx = _x - x;
		double dy = _y - y;
		while (((dx * dx) + (dy * dy)) > _radS)
		{
			x = Rnd.get(x2, x3);
			y = Rnd.get(y2, y3);
			dx = _x - x;
			dy = _y - y;
		}
		
		return new Location(x, y, GeoEngine.getInstance().getHeight(x, y, (_z1 + _z2) / 2));
	}
	
	/**
	 * Retrieves the center point of this zone.<br>
	 * This method returns the {@code Location} object stored in the internal field.
	 * @return The {@code Location} representing the center of the cylinder.
	 */
	@Override
	public Location getCenterPoint()
	{
		return _centerPoint;
	}
}
