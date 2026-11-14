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
package org.l2jmobius.gameserver.entity.actor.holders.creature;

import org.l2jmobius.gameserver.entity.Location;

/**
 * Represents a specific coordinate point within a vehicle's path.<br>
 * This class extends {@code Location} to provide spatial data for navigation.<br>
 * It is used by the game server to define waypoints for moving creatures.
 */
public class VehiclePathPoint extends Location
{
	private final int _moveSpeed;
	private final int _rotationSpeed;
	
	/**
	 * Creates a new {@code VehiclePathPoint} using an existing {@code Location}.<br>
	 * This constructor copies the coordinates from the provided object.
	 * @param loc The {@code Location} to use for this point.
	 */
	public VehiclePathPoint(Location loc)
	{
		this(loc.getX(), loc.getY(), loc.getZ());
	}
	
	/**
	 * Creates a new {@code VehiclePathPoint} at the specified coordinates.<br>
	 * This constructor sets default values for movement and rotation speeds.
	 * @param x The X coordinate of the point.
	 * @param y The Y coordinate of the point.
	 * @param z The Z coordinate of the point.
	 */
	public VehiclePathPoint(int x, int y, int z)
	{
		super(x, y, z);
		_moveSpeed = 350;
		_rotationSpeed = 4000;
	}
	
	/**
	 * Creates a new {@code VehiclePathPoint} at specific coordinates.<br>
	 * This constructor sets the movement and rotation speeds for the path point.
	 * @param x The X coordinate of the location.
	 * @param y The Y coordinate of the location.
	 * @param z The Z coordinate of the location.
	 * @param moveSpeed The speed at which the vehicle moves.
	 * @param rotationSpeed The speed at which the vehicle rotates.
	 */
	public VehiclePathPoint(int x, int y, int z, int moveSpeed, int rotationSpeed)
	{
		super(x, y, z);
		_moveSpeed = moveSpeed;
		_rotationSpeed = rotationSpeed;
	}
	
	/**
	 * Returns the movement speed of this path point.<br>
	 * This value determines how fast a vehicle moves through this location.
	 * @return the current {@code moveSpeed} as an {@code int}.
	 */
	public int getMoveSpeed()
	{
		return _moveSpeed;
	}
	
	/**
	 * Gets the current rotation speed of the vehicle.<br>
	 * This value determines how fast the vehicle turns.
	 * @return The rotation speed as an {@code int}.
	 */
	public int getRotationSpeed()
	{
		return _rotationSpeed;
	}
}
