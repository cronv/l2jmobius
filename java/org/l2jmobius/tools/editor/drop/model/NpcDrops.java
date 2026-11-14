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

import java.util.ArrayList;
import java.util.List;

/**
 * Mutable drop model for one NPC together with its source-file fingerprint.
 * @author BazookaRpm
 */
public final class NpcDrops
{
	private final int _npcId;
	private final List<DropSection> _sections = new ArrayList<>();
	private String _sourceSha256;
	
	public NpcDrops(int npcId)
	{
		_npcId = npcId;
	}
	
	public int getNpcId()
	{
		return _npcId;
	}
	
	public List<DropSection> getSections()
	{
		return _sections;
	}
	
	public String getSourceSha256()
	{
		return _sourceSha256;
	}
	
	public void setSourceSha256(String sourceSha256)
	{
		_sourceSha256 = sourceSha256;
	}
	
	public DropSection getOrCreateSection(DropType type)
	{
		for (DropSection section : _sections)
		{
			if (section.getType() == type)
			{
				return section;
			}
		}
		
		final DropSection section = new DropSection(type);
		_sections.add(section);
		return section;
	}
	
	public void removeEmptySections()
	{
		for (int i = _sections.size() - 1; i >= 0; i--)
		{
			if (_sections.get(i).isEmpty())
			{
				_sections.remove(i);
			}
		}
	}
}
