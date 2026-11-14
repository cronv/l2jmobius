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
package org.l2jmobius.gameserver.mechanics.krateisCube;

import org.l2jmobius.gameserver.entity.actor.Player;

/**
 * Represents a player entity involved in the {@code KrateiCube} mechanic.<br>
 * This class handles specific logic and state for players interacting with the cube.
 * @author LordWinter
 */
public class KrateiCubePlayer
{
	private final Player _player;
	private boolean _isInside = false;
	private boolean _isRegister = false;
	private int _points = 0;
	
	/**
	 * Creates a new {@code KrateiCubePlayer} instance.<br>
	 * This constructor links the object to a specific {@code Player}.
	 * @param player The {@code Player} associated with this cube instance.
	 */
	public KrateiCubePlayer(Player player)
	{
		_player = player;
	}
	
	/**
	 * Retrieves the {@code Player} associated with this instance.<br>
	 * This method returns the current player object.
	 * @return The {@code Player} object.
	 */
	public Player getPlayer()
	{
		return _player;
	}
	
	/**
	 * Updates the registration status of the player.<br>
	 * Sets the {@code _isRegister} field to the provided {@code value}.
	 * @param value The new registration status.
	 */
	public void setIsRegister(boolean value)
	{
		_isRegister = value;
	}
	
	/**
	 * Checks if the player is currently registered.<br>
	 * This method returns the current registration status.
	 * @return {@code true} if the player is registered, {@code false} otherwise.
	 */
	public boolean isRegister()
	{
		return _isRegister;
	}
	
	/**
	 * Updates the status of whether the player is currently inside the cube.<br>
	 * Sets the internal state to {@code true} or {@code false}.
	 * @param value The new status to set.
	 */
	public void setIsInside(boolean value)
	{
		_isInside = value;
	}
	
	/**
	 * Checks if the player is currently inside the cube.
	 * @return {@code true} if the player is inside, {@code false} otherwise.
	 */
	public boolean isInside()
	{
		return _isInside;
	}
	
	/**
	 * Updates the total score for the player.<br>
	 * This method sets the {@code _points} field to a new value.
	 * @param points The new number of points to assign.
	 */
	public void addPoints(int points)
	{
		_points = points;
	}
	
	/**
	 * Retrieves the current score of the player.<br>
	 * This value represents the total points accumulated during the activity.
	 * @return The current number of points as an {@code int}.
	 */
	public int getPoints()
	{
		return _points;
	}
}
