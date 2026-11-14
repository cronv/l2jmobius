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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Describes one existing item &lt;set&gt; property and the safe editor metadata associated with it.
 * @author BazookaRpm
 */
public final class ItemSetProperty
{
	public enum Category
	{
		GENERAL,
		EQUIPMENT,
		TYPE_SPECIFIC,
		FLAGS,
		OTHER
	}
	
	public enum Kind
	{
		TEXT,
		INTEGER,
		LONG,
		BOOLEAN,
		ENUM,
		INTEGER_PAIR,
		DAMAGE_RANGE
	}
	
	private final int _ordinal;
	private final String _name;
	private final String _value;
	private final Category _category;
	private final Kind _kind;
	private final List<String> _options;
	
	public ItemSetProperty(int ordinal, String name, String value, Category category, Kind kind, List<String> options)
	{
		_ordinal = ordinal;
		_name = name == null ? "" : name;
		_value = value == null ? "" : value;
		_category = category == null ? Category.OTHER : category;
		_kind = kind == null ? Kind.TEXT : kind;
		_options = Collections.unmodifiableList(new ArrayList<>(options == null ? Collections.emptyList() : options));
	}
	
	public int getOrdinal()
	{
		return _ordinal;
	}
	
	public String getName()
	{
		return _name;
	}
	
	public String getValue()
	{
		return _value;
	}
	
	public Category getCategory()
	{
		return _category;
	}
	
	public Kind getKind()
	{
		return _kind;
	}
	
	public List<String> getOptions()
	{
		return _options;
	}
}
