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
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.options.EnchantOptions;

/**
 * This class handles the loading and parsing of enchantment options from XML files.<br>
 * It maps data to {@code EnchantOptions} objects for use in item enchantments.
 * @author UnAfraid, Mobius
 */
public class EnchantItemOptionsData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(EnchantItemOptionsData.class.getName());
	
	private final Map<Integer, Map<Integer, EnchantOptions>> _data = new HashMap<>();
	
	/**
	 * Initializes a new instance of {@code EnchantItemOptionsData}.<br>
	 * This constructor calls the {@code load()} method to populate data.
	 */
	protected EnchantItemOptionsData()
	{
		load();
	}
	
	/**
	 * Loads the enchantment options from the data files.<br>
	 * This method clears existing data before parsing the XML file.<br>
	 * It ensures thread safety by using the {@code synchronized} keyword.
	 */
	@Override
	public synchronized void load()
	{
		_data.clear();
		parseDatapackFile("data/EnchantItemOptions.xml");
	}
	
	/**
	 * Parses the XML content from a {@code Document} to populate enchantment data.<br>
	 * This method reads item IDs and their associated enchantment options from the file.<br>
	 * It logs warnings if any required templates or options are missing.
	 * @param document The {@code Document} object containing the XML structure.
	 * @param file The {@code File} source of the XML data.
	 */
	@Override
	public void parseDocument(Document document, File file)
	{
		int counter = 0;
		for (Node n = document.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				ITEM: for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if ("item".equalsIgnoreCase(d.getNodeName()))
					{
						final int itemId = parseInteger(d.getAttributes(), "id");
						final ItemTemplate template = ItemData.getInstance().getTemplate(itemId);
						if (template == null)
						{
							LOGGER.warning(getClass().getSimpleName() + ": Could not find item template for id " + itemId);
							continue ITEM;
						}
						
						for (Node cd = d.getFirstChild(); cd != null; cd = cd.getNextSibling())
						{
							if ("options".equalsIgnoreCase(cd.getNodeName()))
							{
								final NamedNodeMap cdAttrs = cd.getAttributes();
								final EnchantOptions op = new EnchantOptions(parseInteger(cdAttrs, "level"));
								for (byte i = 0; i < 3; i++)
								{
									final Node att = cdAttrs.getNamedItem("option" + (i + 1));
									if ((att != null) && StringUtil.isNumeric(att.getNodeValue()))
									{
										final int id = parseInteger(att);
										if (OptionData.getInstance().getOptions(id) == null)
										{
											LOGGER.warning(getClass().getSimpleName() + ": Could not find option " + id + " for item " + template);
											continue ITEM;
										}
										
										Map<Integer, EnchantOptions> data = _data.get(itemId);
										if (data == null)
										{
											data = new HashMap<>();
											_data.put(itemId, data);
										}
										
										data.putIfAbsent(op.getLevel(), op);
										
										op.setOption(i, id);
									}
								}
								
								counter++;
							}
						}
					}
				}
			}
		}
		
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _data.size() + " items and " + counter + " options.");
	}
	
	/**
	 * Checks if a specific item has any enchantment options.<br>
	 * This method looks up the {@code itemId} in the internal data map.
	 * @param itemId The unique identifier of the item to check.
	 * @return {@code true} if the item has options, {@code false} otherwise.
	 */
	public boolean hasOptions(int itemId)
	{
		return _data.containsKey(itemId);
	}
	
	/**
	 * Retrieves the enchantment options for a specific item and level.<br>
	 * This method looks up the data in the internal map.<br>
	 * It returns {@code null} if no data is found.
	 * @param itemId The unique identifier of the item.
	 * @param enchantLevel The level of the enchantment.
	 * @return The {@code EnchantOptions} object or {@code null}.
	 */
	public EnchantOptions getOptions(int itemId, int enchantLevel)
	{
		final Map<Integer, EnchantOptions> levelMap = _data.get(itemId);
		return (levelMap != null) ? levelMap.get(enchantLevel) : null;
	}
	
	/**
	 * Retrieves the enchantment options for a specific {@code Item}.<br>
	 * This method checks if the provided {@code item} is {@code null}.<br>
	 * If it is not null, it fetches options based on its ID and enchant level.
	 * @param item The {@code Item} to check.
	 * @return The {@code EnchantOptions} for the item, or {@code null} if the input is {@code null}.
	 */
	public EnchantOptions getOptions(Item item)
	{
		return item != null ? getOptions(item.getId(), item.getEnchantLevel()) : null;
	}
	
	/**
	 * Provides the global instance of {@code EnchantItemOptionsData}.<br>
	 * This method follows the singleton pattern.<br>
	 * Use this to access enchantment options data throughout the game server.
	 * @return The single instance of {@code EnchantItemOptionsData}.
	 */
	public static EnchantItemOptionsData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final EnchantItemOptionsData INSTANCE = new EnchantItemOptionsData();
	}
}
