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

import java.nio.file.Path;

/**
 * Immutable metadata for one top-level item declaration and its source file.
 * @author BazookaRpm
 */
public final class ItemMetadataRecord
{
	private final int _id;
	private final String _name;
	private final String _additionalName;
	private final String _type;
	private final Path _file;
	private final int _idOccurrence;
	
	public ItemMetadataRecord(int id, String name, String additionalName, String type, Path file, int idOccurrence)
	{
		_id = id;
		_name = name == null ? "" : name;
		_additionalName = additionalName == null ? "" : additionalName;
		_type = type == null ? "" : type;
		_file = file;
		_idOccurrence = idOccurrence;
	}
	
	public int getId()
	{
		return _id;
	}
	
	public String getName()
	{
		return _name;
	}
	
	public String getAdditionalName()
	{
		return _additionalName;
	}
	
	public String getType()
	{
		return _type;
	}
	
	public Path getFile()
	{
		return _file;
	}
	
	public int getIdOccurrence()
	{
		return _idOccurrence;
	}
	
	public boolean hasSameSourceIdentity(ItemMetadataRecord other)
	{
		return (other != null) && (_id == other._id) && (_idOccurrence == other._idOccurrence) && _file.equals(other._file);
	}
	
	@Override
	public String toString()
	{
		return _id + " - " + _name;
	}
}
