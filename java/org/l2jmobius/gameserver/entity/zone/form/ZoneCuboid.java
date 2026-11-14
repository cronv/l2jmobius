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

import java.awt.Rectangle;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.itemcontainer.Inventory;
import org.l2jmobius.gameserver.entity.zone.ZoneForm;
import org.l2jmobius.gameserver.geoengine.GeoEngine;

/**
 * Represents a basic rectangular area defined as a cuboid.<br>
 * This class is used to define spatial boundaries for zones in the game world.
 * @author durgus, Mobius
 */
public class ZoneCuboid extends ZoneForm
{
	private final int _z1;
	private final int _z2;
	private final Rectangle _r;
	private final Location _centerPoint;
	
	/**
	 * Creates a new {@code ZoneCuboid} using the specified coordinates.<br>
	 * This constructor calculates the boundaries and the center point of the zone.
	 * @param x1 The first X coordinate.
	 * @param x2 The second X coordinate.
	 * @param y1 The first Y coordinate.
	 * @param y2 The second Y coordinate.
	 * @param z1 The first Z coordinate.
	 * @param z2 The second Z coordinate.
	 */
	public ZoneCuboid(int x1, int x2, int y1, int y2, int z1, int z2)
	{
		final int _x1 = Math.min(x1, x2);
		final int _x2 = Math.max(x1, x2);
		final int _y1 = Math.min(y1, y2);
		final int _y2 = Math.max(y1, y2);
		
		_r = new Rectangle(_x1, _y1, _x2 - _x1, _y2 - _y1);
		
		_z1 = Math.min(z1, z2);
		_z2 = Math.max(z1, z2);
		
		_centerPoint = new Location((_x1 + _x2) / 2, (_y1 + _y2) / 2, (_z1 + _z2) / 2);
	}
	
	/**
	 * Checks if a specific point is located within this zone.<br>
	 * It verifies both the 2D coordinates and the height range.
	 * @param x The x coordinate of the point.
	 * @param y The y coordinate of the point.
	 * @param z The z coordinate of the point.
	 * @return {@code true} if the point is inside the zone, otherwise {@code false}.
	 */
	@Override
	public boolean isInsideZone(int x, int y, int z)
	{
		return _r.contains(x, y) && (z >= _z1) && (z <= _z2);
	}
	
	/**
	 * Checks if this zone overlaps with a given rectangle.<br>
	 * It compares the boundaries of the two shapes.
	 * @param ax1 The first x-coordinate of the target rectangle.
	 * @param ax2 The second x-coordinate of the target rectangle.
	 * @param ay1 The first y-coordinate of the target rectangle.
	 * @param ay2 The second y-coordinate of the target rectangle.
	 * @return {@code true} if the shapes intersect, otherwise {@code false}.
	 */
	@Override
	public boolean intersectsRectangle(int ax1, int ax2, int ay1, int ay2)
	{
		return _r.intersects(Math.min(ax1, ax2), Math.min(ay1, ay2), Math.abs(ax2 - ax1), Math.abs(ay2 - ay1));
	}
	
	/**
	 * Calculates the shortest distance from a 2D point to this zone.<br>
	 * This method ignores the {@code z} coordinate.
	 * @param x The x-coordinate of the point.
	 * @param y The y-coordinate of the point.
	 * @return The distance as a {@code double}.
	 */
	@Override
	public double getDistanceToZone(int x, int y)
	{
		final int _x1 = _r.x;
		final int _x2 = _r.x + _r.width;
		final int _y1 = _r.y;
		final int _y2 = _r.y + _r.height;
		final double dx1 = _x1 - x;
		final double dx2 = _x2 - x;
		final double dy1 = _y1 - y;
		final double dy2 = _y2 - y;
		final double dx1Sq = dx1 * dx1;
		final double dx2Sq = dx2 * dx2;
		final double dy1Sq = dy1 * dy1;
		final double dy2Sq = dy2 * dy2;
		double test = dx1Sq + dy2Sq;
		double shortestDist = dx1Sq + dy1Sq;
		
		if (test < shortestDist)
		{
			shortestDist = test;
		}
		
		test = dx2Sq + dy1Sq;
		if (test < shortestDist)
		{
			shortestDist = test;
		}
		
		test = dx2Sq + dy2Sq;
		if (test < shortestDist)
		{
			shortestDist = test;
		}
		
		return Math.sqrt(shortestDist);
	}
	
	/*
	 * getLowZ() / getHighZ() - These two functions were added to cope with the demand of the new fishing algorithms, which are now able to correctly place the hook in the water, thanks to getHighZ(). getLowZ() was added, considering potential future modifications.
	 */
	/**
	 * Retrieves the minimum height of the zone.<br>
	 * This value corresponds to the {@code _z1} coordinate.
	 * @return The lowest {@code int} Z-coordinate of this zone.
	 */
	@Override
	public int getLowZ()
	{
		return _z1;
	}
	
	/**
	 * Returns the maximum height of this zone.<br>
	 * This value corresponds to the {@code _z2} coordinate.
	 * @return The highest Z coordinate of the zone.
	 */
	@Override
	public int getHighZ()
	{
		return _z2;
	}
	
	/**
	 * This method draws the boundaries of the zone for debugging.<br>
	 * It places debug items at the edges of the rectangle.<br>
	 * The visualization occurs at a specific height.
	 * @param z The vertical coordinate where the zone will be visualized.
	 */
	@Override
	public void visualizeZone(int z)
	{
		final int _x1 = _r.x;
		final int _x2 = _r.x + _r.width;
		final int _y1 = _r.y;
		final int _y2 = _r.y + _r.height;
		
		// x1->x2
		for (int x = _x1; x < _x2; x += STEP)
		{
			dropDebugItem(Inventory.ADENA_ID, 1, x, _y1, z);
			dropDebugItem(Inventory.ADENA_ID, 1, x, _y2, z);
		}
		
		// y1->y2
		for (int y = _y1; y < _y2; y += STEP)
		{
			dropDebugItem(Inventory.ADENA_ID, 1, _x1, y, z);
			dropDebugItem(Inventory.ADENA_ID, 1, _x2, y, z);
		}
	}
	
	/**
	 * Generates a random coordinate within the boundaries of this zone.<br>
	 * It calculates a random x and y position based on the rectangle dimensions.<br>
	 * The height is determined by the average z-level of the zone.
	 * @return A new {@code Location} object at the random point.
	 */
	@Override
	public Location getRandomPoint()
	{
		final int x = Rnd.get(_r.x, _r.x + _r.width);
		final int y = Rnd.get(_r.y, _r.y + _r.height);
		
		return new Location(x, y, GeoEngine.getInstance().getHeight(x, y, (_z1 + _z2) / 2));
	}
	
	/**
	 * Retrieves the center point of this zone.<br>
	 * This method returns a {@code Location} object representing the middle coordinates.
	 * @return The {@code Location} of the center point.
	 */
	@Override
	public Location getCenterPoint()
	{
		return _centerPoint;
	}
}
