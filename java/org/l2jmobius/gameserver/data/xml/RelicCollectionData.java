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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.config.RelicSystemConfig;
import org.l2jmobius.gameserver.data.holders.RelicCollectionDataHolder;
import org.l2jmobius.gameserver.data.holders.RelicCollectionInfoHolder;
import org.l2jmobius.gameserver.data.holders.RelicDataHolder;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the loading and parsing of relic collection data from XML files.<br>
 * It populates {@code RelicCollectionDataHolder} objects to define available relics in the game.<br>
 * It serves as a data provider for the relic system configuration.
 * @author CostyKiller
 */
public class RelicCollectionData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(RelicCollectionData.class.getName());
	
	private static final Map<Integer, RelicCollectionDataHolder> RELIC_COLLECTIONS = new HashMap<>();
	private static final Map<Integer, List<RelicCollectionDataHolder>> RELIC_COLLECTION_CATEGORIES = new HashMap<>();
	
	/**
	 * This is a protected constructor for the {@code RelicCollectionData} class.<br>
	 * It initializes the data by calling the {@code load()} method.
	 */
	protected RelicCollectionData()
	{
		load();
	}
	
	/**
	 * Loads the relic collection data from the configuration files.<br>
	 * This method clears existing data before parsing the XML file.<br>
	 * It checks if the relic system is enabled in {@code RelicSystemConfig}.
	 */
	@Override
	public void load()
	{
		RELIC_COLLECTIONS.clear();
		
		if (RelicSystemConfig.RELIC_SYSTEM_ENABLED)
		{
			parseDatapackFile("data/RelicCollectionData.xml");
		}
		
		if (!RELIC_COLLECTIONS.isEmpty())
		{
			LOGGER.info(getClass().getSimpleName() + ": Loaded " + RELIC_COLLECTIONS.size() + " relic collections.");
		}
		else
		{
			LOGGER.info(getClass().getSimpleName() + ": System is disabled.");
		}
	}
	
	/**
	 * Parses the provided {@code Document} to populate relic collection data.<br>
	 * This method reads XML nodes and stores them in internal maps.
	 * @param document The {@code Document} object containing the XML content.
	 * @param file The {@code File} associated with the parsed document.
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
					if ("relicCollection".equalsIgnoreCase(d.getNodeName()))
					{
						NamedNodeMap attrs = d.getAttributes();
						Node att;
						final StatSet set = new StatSet();
						for (int i = 0; i < attrs.getLength(); i++)
						{
							att = attrs.item(i);
							set.set(att.getNodeName(), att.getNodeValue());
						}
						
						final int id = parseInteger(attrs, "id");
						final String name = parseString(attrs, "name", "");
						final int optionId = parseInteger(attrs, "optionId");
						final int category = parseInteger(attrs, "category");
						final int completeCount = parseInteger(attrs, "completeCount");
						final List<RelicCollectionInfoHolder> relics = new ArrayList<>();
						for (Node b = d.getFirstChild(); b != null; b = b.getNextSibling())
						{
							attrs = b.getAttributes();
							if ("relic".equalsIgnoreCase(b.getNodeName()))
							{
								final int relicId = parseInteger(attrs, "id");
								final RelicDataHolder relic = RelicData.getInstance().getRelic(relicId);
								if (relic == null)
								{
									LOGGER.severe(getClass().getSimpleName() + ": Relic null for relicId: " + relicId + " relics collection item: " + id);
									continue;
								}
								
								final int enchantLevel = parseInteger(attrs, "enchantLevel", 0);
								relics.add(new RelicCollectionInfoHolder(relicId, enchantLevel));
							}
						}
						
						final RelicCollectionDataHolder template = new RelicCollectionDataHolder(id, name, optionId, category, completeCount, relics);
						RELIC_COLLECTIONS.put(id, template);
						RELIC_COLLECTION_CATEGORIES.computeIfAbsent(template.getCategory(), _ -> new ArrayList<>()).add(template);
					}
				}
			}
		}
	}
	
	/**
	 * Retrieves a specific relic collection based on its unique identifier.<br>
	 * This method looks up the data in the internal registry.
	 * @param id The unique {@code int} ID of the relic collection to retrieve.
	 * @return The {@code RelicCollectionDataHolder} associated with the provided ID, or {@code null} if not found.
	 */
	public RelicCollectionDataHolder getRelicCollection(int id)
	{
		return RELIC_COLLECTIONS.get(id);
	}
	
	/**
	 * Retrieves a list of relic collections for a specific category.<br>
	 * This method uses the provided {@code tabId} to look up the correct group.<br>
	 * It returns an empty list if no data is found for that ID.
	 * @param tabId The unique identifier for the relic category.
	 * @return A {@code List} of {@code RelicCollectionDataHolder} objects belonging to the category.
	 */
	public List<RelicCollectionDataHolder> getRelicCategory(int tabId)
	{
		return RELIC_COLLECTION_CATEGORIES.getOrDefault(tabId, Collections.emptyList());
	}
	
	/**
	 * Retrieves all loaded relic collections.<br>
	 * This method returns the values stored in the internal collection map.
	 * @return A {@code Collection} of {@code RelicCollectionDataHolder} objects.
	 */
	public Collection<RelicCollectionDataHolder> getRelicCollections()
	{
		return RELIC_COLLECTIONS.values();
	}
	
	/**
	 * Provides access to the singleton instance of {@code RelicCollectionData}.<br>
	 * Use this method to get the global data holder for relic collections.
	 * @return The single instance of {@code RelicCollectionData}.
	 */
	public static RelicCollectionData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final RelicCollectionData INSTANCE = new RelicCollectionData();
	}
}
