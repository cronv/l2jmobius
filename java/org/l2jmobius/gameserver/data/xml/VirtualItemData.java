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
import org.l2jmobius.gameserver.config.IllusoryEquipmentConfig;
import org.l2jmobius.gameserver.data.holders.VirtualItemHolder;
import org.l2jmobius.gameserver.managers.GlobalVariablesManager;

/**
 * This class handles the loading and parsing of virtual item data from XML files.<br>
 * It maps configuration values to {@code VirtualItemHolder} objects for use in the game server.
 * @author CostyKiller
 */
public class VirtualItemData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(VirtualItemData.class.getName());
	
	/** Virtual items mapped by their main index, then by their sub index. */
	private static final Map<Integer, Map<Integer, VirtualItemHolder>> VIRTUAL_ITEMS = new HashMap<>();
	
	/** Virtual items carried as an equipped item, mapped by their item id, then by their enchant level. */
	private static final Map<Integer, Map<Integer, VirtualItemHolder>> VIRTUAL_ITEMS_BY_ITEM = new HashMap<>();
	
	/** Virtual items carried as a skill, mapped by their skill id, then by their skill level. */
	private static final Map<Integer, Map<Integer, VirtualItemHolder>> VIRTUAL_ITEMS_BY_SKILL = new HashMap<>();
	
	/**
	 * This is the default constructor for {@code VirtualItemData}.<br>
	 * It initializes the data object and checks if illusory equipment is enabled.<br>
	 * If enabled, it calls the {@code load()} method to populate the items.
	 */
	protected VirtualItemData()
	{
		if (IllusoryEquipmentConfig.ILLUSORY_EQUIPMENT_ENABLED)
		{
			load();
		}
	}
	
	/**
	 * Loads the virtual item data from the configuration files.<br>
	 * This method clears existing data before parsing new values.<br>
	 * It checks if {@code IllusoryEquipmentConfig.ILLUSORY_EQUIPMENT_ENABLED} is true before loading.<br>
	 * A log message is printed to indicate the number of items loaded or if the system is disabled.
	 */
	@Override
	public void load()
	{
		VIRTUAL_ITEMS.clear();
		VIRTUAL_ITEMS_BY_ITEM.clear();
		VIRTUAL_ITEMS_BY_SKILL.clear();
		
		if (IllusoryEquipmentConfig.ILLUSORY_EQUIPMENT_ENABLED)
		{
			parseDatapackFile("data/VirtualItemData.xml");
			
			// Stamp the event start on the first startup with the system enabled, the client counts down from it.
			final GlobalVariablesManager globalVariables = GlobalVariablesManager.getInstance();
			if (globalVariables.getLong(GlobalVariablesManager.VIRTUAL_ITEM_EVENT_START, 0) == 0)
			{
				globalVariables.set(GlobalVariablesManager.VIRTUAL_ITEM_EVENT_START, System.currentTimeMillis());
			}
		}
		
		int itemCount = 0;
		for (Map<Integer, VirtualItemHolder> category : VIRTUAL_ITEMS.values())
		{
			itemCount += category.size();
		}
		
		if (itemCount > 0)
		{
			LOGGER.info(getClass().getSimpleName() + ": Loaded " + itemCount + " virtual items in " + VIRTUAL_ITEMS.size() + " categories.");
			
			if (IllusoryEquipmentConfig.ILLUSORY_EQUIPMENT_EVENT_DEBUG_ENABLED)
			{
				LOGGER.info(getClass().getSimpleName() + ": Event runs for another " + getEventRemainingTime() + " seconds.");
				for (Map.Entry<Integer, Map<Integer, VirtualItemHolder>> category : VIRTUAL_ITEMS.entrySet())
				{
					LOGGER.info(getClass().getSimpleName() + ": Category " + category.getKey() + " holds " + category.getValue().size() + " entries.");
				}
			}
		}
		else
		{
			LOGGER.info(getClass().getSimpleName() + ": System is disabled.");
		}
	}
	
	/**
	 * Parses the provided {@code Document} to load virtual items.<br>
	 * It reads data from the XML file and populates the internal item map.
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
					if ("virtualItem".equalsIgnoreCase(d.getNodeName()))
					{
						final int indexMain = parseInteger(d.getAttributes(), "indexMain");
						for (Node b = d.getFirstChild(); b != null; b = b.getNextSibling())
						{
							if (!"virtualItemStat".equalsIgnoreCase(b.getNodeName()))
							{
								continue;
							}
							
							final NamedNodeMap attrs = b.getAttributes();
							final long slot = parseLong(attrs, "slot");
							final int indexSub = parseInteger(attrs, "indexSub");
							final int itemId = parseInteger(attrs, "itemId");
							final int enchant = parseInteger(attrs, "enchant");
							final int costVISPoint = parseInteger(attrs, "costVISPoint");
							
							// Entries without a body part slot grant a skill, the remaining ones equip an item.
							if (slot == 0)
							{
								if (SkillData.getInstance().getSkill(itemId, enchant) == null)
								{
									LOGGER.warning(getClass().getSimpleName() + ": Could not find skill " + itemId + " level " + enchant + " for virtual item " + indexMain + "-" + indexSub + ".");
									continue;
								}
							}
							else if (ItemData.getInstance().getTemplate(itemId) == null)
							{
								LOGGER.warning(getClass().getSimpleName() + ": Could not find item template " + itemId + " for virtual item " + indexMain + "-" + indexSub + ".");
								continue;
							}
							
							final VirtualItemHolder holder = new VirtualItemHolder(indexMain, slot, indexSub, itemId, enchant, costVISPoint);
							addToIndex(VIRTUAL_ITEMS, indexMain, indexSub, holder);
							if (slot == 0)
							{
								addToIndex(VIRTUAL_ITEMS_BY_SKILL, itemId, enchant, holder);
							}
							else
							{
								addToIndex(VIRTUAL_ITEMS_BY_ITEM, itemId, enchant, holder);
							}
						}
					}
				}
			}
		}
	}
	
	/**
	 * Retrieves a specific virtual item.
	 * @param indexMain The category index of the virtual item.
	 * @param indexSub The index of the virtual item inside its category.
	 * @return The {@code VirtualItemHolder} for the given indexes, or {@code null} if not found.
	 */
	public VirtualItemHolder getVirtualItem(int indexMain, int indexSub)
	{
		return getFromIndex(VIRTUAL_ITEMS, indexMain, indexSub);
	}
	
	/**
	 * Retrieves the virtual item an equipped item stands for.
	 * @param itemId The identifier of the equipped item.
	 * @param enchant The enchant level of the equipped item.
	 * @return The matching {@code VirtualItemHolder}, or {@code null} when the item is not part of the system.
	 */
	public VirtualItemHolder getVirtualItemByItem(int itemId, int enchant)
	{
		return getFromIndex(VIRTUAL_ITEMS_BY_ITEM, itemId, enchant);
	}
	
	/**
	 * Retrieves the virtual item a known skill stands for.
	 * @param skillId The identifier of the skill.
	 * @param level The level of the skill.
	 * @return The matching {@code VirtualItemHolder}, or {@code null} when the skill is not part of the system.
	 */
	public VirtualItemHolder getVirtualItemBySkill(int skillId, int level)
	{
		return getFromIndex(VIRTUAL_ITEMS_BY_SKILL, skillId, level);
	}
	
	/**
	 * Retrieves every virtual item of a category.
	 * @param indexMain The category index of the virtual items.
	 * @return A {@code Collection} with the items of the category, empty if the category is unknown.
	 */
	public Collection<VirtualItemHolder> getVirtualItems(int indexMain)
	{
		final Map<Integer, VirtualItemHolder> category = VIRTUAL_ITEMS.get(indexMain);
		if (category == null)
		{
			return Collections.emptyList();
		}
		
		return category.values();
	}
	
	/**
	 * Retrieves all loaded virtual items, mapped by their main index, then by their sub index.
	 * @return A {@code Map} containing every {@code VirtualItemHolder} stored in the system.
	 */
	public Map<Integer, Map<Integer, VirtualItemHolder>> getVirtualItems()
	{
		return VIRTUAL_ITEMS;
	}
	
	/**
	 * Calculates the points a virtual item costs in total.<br>
	 * Every entry of the category up to the wanted sub index is paid for, as the costs in the data are<br>
	 * the steps taken to reach that entry, not its full price. This is what the window charges as well.
	 * @param holder The virtual item to price.
	 * @return The amount of points the given virtual item costs.
	 */
	public int getTotalCost(VirtualItemHolder holder)
	{
		final Map<Integer, VirtualItemHolder> category = VIRTUAL_ITEMS.get(holder.getIndexMain());
		if (category == null)
		{
			return holder.getCostVISPoint();
		}
		
		final int indexSub = holder.getIndexSub();
		int cost = 0;
		for (VirtualItemHolder entry : category.values())
		{
			if (entry.getIndexSub() <= indexSub)
			{
				cost += entry.getCostVISPoint();
			}
		}
		
		return cost;
	}
	
	/**
	 * Calculates how long the event still runs, counted from the start stamped on the first startup.<br>
	 * The client counts down from this value, it is not a unix timestamp.
	 * @return The remaining event time in seconds, {@code 0} once it has run out.
	 */
	public int getEventRemainingTime()
	{
		final long start = GlobalVariablesManager.getInstance().getLong(GlobalVariablesManager.VIRTUAL_ITEM_EVENT_START, 0);
		if (start == 0)
		{
			return 0;
		}
		
		// Duration is configured in months of 30 days.
		final long remaining = ((start + (2592000000L * IllusoryEquipmentConfig.ILLUSORY_EQUIPMENT_EVENT_DURATION)) - System.currentTimeMillis()) / 1000;
		if (remaining < 0)
		{
			return 0;
		}
		
		return (int) remaining;
	}
	
	/**
	 * Stores a virtual item in one of the two level index maps.
	 * @param index The map to store the virtual item in.
	 * @param firstKey The key of the outer map.
	 * @param secondKey The key of the inner map.
	 * @param holder The virtual item to store.
	 */
	private static void addToIndex(Map<Integer, Map<Integer, VirtualItemHolder>> index, int firstKey, int secondKey, VirtualItemHolder holder)
	{
		Map<Integer, VirtualItemHolder> entries = index.get(firstKey);
		if (entries == null)
		{
			entries = new HashMap<>();
			index.put(firstKey, entries);
		}
		
		entries.put(secondKey, holder);
	}
	
	/**
	 * Reads a virtual item from one of the two level index maps.
	 * @param index The map to read the virtual item from.
	 * @param firstKey The key of the outer map.
	 * @param secondKey The key of the inner map.
	 * @return The stored {@code VirtualItemHolder}, or {@code null} when either key is unknown.
	 */
	private static VirtualItemHolder getFromIndex(Map<Integer, Map<Integer, VirtualItemHolder>> index, int firstKey, int secondKey)
	{
		final Map<Integer, VirtualItemHolder> entries = index.get(firstKey);
		if (entries == null)
		{
			return null;
		}
		
		return entries.get(secondKey);
	}
	
	/**
	 * Retrieves the singleton instance of {@code VirtualItemData}.<br>
	 * Use this method to access the global data manager.
	 * @return The single instance of {@code VirtualItemData}.
	 */
	public static VirtualItemData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final VirtualItemData INSTANCE = new VirtualItemData();
	}
}
