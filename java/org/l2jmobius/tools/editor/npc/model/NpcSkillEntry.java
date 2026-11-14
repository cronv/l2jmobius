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
 * Editable entry from an NPC {@code skillList}.
 * @author BazookaRpm
 */
public final class NpcSkillEntry
{
	private final int _originalIndex;
	private final int _originalId;
	private final int _originalLevel;
	private int _id;
	private int _level;
	private boolean _removed;
	
	public NpcSkillEntry(int originalIndex, int id, int level)
	{
		_originalIndex = originalIndex;
		_originalId = id;
		_originalLevel = level;
		_id = id;
		_level = level;
	}
	
	public static NpcSkillEntry create(int id, int level)
	{
		return new NpcSkillEntry(-1, id, level);
	}
	
	public int getOriginalIndex()
	{
		return _originalIndex;
	}
	
	public int getOriginalId()
	{
		return _originalId;
	}
	
	public int getId()
	{
		return _id;
	}
	
	public void setId(int id)
	{
		_id = id;
	}
	
	public int getLevel()
	{
		return _level;
	}
	
	public void setLevel(int level)
	{
		_level = level;
	}
	
	public boolean isNew()
	{
		return _originalIndex < 0;
	}
	
	public boolean isRemoved()
	{
		return _removed;
	}
	
	public void setRemoved(boolean removed)
	{
		_removed = removed;
	}
	
	public boolean isChanged()
	{
		return isNew() || _removed || (_id != _originalId) || (_level != _originalLevel);
	}
	
	public void revert()
	{
		_id = _originalId;
		_level = _originalLevel;
		_removed = false;
	}
}
