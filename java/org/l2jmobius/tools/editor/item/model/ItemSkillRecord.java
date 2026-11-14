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
 * One existing item skill reference from skills or unequip_skills.
 * @author BazookaRpm
 */
public final class ItemSkillRecord
{
	private final String _container;
	private final int _ordinal;
	private final int _id;
	private final int _level;
	private final int _subLevel;
	private final String _type;
	private final String _chance;
	private final String _value;
	
	public ItemSkillRecord(String container, int ordinal, int id, int level, int subLevel, String type, String chance, String value)
	{
		_container = container;
		_ordinal = ordinal;
		_id = id;
		_level = level;
		_subLevel = subLevel;
		_type = type == null ? "NORMAL" : type;
		_chance = chance == null ? "" : chance;
		_value = value == null ? "" : value;
	}
	
	public String getContainer()
	{
		return _container;
	}
	
	public int getOrdinal()
	{
		return _ordinal;
	}
	
	public int getId()
	{
		return _id;
	}
	
	public int getLevel()
	{
		return _level;
	}
	
	public int getSubLevel()
	{
		return _subLevel;
	}
	
	public String getType()
	{
		return _type;
	}
	
	public String getChance()
	{
		return _chance;
	}
	
	public String getValue()
	{
		return _value;
	}
}
