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
 * Loaded editable state for one NPC definition.
 * @author BazookaRpm
 */
public final class NpcDefinition
{
	private final NpcRecord _npc;
	private final List<NpcField> _fields;
	private final NpcStructuredData _structuredData;
	private final List<NpcReferenceSection> _references;
	private String _sourceSha256;
	
	public NpcDefinition(NpcRecord npc, List<NpcField> fields, NpcStructuredData structuredData, List<NpcReferenceSection> references, String sourceSha256)
	{
		_npc = npc;
		_fields = new ArrayList<>(fields);
		_structuredData = structuredData;
		_references = new ArrayList<>(references);
		_sourceSha256 = sourceSha256;
	}
	
	public NpcRecord getNpc()
	{
		return _npc;
	}
	
	public List<NpcField> getFields()
	{
		return Collections.unmodifiableList(_fields);
	}
	
	public NpcStructuredData getStructuredData()
	{
		return _structuredData;
	}
	
	public List<NpcReferenceSection> getReferences()
	{
		return Collections.unmodifiableList(_references);
	}
	
	public String getSourceSha256()
	{
		return _sourceSha256;
	}
	
	public void setSourceSha256(String sourceSha256)
	{
		_sourceSha256 = sourceSha256;
	}
	
	public boolean isDirty()
	{
		for (NpcField field : _fields)
		{
			if (field.isChanged())
			{
				return true;
			}
		}
		
		return _structuredData.isDirty();
	}
	
	public int getChangedCount()
	{
		int count = _structuredData.getChangedCount();
		for (NpcField field : _fields)
		{
			if (field.isChanged())
			{
				count++;
			}
		}
		
		return count;
	}
	
	public void revert()
	{
		for (NpcField field : _fields)
		{
			field.revert();
		}
		
		_structuredData.revert();
	}
}
