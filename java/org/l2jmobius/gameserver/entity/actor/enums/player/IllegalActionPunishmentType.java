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

/**
 * Defines the different types of punishments applied to players for performing illegal actions.<br>
 * This enumeration is used by the server to categorize various penalties.
 * @author xban1x, Mobius
 */
public enum IllegalActionPunishmentType
{
	NONE,
	BROADCAST,
	KICK,
	KICKBAN,
	JAIL;
	
	private static final Map<String, IllegalActionPunishmentType> BY_NAME = new HashMap<>(values().length);
	static
	{
		for (IllegalActionPunishmentType type : values())
		{
			BY_NAME.put(type.name().toLowerCase(), type);
		}
	}
	
	/**
	 * Finds an {@code IllegalActionPunishmentType} based on its string name.<br>
	 * This method is case-insensitive.<br>
	 * It returns {@code NONE} if the name is not found or is {@code null}.
	 * @param name The name of the punishment type to search for.
	 * @return The matching {@code IllegalActionPunishmentType} or {@code NONE}.
	 */
	public static IllegalActionPunishmentType findByName(String name)
	{
		return name == null ? NONE : BY_NAME.getOrDefault(name.toLowerCase(), NONE);
	}
}
