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
package org.l2jmobius.gameserver.data.holders;

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.gameserver.entity.Location;

/**
 * Immutable definition of a scheduled airship, loaded from AirShipData.xml.<br>
 * Holds the spawn location and the circular route steps.<br>
 * The airship spawns at the start location and loops over the steps forever.
 * @author Mobius
 */
public class AirShipHolder
{
	private final int _id;
	private final int _moveSpeed;
	private final int _rotationSpeed;
	private final int _initialDelay;
	private final Location _start;
	private final List<AirShipStepHolder> _steps = new ArrayList<>();
	
	/**
	 * Creates a new {@code AirShipHolder}.
	 * @param id The airship id.
	 * @param moveSpeed The move speed used between the route steps.
	 * @param rotationSpeed The rotation speed used between the route steps.
	 * @param initialDelay The delay in milliseconds before the schedule starts.
	 * @param start The spawn {@code Location} including the spawn heading.
	 */
	public AirShipHolder(int id, int moveSpeed, int rotationSpeed, int initialDelay, Location start)
	{
		_id = id;
		_moveSpeed = moveSpeed;
		_rotationSpeed = rotationSpeed;
		_initialDelay = initialDelay;
		_start = start;
	}
	
	public int getId()
	{
		return _id;
	}
	
	public int getMoveSpeed()
	{
		return _moveSpeed;
	}
	
	public int getRotationSpeed()
	{
		return _rotationSpeed;
	}
	
	public int getInitialDelay()
	{
		return _initialDelay;
	}
	
	public Location getStart()
	{
		return _start;
	}
	
	public void addStep(AirShipStepHolder step)
	{
		_steps.add(step);
	}
	
	public List<AirShipStepHolder> getSteps()
	{
		return _steps;
	}
}
