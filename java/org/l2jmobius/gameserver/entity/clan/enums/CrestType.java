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
package org.l2jmobius.gameserver.entity.clan.enums;

import java.util.HashMap;
import java.util.Map;

/**
 * Types of clan crest (pledge, large pledge, ally).
 */
public enum CrestType
{
	PLEDGE(1),
	PLEDGE_LARGE(2),
	ALLY(3);
	
	private final int _id;
	
	/**
	 * Creates a crest type with the given id.
	 * @param id The crest type id.
	 */
	CrestType(int id)
	{
		_id = id;
	}
	
	/**
	 * Retrieves this crest type id.
	 * @return The id as an {@code int}.
	 */
	public int getId()
	{
		return _id;
	}
	
	private static final Map<Integer, CrestType> BY_ID = new HashMap<>(values().length);
	static
	{
		for (CrestType crestType : values())
		{
			BY_ID.put(crestType.getId(), crestType);
		}
	}
	
	/**
	 * Resolves a crest type from its id.
	 * @param id The crest type id.
	 * @return The matching {@code CrestType}, or {@code null} if none.
	 */
	public static CrestType getById(int id)
	{
		return BY_ID.get(id);
	}
}
