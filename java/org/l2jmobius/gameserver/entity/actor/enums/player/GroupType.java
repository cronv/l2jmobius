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

import java.util.HashMap;
import java.util.Map;

import org.l2jmobius.gameserver.interfaces.MaskComponent;

/**
 * Defines the different types of groups that a player can belong to.<br>
 * This enumeration is used to categorize and manage group-related behaviors.
 * @author malyelfik, Mobius
 */
public enum GroupType implements MaskComponent
{
	NONE(1),
	PARTY(2),
	COMMAND_CHANNEL(4);
	
	private final int _mask;
	
	/**
	 * Creates a new instance of {@code GroupType}.<br>
	 * This constructor assigns the bitmask value to the internal field.
	 * @param mask The integer value representing the group type.
	 */
	private GroupType(int mask)
	{
		_mask = mask;
	}
	
	/**
	 * Retrieves the bitmask associated with this {@code GroupType}.<br>
	 * This value is used to identify specific group categories.
	 * @return The integer mask value.
	 */
	@Override
	public int getMask()
	{
		return _mask;
	}
	
	private static final Map<Integer, GroupType> BY_MASK = new HashMap<>(values().length);
	static
	{
		for (GroupType type : values())
		{
			BY_MASK.put(type.getMask(), type);
		}
	}
	
	/**
	 * Retrieves a {@code GroupType} based on its bitmask.<br>
	 * This method looks up the corresponding enum value for a given integer.
	 * @param flag The integer mask to search for.
	 * @return The matching {@code GroupType} or {@code null} if no match is found.
	 */
	public static GroupType getByMask(int flag)
	{
		return BY_MASK.get(flag);
	}
}
