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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.entity.residences.ResidenceFunctionTemplate;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the loading and management of residence functions from XML data.<br>
 * It parses configuration files to populate {@code ResidenceFunctionTemplate} objects for use in the game server.<br>
 * The functions are published as an immutable snapshot only after the whole file parses, so a failed reload keeps the active functions.
 * @author UnAfraid, BazookaRpm
 */
public class ResidenceFunctionsData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(ResidenceFunctionsData.class.getName());
	
	private volatile Map<Integer, List<ResidenceFunctionTemplate>> _functions = Collections.emptyMap();
	
	/**
	 * Initializes a new instance of the {@code ResidenceFunctionsData} class.<br>
	 * This constructor automatically triggers the {@code load()} method to populate data.
	 */
	protected ResidenceFunctionsData()
	{
		load();
	}
	
	/**
	 * Loads the residence function data from the XML file.<br>
	 * The active functions are replaced only when the file parses successfully, so a failed reload keeps the templates the residences are already using.
	 */
	@Override
	public synchronized void load()
	{
		final Map<Integer, List<ResidenceFunctionTemplate>> previous = _functions;
		parseDatapackFile("data/ResidenceFunctions.xml");
		if (_functions == previous)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Could not load residence functions. Keeping the active functions.");
			return;
		}
		
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _functions.size() + " functions.");
	}
	
	/**
	 * Parses the provided {@code Document} to load residence functions.<br>
	 * This method reads data from the XML structure and publishes the parsed templates.<br>
	 * It maps each function template based on its unique ID.
	 * @param document The {@code Document} object containing the XML data.
	 * @param file The {@code File} associated with the parsed document.
	 */
	@Override
	public void parseDocument(Document document, File file)
	{
		if (document == null)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Cannot parse a null XML document.");
			return;
		}
		
		final Map<Integer, List<ResidenceFunctionTemplate>> functions = new HashMap<>();
		forEach(document, "list", list -> forEach(list, "function", func ->
		{
			final NamedNodeMap attrs = func.getAttributes();
			final StatSet set = new StatSet(HashMap::new);
			final int attrsLen = attrs.getLength();
			for (int i = 0; i < attrsLen; i++)
			{
				final Node node = attrs.item(i);
				set.set(node.getNodeName(), node.getNodeValue());
			}
			
			forEach(func, "function", levelNode ->
			{
				final NamedNodeMap levelAttrs = levelNode.getAttributes();
				final StatSet levelSet = new StatSet(HashMap::new);
				levelSet.merge(set);
				final int levelAttrsLen = levelAttrs.getLength();
				for (int i = 0; i < levelAttrsLen; i++)
				{
					final Node node = levelAttrs.item(i);
					levelSet.set(node.getNodeName(), node.getNodeValue());
				}
				
				final ResidenceFunctionTemplate template = new ResidenceFunctionTemplate(levelSet);
				functions.computeIfAbsent(template.getId(), _ -> new ArrayList<>()).add(template);
			});
		}));
		
		if (functions.isEmpty())
		{
			throw new IllegalArgumentException(getClass().getSimpleName() + ": No residence functions were found in " + (file != null ? file.getAbsolutePath() : "<unknown>") + ".");
		}
		
		for (Entry<Integer, List<ResidenceFunctionTemplate>> entry : functions.entrySet())
		{
			entry.setValue(Collections.unmodifiableList(entry.getValue()));
		}
		
		_functions = Collections.unmodifiableMap(functions);
	}
	
	/**
	 * Retrieves a specific residence function based on its unique ID and level.<br>
	 * This method searches the internal data map for matching templates.
	 * @param id The unique identifier of the residence function.
	 * @param level The specific level required for the function.
	 * @return The matching {@code ResidenceFunctionTemplate} or {@code null} if not found.
	 */
	public ResidenceFunctionTemplate getFunction(int id, int level)
	{
		final List<ResidenceFunctionTemplate> functions = _functions.get(id);
		if (functions != null)
		{
			for (ResidenceFunctionTemplate template : functions)
			{
				if (template.getLevel() == level)
				{
					return template;
				}
			}
		}
		
		return null;
	}
	
	/**
	 * Retrieves a list of functions for a specific residence.<br>
	 * This method looks up the data using the provided {@code id}.
	 * @param id The unique identifier for the residence.
	 * @return An unmodifiable {@code List} of {@code ResidenceFunctionTemplate} objects, or {@code null} if not found.
	 */
	public List<ResidenceFunctionTemplate> getFunctions(int id)
	{
		return _functions.get(id);
	}
	
	/**
	 * Provides access to the singleton instance of {@code ResidenceFunctionsData}.<br>
	 * Use this method to get the global data container for residence functions.
	 * @return The single instance of {@code ResidenceFunctionsData}.
	 */
	public static ResidenceFunctionsData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final ResidenceFunctionsData INSTANCE = new ResidenceFunctionsData();
	}
}
