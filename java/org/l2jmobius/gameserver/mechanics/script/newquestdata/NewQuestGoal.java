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
package org.l2jmobius.gameserver.mechanics.script.newquestdata;

/**
 * Objective a player must fulfill to progress through a quest.
 * @author Magik
 */
public class NewQuestGoal
{
	private final int _itemId;
	private final int _count;
	private final String _goalMessage;
	
	/**
	 * Creates a goal with the required item, quantity and player message.
	 * @param itemId The required item id.
	 * @param count The required item count.
	 * @param goalMessage The text shown to the player.
	 */
	public NewQuestGoal(int itemId, int count, String goalMessage)
	{
		_itemId = itemId;
		_count = count;
		_goalMessage = goalMessage;
	}
	
	/**
	 * Retrieves the required item id.
	 * @return The item id as an {@code int}.
	 */
	public int getItemId()
	{
		return _itemId;
	}
	
	/**
	 * Retrieves the required item count.
	 * @return The count as an {@code int}.
	 */
	public int getCount()
	{
		return _count;
	}
	
	/**
	 * Retrieves the player-facing goal message.
	 * @return The message as a {@code String}.
	 */
	public String getMessage()
	{
		return _goalMessage;
	}
}
