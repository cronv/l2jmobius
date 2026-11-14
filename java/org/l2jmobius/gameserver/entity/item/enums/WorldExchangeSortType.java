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
 * Defines the different sorting methods available for items in the {@code WorldExchange}.<br>
 * This enum helps determine how items are ordered when displayed to players.
 * @author Index, Mobius
 */
public enum WorldExchangeSortType
{
	NONE(0),
	ITEM_NAME_ASCE(2),
	ITEM_NAME_DESC(3),
	// ENCHANT_ASCE(2),
	// ENCHANT_DESC(3),
	PRICE_ASCE(4),
	PRICE_DESC(5),
	AMOUNT_ASCE(6),
	AMOUNT_DESC(7),
	PRICE_PER_PIECE_ASCE(8),
	PRICE_PER_PIECE_DESC(9);
	
	private final int _id;
	
	/**
	 * Creates a new instance of {@code WorldExchangeSortType}.<br>
	 * This constructor assigns an internal identifier to the enum constant.
	 * @param id The unique integer ID for this sort type.
	 */
	private WorldExchangeSortType(int id)
	{
		_id = id;
	}
	
	/**
	 * Returns the unique identifier for this sort type.<br>
	 * This value is used to map the enum to its database representation.
	 * @return The {@code int} ID of the sort type.
	 */
	public int getId()
	{
		return _id;
	}
	
	private static final Map<Integer, WorldExchangeSortType> BY_ID = new HashMap<>(values().length);
	static
	{
		for (WorldExchangeSortType type : values())
		{
			BY_ID.putIfAbsent(type.getId(), type);
		}
	}
	
	/**
	 * Retrieves the {@code WorldExchangeSortType} based on a given ID.<br>
	 * This method looks up the enum value from an internal map.
	 * @param id The unique identifier for the sort type.
	 * @return The corresponding {@code WorldExchangeSortType} or null if not found.
	 */
	public static WorldExchangeSortType getWorldExchangeSortType(int id)
	{
		return BY_ID.get(id);
	}
}
