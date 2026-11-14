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
package org.l2jmobius.gameserver.entity.actor.enums.creature;

import java.util.HashMap;
import java.util.Map;

/**
 * This enum represents the various types of attributes available to creatures.<br>
 * It is used to identify specific stats and properties within the game world.
 * @author NosBit, Mobius
 */
public enum AttributeType
{
	NONE(-2),
	FIRE(0),
	WATER(1),
	WIND(2),
	EARTH(3),
	HOLY(4),
	DARK(5);
	
	public static final AttributeType[] ATTRIBUTE_TYPES =
	{
		FIRE,
		WATER,
		WIND,
		EARTH,
		HOLY,
		DARK
	};
	
	private final byte _clientId;
	
	/**
	 * Creates a new {@code AttributeType} instance.<br>
	 * This constructor assigns the internal client identifier.
	 * @param clientId The unique ID for the attribute.
	 */
	AttributeType(int clientId)
	{
		_clientId = (byte) clientId;
	}
	
	/**
	 * Retrieves the unique identifier for this {@code AttributeType}.<br>
	 * This ID is used to map attributes in the game data.
	 * @return The {@code byte} value of the client ID.
	 */
	public byte getClientId()
	{
		return _clientId;
	}
	
	/**
	 * Returns the attribute that is opposite to the current one.<br>
	 * This method uses the internal {@code _clientId} to find the pair.
	 * @return The {@code AttributeType} representing the opposite element.
	 */
	public AttributeType getOpposite()
	{
		return ATTRIBUTE_TYPES[((_clientId % 2) == 0) ? (_clientId + 1) : (_clientId - 1)];
	}
	
	private static final Map<String, AttributeType> BY_NAME = new HashMap<>(values().length);
	private static final Map<Integer, AttributeType> BY_CLIENT_ID = new HashMap<>(values().length);
	static
	{
		for (AttributeType attributeType : values())
		{
			BY_NAME.put(attributeType.name().toLowerCase(), attributeType);
			BY_CLIENT_ID.put((int) attributeType.getClientId(), attributeType);
		}
	}
	
	/**
	 * Finds an {@code AttributeType} based on its name.<br>
	 * This method is case-insensitive.<br>
	 * It returns {@code null} if the name is not found or is {@code null}.
	 * @param attributeName The name of the attribute to search for.
	 * @return The matching {@code AttributeType} or {@code null}.
	 */
	public static AttributeType findByName(String attributeName)
	{
		return attributeName == null ? null : BY_NAME.get(attributeName.toLowerCase());
	}
	
	/**
	 * Finds an {@code AttributeType} based on its unique client ID.<br>
	 * This method searches the internal mapping for a matching value.
	 * @param clientId The unique identifier of the attribute to find.
	 * @return The corresponding {@code AttributeType} or {@code null} if not found.
	 */
	public static AttributeType findByClientId(int clientId)
	{
		return BY_CLIENT_ID.get(clientId);
	}
}
