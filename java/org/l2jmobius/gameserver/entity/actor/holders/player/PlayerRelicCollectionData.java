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
package org.l2jmobius.gameserver.entity.actor.holders.player;

/**
 * This class stores the collection data for a player's relics.<br>
 * It manages the persistent state of all relics acquired by a {@code Player}.
 * @author CostyKiller
 */
public class PlayerRelicCollectionData
{
	private final int _relicCollectionId;
	private final int _relicId;
	private final int _relicLevel;
	private final int _index;
	
	/**
	 * Creates a new instance of {@code PlayerRelicCollectionData}.<br>
	 * This constructor initializes the data for a specific player relic.
	 * @param relicCollectionId The unique identifier for the relic collection.
	 * @param relicId The unique identifier for the specific relic.
	 * @param relicLevel The current level of the relic.
	 * @param index The position or order of the relic in the collection.
	 */
	public PlayerRelicCollectionData(int relicCollectionId, int relicId, int relicLevel, int index)
	{
		_relicCollectionId = relicCollectionId;
		_relicId = relicId;
		_relicLevel = relicLevel;
		_index = index;
	}
	
	/**
	 * Retrieves the unique identifier for the relic collection.<br>
	 * This value identifies which specific collection a relic belongs to.
	 * @return The {@code int} ID of the relic collection.
	 */
	public int getRelicCollectionId()
	{
		return _relicCollectionId;
	}
	
	/**
	 * Retrieves the unique identifier for the relic.<br>
	 * This value is used to identify which specific relic is being referenced.
	 * @return the {@code int} ID of the relic.
	 */
	public int getRelicId()
	{
		return _relicId;
	}
	
	/**
	 * Retrieves the current level of the relic.<br>
	 * This value is stored in the {@code _relicLevel} field.
	 * @return The integer level of the relic.
	 */
	public int getRelicLevel()
	{
		return _relicLevel;
	}
	
	/**
	 * Returns the current index of the relic.<br>
	 * This value is used to identify the position within a collection.
	 * @return The {@code int} representing the index.
	 */
	public int getIndex()
	{
		return _index;
	}
}
