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
package org.l2jmobius.gameserver.network.enums;

import java.util.HashMap;
import java.util.Map;

/**
 * Betting types of the lucky game (fortune reading) minigame.
 * @author Mobius
 */
public enum LuckyGameBettingType
{
	/** Game played with Fortune Reading Tickets. */
	NORMAL(1),
	
	/** Game played with Luxury Fortune Reading Tickets. */
	PREMIUM(2),
	
	/** VIP game played with Fortune Reading Tickets. */
	VIP_NORMAL(3),
	
	/** VIP game played with Luxury Fortune Reading Tickets. */
	VIP_PREMIUM(4);
	
	private final int _clientId;
	
	private LuckyGameBettingType(int clientId)
	{
		_clientId = clientId;
	}
	
	/**
	 * @return the client id of this betting type
	 */
	public int getClientId()
	{
		return _clientId;
	}
	
	private static final Map<Integer, LuckyGameBettingType> BY_CLIENT_ID = new HashMap<>(values().length);
	static
	{
		for (LuckyGameBettingType type : values())
		{
			BY_CLIENT_ID.put(type.getClientId(), type);
		}
	}
	
	/**
	 * @param clientId the client id to look up
	 * @return the matching {@code LuckyGameBettingType}, or {@code null} when unknown
	 */
	public static LuckyGameBettingType findByClientId(int clientId)
	{
		return BY_CLIENT_ID.get(clientId);
	}
}
