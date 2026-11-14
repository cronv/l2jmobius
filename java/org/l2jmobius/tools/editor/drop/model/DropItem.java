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
package org.l2jmobius.tools.editor.drop.model;

/**
 * Represents one editable item entry from an NPC drop list.
 * @author BazookaRpm
 */
public final class DropItem
{
	private int _itemId;
	private String _min;
	private String _max;
	private String _chance;
	private String _minLevel;
	private String _maxLevel;
	private String _dailyLimit;
	private String _comment;
	
	public DropItem(int itemId, String min, String max, String chance)
	{
		_itemId = itemId;
		_min = min;
		_max = max;
		_chance = chance;
	}
	
	public DropItem copy()
	{
		final DropItem copy = new DropItem(_itemId, _min, _max, _chance);
		copy._minLevel = _minLevel;
		copy._maxLevel = _maxLevel;
		copy._dailyLimit = _dailyLimit;
		copy._comment = _comment;
		return copy;
	}
	
	public int getItemId()
	{
		return _itemId;
	}
	
	public void setItemId(int itemId)
	{
		_itemId = itemId;
	}
	
	public String getMin()
	{
		return _min;
	}
	
	public void setMin(String min)
	{
		_min = min;
	}
	
	public String getMax()
	{
		return _max;
	}
	
	public void setMax(String max)
	{
		_max = max;
	}
	
	public String getChance()
	{
		return _chance;
	}
	
	public void setChance(String chance)
	{
		_chance = chance;
	}
	
	public String getMinLevel()
	{
		return _minLevel;
	}
	
	public void setMinLevel(String minLevel)
	{
		_minLevel = minLevel;
	}
	
	public String getMaxLevel()
	{
		return _maxLevel;
	}
	
	public void setMaxLevel(String maxLevel)
	{
		_maxLevel = maxLevel;
	}
	
	public String getDailyLimit()
	{
		return _dailyLimit;
	}
	
	public void setDailyLimit(String dailyLimit)
	{
		_dailyLimit = dailyLimit;
	}
	
	public String getComment()
	{
		return _comment;
	}
	
	public void setComment(String comment)
	{
		_comment = comment;
	}
}
