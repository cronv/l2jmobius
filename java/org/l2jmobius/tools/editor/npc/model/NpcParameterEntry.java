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
 * Editable string parameter from an NPC {@code parameters} block.
 * @author BazookaRpm
 */
public final class NpcParameterEntry
{
	private final int _originalIndex;
	private final String _originalName;
	private final String _originalValue;
	private String _name;
	private String _value;
	private boolean _removed;
	
	public NpcParameterEntry(int originalIndex, String name, String value)
	{
		_originalIndex = originalIndex;
		_originalName = name;
		_originalValue = value;
		_name = name;
		_value = value;
	}
	
	public static NpcParameterEntry create(String name, String value)
	{
		return new NpcParameterEntry(-1, name, value);
	}
	
	public int getOriginalIndex()
	{
		return _originalIndex;
	}
	
	public String getName()
	{
		return _name;
	}
	
	public void setName(String name)
	{
		_name = name;
	}
	
	public String getValue()
	{
		return _value;
	}
	
	public void setValue(String value)
	{
		_value = value;
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
		return isNew() || _removed || !_name.equals(_originalName) || !_value.equals(_originalValue);
	}
	
	public void revert()
	{
		_name = _originalName;
		_value = _originalValue;
		_removed = false;
	}
}
