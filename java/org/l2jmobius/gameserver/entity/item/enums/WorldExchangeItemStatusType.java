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
package org.l2jmobius.gameserver.entity.item.enums;

import java.util.HashMap;
import java.util.Map;

/**
 * Defines the possible states for items within the world exchange system.<br>
 * This enum helps track whether an item is available, locked, or currently being processed.
 * @author Index, Mobius
 */
public enum WorldExchangeItemStatusType
{
	WORLD_EXCHANGE_REGISTERED(0),
	WORLD_EXCHANGE_SOLD(1),
	WORLD_EXCHANGE_OUT_TIME(2),
	WORLD_EXCHANGE_NONE(3);
	
	private final int _id;
	
	/**
	 * Creates a new instance of {@code WorldExchangeItemStatusType}.<br>
	 * This constructor assigns the internal ID to the enum constant.
	 * @param id The unique integer identifier for the status type.
	 */
	private WorldExchangeItemStatusType(int id)
	{
		_id = id;
	}
	
	/**
	 * Returns the unique identifier for this {@code WorldExchangeItemStatusType}.<br>
	 * This value is used to map the status to its corresponding integer ID.
	 * @return The integer {@code id} associated with this enum constant.
	 */
	public int getId()
	{
		return _id;
	}
	
	private static final Map<Integer, WorldExchangeItemStatusType> BY_ID = new HashMap<>(values().length);
	static
	{
		for (WorldExchangeItemStatusType type : values())
		{
			BY_ID.put(type.getId(), type);
		}
	}
	
	/**
	 * Retrieves the {@code WorldExchangeItemStatusType} based on a unique ID.<br>
	 * This method looks up the status from an internal map.
	 * @param id The integer ID of the status to retrieve.
	 * @return The corresponding {@code WorldExchangeItemStatusType} or null if not found.
	 */
	public static WorldExchangeItemStatusType getWorldExchangeItemStatusType(int id)
	{
		return BY_ID.get(id);
	}
}
