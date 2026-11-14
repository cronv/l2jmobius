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

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.entity.item.holders.ItemHolder;
import org.l2jmobius.gameserver.mechanics.homunculus.HomunculusSlotTemplate;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the loading and parsing of homunculus slot data from XML files.<br>
 * It maps configuration values to {@code HomunculusSlotTemplate} objects for use in the game server.
 * @author Index
 */
public class HomunculusSlotData implements IXmlReader
{
	private static final Map<Integer, HomunculusSlotTemplate> TEMPLATES = new HashMap<>();
	
	/**
	 * Initializes a new instance of {@code HomunculusSlotData}.<br>
	 * This constructor automatically calls the {@code load()} method.<br>
	 * It prepares the data for use by loading templates from the configuration files.
	 */
	protected HomunculusSlotData()
	{
		load();
	}
	
	/**
	 * Loads the homunculus slot data from the XML file.<br>
	 * This method clears existing templates and parses the default datapack.<br>
	 * It logs the total number of loaded templates to the console.
	 */
	@Override
	public void load()
	{
		TEMPLATES.clear();
		parseDatapackFile("data/HomunculusSlotData.xml");
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + TEMPLATES.size() + " templates.");
	}
	
	/**
	 * Parses the provided {@code Document} to populate the template map.<br>
	 * This method reads data from an XML file structure.<br>
	 * It identifies homunculus slots and their associated fees.
	 * @param document The {@code Document} object containing the XML content.
	 * @param file The {@code File} source of the XML data.
	 */
	@Override
	public void parseDocument(Document document, File file)
	{
		StatSet set;
		Node att;
		NamedNodeMap attrs;
		for (Node n = document.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if ("homunculusSlot".equalsIgnoreCase(d.getNodeName()))
					{
						attrs = d.getAttributes();
						set = new StatSet();
						for (int i = 0; i < attrs.getLength(); i++)
						{
							att = attrs.item(i);
							set.set(att.getNodeName(), att.getNodeValue());
						}
						
						List<ItemHolder> fee = Collections.emptyList();
						for (Node c = d.getFirstChild(); c != null; c = c.getNextSibling())
						{
							if ("fee".equalsIgnoreCase(c.getNodeName()))
							{
								fee = getItemList(c);
								break;
							}
						}
						
						final int slotId = set.getInt("slotId");
						TEMPLATES.put(slotId, new HomunculusSlotTemplate(slotId, fee, set.getBoolean("isEnabled", false)));
					}
				}
			}
		}
	}
	
	/**
	 * Parses a list of items from an XML node.<br>
	 * It looks for children with the name "item".<br>
	 * Each item is converted into an {@code ItemHolder}.
	 * @param c The {@code Node} containing the item data.
	 * @return A {@code List} of {@code ItemHolder} objects found in the node.
	 */
	private List<ItemHolder> getItemList(Node c)
	{
		final List<ItemHolder> items = new ArrayList<>();
		for (Node b = c.getFirstChild(); b != null; b = b.getNextSibling())
		{
			if ("item".equalsIgnoreCase(b.getNodeName()))
			{
				final NamedNodeMap bAttrs = b.getAttributes();
				final int itemId = Integer.parseInt(bAttrs.getNamedItem("id").getNodeValue());
				final long itemCount = Long.parseLong(bAttrs.getNamedItem("count").getNodeValue());
				items.add(new ItemHolder(itemId, itemCount));
			}
		}
		
		return items;
	}
	
	/**
	 * Retrieves a specific homunculus slot template from the loaded data.<br>
	 * This method looks up the template using its unique identifier.
	 * @param id The unique integer ID of the template to retrieve.
	 * @return The {@code HomunculusSlotTemplate} associated with the given {@code id}, or {@code null} if not found.
	 */
	public HomunculusSlotTemplate getTemplate(int id)
	{
		return TEMPLATES.get(id);
	}
	
	/**
	 * Provides access to the singleton instance of {@code HomunculusSlotData}.<br>
	 * Use this method to retrieve the global data manager.
	 * @return The single instance of {@code HomunculusSlotData}.
	 */
	public static HomunculusSlotData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final HomunculusSlotData INSTANCE = new HomunculusSlotData();
	}
}
