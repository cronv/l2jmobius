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
import org.l2jmobius.gameserver.data.holders.CollectionDataHolder;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.entity.item.holders.ItemEnchantHolder;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the parsing and storage of collection data from XML files.<br>
 * It maps item requirements and rewards to specific collections within the game.<br>
 * It provides a structured way for the server to access collection-related information.
 * @author Berezkin Nikolay
 */
public class CollectionData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(CollectionData.class.getName());
	
	private final Map<Integer, CollectionDataHolder> _collections = new HashMap<>();
	private final Map<Integer, List<CollectionDataHolder>> _collectionsByTabId = new HashMap<>();
	
	/**
	 * Initializes a new instance of {@code CollectionData}.<br>
	 * This constructor automatically calls the {@code load()} method.<br>
	 * It prepares the internal data structures for use.
	 */
	protected CollectionData()
	{
		load();
	}
	
	/**
	 * Loads the collection data from the XML file.<br>
	 * This method clears existing data and parses {@code data/CollectionData.xml}.<br>
	 * It logs the number of collections loaded or if the system is disabled.
	 */
	@Override
	public void load()
	{
		_collections.clear();
		parseDatapackFile("data/CollectionData.xml");
		
		if (!_collections.isEmpty())
		{
			LOGGER.info(getClass().getSimpleName() + ": Loaded " + _collections.size() + " collections.");
		}
		else
		{
			LOGGER.info(getClass().getSimpleName() + ": System is disabled.");
		}
	}
	
	/**
	 * Parses the provided {@code Document} to populate collection data.<br>
	 * This method reads XML nodes and creates {@code CollectionDataHolder} objects.<br>
	 * It maps these holders by their unique IDs and categories.
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
					if ("collection".equalsIgnoreCase(d.getNodeName()))
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
						final List<ItemEnchantHolder> items = new ArrayList<>();
						for (Node b = d.getFirstChild(); b != null; b = b.getNextSibling())
						{
							attrs = b.getAttributes();
							if ("item".equalsIgnoreCase(b.getNodeName()))
							{
								final int itemId = parseInteger(attrs, "id");
								final ItemTemplate item = ItemData.getInstance().getTemplate(itemId);
								if (item == null)
								{
									LOGGER.severe(getClass().getSimpleName() + ": Item template null for itemId: " + itemId + " collection item: " + id);
									continue;
								}
								
								final long itemCount = parseLong(attrs, "count", 1L);
								final int itemEnchantLevel = parseInteger(attrs, "enchantLevel", 0);
								final int itemIndex = parseInteger(attrs, "index", 0);
								
								items.add(new ItemEnchantHolder(itemId, itemCount, itemEnchantLevel, itemIndex));
							}
						}
						
						final CollectionDataHolder template = new CollectionDataHolder(id, name, optionId, category, completeCount, items);
						_collections.put(id, template);
						_collectionsByTabId.computeIfAbsent(template.getCategory(), _ -> new ArrayList<>()).add(template);
					}
				}
			}
		}
	}
	
	/**
	 * Retrieves a specific collection based on its unique identifier.<br>
	 * This method looks up the data in the internal map.
	 * @param id The unique integer ID of the collection to retrieve.
	 * @return The {@code CollectionDataHolder} associated with the given {@code id}, or {@code null} if not found.
	 */
	public CollectionDataHolder getCollection(int id)
	{
		return _collections.get(id);
	}
	
	/**
	 * Retrieves a list of collections associated with a specific tab ID.<br>
	 * This method returns an empty list if no collections are found for the given {@code tabId}.
	 * @param tabId The unique identifier for the collection tab.
	 * @return A {@code List} of {@code CollectionDataHolder} objects belonging to the specified tab.
	 */
	public List<CollectionDataHolder> getCollectionsByTabId(int tabId)
	{
		return _collectionsByTabId.getOrDefault(tabId, Collections.emptyList());
	}
	
	/**
	 * Retrieves all loaded collections.<br>
	 * This method returns the values from the internal collection map.
	 * @return a {@code Collection} of {@code CollectionDataHolder} objects.
	 */
	public Collection<CollectionDataHolder> getCollections()
	{
		return _collections.values();
	}
	
	/**
	 * Provides access to the singleton instance of {@code CollectionData}.<br>
	 * Use this method to get the global data manager.
	 * @return The single instance of {@code CollectionData}.
	 */
	public static CollectionData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final CollectionData INSTANCE = new CollectionData();
	}
}
