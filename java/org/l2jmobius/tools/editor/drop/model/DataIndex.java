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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Holds the NPC browser index and a lazy item metadata lookup used by the Drop Editor.
 * @author BazookaRpm
 */
public final class DataIndex
{
	private final ProjectLayout _layout;
	private final List<NpcRecord> _npcs = new ArrayList<>();
	private final Map<Integer, NpcRecord> _npcsById = new HashMap<>();
	private final Map<Integer, ItemRecord> _itemsById = new HashMap<>();
	private final ItemLookup _itemLookup;
	
	public DataIndex(ProjectLayout layout)
	{
		this(layout, null);
	}
	
	public DataIndex(ProjectLayout layout, ItemLookup itemLookup)
	{
		_layout = layout;
		_itemLookup = itemLookup;
	}
	
	public ProjectLayout getLayout()
	{
		return _layout;
	}
	
	public void addNpc(NpcRecord npc)
	{
		_npcs.add(npc);
		_npcsById.put(npc.getId(), npc);
	}
	
	public void addItem(ItemRecord item)
	{
		_itemsById.put(item.getId(), item);
	}
	
	public List<NpcRecord> getNpcs()
	{
		return Collections.unmodifiableList(_npcs);
	}
	
	public NpcRecord getNpc(int id)
	{
		return _npcsById.get(id);
	}
	
	public ItemRecord getItem(int id)
	{
		final ItemRecord eager = _itemsById.get(id);
		return eager != null ? eager : (_itemLookup == null ? null : _itemLookup.getItem(id));
	}
	
	public Map<Integer, ItemRecord> getItemsById()
	{
		if (_itemLookup == null)
		{
			return Collections.unmodifiableMap(_itemsById);
		}
		
		if (_itemsById.isEmpty())
		{
			return _itemLookup.getAllItems();
		}
		
		final Map<Integer, ItemRecord> combined = new HashMap<>(_itemLookup.getAllItems());
		combined.putAll(_itemsById);
		return Collections.unmodifiableMap(combined);
	}
	
	public int getLoadedItemCount()
	{
		return _itemsById.size() + (_itemLookup == null ? 0 : _itemLookup.getLoadedItemCount());
	}
	
	public int getItemFileCount()
	{
		return _itemLookup == null ? 0 : _itemLookup.getFileCount();
	}
}
