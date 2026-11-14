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
 * Defines the primary categories for items available in the world exchange.<br>
 * This enum is used to classify different types of tradeable goods.
 * @author Index, Mobius
 */
public enum WorldExchangeItemMainType
{
	ADENA(24),
	EQUIPMENT(0),
	ARTIFACT(1),
	ENCHANT(8),
	CONSUMABLE(20),
	ETC(4),
	COLLECTION(5);
	
	private final int _id;
	
	/**
	 * Creates a new instance of {@code WorldExchangeItemMainType}.<br>
	 * This constructor assigns the internal ID to the enum constant.
	 * @param id The unique identifier for the item type.
	 */
	private WorldExchangeItemMainType(int id)
	{
		_id = id;
	}
	
	/**
	 * Returns the unique identifier for this {@code WorldExchangeItemMainType}.<br>
	 * This value is used to map the enum to its corresponding database ID.
	 * @return The integer ID of the item type.
	 */
	public int getId()
	{
		return _id;
	}
	
	private static final Map<Integer, WorldExchangeItemMainType> BY_ID = new HashMap<>(values().length);
	static
	{
		for (WorldExchangeItemMainType type : values())
		{
			BY_ID.put(type.getId(), type);
		}
	}
	
	/**
	 * Retrieves the {@code WorldExchangeItemMainType} based on a specific ID.<br>
	 * This method looks up the type in the internal mapping.
	 * @param id The unique identifier for the item type.
	 * @return The corresponding {@code WorldExchangeItemMainType} or null if not found.
	 */
	public static WorldExchangeItemMainType getWorldExchangeItemMainType(int id)
	{
		return BY_ID.get(id);
	}
}
