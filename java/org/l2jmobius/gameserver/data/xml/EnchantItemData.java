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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.entity.item.enchant.EnchantScroll;
import org.l2jmobius.gameserver.entity.item.enchant.EnchantSupportItem;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the parsing of enchantment data from XML files.<br>
 * It maps configuration values to {@code EnchantScroll} and {@code EnchantSupportItem} objects.
 * @author UnAfraid, BazookaRpm
 */
public class EnchantItemData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(EnchantItemData.class.getName());
	
	private final Map<Integer, EnchantScroll> _scrolls = new ConcurrentHashMap<>();
	private final Map<Integer, EnchantSupportItem> _supports = new ConcurrentHashMap<>();
	
	/**
	 * Creates a new instance of {@code EnchantItemData}.<br>
	 * This constructor automatically calls the {@code load()} method.<br>
	 * It initializes the data required for enchant scrolls and support items.
	 */
	protected EnchantItemData()
	{
		load();
	}
	
	/**
	 * Loads the enchantment data from the XML file.<br>
	 * This method clears existing data before parsing the new values.<br>
	 * It logs the total number of scrolls and support items loaded.
	 */
	@Override
	public synchronized void load()
	{
		_scrolls.clear();
		_supports.clear();
		parseDatapackFile("data/EnchantItemData.xml");
		final String simpleName = getClass().getSimpleName();
		LOGGER.info(simpleName + ": Loaded " + _scrolls.size() + " enchant scrolls.");
		LOGGER.info(simpleName + ": Loaded " + _supports.size() + " support items.");
	}
	
	/**
	 * Parses the provided {@code Document} to load enchantment data.<br>
	 * This method populates the internal maps for scrolls and support items.<br>
	 * It reads attributes from the XML nodes to create new objects.
	 * @param document The {@code Document} object containing the XML data.
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
					final String dName = d.getNodeName();
					if ("enchant".equalsIgnoreCase(dName))
					{
						final StatSet set = new StatSet(parseAttributes(d));
						try
						{
							final EnchantScroll item = new EnchantScroll(set);
							for (Node cd = d.getFirstChild(); cd != null; cd = cd.getNextSibling())
							{
								if ("item".equalsIgnoreCase(cd.getNodeName()))
								{
									final NamedNodeMap cdAttrs = cd.getAttributes();
									item.addItem(parseInteger(cdAttrs, "id"), parseInteger(cdAttrs, "altScrollGroupId", -1));
								}
							}
							
							_scrolls.put(item.getId(), item);
						}
						catch (NullPointerException e)
						{
							LOGGER.warning(getClass().getSimpleName() + ": Unexistent enchant scroll: " + set.getString("id") + " defined in enchant data!");
						}
						catch (IllegalAccessError e)
						{
							LOGGER.warning(getClass().getSimpleName() + ": Wrong enchant scroll item type: " + set.getString("id") + " defined in enchant data!");
						}
					}
					else if ("support".equalsIgnoreCase(dName))
					{
						final StatSet set = new StatSet(parseAttributes(d));
						try
						{
							final EnchantSupportItem item = new EnchantSupportItem(set);
							_supports.put(item.getId(), item);
						}
						catch (NullPointerException e)
						{
							LOGGER.warning(getClass().getSimpleName() + ": Unexistent enchant support item: " + set.getString("id") + " defined in enchant data!");
						}
						catch (IllegalAccessError e)
						{
							LOGGER.warning(getClass().getSimpleName() + ": Wrong enchant support item type: " + set.getString("id") + " defined in enchant data!");
						}
					}
				}
			}
		}
	}
	
	/**
	 * Retrieves all loaded enchant scrolls.<br>
	 * This method returns the collection of {@code EnchantScroll} objects.
	 * @return a {@code Collection} of {@code EnchantScroll} objects.
	 */
	public Collection<EnchantScroll> getScrolls()
	{
		return _scrolls.values();
	}
	
	/**
	 * Retrieves the {@code EnchantScroll} associated with a specific item.<br>
	 * This method looks up the scroll using the unique ID of the provided {@code Item}.<br>
	 * It returns {@code null} if the item is {@code null} or has no matching scroll.
	 * @param item The {@code Item} to check for an enchantment scroll.
	 * @return The corresponding {@code EnchantScroll} object, or {@code null} if not found.
	 */
	public EnchantScroll getEnchantScroll(Item item)
	{
		if (item == null)
		{
			return null;
		}
		
		return _scrolls.get(item.getId());
	}
	
	/**
	 * Retrieves the {@code EnchantSupportItem} associated with a specific item.<br>
	 * This method looks up the support data using the unique ID of the provided {@code Item}.<br>
	 * It returns {@code null} if the input is {@code null} or no support item exists.
	 * @param item The {@code Item} to check for support data.
	 * @return The corresponding {@code EnchantSupportItem}, or {@code null} if not found.
	 */
	public EnchantSupportItem getSupportItem(Item item)
	{
		if (item == null)
		{
			return null;
		}
		
		return _supports.get(item.getId());
	}
	
	/**
	 * Retrieves the global instance of {@code EnchantItemData}.<br>
	 * This method ensures that only one instance of the data is used throughout the application.
	 * @return The singleton instance of {@code EnchantItemData}.
	 */
	public static EnchantItemData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final EnchantItemData INSTANCE = new EnchantItemData();
	}
}
