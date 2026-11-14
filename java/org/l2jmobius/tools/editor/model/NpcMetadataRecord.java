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
package org.l2jmobius.tools.editor.model;

import java.nio.file.Path;

/**
 * Immutable NPC metadata shared by offline datapack editors after one streaming NPC scan.
 * @author BazookaRpm
 */
public final class NpcMetadataRecord
{
	private final int _id;
	private final Integer _level;
	private final String _type;
	private final String _name;
	private final String _title;
	private final String _race;
	private final String _sex;
	private final Path _file;
	
	public NpcMetadataRecord(int id, Integer level, String type, String name, String title, String race, String sex, Path file)
	{
		_id = id;
		_level = level;
		_type = type == null ? "" : type;
		_name = name == null ? "" : name;
		_title = title == null ? "" : title;
		_race = race == null ? "" : race;
		_sex = sex == null ? "" : sex;
		_file = file;
	}
	
	public int getId()
	{
		return _id;
	}
	
	public int getLevel(int defaultValue)
	{
		return _level == null ? defaultValue : _level.intValue();
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
	
	public String getRace()
	{
		return _race;
	}
	
	public String getSex()
	{
		return _sex;
	}
	
	public Path getFile()
	{
		return _file;
	}
}
