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
import java.util.List;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.entity.item.combination.CombinationItem;
import org.l2jmobius.gameserver.entity.item.combination.CombinationItemReward;
import org.l2jmobius.gameserver.entity.item.combination.CombinationItemType;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the loading and management of combination item data from XML files.<br>
 * It maps configuration data to {@code CombinationItem} objects for use in the game server.
 * @author UnAfraid, BazookaRpm
 */
public class CombinationItemsData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(CombinationItemsData.class.getName());
	
	private volatile List<CombinationItem> _items = Collections.emptyList();
	
	/**
	 * Initializes a new instance of {@code CombinationItemsData}.<br>
	 * This constructor automatically calls the {@code load()} method.<br>
	 * It prepares the data for use by loading all necessary items.
	 */
	protected CombinationItemsData()
	{
		load();
	}
	
	/**
	 * Loads the combination data from the XML file.<br>
	 * The active list is replaced only when the file parses successfully, so a failed reload keeps the combinations the compound window is already reading.
	 */
	@Override
	public synchronized void load()
	{
		final List<CombinationItem> previous = _items;
		parseDatapackFile("data/CombinationItems.xml");
		if (_items == previous)
		{
			LOGGER.warning("CombinationItemsData: Could not load combinations. Keeping the active list.");
			return;
		}
		
		LOGGER.info("CombinationItemsData: Loaded " + _items.size() + " combinations.");
	}
	
	/**
	 * This method reads data from an XML file.<br>
	 * It populates the internal list of {@code CombinationItem} objects.<br>
	 * The process iterates through all items and rewards defined in the document.
	 * @param document The {@code Document} object containing the parsed XML content.
	 * @param file The {@code File} source used for identifying the data.
	 */
	@Override
	public void parseDocument(Document document, File file)
	{
		if (document == null)
		{
			LOGGER.warning("CombinationItemsData: Cannot parse a null XML document.");
			return;
		}
		
		final List<CombinationItem> items = new ArrayList<>();
		forEach(document, "list", listNode -> forEach(listNode, "item", itemNode ->
		{
			// Create a CombinationItem using attributes from itemNode.
			final CombinationItem item = new CombinationItem(new StatSet(parseAttributes(itemNode)));
			
			forEach(itemNode, "reward", rewardNode ->
			{
				// Parse reward attributes.
				final NamedNodeMap rewardAttrs = rewardNode.getAttributes();
				final int id = parseInteger(rewardAttrs, "id");
				final int count = parseInteger(rewardAttrs, "count", 1);
				final int enchant = parseInteger(rewardAttrs, "enchant", 0);
				final CombinationItemType type = parseEnum(rewardAttrs, CombinationItemType.class, "type");
				
				// Add reward to the item.
				item.addReward(new CombinationItemReward(id, count, type, enchant));
				
				// Log if the item template does not exist.
				if ((id > 0) && (ItemData.getInstance().getTemplate(id) == null))
				{
					LOGGER.info(getClass().getSimpleName() + ": Could not find item with id " + id);
				}
			});
			
			// Collect into the candidate list; it is published once the whole file parses.
			items.add(item);
		}));
		
		if (items.isEmpty())
		{
			throw new IllegalArgumentException("CombinationItemsData: No combinations were found in " + (file != null ? file.getAbsolutePath() : "<unknown>") + ".");
		}
		
		_items = Collections.unmodifiableList(items);
	}
	
	/**
	 * Returns the total number of items loaded into memory.<br>
	 * This count corresponds to the size of the internal {@code _items} list.
	 * @return The number of loaded elements as an {@code int}.
	 */
	public int getLoadedElementsCount()
	{
		return _items.size();
	}
	
	/**
	 * Retrieves the list of all loaded combination items.<br>
	 * This method returns the internal collection of {@code CombinationItem} objects.
	 * @return A {@code List} containing all {@code CombinationItem} entries.
	 */
	public List<CombinationItem> getItems()
	{
		return _items;
	}
	
	/**
	 * Retrieves a {@code CombinationItem} based on specific slot and enchantment values.<br>
	 * It searches through all loaded items to find a matching pair.
	 * @param firstSlot The ID of the first item in the combination.
	 * @param enchantOne The enchantment level of the first item.
	 * @param secondSlot The ID of the second item in the combination.
	 * @param enchantTwo The enchantment level of the second item.
	 * @return The matching {@code CombinationItem} or {@code null} if no match is found.
	 */
	public CombinationItem getItemsBySlots(int firstSlot, int enchantOne, int secondSlot, int enchantTwo)
	{
		for (CombinationItem item : _items)
		{
			if ((item.getItemOne() == firstSlot) && (item.getItemTwo() == secondSlot) && (item.getEnchantOne() == enchantOne) && (item.getEnchantTwo() == enchantTwo))
			{
				return item;
			}
		}
		
		return null;
	}
	
	/**
	 * Retrieves a list of {@code CombinationItem} objects based on the first slot.<br>
	 * It filters items where the ID and enchantment match the provided values.
	 * @param id The unique identifier for the item in the first slot.
	 * @param enchantOne The enchantment level of the item in the first slot.
	 * @return A {@code List} containing all matching {@code CombinationItem} objects.
	 */
	public List<CombinationItem> getItemsByFirstSlot(int id, int enchantOne)
	{
		final List<CombinationItem> result = new ArrayList<>();
		for (CombinationItem item : _items)
		{
			if ((item.getItemOne() == id) && (item.getEnchantOne() == enchantOne))
			{
				result.add(item);
			}
		}
		
		return result;
	}
	
	/**
	 * Retrieves the singleton instance of {@code CombinationItemsData}.<br>
	 * This method provides a global access point to the data.
	 * @return The single instance of {@code CombinationItemsData}.
	 */
	public static CombinationItemsData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final CombinationItemsData INSTANCE = new CombinationItemsData();
	}
}
