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
package org.l2jmobius.tools.editor.drop.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
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

import org.l2jmobius.tools.editor.drop.model.ItemLookup;
import org.l2jmobius.tools.editor.drop.model.ItemRecord;

/**
 * Resolves item names on demand by loading only the XML range files needed by the current Drop Editor session.
 * @author BazookaRpm
 */
public final class LazyItemLookup implements ItemLookup
{
	private static final Pattern RANGE_FILE = Pattern.compile("(\\d{5})-(\\d{5})\\.xml", Pattern.CASE_INSENSITIVE);
	
	private final List<XmlFile> _files = new ArrayList<>();
	private final Map<Integer, ItemRecord> _items = new LinkedHashMap<>();
	private final Map<Integer, Integer> _sourceOrder = new HashMap<>();
	private boolean _fullyLoaded;
	
	public LazyItemLookup(Path itemRoot) throws Exception
	{
		final List<Path> paths = collectXmlFiles(itemRoot);
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
	public synchronized ItemRecord getItem(int itemId)
	{
		if (_fullyLoaded)
		{
			return _items.get(itemId);
		}
		
		for (XmlFile file : _files)
		{
			if (file.contains(itemId))
			{
				loadFile(file);
			}
		}
		
		final ItemRecord item = _items.get(itemId);
		if (item != null)
		{
			return item;
		}
		
		loadAll();
		return _items.get(itemId);
	}
	
	@Override
	public synchronized Map<Integer, ItemRecord> getAllItems()
	{
		loadAll();
		return Collections.unmodifiableMap(_items);
	}
	
	@Override
	public synchronized int getLoadedItemCount()
	{
		return _items.size();
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
			throw new IllegalStateException("Could not load item metadata from " + file._path + ".", e);
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
						if ((depth == 2) && "item".equalsIgnoreCase(reader.getLocalName()))
						{
							final String idValue = attribute(reader, "id");
							if ((idValue != null) && !idValue.isBlank())
							{
								final int id = parseInt(idValue, "item id", file._path);
								put(id, new ItemRecord(id, attribute(reader, "name")), file._order);
							}
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
	
	private void put(int id, ItemRecord item, int order)
	{
		final Integer currentOrder = _sourceOrder.get(id);
		if ((currentOrder == null) || (order >= currentOrder.intValue()))
		{
			_items.put(id, item);
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
	 * Describes one item XML file and its optional filename ID range.
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
