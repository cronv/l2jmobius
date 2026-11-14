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
package org.l2jmobius.gameserver.mechanics.announce;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Defines the different categories of announcements available in the game.<br>
 * This enum is used to determine how and where messages are displayed to players.
 * @author UnAfraid, Mobius
 */
public enum AnnouncementType
{
	NORMAL,
	CRITICAL,
	EVENT,
	AUTO_NORMAL,
	AUTO_CRITICAL;
	
	private static final Logger LOGGER = Logger.getLogger(AnnouncementType.class.getName());
	
	private static final Map<Integer, AnnouncementType> BY_ID = new HashMap<>(values().length);
	private static final Map<String, AnnouncementType> BY_NAME = new HashMap<>(values().length);
	static
	{
		for (AnnouncementType type : values())
		{
			BY_ID.put(type.ordinal(), type);
			BY_NAME.put(type.name().toLowerCase(), type);
		}
	}
	
	/**
	 * Finds an {@code AnnouncementType} based on its unique ID.<br>
	 * If the ID is not found, it returns {@code NORMAL}.
	 * @param id The numeric identifier of the announcement type.
	 * @return The corresponding {@code AnnouncementType} or {@code NORMAL}.
	 */
	public static AnnouncementType findById(int id)
	{
		final AnnouncementType type = BY_ID.get(id);
		if (type == null)
		{
			LOGGER.log(Level.WARNING, AnnouncementType.class.getSimpleName() + ": Unexistent id specified: " + id + "!", new IllegalStateException());
			return NORMAL;
		}
		return type;
	}
	
	/**
	 * Finds an {@code AnnouncementType} based on its string name.<br>
	 * The search is case-insensitive.<br>
	 * If the name is not found, it returns {@code NORMAL}.
	 * @param name The name of the announcement type to find.
	 * @return The matching {@code AnnouncementType} or {@code NORMAL} if no match exists.
	 */
	public static AnnouncementType findByName(String name)
	{
		final AnnouncementType type = name == null ? null : BY_NAME.get(name.toLowerCase());
		if (type == null)
		{
			LOGGER.log(Level.WARNING, AnnouncementType.class.getSimpleName() + ": Unexistent name specified: " + name + "!", new IllegalStateException());
			return NORMAL;
		}
		return type;
	}
}
