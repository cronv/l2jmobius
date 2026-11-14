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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Read-only repeated XML data displayed without duplicating it as editable scalar controls.
 * @author BazookaRpm
 */
public final class NpcReferenceSection
{
	private final String _title;
	private final String[] _columns;
	private final List<String[]> _rows = new ArrayList<>();
	
	public NpcReferenceSection(String title, String... columns)
	{
		_title = title;
		_columns = columns.clone();
	}
	
	public void addRow(String... values)
	{
		_rows.add(values.clone());
	}
	
	public String getTitle()
	{
		return _title;
	}
	
	public String[] getColumns()
	{
		return _columns.clone();
	}
	
	public List<String[]> getRows()
	{
		return Collections.unmodifiableList(_rows);
	}
}
