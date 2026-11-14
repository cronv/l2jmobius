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
 * Defines the different types of private stores available to players.<br>
 * This enumeration is used to categorize store behaviors and properties within the game world.
 * @author NosBit, Mobius
 */
public enum PrivateStoreType
{
	NONE(0),
	SELL(1),
	SELL_MANAGE(2),
	BUY(3),
	BUY_MANAGE(4),
	MANUFACTURE(5),
	PACKAGE_SELL(8),
	SELL_BUFFS(9);
	
	private int _id;
	
	/**
	 * Creates a new instance of {@code PrivateStoreType}.<br>
	 * This constructor assigns the internal ID to the enum constant.
	 * @param id The unique identifier for the store type.
	 */
	private PrivateStoreType(int id)
	{
		_id = id;
	}
	
	/**
	 * Returns the unique identifier for this {@code PrivateStoreType}.<br>
	 * This value is used to identify the specific type of private store.
	 * @return The integer ID associated with this enum constant.
	 */
	public int getId()
	{
		return _id;
	}
	
	private static final Map<Integer, PrivateStoreType> BY_ID = new HashMap<>(values().length);
	static
	{
		for (PrivateStoreType privateStoreType : values())
		{
			BY_ID.put(privateStoreType.getId(), privateStoreType);
		}
	}
	
	/**
	 * Retrieves a {@code PrivateStoreType} based on its unique identifier.<br>
	 * This method searches the internal mapping for the provided {@code id}.
	 * @param id The integer ID of the store type to find.
	 * @return The matching {@code PrivateStoreType} or {@code null} if not found.
	 */
	public static PrivateStoreType findById(int id)
	{
		return BY_ID.get(id);
	}
}
