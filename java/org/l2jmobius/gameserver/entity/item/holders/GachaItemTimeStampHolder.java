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
package org.l2jmobius.gameserver.entity.item.holders;

import org.l2jmobius.gameserver.entity.item.enums.UniqueGachaRank;

/**
 * This class manages the timestamp for items obtained through a gacha system.<br>
 * It extends {@code GachaItemHolder} to provide specific tracking of when an item was acquired.
 */
public class GachaItemTimeStampHolder extends GachaItemHolder
{
	private final long _timeStamp;
	private boolean _storedInDatabase;
	
	/**
	 * Creates a new instance of {@code GachaItemTimeStampHolder}.<br>
	 * This constructor initializes the item data and its storage status.
	 * @param itemId The unique identifier for the item.
	 * @param itemCount The total number of items.
	 * @param enchantLevel The current level of enchantment.
	 * @param rank The {@code UniqueGachaRank} assigned to the item.
	 * @param timeStamp The system time when the item was created.
	 * @param stored A boolean indicating if the data is saved in the database.
	 */
	public GachaItemTimeStampHolder(int itemId, long itemCount, int enchantLevel, UniqueGachaRank rank, long timeStamp, boolean stored)
	{
		super(itemId, itemCount, 0, enchantLevel, rank);
		_timeStamp = timeStamp;
		_storedInDatabase = stored;
	}
	
	/**
	 * Retrieves the unique timestamp associated with this item.<br>
	 * This value is stored internally as a {@code long}.
	 * @return The current timestamp of the item.
	 */
	public long getTimeStamp()
	{
		return _timeStamp;
	}
	
	/**
	 * Calculates the difference between the current time and the item's timestamp.<br>
	 * The result represents how many seconds have passed since the item was received.<br>
	 * It converts both values to seconds before performing the subtraction.
	 * @return The number of seconds elapsed as an {@code int}.
	 */
	public int getTimeStampFromNow()
	{
		final long timeOfReceive = _timeStamp / 1000L;
		final long currentTime = System.currentTimeMillis() / 1000L;
		return (int) (currentTime - timeOfReceive);
	}
	
	/**
	 * Checks if the item status is currently saved in the database.<br>
	 * If it is not saved, this method updates the status to {@code true}.
	 * @return {@code true} if the status was already saved, or {@code false} if it was just updated.
	 */
	public boolean getStoredStatus()
	{
		if (_storedInDatabase)
		{
			return true;
		}
		
		_storedInDatabase = true;
		return false;
	}
}
