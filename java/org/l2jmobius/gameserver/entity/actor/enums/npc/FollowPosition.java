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
package org.l2jmobius.gameserver.entity.actor.enums.npc;

/**
 * Position of a route follower relative to the direction its leader walks in.<br>
 * The angle is added to the leader movement direction to place the follower around the destination point.
 * @author Mobius
 */
public enum FollowPosition
{
	FRONT(0),
	FRONT_RIGHT(45),
	RIGHT(90),
	BACK_RIGHT(135),
	BACK(180),
	BACK_LEFT(225),
	LEFT(270),
	FRONT_LEFT(315);
	
	private final int _angle;
	
	private FollowPosition(int angle)
	{
		_angle = angle;
	}
	
	/**
	 * @return The angle in degrees relative to the leader movement direction.
	 */
	public int getAngle()
	{
		return _angle;
	}
}
