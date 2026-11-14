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

import java.nio.file.Path;
import java.util.Locale;

/**
 * Indexed NPC metadata used for filtering and locating the source XML file.
 * @author BazookaRpm
 */
public final class NpcRecord
{
	private final int _id;
	private final int _level;
	private final String _type;
	private final String _name;
	private final String _title;
	private final Path _file;
	private final String _searchKey;
	
	public NpcRecord(int id, int level, String type, String name, String title, Path file)
	{
		_id = id;
		_level = level;
		_type = type == null ? "Folk" : type;
		_name = name == null ? "" : name;
		_title = title == null ? "" : title;
		_file = file;
		_searchKey = (id + " " + _name + " " + _title + " " + _type).toLowerCase(Locale.ROOT);
	}
	
	public int getId()
	{
		return _id;
	}
	
	public int getLevel()
	{
		return _level;
	}
	
	public String getType()
	{
		return _type;
	}
	
	public String getName()
	{
		return _name;
	}
	
	public String getTitle()
	{
		return _title;
	}
	
	public Path getFile()
	{
		return _file;
	}
	
	public boolean matchesSearch(String query)
	{
		return _searchKey.contains(query);
	}
	
	public boolean isQuestMonster()
	{
		return _title.contains("Quest");
	}
	
	public boolean isMonster()
	{
		return "Monster".equals(_type);
	}
	
	public boolean isRaidBoss()
	{
		return "RaidBoss".equals(_type);
	}
	
	public boolean isGrandBoss()
	{
		return "GrandBoss".equals(_type);
	}
}
