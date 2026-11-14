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
package org.l2jmobius.gameserver.entity.groups;

import java.util.HashMap;
import java.util.Map;

/**
 * Defines the different types of party distribution logic.<br>
 * This enum is used to determine how members are organized within a group.<br>
 * It helps the server manage various party configurations.
 * @author NosBit, Mobius
 */
public enum PartyDistributionType
{
	FINDERS_KEEPERS(0, 487),
	RANDOM(1, 488),
	RANDOM_INCLUDING_SPOIL(2, 798),
	BY_TURN(3, 799),
	BY_TURN_INCLUDING_SPOIL(4, 800);
	
	private final int _id;
	private final int _sysStringId;
	
	/**
	 * Creates a new instance of {@code PartyDistributionType}.<br>
	 * This constructor initializes the internal ID and system string values.
	 * @param id The unique identifier used for network packets.
	 * @param sysStringId The identification number for the corresponding system string.
	 */
	private PartyDistributionType(int id, int sysStringId)
	{
		_id = id;
		_sysStringId = sysStringId;
	}
	
	/**
	 * Retrieves the unique identifier for this distribution type.<br>
	 * This value is used by packets to identify the specific type.
	 * @return the {@code int} id of the distribution type.
	 */
	public int getId()
	{
		return _id;
	}
	
	/**
	 * Retrieves the system string identifier for this distribution type.<br>
	 * This ID is used to look up localized text in the game files.
	 * @return the {@code int} value of the system string ID.
	 */
	public int getSysStringId()
	{
		return _sysStringId;
	}
	
	private static final Map<Integer, PartyDistributionType> BY_ID = new HashMap<>(values().length);
	static
	{
		for (PartyDistributionType partyDistributionType : values())
		{
			BY_ID.put(partyDistributionType.getId(), partyDistributionType);
		}
	}
	
	/**
	 * Finds a {@code PartyDistributionType} based on its unique ID.<br>
	 * This method searches the internal map for the matching value.
	 * @param id The unique identifier to search for.
	 * @return The corresponding {@code PartyDistributionType} or {@code null} if not found.
	 */
	public static PartyDistributionType findById(int id)
	{
		return BY_ID.get(id);
	}
}
