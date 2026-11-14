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
 * Defines the different categories of special items available in the game.<br>
 * This enumeration is used to identify specific item behaviors and properties.
 * @author Nik, Mobius
 */
public enum SpecialItemType
{
	PC_CAFE_POINTS(-100),
	CLAN_REPUTATION(-200),
	FAME(-300),
	FIELD_CYCLE_POINTS(-400),
	RAIDBOSS_POINTS(-500),
	HONOR_COINS(-700),
	VITALITY_POINTS(-800),
	ILLUSORY_POINTS(-1100);
	
	private final int _clientId;
	
	/**
	 * Creates a new instance of {@code SpecialItemType}.<br>
	 * This constructor initializes the internal client identifier.
	 * @param clientId The unique ID associated with this item type.
	 */
	private SpecialItemType(int clientId)
	{
		_clientId = clientId;
	}
	
	/**
	 * Retrieves the unique identifier for this {@code SpecialItemType}.<br>
	 * This ID is used to map the item type to its specific internal value.
	 * @return The integer ID associated with this item type.
	 */
	public int getClientId()
	{
		return _clientId;
	}
	
	private static final Map<Integer, SpecialItemType> BY_CLIENT_ID = new HashMap<>(values().length);
	static
	{
		for (SpecialItemType type : values())
		{
			BY_CLIENT_ID.put(type.getClientId(), type);
		}
	}
	
	/**
	 * Retrieves a {@code SpecialItemType} based on its unique client ID.<br>
	 * This method searches the internal mapping for the provided {@code clientId}.
	 * @param clientId The unique identifier of the special item type.
	 * @return The corresponding {@code SpecialItemType} or {@code null} if not found.
	 */
	public static SpecialItemType getByClientId(int clientId)
	{
		return BY_CLIENT_ID.get(clientId);
	}
}
