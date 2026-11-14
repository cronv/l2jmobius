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
package org.l2jmobius.tools.editor.item.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

import org.l2jmobius.tools.editor.item.model.ItemMetadataIndex;
import org.l2jmobius.tools.editor.item.model.ItemMetadataRecord;

/**
 * Caches per-file item metadata and reparses only XML files changed on disk.
 * @author BazookaRpm
 */
public final class ItemMetadataCache
{
	private static final Map<Path, RootEntry> CACHE = new HashMap<>();
	
	private ItemMetadataCache()
	{
	}
	
	public static synchronized ItemMetadataIndex load(Path itemRoot) throws Exception
	{
		final RootEntry rootEntry = refreshRoot(normalizeRoot(itemRoot));
		final List<ItemMetadataRecord> records = new ArrayList<>();
		final XMLInputFactory factory = createFactory();
		int cachedFileCount = 0;
		for (Path file : rootEntry._paths)
		{
			final BasicFileAttributes attributes = Files.readAttributes(file, BasicFileAttributes.class);
			FileEntry fileEntry = rootEntry._files.get(file);
			if ((fileEntry != null) && fileEntry.matches(attributes))
			{
				cachedFileCount++;
			}
			else
			{
				fileEntry = new FileEntry(attributes.size(), attributes.lastModifiedTime().toMillis(), parseFile(file, factory));
				rootEntry._files.put(file, fileEntry);
			}
			
			records.addAll(fileEntry._records);
		}
		
		return new ItemMetadataIndex(records, rootEntry._paths.size(), cachedFileCount);
	}
	
	private static Path normalizeRoot(Path itemRoot)
	{
		if (itemRoot == null)
		{
			throw new IllegalArgumentException("An item root path is required.");
		}
		
		return itemRoot.toAbsolutePath().normalize();
	}
	
	private static RootEntry refreshRoot(Path root) throws IOException
	{
		final List<Path> paths = collectXmlFiles(root);
		final RootEntry previous = CACHE.get(root);
		final Map<Path, FileEntry> files = new LinkedHashMap<>();
		if (previous != null)
		{
			for (Path path : paths)
			{
				final FileEntry entry = previous._files.get(path);
				if (entry != null)
				{
					files.put(path, entry);
				}
			}
		}
		
		final RootEntry refreshed = new RootEntry(paths, files);
		CACHE.put(root, refreshed);
		return refreshed;
	}
	
	private static List<ItemMetadataRecord> parseFile(Path file, XMLInputFactory factory) throws Exception
	{
		final List<ItemMetadataRecord> records = new ArrayList<>();
		final Map<Integer, Integer> occurrences = new HashMap<>();
		try (InputStream input = Files.newInputStream(file))
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
						if ((depth == 2) && "item".equals(reader.getLocalName()))
						{
							final String idValue = attribute(reader, "id");
							if ((idValue == null) || idValue.isBlank())
							{
								continue;
							}
							
							// ItemXmlSpans counts every top-level item with this id, so the occurrence advances before incomplete items are skipped.
							final int id = parseInt(idValue, "item id", file);
							final int occurrence = occurrences.getOrDefault(id, 0);
							occurrences.put(id, occurrence + 1);
							final String name = attribute(reader, "name");
							final String type = attribute(reader, "type");
							if ((name == null) || (type == null))
							{
								continue;
							}
							
							records.add(new ItemMetadataRecord(id, name, attribute(reader, "additionalName"), type, file, occurrence));
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
		
		return Collections.unmodifiableList(records);
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
	
	private static List<Path> collectXmlFiles(Path root) throws IOException
	{
		if (!Files.isDirectory(root))
		{
			throw new IOException("Item directory does not exist: " + root);
		}
		
		final List<Path> files = new ArrayList<>();
		collectXmlFiles(root, files);
		final Path custom = root.resolve("custom");
		if (Files.isDirectory(custom))
		{
			collectXmlFiles(custom, files);
		}
		
		files.sort(Comparator.comparing(Path::toString, String.CASE_INSENSITIVE_ORDER));
		return Collections.unmodifiableList(files);
	}
	
	private static void collectXmlFiles(Path root, List<Path> files) throws IOException
	{
		try (DirectoryStream<Path> entries = Files.newDirectoryStream(root))
		{
			for (Path path : entries)
			{
				if (Files.isRegularFile(path) && isXml(path))
				{
					files.add(path.toAbsolutePath().normalize());
				}
			}
		}
	}
	
	private static boolean isXml(Path file)
	{
		return file.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".xml");
	}
	
	private static final class RootEntry
	{
		private final List<Path> _paths;
		private final Map<Path, FileEntry> _files;
		
		private RootEntry(List<Path> paths, Map<Path, FileEntry> files)
		{
			_paths = paths;
			_files = files;
		}
	}
	
	private static final class FileEntry
	{
		private final long _size;
		private final long _lastModified;
		private final List<ItemMetadataRecord> _records;
		
		private FileEntry(long size, long lastModified, List<ItemMetadataRecord> records)
		{
			_size = size;
			_lastModified = lastModified;
			_records = records;
		}
		
		private boolean matches(BasicFileAttributes attributes)
		{
			return (_size == attributes.size()) && (_lastModified == attributes.lastModifiedTime().toMillis());
		}
	}
}
