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
 * Editable named minion set from an NPC {@code parameters} block.
 * @author BazookaRpm
 */
public final class NpcMinionSet
{
	private final int _originalIndex;
	private final String _originalName;
	private final List<NpcMinionEntry> _minions = new ArrayList<>();
	private String _name;
	private boolean _removed;
	
	public NpcMinionSet(int originalIndex, String name)
	{
		_originalIndex = originalIndex;
		_originalName = name;
		_name = name;
	}
	
	public static NpcMinionSet create(String name)
	{
		return new NpcMinionSet(-1, name);
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
	
	public void addLoadedMinion(NpcMinionEntry minion)
	{
		_minions.add(minion);
	}
	
	public void addMinion(NpcMinionEntry minion)
	{
		_minions.add(minion);
	}
	
	public void removeMinion(NpcMinionEntry minion)
	{
		if (minion.isNew())
		{
			_minions.remove(minion);
		}
		else
		{
			minion.setRemoved(true);
		}
	}
	
	public List<NpcMinionEntry> getMinions()
	{
		final List<NpcMinionEntry> result = new ArrayList<>();
		for (NpcMinionEntry minion : _minions)
		{
			if (!minion.isRemoved())
			{
				result.add(minion);
			}
		}
		
		return Collections.unmodifiableList(result);
	}
	
	public List<NpcMinionEntry> getAllMinions()
	{
		return Collections.unmodifiableList(_minions);
	}
	
	public boolean isChanged()
	{
		if (isNew() || _removed || !_name.equals(_originalName))
		{
			return true;
		}
		
		for (NpcMinionEntry minion : _minions)
		{
			if (minion.isChanged())
			{
				return true;
			}
		}
		
		return false;
	}
	
	public int getChangedCount()
	{
		int count = (isNew() || _removed || !_name.equals(_originalName)) ? 1 : 0;
		for (NpcMinionEntry minion : _minions)
		{
			if (minion.isChanged())
			{
				count++;
			}
		}
		
		return count;
	}
	
	public void revert()
	{
		_name = _originalName;
		_removed = false;
		for (int index = _minions.size() - 1; index >= 0; index--)
		{
			if (_minions.get(index).isNew())
			{
				_minions.remove(index);
			}
		}
		
		for (NpcMinionEntry minion : _minions)
		{
			minion.revert();
		}
	}
}
