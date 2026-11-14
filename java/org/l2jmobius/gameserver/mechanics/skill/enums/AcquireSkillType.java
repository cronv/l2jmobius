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
package org.l2jmobius.gameserver.mechanics.skill.enums;

import java.util.HashMap;
import java.util.Map;

/**
 * Defines the different methods used to acquire skills.<br>
 * This enum categorizes how a character can learn new abilities within the game.
 * @author Zoey76, Mobius
 */
public enum AcquireSkillType
{
	CLASS(0),
	DUMMY(1),
	PLEDGE(2),
	SUBPLEDGE(3),
	TRANSFORM(4),
	TRANSFER(5),
	SUBCLASS(6),
	COLLECT(7),
	DUMMY2(8),
	DUMMY3(9),
	FISHING(10),
	REVELATION(11), // Need proper ID
	REVELATION_DUALCLASS(12), // Need proper ID
	DUALCLASS(13), // Need proper ID
	ALCHEMY(140);
	
	private final int _id;
	
	/**
	 * Creates a new instance of {@code AcquireSkillType}.<br>
	 * This constructor assigns the internal ID to the enum constant.
	 * @param id The unique integer identifier for the skill type.
	 */
	private AcquireSkillType(int id)
	{
		_id = id;
	}
	
	/**
	 * Retrieves the unique identifier for this {@code AcquireSkillType}.<br>
	 * This value is used to map the enum to its corresponding database ID.
	 * @return The integer ID of the skill type.
	 */
	public int getId()
	{
		return _id;
	}
	
	private static final Map<Integer, AcquireSkillType> BY_ID = new HashMap<>(values().length);
	static
	{
		for (AcquireSkillType type : values())
		{
			BY_ID.put(type.getId(), type);
		}
	}
	
	/**
	 * Retrieves a specific {@code AcquireSkillType} based on its unique ID.<br>
	 * This method looks up the value in the internal mapping.
	 * @param id The integer ID of the skill type to find.
	 * @return The corresponding {@code AcquireSkillType} or {@code null} if not found.
	 */
	public static AcquireSkillType getAcquireSkillType(int id)
	{
		return BY_ID.get(id);
	}
}
