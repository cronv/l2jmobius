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
package org.l2jmobius.gameserver.mechanics.punishment;

import java.util.HashMap;
import java.util.Map;

/**
 * Defines the different types of punishments available in the game.<br>
 * This enum is used to categorize various penalties applied to players.
 * @author UnAfraid, Mobius
 */
public enum PunishmentType
{
	BAN,
	CHAT_BAN,
	PARTY_BAN,
	JAIL;
	
	private static final Map<String, PunishmentType> BY_NAME = new HashMap<>(values().length);
	static
	{
		for (PunishmentType type : values())
		{
			BY_NAME.put(type.name().toLowerCase(), type);
		}
	}
	
	/**
	 * Retrieves a {@code PunishmentType} based on its string name.<br>
	 * The search is case-insensitive.
	 * @param name The name of the punishment type to find.
	 * @return The matching {@code PunishmentType} or {@code null} if no match is found.
	 */
	public static PunishmentType getByName(String name)
	{
		return name == null ? null : BY_NAME.get(name.toLowerCase());
	}
}
