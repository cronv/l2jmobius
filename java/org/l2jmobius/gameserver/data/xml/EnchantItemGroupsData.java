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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.data.holders.RangeChanceHolder;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.entity.item.enchant.EnchantItemGroup;
import org.l2jmobius.gameserver.entity.item.enchant.EnchantRateItem;
import org.l2jmobius.gameserver.entity.item.enchant.EnchantScrollGroup;
import org.l2jmobius.gameserver.entity.item.enums.BodyPart;

/**
 * This class handles the loading and parsing of enchantment item group data from XML files.<br>
 * It maps specific items to their respective {@code EnchantItemGroup} categories for use in the game server.
 * @author UnAfraid, BazookaRpm
 */
public class EnchantItemGroupsData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(EnchantItemGroupsData.class.getName());
	
	private final Map<String, EnchantItemGroup> _itemGroups = new ConcurrentHashMap<>();
	private final Map<Integer, EnchantScrollGroup> _scrollGroups = new ConcurrentHashMap<>();
	private int _maxWeaponEnchant = 0;
	private int _maxArmorEnchant = 0;
	private int _maxAccessoryEnchant = 0;
	
	/**
	 * Initializes the {@code EnchantItemGroupsData} instance.<br>
	 * This constructor calls the {@code load()} method to populate data.
	 */
	protected EnchantItemGroupsData()
	{
		load();
	}
	
	/**
	 * Loads the enchantment item group data from the XML file.<br>
	 * This method clears existing maps and parses the configuration.<br>
	 * It also logs the number of loaded templates to the server log.
	 */
	@Override
	public synchronized void load()
	{
		_itemGroups.clear();
		_scrollGroups.clear();
		_maxWeaponEnchant = 0;
		_maxArmorEnchant = 0;
		_maxAccessoryEnchant = 0;
		parseDatapackFile("data/EnchantItemGroups.xml");
		final String simpleName = getClass().getSimpleName();
		LOGGER.info(simpleName + ": Loaded " + _itemGroups.size() + " item group templates.");
		LOGGER.info(simpleName + ": Loaded " + _scrollGroups.size() + " scroll group templates.");
		
		if (PlayerConfig.OVER_ENCHANT_PROTECTION)
		{
			LOGGER.info(simpleName + ": Max weapon enchant is set to " + _maxWeaponEnchant + ".");
			LOGGER.info(simpleName + ": Max armor enchant is set to " + _maxArmorEnchant + ".");
			LOGGER.info(simpleName + ": Max accessory enchant is set to " + _maxAccessoryEnchant + ".");
		}
	}
	
	/**
	 * Parses the provided {@code Document} to populate enchantment data.<br>
	 * This method reads item groups and scroll groups from the XML structure.<br>
	 * It also calculates the maximum enchantment levels for different equipment types.
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
					if ("enchantRateGroup".equalsIgnoreCase(dName))
					{
						final String name = parseString(d.getAttributes(), "name");
						final EnchantItemGroup group = new EnchantItemGroup(name);
						for (Node cd = d.getFirstChild(); cd != null; cd = cd.getNextSibling())
						{
							if ("current".equalsIgnoreCase(cd.getNodeName()))
							{
								final NamedNodeMap cdAttrs = cd.getAttributes();
								final String range = parseString(cdAttrs, "enchant");
								final double chance = parseDouble(cdAttrs, "chance");
								int min = -1;
								int max = 0;
								if (range.contains("-"))
								{
									final String[] split = range.split("-");
									if ((split.length == 2) && StringUtil.isNumeric(split[0]) && StringUtil.isNumeric(split[1]))
									{
										min = Integer.parseInt(split[0]);
										max = Integer.parseInt(split[1]);
									}
								}
								else if (StringUtil.isNumeric(range))
								{
									min = Integer.parseInt(range);
									max = min;
								}
								
								if ((min > -1) && (max > -1))
								{
									group.addChance(new RangeChanceHolder(min, max, chance));
								}
								
								// Try to get a generic max value.
								if (chance > 0)
								{
									if (name.contains("WEAPON"))
									{
										if (_maxWeaponEnchant < max)
										{
											_maxWeaponEnchant = max;
										}
									}
									else if (name.contains("ACCESSORIES") || name.contains("RING") || name.contains("EARRING") || name.contains("NECK"))
									{
										if (_maxAccessoryEnchant < max)
										{
											_maxAccessoryEnchant = max;
										}
									}
									else if (_maxArmorEnchant < max)
									{
										_maxArmorEnchant = max;
									}
								}
							}
						}
						
						_itemGroups.put(name, group);
					}
					else if ("enchantScrollGroup".equalsIgnoreCase(dName))
					{
						final int id = parseInteger(d.getAttributes(), "id");
						final EnchantScrollGroup group = new EnchantScrollGroup(id);
						for (Node cd = d.getFirstChild(); cd != null; cd = cd.getNextSibling())
						{
							if ("enchantRate".equalsIgnoreCase(cd.getNodeName()))
							{
								final EnchantRateItem rateGroup = new EnchantRateItem(parseString(cd.getAttributes(), "group"));
								for (Node z = cd.getFirstChild(); z != null; z = z.getNextSibling())
								{
									if ("item".equalsIgnoreCase(z.getNodeName()))
									{
										final NamedNodeMap attrs = z.getAttributes();
										if (attrs.getNamedItem("slot") != null)
										{
											rateGroup.addSlot(BodyPart.fromName(parseString(attrs, "slot")).getMask());
										}
										
										if (attrs.getNamedItem("magicWeapon") != null)
										{
											rateGroup.setMagicWeapon(parseBoolean(attrs, "magicWeapon"));
										}
										
										if (attrs.getNamedItem("itemId") != null)
										{
											rateGroup.addItemId(parseInteger(attrs, "itemId"));
										}
									}
								}
								
								group.addRateGroup(rateGroup);
							}
						}
						
						_scrollGroups.put(id, group);
					}
				}
			}
		}
		
		// In case there is no accessories group set.
		if (_maxAccessoryEnchant == 0)
		{
			_maxAccessoryEnchant = _maxArmorEnchant;
		}
		
		// Max enchant values are set to current max enchant + 1.
		_maxWeaponEnchant += 1;
		_maxArmorEnchant += 1;
		_maxAccessoryEnchant += 1;
	}
	
	/**
	 * Retrieves the {@code EnchantItemGroup} for a specific item and scroll group.<br>
	 * This method finds the correct rate group based on the provided {@code scrollGroup}.<br>
	 * It returns the associated group or {@code null} if no match is found.
	 * @param item The {@code ItemTemplate} to check.
	 * @param scrollGroup The unique ID of the {@code EnchantScrollGroup}.
	 * @return The corresponding {@code EnchantItemGroup} or {@code null}.
	 */
	public EnchantItemGroup getItemGroup(ItemTemplate item, int scrollGroup)
	{
		final EnchantScrollGroup group = _scrollGroups.get(scrollGroup);
		final EnchantRateItem rateGroup = group.getRateGroup(item);
		return rateGroup != null ? _itemGroups.get(rateGroup.getName()) : null;
	}
	
	/**
	 * Retrieves an {@code EnchantItemGroup} based on its unique name.<br>
	 * This method looks up the group in the internal data map.
	 * @param name The unique identifier for the item group.
	 * @return The corresponding {@code EnchantItemGroup} or {@code null} if not found.
	 */
	public EnchantItemGroup getItemGroup(String name)
	{
		return _itemGroups.get(name);
	}
	
	/**
	 * Retrieves a specific {@code EnchantScrollGroup} based on its unique identifier.<br>
	 * This method looks up the group in the internal data map.
	 * @param id The unique integer ID of the scroll group to find.
	 * @return The {@code EnchantScrollGroup} associated with the provided ID, or {@code null} if not found.
	 */
	public EnchantScrollGroup getScrollGroup(int id)
	{
		return _scrollGroups.get(id);
	}
	
	/**
	 * Retrieves the maximum enchantment level allowed for weapons.<br>
	 * This value is loaded from the configuration data.
	 * @return The maximum weapon enchantment level as an {@code int}.
	 */
	public int getMaxWeaponEnchant()
	{
		return _maxWeaponEnchant;
	}
	
	/**
	 * Returns the maximum enchantment level for armor items.<br>
	 * This value is loaded from the configuration data.
	 * @return the maximum armor enchantment level as an {@code int}.
	 */
	public int getMaxArmorEnchant()
	{
		return _maxArmorEnchant;
	}
	
	/**
	 * Retrieves the maximum enchantment level for accessory items.<br>
	 * This value is loaded from the configuration data.
	 * @return The maximum allowed enchantment level as an {@code int}.
	 */
	public int getMaxAccessoryEnchant()
	{
		return _maxAccessoryEnchant;
	}
	
	/**
	 * Retrieves the singleton instance of {@code EnchantItemGroupsData}.<br>
	 * This method provides a global access point to the enchantment data.
	 * @return The single instance of {@code EnchantItemGroupsData}.
	 */
	public static EnchantItemGroupsData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final EnchantItemGroupsData INSTANCE = new EnchantItemGroupsData();
	}
}
