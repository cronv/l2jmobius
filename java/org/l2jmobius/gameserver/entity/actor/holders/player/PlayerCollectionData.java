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
 * This class holds the collection data for a {@code Player}.<br>
 * It manages various items and objects gathered by the player during gameplay.
 * @author Berezkin Nikolay
 */
public class PlayerCollectionData
{
	private final int _collectionId;
	private final int _itemId;
	private final int _index;
	
	/**
	 * Creates a new instance of {@code PlayerCollectionData}.<br>
	 * This constructor initializes the data for a specific player item.
	 * @param collectionId The unique identifier for the collection.
	 * @param itemId The unique identifier for the item.
	 * @param index The position of the item within the collection.
	 */
	public PlayerCollectionData(int collectionId, int itemId, int index)
	{
		_collectionId = collectionId;
		_itemId = itemId;
		_index = index;
	}
	
	/**
	 * Retrieves the unique identifier for the collection.<br>
	 * This value is used to identify which group of items this data belongs to.
	 * @return The {@code int} ID of the collection.
	 */
	public int getCollectionId()
	{
		return _collectionId;
	}
	
	/**
	 * Retrieves the unique identifier for the item.<br>
	 * This value is stored within the {@code PlayerCollectionData} object.
	 * @return The {@code int} ID of the item.
	 */
	public int getItemId()
	{
		return _itemId;
	}
	
	/**
	 * Retrieves the current position of the item in the collection.<br>
	 * This value is used to identify where the item sits within its group.
	 * @return The integer index of the item.
	 */
	public int getIndex()
	{
		return _index;
	}
}
