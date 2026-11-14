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
 * One unique scalar XML value exposed by the NPC editor.
 * @author BazookaRpm
 */
public final class NpcField
{
	/**
	 * Defines the scalar value categories supported by NPC field validation and editors.
	 */
	public enum ValueKind
	{
		TEXT,
		INTEGER,
		DECIMAL,
		BOOLEAN
	}
	
	private final String _key;
	private final String _tab;
	private final String _group;
	private final String _label;
	private final String _elementPath;
	private final String _attribute;
	private final ValueKind _kind;
	private final boolean _readOnly;
	private final String _originalValue;
	private String _value;
	
	public NpcField(String key, String tab, String group, String label, String elementPath, String attribute, ValueKind kind, boolean readOnly, String value)
	{
		_key = key;
		_tab = tab;
		_group = group;
		_label = label;
		_elementPath = elementPath;
		_attribute = attribute;
		_kind = kind;
		_readOnly = readOnly;
		_originalValue = value == null ? "" : value;
		_value = _originalValue;
	}
	
	public String getKey()
	{
		return _key;
	}
	
	public String getTab()
	{
		return _tab;
	}
	
	public String getGroup()
	{
		return _group;
	}
	
	public String getLabel()
	{
		return _label;
	}
	
	public String getElementPath()
	{
		return _elementPath;
	}
	
	public String getAttribute()
	{
		return _attribute;
	}
	
	public ValueKind getKind()
	{
		return _kind;
	}
	
	public boolean isReadOnly()
	{
		return _readOnly;
	}
	
	public String getOriginalValue()
	{
		return _originalValue;
	}
	
	public String getValue()
	{
		return _value;
	}
	
	public void setValue(String value)
	{
		if (!_readOnly)
		{
			_value = value == null ? "" : value;
		}
	}
	
	public boolean isChanged()
	{
		return !_originalValue.equals(_value);
	}
	
	public void revert()
	{
		_value = _originalValue;
	}
}
