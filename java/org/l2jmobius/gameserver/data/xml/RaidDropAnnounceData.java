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
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the loading and management of raid drop announcement data from XML files.<br>
 * It allows the server to define which items trigger a global announcement when dropped during raids.
 * @author MacuK
 */
public class RaidDropAnnounceData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(RaidDropAnnounceData.class.getName());
	
	private final Set<Integer> _itemIds = new HashSet<>();
	
	/**
	 * This constructor initializes a new instance of {@code RaidDropAnnounceData}.<br>
	 * It automatically calls the {@code load()} method to populate the data.
	 */
	protected RaidDropAnnounceData()
	{
		load();
	}
	
	/**
	 * Loads the raid drop announcement data from the configuration file.<br>
	 * This method clears existing IDs and parses the {@code data/RaidDropAnnounceData.xml} file.<br>
	 * It logs a message if any items are successfully loaded.
	 */
	@Override
	public void load()
	{
		_itemIds.clear();
		parseDatapackFile("data/RaidDropAnnounceData.xml");
		if (!_itemIds.isEmpty())
		{
			LOGGER.info(getClass().getSimpleName() + ": Loaded " + _itemIds.size() + " raid drop announce data.");
		}
	}
	
	/**
	 * Parses the XML content from a file to populate the internal list of items.<br>
	 * It iterates through all {@code item} nodes within the {@code list} section.<br>
	 * Valid item IDs are added to the internal set if they exist in the database.
	 * @param document The {@code Document} object containing the XML data.
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
					if ("item".equalsIgnoreCase(d.getNodeName()))
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
						final ItemTemplate item = ItemData.getInstance().getTemplate(id);
						if (item != null)
						{
							_itemIds.add(id);
						}
						else
						{
							LOGGER.warning(getClass().getSimpleName() + ": Could not find item with id: " + id);
						}
					}
				}
			}
		}
	}
	
	/**
	 * Checks if a specific item should be announced.<br>
	 * This method looks up the {@code itemId} in the internal list.
	 * @param itemId The unique identifier of the item to check.
	 * @return {@code true} if the item is set for announcement, {@code false} otherwise.
	 */
	public boolean isAnnounce(int itemId)
	{
		return _itemIds.contains(itemId);
	}
	
	/**
	 * Retrieves the singleton instance of {@code RaidDropAnnounceData}.<br>
	 * This method provides a global access point to the shared data.
	 * @return The single instance of {@code RaidDropAnnounceData}.
	 */
	public static RaidDropAnnounceData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final RaidDropAnnounceData INSTANCE = new RaidDropAnnounceData();
	}
}
