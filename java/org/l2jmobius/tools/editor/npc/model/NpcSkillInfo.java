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
package org.l2jmobius.tools.editor.npc.model;

/**
 * Skill metadata loaded directly from the active chronicle's skill XML.
 * @author BazookaRpm
 */
public final class NpcSkillInfo
{
	private final int _id;
	private final String _name;
	private final int _minLevel;
	private final int _maxLevel;
	
	public NpcSkillInfo(int id, String name, int minLevel, int maxLevel)
	{
		_id = id;
		_name = name;
		_minLevel = minLevel;
		_maxLevel = maxLevel;
	}
	
	public int getId()
	{
		return _id;
	}
	
	public String getName()
	{
		return _name;
	}
	
	public int getMinLevel()
	{
		return _minLevel;
	}
	
	public int getMaxLevel()
	{
		return _maxLevel;
	}
	
	public boolean supportsLevel(int level)
	{
		return (level >= _minLevel) && (level <= _maxLevel);
	}
}
