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
package org.l2jmobius.tools.editor.item.model;

/**
 * One existing capsuled item product entry.
 * @author BazookaRpm
 */
public final class ItemCapsuleRecord
{
	private final int _ordinal;
	private final int _itemId;
	private final String _min;
	private final String _max;
	private final String _chance;
	private final String _minEnchant;
	private final String _maxEnchant;
	
	public ItemCapsuleRecord(int ordinal, int itemId, String min, String max, String chance, String minEnchant, String maxEnchant)
	{
		_ordinal = ordinal;
		_itemId = itemId;
		_min = min == null ? "" : min;
		_max = max == null ? "" : max;
		_chance = chance == null ? "" : chance;
		_minEnchant = minEnchant == null ? "" : minEnchant;
		_maxEnchant = maxEnchant == null ? "" : maxEnchant;
	}
	
	public int getOrdinal()
	{
		return _ordinal;
	}
	
	public int getItemId()
	{
		return _itemId;
	}
	
	public String getMin()
	{
		return _min;
	}
	
	public String getMax()
	{
		return _max;
	}
	
	public String getChance()
	{
		return _chance;
	}
	
	public String getMinEnchant()
	{
		return _minEnchant;
	}
	
	public String getMaxEnchant()
	{
		return _maxEnchant;
	}
}
