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
package org.l2jmobius.gameserver.entity.actor.stat;

import org.l2jmobius.gameserver.entity.actor.Vehicle;

/**
 * Represents the statistics specific to a {@code Vehicle} entity.<br>
 * This class handles attributes that define how vehicles behave in the game world.
 */
public class VehicleStat extends CreatureStat
{
	private float _moveSpeed = 0;
	private int _rotationSpeed = 0;
	
	/**
	 * Creates a new {@code VehicleStat} instance.<br>
	 * This constructor initializes the stats for a specific vehicle.<br>
	 * It uses the provided {@code Vehicle} to set up the base data.
	 * @param activeChar The {@code Vehicle} object associated with these statistics.
	 */
	public VehicleStat(Vehicle activeChar)
	{
		super(activeChar);
	}
	
	/**
	 * Retrieves the current movement speed of the vehicle.<br>
	 * This value is used to determine how fast the entity moves.
	 * @return The move speed as a {@code double}.
	 */
	@Override
	public double getMoveSpeed()
	{
		return _moveSpeed;
	}
	
	/**
	 * Sets the movement speed of the vehicle.<br>
	 * This updates the internal {@code _moveSpeed} value.
	 * @param speed The new movement speed to apply.
	 */
	public void setMoveSpeed(float speed)
	{
		_moveSpeed = speed;
	}
	
	/**
	 * Retrieves the current rotation speed of the vehicle.<br>
	 * This value determines how fast the vehicle turns.
	 * @return The rotation speed as a {@code double}.
	 */
	public double getRotationSpeed()
	{
		return _rotationSpeed;
	}
	
	/**
	 * Sets the rotation speed for the vehicle.<br>
	 * This value determines how fast the vehicle turns.
	 * @param speed The new rotation speed to apply.
	 */
	public void setRotationSpeed(int speed)
	{
		_rotationSpeed = speed;
	}
}
