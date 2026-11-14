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
import org.l2jmobius.gameserver.entity.actor.instance.StaticObject;
import org.l2jmobius.gameserver.entity.actor.templates.CreatureTemplate;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class represents the data structure for static objects defined in XML files.<br>
 * It handles the parsing and storage of properties for {@code StaticObject} instances.
 * @author UnAfraid, BazookaRpm
 */
public class StaticObjectData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(StaticObjectData.class.getName());
	
	private volatile Map<Integer, StaticObject> _staticObjects = Collections.emptyMap();
	private Map<Integer, StaticObject> _loadingObjects;
	
	/**
	 * This is the default constructor for {@code StaticObjectData}.<br>
	 * It initializes the object by calling the {@code load()} method.<br>
	 * Use this to prepare the data for use in the game server.
	 */
	protected StaticObjectData()
	{
		load();
	}
	
	/**
	 * Loads the static object data from the XML file.<br>
	 * Objects spawned by a previous load are removed from the world once the new ones parse, so repeated reloads do not leave duplicates behind.
	 */
	@Override
	public synchronized void load()
	{
		final Map<Integer, StaticObject> previous = _staticObjects;
		final Map<Integer, StaticObject> loadingObjects = new HashMap<>();
		_loadingObjects = loadingObjects;
		try
		{
			parseDatapackFile("data/StaticObjects.xml");
		}
		finally
		{
			_loadingObjects = null;
		}
		
		if (loadingObjects.isEmpty())
		{
			LOGGER.warning(getClass().getSimpleName() + ": No static objects were loaded. Keeping the active objects.");
			return;
		}
		
		// Remove the objects spawned by the previous load; clearing the map alone left them in the world.
		for (StaticObject staticObject : previous.values())
		{
			staticObject.decayMe();
		}
		
		_staticObjects = Collections.unmodifiableMap(loadingObjects);
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _staticObjects.size() + " static object templates.");
	}
	
	/**
	 * Parses the provided {@code Document} to extract static object data.<br>
	 * It reads all attributes from objects within the XML file.<br>
	 * The extracted data is then added to the internal collection.
	 * @param document The {@code Document} containing the XML content.
	 * @param file The {@code File} source of the XML data.
	 */
	@Override
	public void parseDocument(Document document, File file)
	{
		for (Node n = document.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if ("object".equalsIgnoreCase(d.getNodeName()))
					{
						final NamedNodeMap attrs = d.getAttributes();
						final StatSet set = new StatSet();
						for (int i = 0; i < attrs.getLength(); i++)
						{
							final Node att = attrs.item(i);
							set.set(att.getNodeName(), att.getNodeValue());
						}
						
						addObject(set);
					}
				}
			}
		}
	}
	
	/**
	 * Creates a new {@code StaticObject} from the provided data.<br>
	 * This method extracts values from the {@code StatSet} to initialize the object properties.<br>
	 * The resulting object is then added to the internal collection.
	 * @param set The {@code StatSet} containing the configuration data for the object.
	 */
	private void addObject(StatSet set)
	{
		final StaticObject obj = new StaticObject(new CreatureTemplate(new StatSet()), set.getInt("id"));
		obj.setType(set.getInt("type", 0));
		obj.setName(set.getString("name"));
		obj.setMap(set.getString("texture", "none"), set.getInt("map_x", 0), set.getInt("map_y", 0));
		obj.spawnMe(set.getInt("x"), set.getInt("y"), set.getInt("z"));
		final Map<Integer, StaticObject> loadingObjects = _loadingObjects;
		if (loadingObjects == null)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Skipping static object outside loading context. id: " + obj.getObjectId());
			obj.decayMe();
			return;
		}
		
		loadingObjects.put(obj.getObjectId(), obj);
	}
	
	/**
	 * Retrieves all loaded static objects.<br>
	 * This method returns the collection of {@code StaticObject} instances.
	 * @return a {@code Collection} containing all {@code StaticObject} items.
	 */
	public Collection<StaticObject> getStaticObjects()
	{
		return _staticObjects.values();
	}
	
	/**
	 * Retrieves the singleton instance of {@code StaticObjectData}.<br>
	 * This method provides a global access point to the static object data.
	 * @return The single instance of {@code StaticObjectData}.
	 */
	public static StaticObjectData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final StaticObjectData INSTANCE = new StaticObjectData();
	}
}
