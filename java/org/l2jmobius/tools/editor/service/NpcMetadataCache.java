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
package org.l2jmobius.tools.editor.service;

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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

import org.l2jmobius.tools.editor.model.NpcMetadataRecord;

/**
 * Shares one hardened streaming NPC metadata scan between offline editors when the datapack files have not changed.
 * @author BazookaRpm
 */
public final class NpcMetadataCache
{
	private static final int MAX_ENTRIES = 4;
	private static final Map<Path, CacheEntry> CACHE = new LinkedHashMap<>(8, 0.75f, true)
	{
		private static final long serialVersionUID = 1L;
		
		@Override
		protected boolean removeEldestEntry(Entry<Path, CacheEntry> eldest)
		{
			return size() > MAX_ENTRIES;
		}
	};
	
	private NpcMetadataCache()
	{
	}
	
	public static synchronized List<NpcMetadataRecord> load(Path npcRoot) throws Exception
	{
		final Path normalizedRoot = npcRoot.toAbsolutePath().normalize();
		final List<Path> files = collectXmlFiles(normalizedRoot);
		final long fingerprint = fingerprint(files);
		final CacheEntry cached = CACHE.get(normalizedRoot);
		if ((cached != null) && (cached._fingerprint == fingerprint) && (cached._fileCount == files.size()))
		{
			return cached._records;
		}
		
		final List<NpcMetadataRecord> records = new ArrayList<>();
		for (Path file : files)
		{
			loadFile(file, records);
		}
		
		final List<NpcMetadataRecord> immutable = Collections.unmodifiableList(records);
		CACHE.put(normalizedRoot, new CacheEntry(fingerprint, files.size(), immutable));
		return immutable;
	}
	
	private static void loadFile(Path file, List<NpcMetadataRecord> records) throws Exception
	{
		final XMLInputFactory factory = createFactory();
		try (InputStream input = Files.newInputStream(file))
		{
			final XMLStreamReader reader = factory.createXMLStreamReader(input);
			try
			{
				int depth = 0;
				int npcDepth = -1;
				Integer id = null;
				Integer level = null;
				String type = "";
				String name = "";
				String title = "";
				String race = "";
				String sex = "";
				String textElement = null;
				StringBuilder text = null;
				
				while (reader.hasNext())
				{
					final int event = reader.next();
					if (event == XMLStreamConstants.START_ELEMENT)
					{
						depth++;
						final String localName = reader.getLocalName();
						if ((npcDepth < 0) && "npc".equalsIgnoreCase(localName))
						{
							npcDepth = depth;
							final String idValue = attribute(reader, "id");
							if ((idValue == null) || idValue.isBlank())
							{
								id = null;
							}
							else
							{
								id = Integer.valueOf(parseInt(idValue, "npc id", file));
							}
							
							final String levelValue = attribute(reader, "level");
							level = ((levelValue == null) || levelValue.isBlank()) ? null : Integer.valueOf(parseInt(levelValue, "npc level", file));
							type = value(attribute(reader, "type"));
							name = value(attribute(reader, "name"));
							title = value(attribute(reader, "title"));
							race = "";
							sex = "";
						}
						else if ((npcDepth > 0) && (depth == (npcDepth + 1)) && ("race".equals(localName) || "sex".equals(localName)))
						{
							textElement = localName;
							text = new StringBuilder();
						}
					}
					else if (((event == XMLStreamConstants.CHARACTERS) || (event == XMLStreamConstants.CDATA)) && (text != null))
					{
						text.append(reader.getText());
					}
					else if (event == XMLStreamConstants.END_ELEMENT)
					{
						final String localName = reader.getLocalName();
						if ((textElement != null) && textElement.equals(localName) && (depth == (npcDepth + 1)))
						{
							final String value = (text == null) ? "" : text.toString().trim();
							if ("race".equals(textElement))
							{
								race = value;
							}
							else
							{
								sex = value;
							}
							
							textElement = null;
							text = null;
						}
						
						if ((npcDepth > 0) && (depth == npcDepth) && "npc".equalsIgnoreCase(localName))
						{
							if (id != null)
							{
								records.add(new NpcMetadataRecord(id.intValue(), level, type, name, title, race, sex, file));
							}
							
							npcDepth = -1;
							id = null;
						}
						
						depth--;
					}
				}
			}
			finally
			{
				reader.close();
			}
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
			// Optional parser property; the required parser path remains local-file only.
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
	
	private static String value(String value)
	{
		return value == null ? "" : value;
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
	
	private static long fingerprint(List<Path> files) throws IOException
	{
		long fingerprint = 1125899906842597L;
		for (Path file : files)
		{
			final BasicFileAttributes attributes = Files.readAttributes(file, BasicFileAttributes.class);
			fingerprint = (31 * fingerprint) + file.toString().toLowerCase(Locale.ROOT).hashCode();
			fingerprint = (31 * fingerprint) + attributes.size();
			fingerprint = (31 * fingerprint) + attributes.lastModifiedTime().toMillis();
		}
		
		return fingerprint;
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
	 * Stores one NPC directory fingerprint and its immutable scan result.
	 */
	private static final class CacheEntry
	{
		private final long _fingerprint;
		private final int _fileCount;
		private final List<NpcMetadataRecord> _records;
		
		private CacheEntry(long fingerprint, int fileCount, List<NpcMetadataRecord> records)
		{
			_fingerprint = fingerprint;
			_fileCount = fileCount;
			_records = records;
		}
	}
}
