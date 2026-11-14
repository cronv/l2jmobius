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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Immutable item metadata snapshot with O(1) lookup by item ID and cache statistics.
 * @author BazookaRpm
 */
public final class ItemMetadataIndex
{
	private final List<ItemMetadataRecord> _records;
	private final Map<Integer, ItemMetadataRecord> _recordsById;
	private final int _fileCount;
	private final int _cachedFileCount;
	
	public ItemMetadataIndex(List<ItemMetadataRecord> records, int fileCount, int cachedFileCount)
	{
		_records = Collections.unmodifiableList(new ArrayList<>(records));
		final Map<Integer, ItemMetadataRecord> byId = new LinkedHashMap<>();
		for (ItemMetadataRecord record : _records)
		{
			byId.put(record.getId(), record);
		}
		
		_recordsById = Collections.unmodifiableMap(byId);
		_fileCount = fileCount;
		_cachedFileCount = cachedFileCount;
	}
	
	public List<ItemMetadataRecord> getRecords()
	{
		return _records;
	}
	
	public boolean contains(int itemId)
	{
		return _recordsById.containsKey(itemId);
	}
	
	public int size()
	{
		return _recordsById.size();
	}
	
	public int getFileCount()
	{
		return _fileCount;
	}
	
	public int getCachedFileCount()
	{
		return _cachedFileCount;
	}
}
