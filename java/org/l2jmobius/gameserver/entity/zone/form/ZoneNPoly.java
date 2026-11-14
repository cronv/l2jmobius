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

import java.awt.Polygon;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.itemcontainer.Inventory;
import org.l2jmobius.gameserver.entity.zone.ZoneForm;
import org.l2jmobius.gameserver.geoengine.GeoEngine;

/**
 * Represents a non-primitive polygon zone for defining complex areas.<br>
 * This class extends {@code ZoneForm} to provide advanced spatial boundaries.
 * @author durgus, Mobius
 */
public class ZoneNPoly extends ZoneForm
{
	private final Polygon _p;
	private final int _z1;
	private final int _z2;
	private final Location _centerPoint;
	
	/**
	 * Creates a new {@code ZoneNPoly} instance using an array of coordinates.<br>
	 * This constructor calculates the area and center point of the polygon.<br>
	 * It also determines the minimum and maximum height values.
	 * @param x An array of x-coordinates for the polygon vertices.
	 * @param y An array of y-coordinates for the polygon vertices.
	 * @param z1 The first height value.
	 * @param z2 The second height value.
	 */
	public ZoneNPoly(int[] x, int[] y, int z1, int z2)
	{
		_p = new Polygon(x, y, x.length);
		
		_z1 = Math.min(z1, z2);
		_z2 = Math.max(z1, z2);
		
		double area = 0;
		double cx = 0;
		double cy = 0;
		for (int i = 0; i < _p.npoints; i++)
		{
			final int nextIndex = (i + 1) % _p.npoints;
			final double crossProduct = (_p.xpoints[i] * _p.ypoints[nextIndex]) - (_p.xpoints[nextIndex] * _p.ypoints[i]);
			area += crossProduct;
			cx += (_p.xpoints[i] + _p.xpoints[nextIndex]) * crossProduct;
			cy += (_p.ypoints[i] + _p.ypoints[nextIndex]) * crossProduct;
		}
		
		area /= 2.0;
		if (area == 0)
		{
			cx = 0;
			cy = 0;
			for (int i = 0; i < _p.npoints; i++)
			{
				cx += _p.xpoints[i];
				cy += _p.ypoints[i];
			}
			
			cx /= _p.npoints;
			cy /= _p.npoints;
		}
		else
		{
			cx /= (6.0 * area);
			cy /= (6.0 * area);
		}
		
		_centerPoint = new Location((int) cx, (int) cy, (_z1 + _z2) / 2);
	}
	
	/**
	 * Checks if a specific coordinate is within this zone.<br>
	 * It verifies the {@code x} and {@code y} values against the polygon shape.<br>
	 * It also checks if the {@code z} value is between the allowed height limits.
	 * @param x The horizontal coordinate to check.
	 * @param y The vertical coordinate to check.
	 * @param z The height coordinate to check.
	 * @return {@code true} if the point is inside the zone, {@code false} otherwise.
	 */
	@Override
	public boolean isInsideZone(int x, int y, int z)
	{
		return _p.contains(x, y) && (z >= _z1) && (z <= _z2);
	}
	
	/**
	 * Checks if this zone overlaps with a given rectangle.<br>
	 * The method compares the polygon shape against the provided coordinates.
	 * @param ax1 The first x-coordinate of the rectangle.
	 * @param ax2 The second x-coordinate of the rectangle.
	 * @param ay1 The first y-coordinate of the rectangle.
	 * @param ay2 The second y-coordinate of the rectangle.
	 * @return {@code true} if the zone and rectangle intersect, {@code false} otherwise.
	 */
	@Override
	public boolean intersectsRectangle(int ax1, int ax2, int ay1, int ay2)
	{
		return _p.intersects(Math.min(ax1, ax2), Math.min(ay1, ay2), Math.abs(ax2 - ax1), Math.abs(ay2 - ay1));
	}
	
	/**
	 * Calculates the distance from a specific point to this zone.<br>
	 * It finds the shortest distance based on the polygon vertices.
	 * @param x The x-coordinate of the point.
	 * @param y The y-coordinate of the point.
	 * @return The distance as a {@code double}.
	 */
	@Override
	public double getDistanceToZone(int x, int y)
	{
		final int[] xPoints = _p.xpoints;
		final int[] yPoints = _p.ypoints;
		double test;
		double dx = xPoints[0] - x;
		double dy = yPoints[0] - y;
		double shortestDist = (dx * dx) + (dy * dy);
		
		for (int i = 1; i < _p.npoints; i++)
		{
			dx = xPoints[i] - x;
			dy = yPoints[i] - y;
			test = (dx * dx) + (dy * dy);
			if (test < shortestDist)
			{
				shortestDist = test;
			}
		}
		
		return Math.sqrt(shortestDist);
	}
	
	// getLowZ() / getHighZ() - These two functions were added to cope with the demand of the new fishing algorithms, wich are now able to correctly place the hook in the water, thanks to getHighZ(). getLowZ() was added, considering potential future modifications.
	/**
	 * Retrieves the minimum height value of the zone.<br>
	 * This corresponds to the {@code _z1} variable.
	 * @return The lowest {@code int} height for this zone.
	 */
	@Override
	public int getLowZ()
	{
		return _z1;
	}
	
	/**
	 * Retrieves the maximum height of the zone.<br>
	 * This value corresponds to the {@code _z2} variable.
	 * @return The highest {@code int} coordinate for this zone.
	 */
	@Override
	public int getHighZ()
	{
		return _z2;
	}
	
	/**
	 * This method draws the boundaries of the zone for debugging.<br>
	 * It drops {@code Inventory.ADENA_ID} items along the edges.<br>
	 * The visualization occurs at a specific height level.
	 * @param z The vertical coordinate to render the zone at.
	 */
	@Override
	public void visualizeZone(int z)
	{
		for (int i = 0; i < _p.npoints; i++)
		{
			final int nextIndex = (i + 1) == _p.xpoints.length ? 0 : i + 1;
			final int vx = _p.xpoints[nextIndex] - _p.xpoints[i];
			final int vy = _p.ypoints[nextIndex] - _p.ypoints[i];
			final float length = (float) Math.sqrt((vx * vx) + (vy * vy)) / STEP;
			for (int o = 1; o <= length; o++)
			{
				dropDebugItem(Inventory.ADENA_ID, 1, (int) (_p.xpoints[i] + ((o / length) * vx)), (int) (_p.ypoints[i] + ((o / length) * vy)), z);
			}
		}
	}
	
	/**
	 * Generates a random coordinate within the boundaries of this zone.<br>
	 * It ensures the point is inside the {@code Polygon} shape.<br>
	 * The height is calculated based on the average of the zone's vertical limits.
	 * @return A new {@code Location} object at the random position.
	 */
	@Override
	public Location getRandomPoint()
	{
		final int minX = _p.getBounds().x;
		final int maxX = _p.getBounds().x + _p.getBounds().width;
		final int minY = _p.getBounds().y;
		final int maxY = _p.getBounds().y + _p.getBounds().height;
		
		int x = Rnd.get(minX, maxX);
		int y = Rnd.get(minY, maxY);
		
		int antiBlocker = 0;
		while (!_p.contains(x, y) && (antiBlocker++ < 1000))
		{
			x = Rnd.get(minX, maxX);
			y = Rnd.get(minY, maxY);
		}
		
		return new Location(x, y, GeoEngine.getInstance().getHeight(x, y, (_z1 + _z2) / 2));
	}
	
	/**
	 * Retrieves the x-coordinates of the zone polygon.<br>
	 * These values correspond to the vertices of the {@code ZoneNPoly}.
	 * @return an {@code int[]} array containing the x-coordinates.
	 */
	public int[] getX()
	{
		return _p.xpoints;
	}
	
	/**
	 * Retrieves the Y coordinates of the zone polygon.<br>
	 * These values correspond to the points defined in {@code Polygon}.
	 * @return an {@code int[]} containing the Y coordinates.
	 */
	public int[] getY()
	{
		return _p.ypoints;
	}
	
	/**
	 * Retrieves the center point of this zone.<br>
	 * This method returns a {@code Location} object representing the middle coordinates.
	 * @return The center {@code Location} of the zone.
	 */
	@Override
	public Location getCenterPoint()
	{
		return _centerPoint;
	}
}
