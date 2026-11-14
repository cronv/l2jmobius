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
package org.l2jmobius.gameserver.util;

import java.util.Arrays;
import java.util.logging.Logger;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.entity.Location;

/**
 * Defines a polygonal territory and resolves random positions inside its boundaries.<br>
 * Territory changes are published as complete snapshots so readers do not observe partially updated limits or weights.
 * <ul>
 * <li>Evaluates two-dimensional point containment with ray crossing parity.</li>
 * <li>Selects weighted vertices when the territory defines positive probabilities.</li>
 * <li>Samples coordinates from the territory area when no weighted vertex is selected.</li>
 * </ul>
 * @author BazookaRpm
 */
public class PolygonTerritory
{
	private static final Logger LOGGER = Logger.getLogger(PolygonTerritory.class.getName());
	
	// Territory Defaults.
	private static final int EMPTY_MINIMUM_COORDINATE = 999999;
	private static final int EMPTY_MAXIMUM_COORDINATE = -999999;
	private static final int RANDOM_POINT_ATTEMPTS = 100;
	private static final Point[] NO_POINTS = new Point[0];
	
	/**
	 * Defines one polygon vertex and its vertical spawn interval.
	 */
	protected static class Point
	{
		protected int _x;
		protected int _y;
		protected int _zmin;
		protected int _zmax;
		protected int _proc;
		
		Point(int x, int y, int zmin, int zmax, int proc)
		{
			_x = x;
			_y = y;
			_zmin = zmin;
			_zmax = zmax;
			_proc = proc;
		}
	}
	
	/**
	 * Captures all data required by one territory read operation.
	 */
	private static class TerritoryState
	{
		private final Point[] _vertices;
		private final int _minimumX;
		private final int _maximumX;
		private final int _minimumY;
		private final int _maximumY;
		private final int _minimumZ;
		private final int _maximumZ;
		private final int _weight;
		
		private TerritoryState(Point[] vertices, int minimumX, int maximumX, int minimumY, int maximumY, int minimumZ, int maximumZ, int weight)
		{
			_vertices = vertices;
			_minimumX = minimumX;
			_maximumX = maximumX;
			_minimumY = minimumY;
			_maximumY = maximumY;
			_minimumZ = minimumZ;
			_maximumZ = maximumZ;
			_weight = weight;
		}
		
		private static TerritoryState createEmpty()
		{
			return new TerritoryState(NO_POINTS, EMPTY_MINIMUM_COORDINATE, EMPTY_MAXIMUM_COORDINATE, EMPTY_MINIMUM_COORDINATE, EMPTY_MAXIMUM_COORDINATE, EMPTY_MINIMUM_COORDINATE, EMPTY_MAXIMUM_COORDINATE, 0);
		}
		
		private TerritoryState withVertex(Point vertex)
		{
			final Point[] vertices = Arrays.copyOf(_vertices, _vertices.length + 1);
			vertices[_vertices.length] = vertex;
			return new TerritoryState(vertices, Math.min(_minimumX, vertex._x), Math.max(_maximumX, vertex._x), Math.min(_minimumY, vertex._y), Math.max(_maximumY, vertex._y), Math.min(_minimumZ, vertex._zmin), Math.max(_maximumZ, vertex._zmax), _weight + vertex._proc);
		}
	}
	
	// Territory State.
	private final int _territoryId;
	private final Object _updateLock = new Object();
	private volatile TerritoryState _state;
	
	/**
	 * Creates an empty territory.
	 * @param terr territory identifier used for diagnostics.
	 */
	public PolygonTerritory(int terr)
	{
		_territoryId = terr;
		_state = TerritoryState.createEmpty();
	}
	
	/**
	 * Adds a polygon vertex and its vertical range.
	 * @param x vertex X coordinate.
	 * @param y vertex Y coordinate.
	 * @param zmin minimum Z coordinate.
	 * @param zmax maximum Z coordinate.
	 * @param proc weighted selection value.
	 */
	public void add(int x, int y, int zmin, int zmax, int proc)
	{
		final Point vertex = new Point(x, y, zmin, zmax, proc);
		synchronized (_updateLock)
		{
			_state = _state.withVertex(vertex);
		}
	}
	
	/**
	 * Tests whether a horizontal ray starting at a coordinate intersects an edge.<br>
	 * Horizontal edges do not cross the ray, while edge endpoints remain inclusive.
	 * @param x coordinate X.
	 * @param y coordinate Y.
	 * @param p1 first edge point.
	 * @param p2 second edge point.
	 * @return {@code true} when the edge crosses the ray.
	 */
	public boolean isIntersect(int x, int y, Point p1, Point p2)
	{
		if (p1._y == p2._y)
		{
			return false;
		}
		
		final int lowestY = Math.min(p1._y, p2._y);
		final int highestY = Math.max(p1._y, p2._y);
		if ((y < lowestY) || (y > highestY))
		{
			return false;
		}
		
		final int lowestX = Math.min(p1._x, p2._x);
		if (x <= lowestX)
		{
			return true;
		}
		
		final int highestX = Math.max(p1._x, p2._x);
		if (x > highestX)
		{
			return false;
		}
		
		return getIntersectionX(y, p1, p2) >= x;
	}
	
	/**
	 * Tests whether a coordinate is inside this territory.
	 * @param x coordinate X.
	 * @param y coordinate Y.
	 * @return {@code true} when the coordinate is inside the polygon.
	 */
	public boolean isInside(int x, int y)
	{
		return isInside(_state, x, y);
	}
	
	/**
	 * Creates a random location using configured vertex weights or the polygon area.
	 * @return a territory location, or {@code null} when no valid coordinate is generated.
	 */
	public Location getRandomPoint()
	{
		final TerritoryState state = _state;
		final Point weightedVertex = getWeightedVertex(state);
		if (weightedVertex != null)
		{
			return createLocation(weightedVertex);
		}
		
		return getRandomAreaPoint(state);
	}
	
	/**
	 * Gets the accumulated vertex weight.
	 * @return the total configured weight.
	 */
	public int getProcMax()
	{
		return _state._weight;
	}
	
	/**
	 * Tests whether a coordinate is inside one immutable territory snapshot.
	 * @param state territory data snapshot.
	 * @param x coordinate X.
	 * @param y coordinate Y.
	 * @return {@code true} when the coordinate is inside the snapshot polygon.
	 */
	private boolean isInside(TerritoryState state, int x, int y)
	{
		final Point[] vertices = state._vertices;
		if (vertices.length < 3)
		{
			return false;
		}
		
		boolean contained = false;
		Point previousVertex = vertices[vertices.length - 1];
		for (Point currentVertex : vertices)
		{
			if (isIntersect(x, y, previousVertex, currentVertex))
			{
				contained = !contained;
			}
			
			previousVertex = currentVertex;
		}
		
		return contained;
	}
	
	/**
	 * Calculates the X coordinate where an edge crosses the horizontal ray.
	 * @param y ray Y coordinate.
	 * @param first first edge point.
	 * @param second second edge point.
	 * @return edge intersection X coordinate.
	 */
	private double getIntersectionX(int y, Point first, Point second)
	{
		final double verticalOffset = first._y - y;
		final double horizontalDistance = first._x - second._x;
		final double verticalDistance = first._y - second._y;
		return first._x - ((verticalOffset * horizontalDistance) / verticalDistance);
	}
	
	/**
	 * Resolves a weighted vertex using the established cumulative selection behavior.
	 * @param state territory data snapshot.
	 * @return the selected vertex, or {@code null} when no weighted vertex is selected.
	 */
	private Point getWeightedVertex(TerritoryState state)
	{
		if (state._weight <= 0)
		{
			return null;
		}
		
		final int selectedWeight = Rnd.get(state._weight);
		int cumulativeWeight = 0;
		for (Point vertex : state._vertices)
		{
			cumulativeWeight += vertex._proc;
			if (selectedWeight <= cumulativeWeight)
			{
				return vertex;
			}
		}
		
		return null;
	}
	
	/**
	 * Samples a valid point inside the territory bounding rectangle.
	 * @param state territory data snapshot.
	 * @return a valid location, or {@code null} after all attempts fail.
	 */
	private Location getRandomAreaPoint(TerritoryState state)
	{
		for (int remainingAttempts = RANDOM_POINT_ATTEMPTS; remainingAttempts > 0; remainingAttempts--)
		{
			final int x = Rnd.get(state._minimumX, state._maximumX);
			final int y = Rnd.get(state._minimumY, state._maximumY);
			if (isInside(state, x, y))
			{
				final int zmin = getClosestMinimumZ(state._vertices, x, y, state._minimumZ);
				return new Location(x, y, Rnd.get(zmin, state._maximumZ));
			}
		}
		
		LOGGER.warning("Can't make point for territory " + _territoryId);
		return null;
	}
	
	/**
	 * Creates a location at a weighted vertex.
	 * @param vertex selected vertex.
	 * @return a location within the vertex vertical interval.
	 */
	private Location createLocation(Point vertex)
	{
		return new Location(vertex._x, vertex._y, Rnd.get(vertex._zmin, vertex._zmax));
	}
	
	/**
	 * Gets the minimum Z value associated with the nearest vertex.<br>
	 * Squared distances preserve nearest-point ordering without an unnecessary square root operation.
	 * @param vertices polygon vertices.
	 * @param x coordinate X.
	 * @param y coordinate Y.
	 * @param initialZ fallback minimum Z value.
	 * @return the nearest vertex minimum Z value.
	 */
	private int getClosestMinimumZ(Point[] vertices, int x, int y, int initialZ)
	{
		double closestDistance = 0;
		int zmin = initialZ;
		for (Point vertex : vertices)
		{
			final double deltaX = vertex._x - x;
			final double deltaY = vertex._y - y;
			final double distance = (deltaX * deltaX) + (deltaY * deltaY);
			if ((closestDistance == 0) || (distance < closestDistance))
			{
				closestDistance = distance;
				zmin = vertex._zmin;
			}
		}
		
		return zmin;
	}
}
