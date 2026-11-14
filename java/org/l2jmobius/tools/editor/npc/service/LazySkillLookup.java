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
package org.l2jmobius.tools.editor.npc.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

import org.l2jmobius.tools.editor.npc.model.NpcSkillInfo;
import org.l2jmobius.tools.editor.npc.model.NpcSkillLookup;

/**
 * Resolves skill names and level ranges on demand from the active chronicle's range-organized skill XML files.
 * @author BazookaRpm
 */
public final class LazySkillLookup implements NpcSkillLookup
{
	private static final Pattern RANGE_FILE = Pattern.compile("(\\d{5})-(\\d{5})\\.xml", Pattern.CASE_INSENSITIVE);
	
	private final List<XmlFile> _files = new ArrayList<>();
	private final Map<Integer, NpcSkillInfo> _skills = new LinkedHashMap<>();
	private final Map<Integer, Integer> _sourceOrder = new HashMap<>();
	private boolean _fullyLoaded;
	
	public LazySkillLookup(Path skillRoot) throws Exception
	{
		final List<Path> paths = collectXmlFiles(skillRoot);
		for (int order = 0; order < paths.size(); order++)
		{
			final XmlFile file = new XmlFile(paths.get(order), order);
			_files.add(file);
			if (!file.hasRange())
			{
				loadFileChecked(file);
			}
		}
	}
	
	@Override
	public synchronized NpcSkillInfo getSkill(int skillId)
	{
		if (_fullyLoaded)
		{
			return _skills.get(skillId);
		}
		
		for (XmlFile file : _files)
		{
			if (file.contains(skillId))
			{
				loadFile(file);
			}
		}
		
		final NpcSkillInfo skill = _skills.get(skillId);
		if (skill != null)
		{
			return skill;
		}
		
		loadAll();
		return _skills.get(skillId);
	}
	
	@Override
	public synchronized Collection<NpcSkillInfo> getAllSkills()
	{
		loadAll();
		return Collections.unmodifiableCollection(_skills.values());
	}
	
	@Override
	public synchronized int getLoadedSkillCount()
	{
		return _skills.size();
	}
	
	@Override
	public int getFileCount()
	{
		return _files.size();
	}
	
	private void loadAll()
	{
		if (_fullyLoaded)
		{
			return;
		}
		
		for (XmlFile file : _files)
		{
			loadFile(file);
		}
		
		_fullyLoaded = true;
	}
	
	private void loadFile(XmlFile file)
	{
		if (file._loaded)
		{
			return;
		}
		
		try
		{
			loadFileChecked(file);
		}
		catch (Exception e)
		{
			throw new IllegalStateException("Could not load skill metadata from " + file._path + ".", e);
		}
	}
	
	private void loadFileChecked(XmlFile file) throws Exception
	{
		if (file._loaded)
		{
			return;
		}
		
		final XMLInputFactory factory = createFactory();
		try (InputStream input = Files.newInputStream(file._path))
		{
			final XMLStreamReader reader = factory.createXMLStreamReader(input);
			try
			{
				int depth = 0;
				while (reader.hasNext())
				{
					final int event = reader.next();
					if (event == XMLStreamConstants.START_ELEMENT)
					{
						depth++;
						if ((depth == 2) && "skill".equals(reader.getLocalName()))
						{
							loadSkill(reader, file);
						}
					}
					else if (event == XMLStreamConstants.END_ELEMENT)
					{
						depth--;
					}
				}
			}
			finally
			{
				reader.close();
			}
		}
		
		file._loaded = true;
	}
	
	private void loadSkill(XMLStreamReader reader, XmlFile file)
	{
		final String idValue = attribute(reader, "id");
		final String name = attribute(reader, "name");
		if ((idValue == null) || idValue.isBlank() || (name == null))
		{
			return;
		}
		
		final int id = parseInt(idValue, "skill id", file._path);
		int minLevel = 1;
		int maxLevel = 1;
		final String fromLevel = attribute(reader, "fromLevel");
		final String toLevel = attribute(reader, "toLevel");
		final String levels = attribute(reader, "levels");
		if ((fromLevel != null) && !fromLevel.isBlank())
		{
			minLevel = parseInt(fromLevel, "skill fromLevel", file._path);
		}
		
		if ((toLevel != null) && !toLevel.isBlank())
		{
			maxLevel = parseInt(toLevel, "skill toLevel", file._path);
		}
		else if ((levels != null) && !levels.isBlank())
		{
			maxLevel = parseInt(levels, "skill levels", file._path);
		}
		else
		{
			maxLevel = minLevel;
		}
		
		if (maxLevel < minLevel)
		{
			maxLevel = minLevel;
		}
		
		put(id, new NpcSkillInfo(id, name, minLevel, maxLevel), file._order);
	}
	
	private void put(int id, NpcSkillInfo skill, int order)
	{
		final Integer currentOrder = _sourceOrder.get(id);
		if ((currentOrder == null) || (order >= currentOrder.intValue()))
		{
			_skills.put(id, skill);
			_sourceOrder.put(id, Integer.valueOf(order));
		}
	}
	
	private static XMLInputFactory createFactory()
	{
		final XMLInputFactory factory = XMLInputFactory.newFactory();
		setProperty(factory, XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
		setProperty(factory, "javax.xml.stream.isSupportingExternalEntities", Boolean.FALSE);
		return factory;
	}
	
	private static void setProperty(XMLInputFactory factory, String name, Object value)
	{
		try
		{
			factory.setProperty(name, value);
		}
		catch (IllegalArgumentException e)
		{
			// Optional parser property; source access remains local-file only.
		}
	}
	
	private static String attribute(XMLStreamReader reader, String name)
	{
		for (int index = 0; index < reader.getAttributeCount(); index++)
		{
			if (name.equals(reader.getAttributeLocalName(index)))
			{
				return reader.getAttributeValue(index);
			}
		}
		
		return null;
	}
	
	private static List<Path> collectXmlFiles(Path root) throws IOException
	{
		final List<Path> files = new ArrayList<>();
		Files.walkFileTree(root, new SimpleFileVisitor<Path>()
		{
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attributes)
			{
				if (file.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".xml"))
				{
					files.add(file);
				}
				
				return FileVisitResult.CONTINUE;
			}
		});
		Collections.sort(files, new Comparator<Path>()
		{
			@Override
			public int compare(Path first, Path second)
			{
				return first.toString().compareToIgnoreCase(second.toString());
			}
		});
		
		return files;
	}
	
	private static int parseInt(String value, String field, Path file)
	{
		try
		{
			return Integer.parseInt(value);
		}
		catch (NumberFormatException e)
		{
			throw new IllegalArgumentException("Invalid " + field + " '" + value + "' in " + file, e);
		}
	}
	
	/**
	 * Describes one skill XML file and its optional filename ID range.
	 */
	private static final class XmlFile
	{
		private final Path _path;
		private final int _order;
		private final int _minId;
		private final int _maxId;
		private boolean _loaded;
		
		private XmlFile(Path path, int order)
		{
			_path = path;
			_order = order;
			final Matcher matcher = RANGE_FILE.matcher(path.getFileName().toString());
			if (matcher.matches())
			{
				_minId = Integer.parseInt(matcher.group(1));
				_maxId = Integer.parseInt(matcher.group(2));
			}
			else
			{
				_minId = -1;
				_maxId = -1;
			}
		}
		
		private boolean hasRange()
		{
			return _minId >= 0;
		}
		
		private boolean contains(int id)
		{
			return hasRange() && (id >= _minId) && (id <= _maxId);
		}
	}
}
