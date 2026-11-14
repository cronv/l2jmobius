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
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.instance.Item;

/**
 * Loads and provides enchant challenge point data from XML.
 * @author Serenitty
 */
public class EnchantChallengePointData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(EnchantChallengePointData.class.getName());
	
	public static final int OPTION_PROB_INC1 = 0;
	public static final int OPTION_PROB_INC2 = 1;
	public static final int OPTION_OVER_UP_PROB = 2;
	public static final int OPTION_NUM_RESET_PROB = 3;
	public static final int OPTION_NUM_DOWN_PROB = 4;
	public static final int OPTION_NUM_PROTECT_PROB = 5;
	
	private final Map<Integer, Map<Integer, EnchantChallengePointsOptionInfo>> _groupOptions = new HashMap<>();
	private final Map<Integer, Integer> _fees = new HashMap<>();
	private final Map<Integer, EnchantChallengePointsItemInfo> _items = new HashMap<>();
	private int _maxPoints;
	private int _maxTicketCharge;
	
	/**
	 * Constructs the singleton and loads the configuration data.
	 */
	public EnchantChallengePointData()
	{
		load();
	}
	
	/**
	 * Clears the cached maps and parses the enchant challenge points XML.
	 */
	@Override
	public synchronized void load()
	{
		_groupOptions.clear();
		_fees.clear();
		_items.clear();
		parseDatapackFile("data/EnchantChallengePoints.xml");
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _groupOptions.size() + " groups and " + _fees.size() + " options.");
	}
	
	/**
	 * Parses the XML document to populate max points, fees, group options and items.
	 * @param document The {@code Document} containing the parsed XML.
	 * @param file The source {@code File}.
	 */
	@Override
	public void parseDocument(Document document, File file)
	{
		for (Node n = document.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				for (Node z = n.getFirstChild(); z != null; z = z.getNextSibling())
				{
					final String zName = z.getNodeName();
					if ("maxPoints".equalsIgnoreCase(zName))
					{
						_maxPoints = Integer.parseInt(z.getTextContent());
					}
					else if ("maxTicketCharge".equalsIgnoreCase(zName))
					{
						_maxTicketCharge = Integer.parseInt(z.getTextContent());
					}
					else if ("fees".equalsIgnoreCase(zName))
					{
						for (Node d = z.getFirstChild(); d != null; d = d.getNextSibling())
						{
							if ("option".equalsIgnoreCase(d.getNodeName()))
							{
								final NamedNodeMap attrs = d.getAttributes();
								final int index = parseInteger(attrs, "index");
								final int fee = parseInteger(attrs, "fee");
								_fees.put(index, fee);
							}
						}
					}
					else if ("groups".equalsIgnoreCase(zName))
					{
						for (Node d = z.getFirstChild(); d != null; d = d.getNextSibling())
						{
							if ("group".equalsIgnoreCase(d.getNodeName()))
							{
								final NamedNodeMap attrs = d.getAttributes();
								final int groupId = parseInteger(attrs, "id");
								
								Map<Integer, EnchantChallengePointsOptionInfo> options = _groupOptions.get(groupId);
								if (options == null)
								{
									options = new HashMap<>();
									_groupOptions.put(groupId, options);
								}
								
								for (Node e = d.getFirstChild(); e != null; e = e.getNextSibling())
								{
									final String eName = e.getNodeName();
									if ("option".equalsIgnoreCase(eName))
									{
										final NamedNodeMap optionAttrs = e.getAttributes();
										final int index = parseInteger(optionAttrs, "index");
										final int chance = parseInteger(optionAttrs, "chance");
										final int minEnchant = parseInteger(optionAttrs, "minEnchant");
										final int maxEnchant = parseInteger(optionAttrs, "maxEnchant");
										options.put(index, new EnchantChallengePointsOptionInfo(index, chance, minEnchant, maxEnchant));
									}
									else if ("item".equals(eName))
									{
										final NamedNodeMap itemAttrs = e.getAttributes();
										final String[] itemIdsStr = parseString(itemAttrs, "id").split(";");
										Map<Integer, Integer> enchantLevels = new HashMap<>();
										for (Node g = e.getFirstChild(); g != null; g = g.getNextSibling())
										{
											if ("enchant".equals(g.getNodeName()))
											{
												final NamedNodeMap enchantAttrs = g.getAttributes();
												final int enchantLevel = parseInteger(enchantAttrs, "level");
												final int points = parseInteger(enchantAttrs, "points");
												enchantLevels.put(enchantLevel, points);
											}
										}
										
										for (String itemIdStr : itemIdsStr)
										{
											final int itemId = Integer.parseInt(itemIdStr);
											if (ItemData.getInstance().getTemplate(itemId) == null)
											{
												LOGGER.info(getClass().getSimpleName() + ": Item with id " + itemId + " does not exist.");
											}
											else
											{
												_items.put(itemId, new EnchantChallengePointsItemInfo(itemId, groupId, enchantLevels));
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}
	
	/**
	 * Retrieves the maximum number of points allowed.
	 * @return The maximum points as an {@code int}.
	 */
	public int getMaxPoints()
	{
		return _maxPoints;
	}
	
	/**
	 * Retrieves the maximum allowed ticket charge.
	 * @return The maximum ticket charge as an {@code int}.
	 */
	public int getMaxTicketCharge()
	{
		return _maxTicketCharge;
	}
	
	/**
	 * Retrieves the option configuration for the given group and option index.
	 * @param groupId The group id.
	 * @param optionIndex The option index within the group.
	 * @return The matching {@code EnchantChallengePointsOptionInfo}.
	 */
	public EnchantChallengePointsOptionInfo getOptionInfo(int groupId, int optionIndex)
	{
		return _groupOptions.get(groupId).get(optionIndex);
	}
	
	/**
	 * Retrieves the challenge point item info for the given item id.
	 * @param itemId The item id to look up.
	 * @return The matching {@code EnchantChallengePointsItemInfo}, or {@code null} if not found.
	 */
	public EnchantChallengePointsItemInfo getInfoByItemId(int itemId)
	{
		return _items.get(itemId);
	}
	
	/**
	 * Retrieves the fee for the given option index.
	 * @param optionIndex The option index.
	 * @return The fee as an {@code int}.
	 */
	public int getFeeForOptionIndex(int optionIndex)
	{
		return _fees.get(optionIndex);
	}
	
	/**
	 * Awards challenge points for a failed enchantment based on the item enchant level.
	 * @param player The {@code Player} who attempted the enchantment.
	 * @param item The enchanted {@code Item}.
	 * @return An {@code int[]} of group id and points awarded, or {@code -1} for both if the item is unknown.
	 */
	public int[] handleFailure(Player player, Item item)
	{
		final EnchantChallengePointsItemInfo info = getInfoByItemId(item.getId());
		if (info == null)
		{
			return new int[]
			{
				-1,
				-1
			};
		}
		
		final int groupId = info.groupId();
		final int pointsToGive = info.pointsByEnchantLevel().getOrDefault(item.getEnchantLevel(), 0);
		if (pointsToGive > 0)
		{
			final var challengeInfo = player.getChallengeInfo();
			challengeInfo.getChallengePoints().compute(groupId, (_, v) -> v == null ? Math.min(getMaxPoints(), pointsToGive) : Math.min(getMaxPoints(), v + pointsToGive));
			challengeInfo.setNowGroup(groupId);
			challengeInfo.setNowGroup(pointsToGive);
		}
		
		return new int[]
		{
			groupId,
			pointsToGive
		};
	}
	
	/**
	 * Configuration for a single enchant challenge option, holding probability and level constraints.
	 * @param index The option index.
	 * @param chance The probability of this option.
	 * @param minEnchant The minimum enchant level.
	 * @param maxEnchant The maximum enchant level.
	 */
	public record EnchantChallengePointsOptionInfo(int index, int chance, int minEnchant, int maxEnchant)
	{
	}
	
	/**
	 * Information for an item in the enchant challenge, mapping enchant levels to point costs.
	 * @param itemId The item id.
	 * @param groupId The group id.
	 * @param pointsByEnchantLevel A map of enchant level to point cost.
	 */
	public record EnchantChallengePointsItemInfo(int itemId, int groupId, Map<Integer, Integer> pointsByEnchantLevel)
	{
	}
	
	/**
	 * Retrieves the singleton instance.
	 * @return The single {@code EnchantChallengePointData} instance.
	 */
	public static EnchantChallengePointData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final EnchantChallengePointData INSTANCE = new EnchantChallengePointData();
	}
}
