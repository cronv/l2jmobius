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

/**
 * The outline of a door or fence that blocks path search steps, held as a quad with a Z window.<br>
 * Geodata has no knowledge of these objects, so pathfinding tests every step against them.
 * @author Mobius
 */
public class PathBlocker
{
	/** Distance the outline is grown by on every side, one geo cell, so paths keep clear of the object. */
	private static final int CLEARANCE = 16;
	
	private final int[] _nodeX;
	private final int[] _nodeY;
	private final int _minX;
	private final int _minY;
	private final int _maxX;
	private final int _maxY;
	private final int _zMin;
	private final int _zMax;
	
	/**
	 * Creates a blocker from a quad outline and the Z range it blocks.<br>
	 * The outline is grown by one geo cell on every side, so a path routed around the object keeps clear of it instead of grazing an edge. The given arrays are left untouched, they belong to the door template.
	 * @param nodeX The four corner X coordinates.
	 * @param nodeY The four corner Y coordinates.
	 * @param zMin The lowest blocking Z coordinate, exclusive.
	 * @param zMax The highest blocking Z coordinate, exclusive.
	 */
	public PathBlocker(int[] nodeX, int[] nodeY, int zMin, int zMax)
	{
		_zMin = zMin;
		_zMax = zMax;
		
		// Corners are pushed away from the middle of the outline, which grows it without moving it.
		int centerX = 0;
		int centerY = 0;
		for (int i = 0; i < 4; i++)
		{
			centerX += nodeX[i];
			centerY += nodeY[i];
		}
		
		centerX /= 4;
		centerY /= 4;
		
		_nodeX = new int[4];
		_nodeY = new int[4];
		int minX = Integer.MAX_VALUE;
		int minY = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE;
		int maxY = Integer.MIN_VALUE;
		for (int i = 0; i < 4; i++)
		{
			final int grownX = nodeX[i] + (nodeX[i] >= centerX ? CLEARANCE : -CLEARANCE);
			final int grownY = nodeY[i] + (nodeY[i] >= centerY ? CLEARANCE : -CLEARANCE);
			_nodeX[i] = grownX;
			_nodeY[i] = grownY;
			minX = Math.min(minX, grownX);
			minY = Math.min(minY, grownY);
			maxX = Math.max(maxX, grownX);
			maxY = Math.max(maxY, grownY);
		}
		
		_minX = minX;
		_minY = minY;
		_maxX = maxX;
		_maxY = maxY;
	}
	
	/**
	 * Checks whether this outline overlaps the given world coordinate box.
	 * @param minX The box minimum X coordinate.
	 * @param minY The box minimum Y coordinate.
	 * @param maxX The box maximum X coordinate.
	 * @param maxY The box maximum Y coordinate.
	 * @return {@code true} if the outline overlaps the box, {@code false} otherwise.
	 */
	public boolean isWithin(int minX, int minY, int maxX, int maxY)
	{
		return (_maxX >= minX) && (_minX <= maxX) && (_maxY >= minY) && (_minY <= maxY);
	}
	
	/**
	 * Checks whether a movement segment crosses one of the outline edges inside the blocking Z range.
	 * @param x The segment start world X coordinate.
	 * @param y The segment start world Y coordinate.
	 * @param z The segment start world Z coordinate.
	 * @param tx The segment end world X coordinate.
	 * @param ty The segment end world Y coordinate.
	 * @param tz The segment end world Z coordinate.
	 * @return {@code true} if the segment is blocked, {@code false} otherwise.
	 */
	public boolean intersects(int x, int y, int z, int tx, int ty, int tz)
	{
		// Reject segments that stay entirely on one side of the outline box.
		if (((x < _minX) && (tx < _minX)) || ((x > _maxX) && (tx > _maxX)) || ((y < _minY) && (ty < _minY)) || ((y > _maxY) && (ty > _maxY)))
		{
			return false;
		}
		
		for (int i = 0; i < 4; i++)
		{
			final int j = (i + 1) < 4 ? i + 1 : 0;
			
			// Lower part of the multiplier fraction, if it is 0 we avoid an error and also know that the lines are parallel.
			final int denominator = ((ty - y) * (_nodeX[i] - _nodeX[j])) - ((tx - x) * (_nodeY[i] - _nodeY[j]));
			if (denominator == 0)
			{
				continue;
			}
			
			// Multipliers to the equations of the lines. If they are lower than 0 or bigger than 1, we know that segments don't intersect.
			final float multiplier1 = (float) (((_nodeX[j] - _nodeX[i]) * (y - _nodeY[i])) - ((_nodeY[j] - _nodeY[i]) * (x - _nodeX[i]))) / denominator;
			final float multiplier2 = (float) (((tx - x) * (y - _nodeY[i])) - ((ty - y) * (x - _nodeX[i]))) / denominator;
			if ((multiplier1 >= 0) && (multiplier1 <= 1) && (multiplier2 >= 0) && (multiplier2 <= 1))
			{
				final int intersectZ = Math.round(z + (multiplier1 * (tz - z)));
				if ((intersectZ > _zMin) && (intersectZ < _zMax))
				{
					return true;
				}
			}
		}
		
		return false;
	}
}
