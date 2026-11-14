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
 * This class acts as a data holder for attendance-related items.<br>
 * It stores information regarding rewards granted to players based on their login history.
 * @author Lonely
 */
public class AttendanceItemHolder
{
	private final int _itemId;
	private final int _itemCount;
	private final int _highlight;
	
	/**
	 * Creates a new {@code AttendanceItemHolder} instance.<br>
	 * This constructor initializes the item data for an attendance reward.
	 * @param id The unique identifier of the item.
	 * @param count The total quantity of the item.
	 * @param highlight The visual highlight value for the item.
	 */
	public AttendanceItemHolder(int id, int count, int highlight)
	{
		_itemId = id;
		_itemCount = count;
		_highlight = highlight;
	}
	
	/**
	 * Retrieves the unique identifier for this attendance item.<br>
	 * This value corresponds to the {@code id} provided during construction.
	 * @return The {@code int} ID of the item.
	 */
	public int getItemId()
	{
		return _itemId;
	}
	
	/**
	 * Returns the total number of items.<br>
	 * This value is retrieved from the internal {@code _itemCount} field.
	 * @return The current count of items as an {@code int}.
	 */
	public int getItemCount()
	{
		return _itemCount;
	}
	
	/**
	 * Retrieves the highlight value for this item.<br>
	 * This value determines how the item is displayed in the UI.
	 * @return The {@code int} highlight value.
	 */
	public int getHighlight()
	{
		return _highlight;
	}
}
