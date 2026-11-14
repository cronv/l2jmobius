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
 * Represents the current progress state of a player's daily mission.<br>
 * This enum tracks whether a mission is {@code NOT_STARTED}, {@code IN_PROGRESS}, or {@code COMPLETED}.
 * @author UnAfraid, CostyKiller, Mobius
 */
public enum DailyMissionStatus
{
	NOT_DISPLAYED(0),
	AVAILABLE(1),
	NOT_AVAILABLE(2),
	COMPLETED(3);
	
	private final int _clientId;
	
	/**
	 * This is a private constructor for the {@code DailyMissionStatus} enum.<br>
	 * It initializes the internal status ID.
	 * @param clientId The unique integer identifier for the mission status.
	 */
	private DailyMissionStatus(int clientId)
	{
		_clientId = clientId;
	}
	
	/**
	 * Retrieves the unique identifier for this mission status.<br>
	 * This ID is used to map the enum to its corresponding client-side value.
	 * @return The {@code int} value of the client ID.
	 */
	public int getClientId()
	{
		return _clientId;
	}
	
	private static final Map<Integer, DailyMissionStatus> BY_CLIENT_ID = new HashMap<>(values().length);
	static
	{
		for (DailyMissionStatus type : values())
		{
			BY_CLIENT_ID.put(type.getClientId(), type);
		}
	}
	
	/**
	 * Retrieves a {@code DailyMissionStatus} based on the provided ID.<br>
	 * This method looks up the status in the internal mapping.
	 * @param clientId The unique identifier for the mission status.
	 * @return The corresponding {@code DailyMissionStatus} enum value.
	 */
	public static DailyMissionStatus valueOf(int clientId)
	{
		return BY_CLIENT_ID.get(clientId);
	}
}
