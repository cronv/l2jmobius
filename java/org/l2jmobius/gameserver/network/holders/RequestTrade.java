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
package org.l2jmobius.gameserver.network.holders;

/**
 * Represents a network request to initiate a trade between two players.<br>
 * This class holds the necessary data for the {@code RequestTrade} packet.
 */
public class RequestTrade
{
	int _objectId;
	int _itemId;
	long _count;
	long _price;
	
	/**
	 * Creates a new {@code RequestTrade} instance.<br>
	 * This constructor initializes the trade with an ID, quantity, and cost.
	 * @param objectId The unique identifier for the object.
	 * @param count The total number of items in the trade.
	 * @param price The total price of the trade.
	 */
	public RequestTrade(int objectId, long count, long price)
	{
		_objectId = objectId;
		_count = count;
		_price = price;
	}
	
	/**
	 * Creates a new {@code RequestTrade} object.<br>
	 * This constructor initializes all trade details.
	 * @param objectId The unique identifier for the object.
	 * @param itemId The unique identifier for the item.
	 * @param count The total number of items in the trade.
	 * @param price The cost associated with the trade.
	 */
	public RequestTrade(int objectId, int itemId, long count, long price)
	{
		_objectId = objectId;
		_itemId = itemId;
		_count = count;
		_price = price;
	}
	
	/**
	 * Retrieves the unique identifier for the object.<br>
	 * This value is stored in the {@code _objectId} field.
	 * @return The integer ID of the object.
	 */
	public int getObjectId()
	{
		return _objectId;
	}
	
	/**
	 * Retrieves the unique identifier for the item.<br>
	 * This value is used to identify which specific item is being traded.
	 * @return The {@code int} ID of the item.
	 */
	public int getItemId()
	{
		return _itemId;
	}
	
	/**
	 * Updates the quantity of the item.<br>
	 * This method sets the {@code _count} field to the provided value.
	 * @param count The new number of items to set.
	 */
	public void setCount(long count)
	{
		_count = count;
	}
	
	/**
	 * Retrieves the quantity of the item.<br>
	 * This value is stored in the {@code _count} field.
	 * @return The number of items requested.
	 */
	public long getCount()
	{
		return _count;
	}
	
	/**
	 * Retrieves the price of the trade.<br>
	 * This value is stored in the {@code _price} field.
	 * @return The current price as a {@code long}.
	 */
	public long getPrice()
	{
		return _price;
	}
	
	/**
	 * Returns a hash code value for this object.<br>
	 * This value is primarily based on the {@code _objectId} field.<br>
	 * It is used to determine equality in collections like {@code HashSet}.
	 * @return The integer hash code of this object.
	 */
	@Override
	public int hashCode()
	{
		return _objectId;
	}
	
	/**
	 * Compares this {@code RequestTrade} object with another object for equality.<br>
	 * It checks if the two objects are the same instance or have matching values.
	 * @param obj The object to compare this instance against.
	 * @return {@code true} if the objects are equal, otherwise {@code false}.
	 */
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
		{
			return true;
		}
		
		if (!(obj instanceof RequestTrade))
		{
			return false;
		}
		
		return (_objectId != ((RequestTrade) obj)._objectId);
	}
}
