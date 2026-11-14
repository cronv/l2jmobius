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
package org.l2jmobius.gameserver.entity.actor.enums.player;

/**
 * Defines the various types of actions a {@code Player} can perform within the game.<br>
 * This enumeration is used to identify and handle specific player behaviors.
 * @author UnAfraid, Mobius
 */
public enum PlayerAction
{
	ADMIN_COMMAND,
	ADMIN_SAVE_ZONE,
	ADMIN_SHOW_TERRITORY,
	MERCENARY_CONFIRM,
	USER_ENGAGE,
	OFFLINE_TRADE,
	OFFLINE_PLAY;
	
	private final int _mask;
	
	/**
	 * This is a private constructor for the {@code PlayerAction} enum.<br>
	 * It initializes the internal {@code _mask} value based on the enum's position.<br>
	 * This method should not be called directly.
	 */
	private PlayerAction()
	{
		_mask = 1 << ordinal();
	}
	
	/**
	 * Retrieves the bitmask associated with this {@code PlayerAction}.<br>
	 * This value is used to identify specific actions in bitwise operations.
	 * @return The integer mask for the current action.
	 */
	public int getMask()
	{
		return _mask;
	}
}
