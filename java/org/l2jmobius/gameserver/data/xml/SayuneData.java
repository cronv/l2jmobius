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
package org.l2jmobius.gameserver.data.xml;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.data.holders.SayuneEntry;

/**
 * This class handles the loading and parsing of {@code Sayune} data from XML files.<br>
 * It maps the configuration entries into a collection of {@code SayuneEntry} objects.<br>
 * The maps are published as an immutable snapshot only after the whole file parses, so a reload drops entries removed from the XML and never exposes a half built set.
 * @author UnAfraid, BazookaRpm
 */
public class SayuneData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(SayuneData.class.getName());
	
	private volatile Map<Integer, SayuneEntry> _maps = Collections.emptyMap();
	
	/**
	 * Initializes a new instance of the {@code SayuneData} class.<br>
	 * This constructor automatically calls the {@code load()} method to populate data.
	 */
	protected SayuneData()
	{
		load();
	}
	
	/**
	 * Loads the data from the XML file.<br>
	 * The active maps are replaced only when the file parses successfully, so a failed reload keeps the routes already in use.
	 */
	@Override
	public synchronized void load()
	{
		final Map<Integer, SayuneEntry> previous = _maps;
		parseDatapackFile("data/SayuneData.xml");
		if (_maps == previous)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Could not load sayune maps. Keeping the active maps.");
			return;
		}
		
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _maps.size() + " maps.");
	}
	
	/**
	 * Parses the provided {@code Document} and publishes the parsed maps.<br>
	 * It reads data from the XML file structure.
	 * @param document The {@code Document} object containing the XML content.
	 * @param file The {@code File} source of the XML data.
	 */
	@Override
	public void parseDocument(Document document, File file)
	{
		if (document == null)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Cannot parse a null XML document.");
			return;
		}
		
		final Map<Integer, SayuneEntry> maps = new HashMap<>();
		for (Node n = document.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if ("map".equalsIgnoreCase(d.getNodeName()))
					{
						final int id = parseInteger(d.getAttributes(), "id");
						final SayuneEntry map = new SayuneEntry(id);
						parseEntries(map, d);
						maps.put(map.getId(), map);
					}
				}
			}
		}
		
		_maps = Collections.unmodifiableMap(maps);
	}
	
	/**
	 * Recursively parses XML nodes to populate the {@code SayuneEntry} structure.<br>
	 * It identifies specific tags like selector, choice, and loc.<br>
	 * Each found node is added as an inner entry to the parent object.
	 * @param lastEntry The parent {@code SayuneEntry} that will contain the new entries.
	 * @param n The current XML {@code Node} being processed.
	 */
	private void parseEntries(SayuneEntry lastEntry, Node n)
	{
		NamedNodeMap attrs;
		for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
		{
			final String dName = d.getNodeName();
			if ("selector".equals(dName) || "choice".equals(dName) || "loc".equals(dName))
			{
				attrs = d.getAttributes();
				final int id = parseInteger(attrs, "id");
				final int x = parseInteger(attrs, "x");
				final int y = parseInteger(attrs, "y");
				final int z = parseInteger(attrs, "z");
				parseEntries(lastEntry.addInnerEntry(new SayuneEntry("selector".equals(dName), id, x, y, z)), d);
			}
		}
	}
	
	/**
	 * Retrieves a specific {@code SayuneEntry} from the internal map.<br>
	 * This method uses the provided unique identifier to find the data.
	 * @param id The unique integer ID of the map to retrieve.
	 * @return The {@code SayuneEntry} associated with the given ID, or {@code null} if not found.
	 */
	public SayuneEntry getMap(int id)
	{
		return _maps.get(id);
	}
	
	/**
	 * Retrieves all loaded map entries.<br>
	 * This method returns the collection of {@code SayuneEntry} objects stored in the internal map.
	 * @return An unmodifiable {@code Collection} containing all {@code SayuneEntry} objects.
	 */
	public Collection<SayuneEntry> getMaps()
	{
		return _maps.values();
	}
	
	/**
	 * Retrieves the singleton instance of {@code SayuneData}.<br>
	 * This method provides a global access point to the data.
	 * @return The single instance of {@code SayuneData}.
	 */
	public static SayuneData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final SayuneData INSTANCE = new SayuneData();
	}
}
