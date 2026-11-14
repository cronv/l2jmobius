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
package org.l2jmobius.gameserver.data.holders;

/**
 * This class acts as a data holder for information regarding a player's relic collection.<br>
 * It stores the necessary state to manage and retrieve collected relics within the game.
 * @author Brado
 */
public class RelicCollectionInfoHolder
{
	private final int _id;
	private final int _enchant;
	
	/**
	 * Creates a new instance of {@code RelicCollectionInfoHolder}.<br>
	 * This constructor initializes the relic data.
	 * @param id The unique identifier for the relic.
	 * @param enchant The enchantment level of the relic.
	 */
	public RelicCollectionInfoHolder(int id, int enchant)
	{
		_id = id;
		_enchant = enchant;
	}
	
	/**
	 * Retrieves the unique identifier for the relic.<br>
	 * This value is stored in the {@code _id} field.
	 * @return The {@code int} ID of the relic.
	 */
	public int getRelicId()
	{
		return _id;
	}
	
	/**
	 * Retrieves the current enchantment level of the relic.<br>
	 * This value is stored in the {@code _enchant} field.
	 * @return The integer value representing the enchantment level.
	 */
	public int getEnchantLevel()
	{
		return _enchant;
	}
}
