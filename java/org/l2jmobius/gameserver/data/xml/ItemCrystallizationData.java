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
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.data.enums.CrystallizationType;
import org.l2jmobius.gameserver.data.holders.CrystallizationDataHolder;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.Armor;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.entity.item.Weapon;
import org.l2jmobius.gameserver.entity.item.enchant.RewardItemsOnFailure;
import org.l2jmobius.gameserver.entity.item.holders.ItemChanceHolder;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.item.type.CrystalType;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the loading and storage of crystallization data from XML files.<br>
 * It defines how items are transformed or modified during the crystallization process.
 * @author UnAfraid, Index
 */
public class ItemCrystallizationData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(ItemCrystallizationData.class.getName());
	
	private final Map<CrystalType, Map<CrystallizationType, List<ItemChanceHolder>>> _crystallizationTemplates = new EnumMap<>(CrystalType.class);
	private final Map<Integer, CrystallizationDataHolder> _items = new HashMap<>();
	
	private RewardItemsOnFailure _weaponDestroyGroup = new RewardItemsOnFailure();
	private RewardItemsOnFailure _armorDestroyGroup = new RewardItemsOnFailure();
	
	/**
	 * This is a protected constructor for the {@code ItemCrystallizationData} class.<br>
	 * It automatically calls the {@code load()} method to initialize data.<br>
	 * Use this constructor when creating an instance of {@code ItemCrystallizationData}.
	 */
	protected ItemCrystallizationData()
	{
		load();
	}
	
	/**
	 * Loads the crystallization data from the configuration files.<br>
	 * This method clears existing data and parses the required XML files.<br>
	 * It then generates all necessary internal data structures.
	 */
	@Override
	public void load()
	{
		_crystallizationTemplates.clear();
		for (CrystalType crystalType : CrystalType.values())
		{
			_crystallizationTemplates.put(crystalType, new EnumMap<>(CrystallizationType.class));
		}
		
		_items.clear();
		
		_weaponDestroyGroup = new RewardItemsOnFailure();
		_armorDestroyGroup = new RewardItemsOnFailure();
		
		parseDatapackFile("data/CrystallizableItems.xml");
		
		final String simpleName = getClass().getSimpleName();
		if (!_crystallizationTemplates.isEmpty())
		{
			LOGGER.info(simpleName + ": Loaded " + _crystallizationTemplates.size() + " crystallization templates.");
		}
		
		if (!_items.isEmpty())
		{
			LOGGER.info(simpleName + ": Loaded " + _items.size() + " pre-defined crystallizable items.");
		}
		
		// Generate remaining data.
		generateCrystallizationData();
		
		if (!_weaponDestroyGroup.isEmpty())
		{
			LOGGER.info(simpleName + ": Loaded " + _weaponDestroyGroup.size() + " weapon enchant failure rewards.");
		}
		
		if (!_armorDestroyGroup.isEmpty())
		{
			LOGGER.info(simpleName + ": Loaded " + _armorDestroyGroup.size() + " armor enchant failure rewards.");
		}
	}
	
	/**
	 * Parses the XML data from a {@code Document} to populate crystallization templates.<br>
	 * This method reads both template and item definitions from the provided file structure.
	 * @param document The {@code Document} object containing the parsed XML content.
	 * @param file The {@code File} source used for identifying the data being processed.
	 */
	@Override
	public void parseDocument(Document document, File file)
	{
		for (Node n = document.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				for (Node o = n.getFirstChild(); o != null; o = o.getNextSibling())
				{
					if ("templates".equalsIgnoreCase(o.getNodeName()))
					{
						for (Node d = o.getFirstChild(); d != null; d = d.getNextSibling())
						{
							if ("crystallizable_template".equalsIgnoreCase(d.getNodeName()))
							{
								final NamedNodeMap dAttrs = d.getAttributes();
								final CrystalType crystalType = parseEnum(dAttrs, CrystalType.class, "crystalType");
								final CrystallizationType crystallizationType = parseEnum(dAttrs, CrystallizationType.class, "crystallizationType");
								final List<ItemChanceHolder> crystallizeRewards = new ArrayList<>();
								for (Node c = d.getFirstChild(); c != null; c = c.getNextSibling())
								{
									if ("item".equalsIgnoreCase(c.getNodeName()))
									{
										final NamedNodeMap attrs = c.getAttributes();
										final int itemId = parseInteger(attrs, "id");
										final long itemCount = parseLong(attrs, "count");
										final double itemChance = parseDouble(attrs, "chance");
										crystallizeRewards.add(new ItemChanceHolder(itemId, itemChance, itemCount));
									}
								}
								
								_crystallizationTemplates.get(crystalType).put(crystallizationType, crystallizeRewards);
							}
						}
					}
					else if ("items".equalsIgnoreCase(o.getNodeName()))
					{
						for (Node d = o.getFirstChild(); d != null; d = d.getNextSibling())
						{
							if ("crystallizable_item".equalsIgnoreCase(d.getNodeName()))
							{
								final int id = parseInteger(d.getAttributes(), "id");
								final List<ItemChanceHolder> crystallizeRewards = new ArrayList<>();
								for (Node c = d.getFirstChild(); c != null; c = c.getNextSibling())
								{
									if ("item".equalsIgnoreCase(c.getNodeName()))
									{
										final NamedNodeMap attrs = c.getAttributes();
										final int itemId = parseInteger(attrs, "id");
										final long itemCount = parseLong(attrs, "count");
										final double itemChance = parseDouble(attrs, "chance");
										crystallizeRewards.add(new ItemChanceHolder(itemId, itemChance, itemCount));
									}
								}
								
								_items.put(id, new CrystallizationDataHolder(id, crystallizeRewards));
							}
						}
					}
					else if ("itemsOnEnchantFailure".equals(o.getNodeName()))
					{
						for (Node d = o.getFirstChild(); d != null; d = d.getNextSibling())
						{
							final String dName = d.getNodeName();
							if ("armor".equalsIgnoreCase(dName))
							{
								_armorDestroyGroup = getFormedHolder(d);
							}
							else if ("weapon".equalsIgnoreCase(dName))
							{
								_weaponDestroyGroup = getFormedHolder(d);
							}
						}
					}
				}
			}
		}
	}
	
	/**
	 * Parses a {@code Node} to create a {@code RewardItemsOnFailure} object.<br>
	 * It iterates through all child nodes named "item".<br>
	 * Each item node is converted into a reward entry based on its attributes.
	 * @param node The XML {@code Node} containing the failure rewards.
	 * @return A populated {@code RewardItemsOnFailure} holder object.
	 */
	private RewardItemsOnFailure getFormedHolder(Node node)
	{
		final RewardItemsOnFailure holder = new RewardItemsOnFailure();
		for (Node z = node.getFirstChild(); z != null; z = z.getNextSibling())
		{
			if ("item".equals(z.getNodeName()))
			{
				final StatSet failItems = new StatSet(parseAttributes(z));
				final int rewardId = failItems.getInt("id");
				final int enchantLevel = failItems.getInt("enchant");
				final double chance = failItems.getDouble("chance");
				
				final String[] targetIds = failItems.getString("targetIds", "").split(",");
				for (String s : targetIds)
				{
					if (!s.isEmpty())
					{
						final int destroyedItemId = Integer.parseInt(s.trim());
						final long count = failItems.getLong("amountR", 1);
						holder.addItemToHolder(destroyedItemId, enchantLevel, rewardId, count, chance);
					}
				}
			}
		}
		
		return holder;
	}
	
	/**
	 * Retrieves the list of reward items for a specific item and enchantment level.<br>
	 * This method checks if the item is a weapon or armor to determine the correct group.<br>
	 * It returns an empty {@code List} if the item is {@code null} or no rewards are defined.
	 * @param item The {@code Item} instance to check for rewards.
	 * @param enchantLevel The current enchantment level of the item.
	 * @return A {@code List} of {@code ItemChanceHolder} objects containing the possible rewards.
	 */
	public List<ItemChanceHolder> getRewardItems(Item item, int enchantLevel)
	{
		if (item == null)
		{
			return Collections.emptyList();
		}
		
		final RewardItemsOnFailure holder = item.isWeapon() ? _weaponDestroyGroup : _armorDestroyGroup;
		if (holder == null)
		{
			return Collections.emptyList();
		}
		
		return holder.getRewardItems(item.getId(), enchantLevel);
	}
	
	/**
	 * Returns the total number of crystallization templates loaded.<br>
	 * This count represents the size of the {@code _crystallizationTemplates} map.
	 * @return The number of loaded templates as an {@code int}.
	 */
	public int getLoadedCrystallizationTemplateCount()
	{
		return _crystallizationTemplates.size();
	}
	
	/**
	 * Calculates the final reward list for an item crystallization.<br>
	 * This method adjusts the drop chances and counts based on the crystal count of the {@code ItemTemplate}.<br>
	 * It ensures that if a chance exceeds 100.0, both the chance and the count are scaled proportionally.
	 * @param item The {@code ItemTemplate} being crystallized.
	 * @param crystallizeRewards The base list of {@code ItemChanceHolder} rewards to process.
	 * @return A new {@code List} containing the calculated {@code ItemChanceHolder} rewards, or {@code null} if the input list is null.
	 */
	private List<ItemChanceHolder> calculateCrystallizeRewards(ItemTemplate item, List<ItemChanceHolder> crystallizeRewards)
	{
		if (crystallizeRewards == null)
		{
			return null;
		}
		
		final List<ItemChanceHolder> rewards = new ArrayList<>();
		for (ItemChanceHolder reward : crystallizeRewards)
		{
			double chance = reward.getChance() * item.getCrystalCount();
			long count = reward.getCount();
			if (chance > 100.)
			{
				final double countMul = Math.ceil(chance / 100.);
				chance /= countMul;
				count *= countMul;
			}
			
			rewards.add(new ItemChanceHolder(reward.getId(), chance, count));
		}
		
		return rewards;
	}
	
	/**
	 * Populates the internal item map with crystallization data.<br>
	 * This method iterates through all items in {@code ItemData}.<br>
	 * It identifies weapons and armor that are marked as crystallizable.<br>
	 * New entries are added to the {@code _items} map based on existing templates.
	 */
	private void generateCrystallizationData()
	{
		final int previousCount = _items.size();
		for (ItemTemplate item : ItemData.getInstance().getAllItems())
		{
			// Check if the data has not been generated.
			if (((item instanceof Weapon) || (item instanceof Armor)) && item.isCrystallizable())
			{
				final Integer itemId = item.getId();
				if (!_items.containsKey(itemId))
				{
					final List<ItemChanceHolder> holder = _crystallizationTemplates.get(item.getCrystalType()).get((item instanceof Weapon) ? CrystallizationType.WEAPON : CrystallizationType.ARMOR);
					if (holder != null)
					{
						_items.put(itemId, new CrystallizationDataHolder(itemId, calculateCrystallizeRewards(item, holder)));
					}
				}
			}
		}
		
		final int generated = _items.size() - previousCount;
		if (generated > 0)
		{
			LOGGER.info(getClass().getSimpleName() + ": Generated " + generated + " crystallizable items from templates.");
		}
	}
	
	/**
	 * Retrieves the list of possible rewards for a specific crystallization.<br>
	 * This method looks up the template based on the provided crystal and type.
	 * @param crystalType The type of crystal used for the process.
	 * @param crystallizationType The specific category of crystallization.
	 * @return A {@code List} of {@code ItemChanceHolder} objects representing the rewards.
	 */
	public List<ItemChanceHolder> getCrystallizationTemplate(CrystalType crystalType, CrystallizationType crystallizationType)
	{
		return _crystallizationTemplates.get(crystalType).get(crystallizationType);
	}
	
	/**
	 * Retrieves the crystallization data for a specific item.<br>
	 * This method looks up the data using the provided unique identifier.
	 * @param itemId The unique ID of the item to look up.
	 * @return The {@code CrystallizationDataHolder} associated with the item, or {@code null} if not found.
	 */
	public CrystallizationDataHolder getCrystallizationData(int itemId)
	{
		return _items.get(itemId);
	}
	
	/**
	 * Retrieves the list of rewards for crystallizing a specific item.<br>
	 * This method checks if the item has defined crystallization data.<br>
	 * If no data exists, it returns a default crystal reward based on the item template.
	 * @param item The {@code Item} instance to check for rewards.
	 * @return A {@code List} of {@code ItemChanceHolder} objects containing the rewards.
	 */
	public List<ItemChanceHolder> getCrystallizationRewards(Item item)
	{
		final List<ItemChanceHolder> result = new ArrayList<>();
		final int crystalItemId = item.getTemplate().getCrystalItemId();
		final CrystallizationDataHolder data = getCrystallizationData(item.getId());
		if (data != null)
		{
			// If there are no crystals on the template, add such.
			boolean found = false;
			final List<ItemChanceHolder> items = data.getItems();
			for (ItemChanceHolder holder : items)
			{
				if (holder.getId() == crystalItemId)
				{
					found = true;
					break;
				}
			}
			
			if (!found)
			{
				result.add(new ItemChanceHolder(crystalItemId, 100, item.getCrystalCount()));
			}
			
			result.addAll(items);
		}
		else
		{
			// Add basic crystal reward.
			result.add(new ItemChanceHolder(crystalItemId, 100, item.getCrystalCount()));
		}
		
		return result;
	}
	
	/**
	 * Retrieves the list of items rewarded when an object is destroyed.<br>
	 * This method checks if the {@code item} is a weapon or armor to determine the correct reward group.<br>
	 * It returns an empty list if either {@code player} or {@code item} is {@code null}.
	 * @param player The {@code Player} who destroyed the item.
	 * @param item The {@code Item} that was destroyed.
	 * @return A {@code List} of {@code ItemChanceHolder} objects containing the rewards.
	 */
	public List<ItemChanceHolder> getItemOnDestroy(Player player, Item item)
	{
		if ((player == null) || (item == null))
		{
			return Collections.emptyList();
		}
		
		final RewardItemsOnFailure holder = item.isWeapon() ? _weaponDestroyGroup : _armorDestroyGroup;
		final List<ItemChanceHolder> rewards = holder.getRewardItems(item.getId(), item.getEnchantLevel());
		return rewards.isEmpty() ? Collections.emptyList() : rewards;
	}
	
	/**
	 * Retrieves the singleton instance of {@code ItemCrystallizationData}.<br>
	 * This method provides a global access point to the crystallization data.
	 * @return The single instance of {@code ItemCrystallizationData}.
	 */
	public static ItemCrystallizationData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final ItemCrystallizationData INSTANCE = new ItemCrystallizationData();
	}
}
