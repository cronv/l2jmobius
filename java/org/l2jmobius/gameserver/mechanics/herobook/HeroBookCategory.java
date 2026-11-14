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
package org.l2jmobius.gameserver.mechanics.herobook;

import java.util.HashMap;
import java.util.Map;

/**
 * Defines the different categories for {@code HeroBook} items.<br>
 * This enumeration is used to group and organize various hero books within the game mechanics.
 * @author Atronic, Mobius
 */
public enum HeroBookCategory
{
	TALES_OF_HERO(1),
	WANDERINGS(2);
	
	private final int id;
	
	/**
	 * Creates a new {@code HeroBookCategory} instance.<br>
	 * This constructor assigns an internal ID to the category.
	 * @param id The unique identifier for the category.
	 */
	HeroBookCategory(int id)
	{
		this.id = id;
	}
	
	/**
	 * Retrieves the unique identifier for this {@code HeroBookCategory}.<br>
	 * This value is used to map categories to their database entries.
	 * @return The integer {@code id} associated with this category.
	 */
	public int getId()
	{
		return id;
	}
	
	private static final Map<Integer, HeroBookCategory> BY_ID = new HashMap<>(values().length);
	static
	{
		for (HeroBookCategory c : values())
		{
			BY_ID.put(c.id, c);
		}
	}
	
	/**
	 * Creates a {@code HeroBookCategory} based on the provided ID.<br>
	 * If the ID is not found, it returns {@code TALES_OF_HERO}.
	 * @param id The unique identifier for the category.
	 * @return The corresponding {@code HeroBookCategory} or a default value.
	 */
	public static HeroBookCategory of(int id)
	{
		// Fallback to main category to preserve behavior.
		return BY_ID.getOrDefault(id, TALES_OF_HERO);
	}
}