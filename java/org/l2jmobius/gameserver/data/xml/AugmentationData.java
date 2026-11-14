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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.entity.item.EtcItem;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.options.Augmentation;
import org.l2jmobius.gameserver.mechanics.options.AugmentationFee;
import org.l2jmobius.gameserver.mechanics.options.AugmentationInstance;
import org.l2jmobius.gameserver.mechanics.options.OptionDataCategory;
import org.l2jmobius.gameserver.mechanics.options.OptionDataGroup;
import org.l2jmobius.gameserver.mechanics.options.Options;

/**
 * This class handles the loading and storage of {@code Augmentation} data from XML files.<br>
 * It provides a structured way to manage game mechanics related to item augmentations.
 * @author Pere, Mobius
 */
public class AugmentationData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(AugmentationData.class.getSimpleName());
	
	private final Map<Integer, Set<Integer>> _itemGroups = new HashMap<>();
	private final Map<Integer, List<Augmentation>> _augmentations = new ConcurrentHashMap<>();
	private final Map<Integer, Map<Integer, AugmentationFee>> _fees = new ConcurrentHashMap<>();
	
	/**
	 * Initializes a new instance of the {@code AugmentationData} class.<br>
	 * This constructor automatically calls the {@code load()} method.<br>
	 * It prepares the data required for augmentation processing.
	 */
	protected AugmentationData()
	{
		load();
	}
	
	/**
	 * Loads the augmentation data from the XML file.<br>
	 * This method clears existing data before parsing the new values.<br>
	 * It logs the total number of augmentations and fees loaded to the console.
	 */
	@Override
	public void load()
	{
		_itemGroups.clear();
		_augmentations.clear();
		_fees.clear();
		parseDatapackFile("data/stats/augmentation/Augmentations.xml");
		final String simpleName = getClass().getSimpleName();
		LOGGER.info(simpleName + ": Loaded " + _itemGroups.size() + " item groups.");
		LOGGER.info(simpleName + ": Loaded " + _augmentations.size() + " augmentations.");
		LOGGER.info(simpleName + ": Loaded " + _fees.size() + " fees.");
	}
	
	/**
	 * Parses the provided {@code Document} to load augmentation and fee data.<br>
	 * This method reads augmentations, option groups, and fees from the XML structure.<br>
	 * It populates internal maps with mineral IDs and associated item fees.
	 * @param document The {@code Document} object containing the XML data.
	 * @param file The {@code File} source of the parsed document.
	 */
	@Override
	public void parseDocument(Document document, File file)
	{
		forEach(document, "list", listNode ->
		{
			forEach(listNode, "augmentations", augmentationsNode -> forEach(augmentationsNode, "augmentation", augmentationNode ->
			{
				final NamedNodeMap augmentationAttrs = augmentationNode.getAttributes();
				final int mineralId = parseInteger(augmentationAttrs, "mineralId");
				final int itemGroup = parseInteger(augmentationAttrs, "itemGroup", -1);
				if (ItemData.getInstance().getTemplate(mineralId) == null)
				{
					LOGGER.warning(getClass().getSimpleName() + ": Mineral with item id " + mineralId + " was not found.");
				}
				
				final Augmentation augmentation = new Augmentation(mineralId, itemGroup);
				
				forEach(augmentationNode, "optionGroup", groupNode ->
				{
					final int order = parseInteger(groupNode.getAttributes(), "order");
					final List<OptionDataCategory> sets = new ArrayList<>();
					forEach(groupNode, "optionCategory", categoryNode ->
					{
						final double chance = parseDouble(categoryNode.getAttributes(), "chance");
						final Map<Options, Double> options = new HashMap<>();
						forEach(categoryNode, "option", optionNode ->
						{
							final NamedNodeMap optAttrs = optionNode.getAttributes();
							final int optionId = parseInteger(optAttrs, "id");
							final Options opt = OptionData.getInstance().getOptions(optionId);
							if (opt == null)
							{
								LOGGER.warning(getClass().getSimpleName() + ": Null option for id " + optionId + " mineral " + mineralId);
								return;
							}
							
							final double optionChance = parseDouble(optAttrs, "chance");
							options.put(opt, optionChance);
						});
						forEach(categoryNode, "optionRange", optionNode ->
						{
							final NamedNodeMap optAttrs = optionNode.getAttributes();
							final double optionChance = parseDouble(optAttrs, "chance");
							final int fromId = parseInteger(optAttrs, "from");
							final int toId = parseInteger(optAttrs, "to");
							for (int id = fromId; id <= toId; id++)
							{
								final Options op = OptionData.getInstance().getOptions(id);
								if (op == null)
								{
									LOGGER.warning(getClass().getSimpleName() + ": Null option for id " + id + " mineral " + mineralId);
									return;
								}
								
								options.put(op, optionChance);
							}
						});
						
						// Support for specific item ids.
						final Set<Integer> itemIds = new HashSet<>();
						forEach(categoryNode, "item", optionNode ->
						{
							final int itemId = parseInteger(optionNode.getAttributes(), "id");
							itemIds.add(itemId);
						});
						forEach(categoryNode, "items", optionNode ->
						{
							final NamedNodeMap optAttrs = optionNode.getAttributes();
							final int fromId = parseInteger(optAttrs, "from");
							final int toId = parseInteger(optAttrs, "to");
							for (int id = fromId; id <= toId; id++)
							{
								itemIds.add(id);
							}
						});
						
						sets.add(new OptionDataCategory(options, itemIds, chance));
					});
					
					augmentation.setEffectGroup(order, new OptionDataGroup(order, sets));
				});
				
				List<Augmentation> list = _augmentations.get(mineralId);
				if (list == null)
				{
					list = new ArrayList<>();
				}
				
				list.add(augmentation);
				
				_augmentations.put(mineralId, list);
				((EtcItem) ItemData.getInstance().getTemplate(mineralId)).setMineral();
			}));
			
			forEach(listNode, "itemGroups", augmentationsNode -> forEach(augmentationsNode, "itemGroup", augmentationNode ->
			{
				final int id = parseInteger(augmentationNode.getAttributes(), "id");
				final Set<Integer> items = new HashSet<>();
				forEach(augmentationNode, "item", itemNode ->
				{
					final int itemId = parseInteger(itemNode.getAttributes(), "id");
					if (ItemData.getInstance().getTemplate(itemId) == null)
					{
						LOGGER.warning(getClass().getSimpleName() + ": Item with id " + itemId + " was not found.");
					}
					
					items.add(itemId);
				});
				
				final Set<Integer> existing = _itemGroups.get(id);
				if (existing != null)
				{
					existing.addAll(items);
				}
				else
				{
					_itemGroups.put(id, items);
				}
			}));
			
			forEach(listNode, "fees", augmentationNode -> forEach(augmentationNode, "fee", feeNode ->
			{
				final NamedNodeMap feeAttrs = feeNode.getAttributes();
				final int itemGroupId = parseInteger(feeAttrs, "itemGroup");
				final Set<Integer> itemGroup = _itemGroups.get(itemGroupId);
				final int itemId = parseInteger(feeAttrs, "itemId", 0);
				final long itemCount = parseLong(feeAttrs, "itemCount", 0L);
				final long adenaFee = parseLong(feeAttrs, "adenaFee", 0L);
				final long cancelFee = parseLong(feeAttrs, "cancelFee", 0L);
				if ((itemId != 0) && (ItemData.getInstance().getTemplate(itemId) == null))
				{
					LOGGER.warning(getClass().getSimpleName() + ": Item with id " + itemId + " was not found.");
				}
				
				final AugmentationFee fee = new AugmentationFee(itemId, itemCount, adenaFee, cancelFee);
				final Map<Integer, AugmentationFee> feeByMinerals = new HashMap<>();
				forEach(feeNode, "mineral", mineralNode ->
				{
					final int mId = parseInteger(mineralNode.getAttributes(), "id");
					feeByMinerals.put(mId, fee);
				});
				forEach(feeNode, "mineralRange", mineralNode ->
				{
					final NamedNodeMap mineralAttrs = mineralNode.getAttributes();
					final int fromId = parseInteger(mineralAttrs, "from");
					final int toId = parseInteger(mineralAttrs, "to");
					for (int id = fromId; id <= toId; id++)
					{
						feeByMinerals.put(id, fee);
					}
				});
				
				for (int item : itemGroup)
				{
					Map<Integer, AugmentationFee> fees = _fees.get(item);
					if (fees == null)
					{
						fees = new HashMap<>();
					}
					
					fees.putAll(feeByMinerals);
					_fees.put(item, fees);
				}
			}));
		});
	}
	
	/**
	 * Returns the total number of augmentations loaded.<br>
	 * This count is based on the size of the internal augmentation map.
	 * @return The total count of {@code Augmentation} objects.
	 */
	public int getAugmentationCount()
	{
		return _augmentations.size();
	}
	
	/**
	 * Returns the total number of fees currently loaded.<br>
	 * This count represents the size of the internal {@code _fees} map.
	 * @return The total count of fee entries.
	 */
	public int getFeeCount()
	{
		return _fees.size();
	}
	
	/**
	 * Creates a random augmentation for a specific item.<br>
	 * This method determines the weapon type based on the {@code targetItem}.<br>
	 * It then selects a random instance from the provided {@code augmentation}.
	 * @param augmentation The {@code Augmentation} data to use for generation.
	 * @param targetItem The {@code Item} that will receive the augmentation.
	 * @return A new {@code AugmentationInstance} assigned to the item.
	 */
	public AugmentationInstance generateRandomAugmentation(Augmentation augmentation, Item targetItem)
	{
		return generateRandomAugmentation(augmentation, targetItem.getId());
	}
	
	/**
	 * Creates a random instance of a {@code Augmentation}.<br>
	 * This method selects three random effects for the specified item.
	 * @param augmentation The {@code Augmentation} template to use.
	 * @param targetItemId The unique ID of the item being modified.
	 * @return A new {@code AugmentationInstance} containing the selected options.
	 */
	private AugmentationInstance generateRandomAugmentation(Augmentation augmentation, int targetItemId)
	{
		final Options option1 = augmentation.getRandomEffect(0, targetItemId);
		final Options option2 = augmentation.getRandomEffect(1, targetItemId);
		final Options option3 = augmentation.getRandomEffect(2, targetItemId);
		return new AugmentationInstance(augmentation.getMineralId(), option1, option2, option3);
	}
	
	/**
	 * Retrieves the specific {@code Augmentation} for a given mineral and item.<br>
	 * It searches for a augmentation that matches the item's group.<br>
	 * If no matching group is found, it returns the first available augmentation.
	 * @param mineralId The unique identifier of the mineral.
	 * @param item The {@code Item} instance to check against.
	 * @return The matching {@code Augmentation} or {@code null} if none exist.
	 */
	public Augmentation getAugmentation(int mineralId, Item item)
	{
		final List<Augmentation> augmentations = _augmentations.get(mineralId);
		if ((augmentations == null) || augmentations.isEmpty())
		{
			return null;
		}
		
		for (Augmentation augmentation : augmentations)
		{
			final Set<Integer> group = _itemGroups.get(augmentation.getItemGroup());
			if ((group != null) && group.contains(item.getId()))
			{
				return augmentation;
			}
		}
		
		return augmentations.get(0);
	}
	
	/**
	 * Checks if a specific mineral has any associated augmentations.<br>
	 * This method looks up the {@code mineralId} in the internal data map.<br>
	 * It returns {@code true} if augmentations exist for that ID.
	 * @param mineralId The unique identifier of the mineral to check.
	 * @return {@code true} if augmentations are found, otherwise {@code false}.
	 */
	public boolean hasAugmentation(int mineralId)
	{
		final List<Augmentation> augmentations = _augmentations.get(mineralId);
		return (augmentations != null) && !augmentations.isEmpty();
	}
	
	/**
	 * Retrieves the fee for a specific item and mineral combination.<br>
	 * This method looks up the {@code AugmentationFee} based on the provided IDs.<br>
	 * It returns {@code null} if no fee is found.
	 * @param itemId The unique identifier of the item.
	 * @param mineralId The unique identifier of the mineral.
	 * @return The {@code AugmentationFee} associated with the given IDs, or {@code null}.
	 */
	public AugmentationFee getFee(int itemId, int mineralId)
	{
		return _fees.getOrDefault(itemId, Collections.emptyMap()).get(mineralId);
	}
	
	/**
	 * Retrieves the cancellation fee for a specific item and mineral.<br>
	 * This method looks up the {@code AugmentationFee} based on the provided IDs.<br>
	 * It returns -1 if no fee data is found.
	 * @param itemId The unique identifier of the item.
	 * @param mineralId The unique identifier of the mineral.
	 * @return The cancellation fee as a {@code long}, or -1 if not found.
	 */
	public long getCancelFee(int itemId, int mineralId)
	{
		final Map<Integer, AugmentationFee> fees = _fees.get(itemId);
		if (fees == null)
		{
			return -1;
		}
		
		AugmentationFee fee = fees.get(mineralId);
		if (fee == null)
		{
			// This will happen when the data is pre-rework or when augments were manually given, but still that's a cheap solution.
			LOGGER.warning(getClass().getSimpleName() + ": Cancellation fee not found for item [" + itemId + "] and mineral [" + mineralId + "]");
			fee = fees.values().iterator().next();
			if (fee == null)
			{
				return -1;
			}
		}
		
		return fee.getCancelFee();
	}
	
	/**
	 * Checks if there is any fee data associated with a specific item.<br>
	 * It looks for entries in the internal fees map.
	 * @param itemId The unique identifier of the item to check.
	 * @return {@code true} if fee data exists, {@code false} otherwise.
	 */
	public boolean hasFeeData(int itemId)
	{
		final Map<Integer, AugmentationFee> itemFees = _fees.get(itemId);
		return (itemFees != null) && !itemFees.isEmpty();
	}
	
	/**
	 * Retrieves the singleton instance of {@code AugmentationData}.<br>
	 * This method provides a global access point to the augmentation data.
	 * @return The single shared instance of {@code AugmentationData}.
	 */
	public static AugmentationData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final AugmentationData INSTANCE = new AugmentationData();
	}
}
