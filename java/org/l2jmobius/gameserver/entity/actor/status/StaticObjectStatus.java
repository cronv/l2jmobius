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
package org.l2jmobius.gameserver.entity.actor.status;

import org.l2jmobius.gameserver.entity.actor.instance.StaticObject;

/**
 * Represents the status effects and properties specific to a {@code StaticObject}.<br>
 * This class handles how static world objects interact with the game's status system.
 */
public class StaticObjectStatus extends CreatureStatus
{
	/**
	 * Creates a new {@code StaticObjectStatus} instance.<br>
	 * This constructor initializes the status for a specific character.
	 * @param activeChar The {@code StaticObject} associated with this status.
	 */
	public StaticObjectStatus(StaticObject activeChar)
	{
		super(activeChar);
	}
	
	/**
	 * Retrieves the {@code StaticObject} associated with this status.<br>
	 * This method returns the active character object.
	 * @return the current {@code StaticObject}
	 */
	@Override
	public StaticObject getActiveChar()
	{
		return (StaticObject) super.getActiveChar();
	}
}
