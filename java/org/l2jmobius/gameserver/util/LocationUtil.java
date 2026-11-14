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

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.interfaces.ILocational;

/**
 * Utility methods for spatial distance, angle, heading and positioning calculations.
 * @author Mobius
 */
public class LocationUtil
{
	/**
	 * Calculates the angle in degrees from an origin to a target location.
	 * @param origin starting reference location
	 * @param target destination location
	 * @return the angle in degrees
	 */
	public static double calculateAngleFrom(ILocational origin, ILocational target)
	{
		return calculateAngleFrom(origin.getX(), origin.getY(), target.getX(), target.getY());
	}
	
	/**
	 * Calculates the angle in degrees (0-360) from an origin point to a target point.
	 * @param originX starting x-coordinate
	 * @param originY starting y-coordinate
	 * @param targetX destination x-coordinate
	 * @param targetY destination y-coordinate
	 * @return the angle in degrees
	 */
	public static double calculateAngleFrom(int originX, int originY, int targetX, int targetY)
	{
		double angle = Math.toDegrees(Math.atan2(targetY - originY, targetX - originX));
		if (angle < 0)
		{
			angle += 360;
		}
		
		return angle;
	}
	
	/**
	 * Calculates the game heading from an origin to a target location.
	 * @param origin starting location
	 * @param target destination location
	 * @return the heading
	 */
	public static int calculateHeadingFrom(ILocational origin, ILocational target)
	{
		return calculateHeadingFrom(origin.getX(), origin.getY(), target.getX(), target.getY());
	}
	
	/**
	 * Calculates the game heading from an origin point to a target point.
	 * @param originX starting x-coordinate
	 * @param originY starting y-coordinate
	 * @param targetX destination x-coordinate
	 * @param targetY destination y-coordinate
	 * @return the heading
	 */
	public static int calculateHeadingFrom(int originX, int originY, int targetX, int targetY)
	{
		double angle = Math.toDegrees(Math.atan2(targetY - originY, targetX - originX));
		if (angle < 0)
		{
			angle += 360;
		}
		
		return (int) (angle * 182.044444444);
	}
	
	/**
	 * Calculates the game heading from coordinate deltas.
	 * @param deltaX difference in the x-coordinate
	 * @param deltaY difference in the y-coordinate
	 * @return the heading
	 */
	public static int calculateHeadingFrom(double deltaX, double deltaY)
	{
		double angle = Math.toDegrees(Math.atan2(deltaY, deltaX));
		if (angle < 0)
		{
			angle += 360;
		}
		
		return (int) (angle * 182.044444444);
	}
	
	/**
	 * Converts a raw client heading into degrees.
	 * @param clientHeading raw heading value from the client
	 * @return the heading in degrees
	 */
	public static double convertHeadingToDegree(int clientHeading)
	{
		return clientHeading / 182.044444444;
	}
	
	/**
	 * Calculates the distance between two 3D points with optional height and squared result.
	 * @param x1 x of the first point
	 * @param y1 y of the first point
	 * @param z1 z of the first point
	 * @param x2 x of the second point
	 * @param y2 y of the second point
	 * @param z2 z of the second point
	 * @param includeZ whether to include the z axis
	 * @param squared whether to return the squared distance
	 * @return the distance
	 */
	public static double calculateDistance(double x1, double y1, double z1, double x2, double y2, double z2, boolean includeZ, boolean squared)
	{
		final double dx = x1 - x2;
		final double dy = y1 - y2;
		final double dz = z1 - z2;
		final double distance = (dx * dx) + (dy * dy) + (includeZ ? (dz * dz) : 0);
		return squared ? distance : Math.sqrt(distance);
	}
	
	/**
	 * Calculates the distance between two locations with optional height and squared result.
	 * @param loc1 first location
	 * @param loc2 second location
	 * @param includeZ whether to include the z coordinate
	 * @param squared whether to return the squared distance
	 * @return the distance
	 */
	public static double calculateDistance(ILocational loc1, ILocational loc2, boolean includeZ, boolean squared)
	{
		return calculateDistance(loc1.getX(), loc1.getY(), loc1.getZ(), loc2.getX(), loc2.getY(), loc2.getZ(), includeZ, squared);
	}
	
	/**
	 * Checks whether two world objects are within range, accounting for creature collision radii.
	 * @param range maximum allowed distance
	 * @param obj1 first object
	 * @param obj2 second object
	 * @param includeZ whether to include the z coordinate
	 * @return {@code true} if within range, {@code false} otherwise
	 */
	public static boolean checkIfInRange(int range, WorldObject obj1, WorldObject obj2, boolean includeZ)
	{
		if ((obj1 == null) || (obj2 == null) || (obj1.getInstanceWorld() != obj2.getInstanceWorld()))
		{
			return false;
		}
		
		int combinedRadius = 0;
		if (obj1.isCreature())
		{
			combinedRadius += obj1.asCreature().getTemplate().getCollisionRadius();
		}
		
		if (obj2.isCreature())
		{
			combinedRadius += obj2.asCreature().getTemplate().getCollisionRadius();
		}
		
		return calculateDistance(obj1, obj2, includeZ, false) <= (range + combinedRadius);
	}
	
	/**
	 * Checks whether a world object is within range of another object resolved by its id.
	 * @param obj source object
	 * @param targetObjId identifier of the target object
	 * @param radius maximum allowed distance
	 * @return {@code true} if found and within range, {@code false} otherwise
	 */
	public static boolean isInsideRangeOfObjectId(WorldObject obj, int targetObjId, int radius)
	{
		final WorldObject target = World.findObject(targetObjId);
		return (target != null) && (obj.calculateDistance3D(target) <= radius);
	}
	
	/**
	 * Generates a random location around a center point within a radius range.
	 * @param center origin location
	 * @param minRadius minimum distance from the center
	 * @param maxRadius maximum distance from the center
	 * @return a new {@code Location} at the random coordinates
	 */
	public static Location getRandomLocation(ILocational center, int minRadius, int maxRadius)
	{
		final int randomX = Rnd.get(minRadius, maxRadius);
		final int randomY = Rnd.get(minRadius, maxRadius);
		final double angle = Math.toRadians(Rnd.get(360));
		final int newX = (int) (center.getX() + (randomX * Math.cos(angle)));
		final int newY = (int) (center.getY() + (randomY * Math.sin(angle)));
		return new Location(newX, newY, center.getZ());
	}
}
