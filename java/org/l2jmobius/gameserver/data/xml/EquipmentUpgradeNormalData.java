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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import org.w3c.dom.Document;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.data.holders.EquipmentUpgradeNormalHolder;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.holders.ItemEnchantHolder;
import org.l2jmobius.gameserver.entity.item.holders.ItemHolder;
import org.l2jmobius.gameserver.network.serverpackets.equipmentupgradenormal.ExUpgradeSystemNormalResult;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the data for normal equipment upgrades.<br>
 * It parses configuration from XML files to define upgrade success rates and properties.<br>
 * It provides the necessary information for the {@code EquipmentUpgradeNormalHolder} system.
 * @author Index
 */
public class EquipmentUpgradeNormalData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(EquipmentUpgradeNormalData.class.getName());
	
	private final Map<Integer, EquipmentUpgradeNormalHolder> _upgrades = new HashMap<>();
	private final Set<ItemHolder> _discount = new HashSet<>();
	private int _commission;
	
	/**
	 * This is the default constructor for {@code EquipmentUpgradeNormalData}.<br>
	 * It initializes a new instance of the data class.<br>
	 * It automatically calls the {@code load()} method to populate the data.
	 */
	protected EquipmentUpgradeNormalData()
	{
		load();
	}
	
	/**
	 * Reloads the equipment upgrade data from the configuration files.<br>
	 * This method notifies all active {@code Player} objects of a failure.<br>
	 * It then calls the {@code load()} method to refresh the internal state.
	 */
	public void reload()
	{
		for (Player player : World.getPlayers())
		{
			player.sendPacket(ExUpgradeSystemNormalResult.FAIL);
		}
		
		load();
	}
	
	/**
	 * Loads the equipment upgrade data from the XML file.<br>
	 * This method initializes the commission, clears existing collections, and parses the datapack.<br>
	 * It logs the number of upgrades and discounts loaded into memory.
	 */
	@Override
	public void load()
	{
		_commission = -1;
		_discount.clear();
		_upgrades.clear();
		parseDatapackFile("data/EquipmentUpgradeNormalData.xml");
		
		if (!_upgrades.isEmpty())
		{
			LOGGER.info(getClass().getSimpleName() + ": Loaded " + _upgrades.size() + " upgrade-normal equipment data. Adena commission is " + _commission + ".");
		}
		
		if (!_discount.isEmpty())
		{
			LOGGER.info(getClass().getSimpleName() + ": Loaded " + _discount.size() + " upgrade-normal discount data.");
		}
	}
	
	/**
	 * Parses the provided {@code Document} to populate upgrade data.<br>
	 * This method reads commission, discount, and upgrade details from the XML file.<br>
	 * It populates the internal collection of {@code EquipmentUpgradeNormalHolder} objects.
	 * @param document The {@code Document} object containing the XML data.
	 * @param file The {@code File} reference for the source XML file.
	 */
	@Override
	public void parseDocument(Document document, File file)
	{
		forEach(document, "list", listNode -> forEach(listNode, "params", paramNode -> _commission = new StatSet(parseAttributes(paramNode)).getInt("commission")));
		if (_commission < 0)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Commission in file EquipmentUpgradeNormalData.xml not set or less than 0! Setting up default value - 100!");
			_commission = 100;
		}
		
		forEach(document, "list", listNode -> forEach(listNode, "discount", discountNode -> forEach(discountNode, "item", itemNode ->
		{
			final StatSet successSet = new StatSet(parseAttributes(itemNode));
			_discount.add(new ItemHolder(successSet.getInt("id"), successSet.getLong("count")));
		})));
		forEach(document, "list", listNode -> forEach(listNode, "upgrade", upgradeNode ->
		{
			final AtomicReference<ItemEnchantHolder> initialItem = new AtomicReference<>();
			final AtomicReference<List<ItemEnchantHolder>> materialItems = new AtomicReference<>(new ArrayList<>());
			final AtomicReference<List<ItemEnchantHolder>> onSuccessItems = new AtomicReference<>(new ArrayList<>());
			final AtomicReference<List<ItemEnchantHolder>> onFailureItems = new AtomicReference<>(new ArrayList<>());
			final AtomicReference<List<ItemEnchantHolder>> bonusItems = new AtomicReference<>(new ArrayList<>());
			final AtomicReference<Double> bonusChance = new AtomicReference<>();
			final StatSet headerSet = new StatSet(parseAttributes(upgradeNode));
			final int id = headerSet.getInt("id");
			final int type = headerSet.getInt("type");
			final double chance = headerSet.getDouble("chance");
			final long commission = _commission == 0 ? 0 : ((headerSet.getLong("commission") * _commission) / 100);
			forEach(upgradeNode, "upgradeItem", upgradeItemNode ->
			{
				final StatSet initialSet = new StatSet(parseAttributes(upgradeItemNode));
				initialItem.set(new ItemEnchantHolder(initialSet.getInt("id"), initialSet.getLong("count"), initialSet.getByte("enchantLevel")));
				final ItemEnchantHolder currentInitialItem = initialItem.get();
				if (currentInitialItem == null)
				{
					LOGGER.warning(getClass().getSimpleName() + ": upgradeItem in file EquipmentUpgradeNormalData.xml for upgrade id " + id + " seems like broken!");
				}
				
				if (currentInitialItem != null && currentInitialItem.getCount() < 0)
				{
					LOGGER.warning(getClass().getSimpleName() + ": upgradeItem -> item -> count in file EquipmentUpgradeNormalData.xml for upgrade id " + id + " cannot be less than 0!");
				}
			});
			forEach(upgradeNode, "material", materialItemNode -> forEach(materialItemNode, "item", itemNode ->
			{
				final StatSet successSet = new StatSet(parseAttributes(itemNode));
				materialItems.get().add(new ItemEnchantHolder(successSet.getInt("id"), successSet.getLong("count"), successSet.getInt("enchantLevel")));
				
				// {
				// LOGGER.warning(getClass().getSimpleName() + ": material -> item -> count in file EquipmentUpgradeNormalData.xml for upgrade id " + id +" cannot be less than 0!");
				// }
			}));
			forEach(upgradeNode, "successItems", successItemNode -> forEach(successItemNode, "item", itemNode ->
			{
				final StatSet successSet = new StatSet(parseAttributes(itemNode));
				onSuccessItems.get().add(new ItemEnchantHolder(successSet.getInt("id"), successSet.getLong("count"), successSet.getInt("enchantLevel")));
			}));
			forEach(upgradeNode, "failureItems", failureItemNode -> forEach(failureItemNode, "item", itemNode ->
			{
				final StatSet successSet = new StatSet(parseAttributes(itemNode));
				onFailureItems.get().add(new ItemEnchantHolder(successSet.getInt("id"), successSet.getLong("count"), successSet.getInt("enchantLevel")));
			}));
			forEach(upgradeNode, "bonus_items", bonusItemNode ->
			{
				final double chanceVal = new StatSet(parseAttributes(bonusItemNode)).getDouble("chance");
				bonusChance.set(chanceVal);
				if (chanceVal < 0)
				{
					LOGGER.warning(getClass().getSimpleName() + ": bonus_items -> chance in file EquipmentUpgradeNormalData.xml for upgrade id " + id + " cannot be less than 0!");
				}
				
				forEach(bonusItemNode, "item", itemNode ->
				{
					final StatSet successSet = new StatSet(parseAttributes(itemNode));
					bonusItems.get().add(new ItemEnchantHolder(successSet.getInt("id"), successSet.getLong("count"), successSet.getInt("enchantLevel")));
				});
			});
			final Double finalBonusChance = bonusChance.get();
			_upgrades.put(id, new EquipmentUpgradeNormalHolder(id, type, commission, chance, initialItem.get(), materialItems.get(), onSuccessItems.get(), onFailureItems.get(), finalBonusChance == null ? 0 : finalBonusChance, bonusItems.get()));
		}));
	}
	
	/**
	 * Retrieves a specific upgrade based on its unique identifier.<br>
	 * This method looks up the data in the internal map.
	 * @param id The unique ID of the equipment upgrade to find.
	 * @return The {@code EquipmentUpgradeNormalHolder} associated with the given ID, or {@code null} if not found.
	 */
	public EquipmentUpgradeNormalHolder getUpgrade(int id)
	{
		return _upgrades.get(id);
	}
	
	/**
	 * Retrieves the set of items that receive a discount.<br>
	 * This method returns all {@code ItemHolder} objects currently marked for discounts.
	 * @return A {@code Set} containing the discounted {@code ItemHolder} objects.
	 */
	public Set<ItemHolder> getDiscount()
	{
		return _discount;
	}
	
	/**
	 * Retrieves the commission fee for equipment upgrades.<br>
	 * This value is loaded from the configuration data.
	 * @return The current commission amount as an {@code int}.
	 */
	public int getCommission()
	{
		return _commission;
	}
	
	/**
	 * Retrieves the singleton instance of {@code EquipmentUpgradeNormalData}.<br>
	 * Use this method to access the global data for normal equipment upgrades.
	 * @return The single instance of {@code EquipmentUpgradeNormalData}.
	 */
	public static EquipmentUpgradeNormalData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final EquipmentUpgradeNormalData INSTANCE = new EquipmentUpgradeNormalData();
	}
}
