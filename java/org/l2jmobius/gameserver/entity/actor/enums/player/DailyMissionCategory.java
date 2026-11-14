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
 * Defines the different categories for daily missions available to players.<br>
 * This enumeration is used to group and organize various types of daily tasks.
 * @author CostyKiller, Mobius
 */
public enum DailyMissionCategory
{
	NORMAL(1),
	DEEPENING(2),
	ACHIEVEMENTS(3),
	EVENT(4),
	DAILY(5);
	
	private final int _clientId;
	
	/**
	 * This is a private constructor for the {@code DailyMissionCategory} enum.<br>
	 * It initializes the category with a specific client identifier.
	 * @param clientId The unique ID associated with this mission category.
	 */
	private DailyMissionCategory(int clientId)
	{
		_clientId = clientId;
	}
	
	/**
	 * Retrieves the unique identifier for this {@code DailyMissionCategory}.<br>
	 * This ID is used to map categories in the game database.
	 * @return The integer value of the client ID.
	 */
	public int getClientId()
	{
		return _clientId;
	}
	
	private static final Map<Integer, DailyMissionCategory> BY_CLIENT_ID = new HashMap<>(values().length);
	static
	{
		for (DailyMissionCategory type : values())
		{
			BY_CLIENT_ID.put(type.getClientId(), type);
		}
	}
	
	/**
	 * Retrieves a {@code DailyMissionCategory} based on its unique ID.<br>
	 * This method looks up the category in the internal mapping.
	 * @param clientId The unique identifier for the mission category.
	 * @return The corresponding {@code DailyMissionCategory} or null if not found.
	 */
	public static DailyMissionCategory valueOf(int clientId)
	{
		return BY_CLIENT_ID.get(clientId);
	}
}
