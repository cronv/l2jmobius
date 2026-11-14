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
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.data.holders.TimedHuntingZoneHolder;
import org.l2jmobius.gameserver.entity.Location;

/**
 * This class handles the loading and parsing of {@code TimedHuntingZoneData} from XML files.<br>
 * It provides the data structure required to initialize {@link org.l2jmobius.gameserver.data.holders.TimedHuntingZoneHolder} objects.
 * @author Mobius
 */
public class TimedHuntingZoneData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(TimedHuntingZoneData.class.getName());
	
	private final Map<Integer, TimedHuntingZoneHolder> _timedHuntingZoneData = new HashMap<>();
	
	/**
	 * Creates a new instance of {@code TimedHuntingZoneData}.<br>
	 * This constructor automatically calls the {@code load()} method.<br>
	 * It initializes the data required for timed hunting zones.
	 */
	protected TimedHuntingZoneData()
	{
		load();
	}
	
	/**
	 * Loads the timed hunting zone data from the XML file.<br>
	 * This method clears existing data before parsing the configuration.<br>
	 * It logs a message indicating how many zones were loaded or if the system is disabled.
	 */
	@Override
	public void load()
	{
		_timedHuntingZoneData.clear();
		parseDatapackFile("data/TimedHuntingZoneData.xml");
		
		if (!_timedHuntingZoneData.isEmpty())
		{
			LOGGER.info(getClass().getSimpleName() + ": Loaded " + _timedHuntingZoneData.size() + " timed hunting zones.");
		}
		else
		{
			LOGGER.info(getClass().getSimpleName() + ": System is disabled.");
		}
	}
	
	/**
	 * Parses the provided {@code Document} to populate the hunting zone data.<br>
	 * This method reads XML nodes and creates new {@code TimedHuntingZoneHolder} objects.<br>
	 * It only processes zones where the enabled attribute is set to {@code true}.
	 * @param document The {@code Document} object containing the XML data.
	 * @param file The {@code File} associated with the parsed document.
	 */
	@Override
	public void parseDocument(Document document, File file)
	{
		for (Node xmlNode = document.getFirstChild(); xmlNode != null; xmlNode = xmlNode.getNextSibling())
		{
			if ("list".equalsIgnoreCase(xmlNode.getNodeName()))
			{
				final NamedNodeMap listAttributes = xmlNode.getAttributes();
				final Node attribute = listAttributes.getNamedItem("enabled");
				if ((attribute != null) && Boolean.parseBoolean(attribute.getNodeValue()))
				{
					for (Node listNode = xmlNode.getFirstChild(); listNode != null; listNode = listNode.getNextSibling())
					{
						if ("zone".equalsIgnoreCase(listNode.getNodeName()))
						{
							final NamedNodeMap zoneAttributes = listNode.getAttributes();
							int id = parseInteger(zoneAttributes, "id");
							String name = parseString(zoneAttributes, "name", "");
							int initialTime = 0;
							int maxAddedTime = 0;
							int resetDelay = 0;
							int entryItemId = 57;
							int entryFee = 150000;
							int minLevel = 1;
							int maxLevel = 999;
							int remainRefillTime = 3600;
							int refillTimeMax = 3600;
							boolean pvpZone = false;
							boolean noPvpZone = false;
							int instanceId = 0;
							boolean soloInstance = true;
							boolean weekly = false;
							boolean useWorldPrefix = false;
							boolean zonePremiumUserOnly = false;
							Location enterLocation = null;
							Location exitLocation = null;
							boolean isEvenWeek = true;
							boolean isSwapWeek = false;
							for (Node zoneNode = listNode.getFirstChild(); zoneNode != null; zoneNode = zoneNode.getNextSibling())
							{
								switch (zoneNode.getNodeName())
								{
									case "enterLocation":
									{
										final String[] coordinates = zoneNode.getTextContent().split(",");
										enterLocation = new Location(Integer.parseInt(coordinates[0]), Integer.parseInt(coordinates[1]), Integer.parseInt(coordinates[2]));
										break;
									}
									case "exitLocation":
									{
										final String[] coordinates = zoneNode.getTextContent().split(",");
										exitLocation = new Location(Integer.parseInt(coordinates[0]), Integer.parseInt(coordinates[1]), Integer.parseInt(coordinates[2]));
										break;
									}
									case "initialTime":
									{
										initialTime = Integer.parseInt(zoneNode.getTextContent()) * 1000;
										break;
									}
									case "maxAddedTime":
									{
										maxAddedTime = Integer.parseInt(zoneNode.getTextContent()) * 1000;
										break;
									}
									case "resetDelay":
									{
										resetDelay = Integer.parseInt(zoneNode.getTextContent()) * 1000;
										break;
									}
									case "entryItemId":
									{
										entryItemId = Integer.parseInt(zoneNode.getTextContent());
										break;
									}
									case "entryFee":
									{
										entryFee = Integer.parseInt(zoneNode.getTextContent());
										break;
									}
									case "minLevel":
									{
										minLevel = Integer.parseInt(zoneNode.getTextContent());
										break;
									}
									case "maxLevel":
									{
										maxLevel = Integer.parseInt(zoneNode.getTextContent());
										break;
									}
									case "remainRefillTime":
									{
										remainRefillTime = Integer.parseInt(zoneNode.getTextContent());
										break;
									}
									case "refillTimeMax":
									{
										refillTimeMax = Integer.parseInt(zoneNode.getTextContent());
										break;
									}
									case "pvpZone":
									{
										pvpZone = Boolean.parseBoolean(zoneNode.getTextContent());
										break;
									}
									case "noPvpZone":
									{
										noPvpZone = Boolean.parseBoolean(zoneNode.getTextContent());
										break;
									}
									case "instanceId":
									{
										instanceId = Integer.parseInt(zoneNode.getTextContent());
										break;
									}
									case "soloInstance":
									{
										soloInstance = Boolean.parseBoolean(zoneNode.getTextContent());
										break;
									}
									case "weekly":
									{
										weekly = Boolean.parseBoolean(zoneNode.getTextContent());
										break;
									}
									case "useWorldPrefix":
									{
										useWorldPrefix = Boolean.parseBoolean(zoneNode.getTextContent());
										break;
									}
									case "zonePremiumUserOnly":
									{
										zonePremiumUserOnly = Boolean.parseBoolean(zoneNode.getTextContent());
										break;
									}
									
									case "isEvenWeek":
									{
										isEvenWeek = Boolean.parseBoolean(zoneNode.getTextContent());
										break;
									}
									case "isSwapWeek":
									{
										isSwapWeek = Boolean.parseBoolean(zoneNode.getTextContent());
									}
								}
							}
							
							_timedHuntingZoneData.put(id, new TimedHuntingZoneHolder(id, name, initialTime, maxAddedTime, resetDelay, entryItemId, entryFee, minLevel, maxLevel, remainRefillTime, refillTimeMax, pvpZone, noPvpZone, instanceId, soloInstance, weekly, useWorldPrefix, zonePremiumUserOnly, enterLocation, exitLocation, isEvenWeek, isSwapWeek));
						}
					}
				}
			}
		}
	}
	
	/**
	 * Retrieves a specific hunting zone based on its unique ID.<br>
	 * This method looks up the data in the internal map.
	 * @param zoneId The unique identifier for the hunting zone.
	 * @return The {@code TimedHuntingZoneHolder} associated with the ID, or {@code null} if not found.
	 */
	public TimedHuntingZoneHolder getHuntingZone(int zoneId)
	{
		return _timedHuntingZoneData.get(zoneId);
	}
	
	/**
	 * Retrieves all loaded hunting zones.<br>
	 * This method returns a collection of every {@code TimedHuntingZoneHolder} in the data map.
	 * @return A {@code Collection} containing all {@code TimedHuntingZoneHolder} objects.
	 */
	public Collection<TimedHuntingZoneHolder> getAllHuntingZones()
	{
		return _timedHuntingZoneData.values();
	}
	
	/**
	 * Returns the total number of hunting zones.<br>
	 * This count is based on the internal data map.
	 * @return The size of the {@code TimedHuntingZoneData} collection as an {@code int}.
	 */
	public int getSize()
	{
		return _timedHuntingZoneData.size();
	}
	
	/**
	 * Retrieves the global instance of {@code TimedHuntingZoneData}.<br>
	 * This method follows the singleton pattern.<br>
	 * Use this to access hunting zone data from anywhere in the code.
	 * @return The single instance of {@code TimedHuntingZoneData}.
	 */
	public static TimedHuntingZoneData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final TimedHuntingZoneData INSTANCE = new TimedHuntingZoneData();
	}
}
