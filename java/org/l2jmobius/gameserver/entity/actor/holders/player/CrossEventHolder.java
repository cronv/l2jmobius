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
 * This class holds data related to cross-server events for a player.<br>
 * It stores specific state information required to manage participation in these events.
 * @author Smoto
 */
public class CrossEventHolder
{
	private final int _cellId;
	private int _horizontal;
	private int _vertical;
	
	/**
	 * Creates a new instance of {@code CrossEventHolder}.<br>
	 * This constructor initializes the position data for an event.
	 * @param cellId The unique identifier for the cell.
	 * @param horizontal The horizontal coordinate value.
	 * @param vertical The vertical coordinate value.
	 */
	public CrossEventHolder(int cellId, int horizontal, int vertical)
	{
		_cellId = cellId;
		_vertical = vertical;
		_horizontal = horizontal;
	}
	
	/**
	 * Creates a new {@code CrossEventHolder} instance.<br>
	 * This constructor initializes the internal coordinates based on the provided cell ID.<br>
	 * The valid range for the input is from {@code 0} to {@code 15}.
	 * @param cellId The unique identifier for the cell.
	 */
	public CrossEventHolder(int cellId)
	{
		_cellId = cellId;
		switch (cellId)
		{
			case 0:
			{
				_vertical = 0;
				_horizontal = 0;
				break;
			}
			case 1:
			{
				_vertical = 0;
				_horizontal = 1;
				break;
			}
			case 2:
			{
				_vertical = 0;
				_horizontal = 2;
				break;
			}
			case 3:
			{
				_vertical = 0;
				_horizontal = 3;
				break;
			}
			case 4:
			{
				_vertical = 1;
				_horizontal = 0;
				break;
			}
			case 5:
			{
				_vertical = 1;
				_horizontal = 1;
				break;
			}
			case 6:
			{
				_vertical = 1;
				_horizontal = 2;
				break;
			}
			case 7:
			{
				_vertical = 1;
				_horizontal = 3;
				break;
			}
			case 8:
			{
				_vertical = 2;
				_horizontal = 0;
				break;
			}
			case 9:
			{
				_vertical = 2;
				_horizontal = 1;
				break;
			}
			case 10:
			{
				_vertical = 2;
				_horizontal = 2;
				break;
			}
			case 11:
			{
				_vertical = 2;
				_horizontal = 3;
				break;
			}
			case 12:
			{
				_vertical = 3;
				_horizontal = 0;
				break;
			}
			case 13:
			{
				_vertical = 3;
				_horizontal = 1;
				break;
			}
			case 14:
			{
				_vertical = 3;
				_horizontal = 2;
				break;
			}
			case 15:
			{
				_vertical = 3;
				_horizontal = 3;
				break;
			}
		}
	}
	
	/**
	 * Retrieves the unique identifier for this cell.<br>
	 * This value is assigned when the {@code CrossEventHolder} is created.
	 * @return The integer ID of the cell.
	 */
	public int cellId()
	{
		return _cellId;
	}
	
	/**
	 * Retrieves the vertical coordinate of the cross event.<br>
	 * This value represents the position on the vertical axis.
	 * @return The {@code int} value of the vertical coordinate.
	 */
	public int getVertical()
	{
		return _vertical;
	}
	
	/**
	 * Retrieves the horizontal position of the cross event.<br>
	 * This value is stored in the {@code _horizontal} field.
	 * @return The current horizontal coordinate as an {@code int}.
	 */
	public int getHorizontal()
	{
		return _horizontal;
	}
}
