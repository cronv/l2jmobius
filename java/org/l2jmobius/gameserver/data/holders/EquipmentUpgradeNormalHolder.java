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
package org.l2jmobius.gameserver.data.holders;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.l2jmobius.gameserver.entity.item.enums.UpgradeDataType;
import org.l2jmobius.gameserver.entity.item.holders.ItemEnchantHolder;

/**
 * This class holds the data for normal equipment upgrades.<br>
 * It manages the properties and states associated with standard item enhancements.
 */
public class EquipmentUpgradeNormalHolder
{
	private final int _id;
	private final int _type;
	private final long _commission;
	private final double _chance;
	private final ItemEnchantHolder _initialItem;
	private final double _chanceToReceiveBonusItems;
	private final Map<UpgradeDataType, List<ItemEnchantHolder>> _items = new HashMap<>();
	
	/**
	 * Creates a new instance of {@code EquipmentUpgradeNormalHolder}.<br>
	 * This constructor initializes all data for a normal equipment upgrade.
	 * @param id The unique ID from the DAT file.
	 * @param type The upgrade type from the DAT file.
	 * @param commission The default Adena cost required for the transformation.
	 * @param chance The success probability of the transformation.
	 * @param initialItem The base item to be upgraded.
	 * @param materialItems The list of materials required for the upgrade.
	 * @param onSuccessItems The items granted if the upgrade succeeds.
	 * @param onFailureItems The items granted if the upgrade fails.
	 * @param chanceToReceiveBonusItems The probability of receiving extra bonus items.
	 * @param bonusItems The list of potential bonus items to receive.
	 */
	public EquipmentUpgradeNormalHolder(int id, int type, long commission, double chance, ItemEnchantHolder initialItem, List<ItemEnchantHolder> materialItems, List<ItemEnchantHolder> onSuccessItems, List<ItemEnchantHolder> onFailureItems, double chanceToReceiveBonusItems, List<ItemEnchantHolder> bonusItems)
	{
		_id = id;
		_type = type;
		_commission = commission;
		_chance = chance;
		_initialItem = initialItem;
		_chanceToReceiveBonusItems = chanceToReceiveBonusItems;
		if (materialItems != null)
		{
			_items.put(UpgradeDataType.MATERIAL, materialItems);
		}
		
		_items.put(UpgradeDataType.ON_SUCCESS, onSuccessItems);
		if (onFailureItems != null)
		{
			_items.put(UpgradeDataType.ON_FAILURE, onFailureItems);
		}
		
		if (bonusItems != null)
		{
			_items.put(UpgradeDataType.BONUS_TYPE, bonusItems);
		}
	}
	
	/**
	 * Retrieves the unique identifier for this upgrade.<br>
	 * This value corresponds to the ID defined in the DAT file.
	 * @return The {@code int} identifier of the upgrade.
	 */
	public int getId()
	{
		return _id;
	}
	
	/**
	 * Retrieves the type of the equipment upgrade.<br>
	 * This value corresponds to the type defined in the DAT file.
	 * @return The integer value representing the upgrade type.
	 */
	public int getType()
	{
		return _type;
	}
	
	/**
	 * Retrieves the commission cost for this upgrade.<br>
	 * This value represents the default amount of Adena required.
	 * @return The commission amount as a {@code long}.
	 */
	public long getCommission()
	{
		return _commission;
	}
	
	/**
	 * Retrieves the success chance for this upgrade.<br>
	 * The value represents the probability of a successful transformation.
	 * @return The success chance as a {@code double}.
	 */
	public double getChance()
	{
		return _chance;
	}
	
	/**
	 * Retrieves the base item required for this upgrade.<br>
	 * This method returns the {@code ItemEnchantHolder} associated with the initial item.
	 * @return The {@code ItemEnchantHolder} of the initial item.
	 */
	public ItemEnchantHolder getInitialItem()
	{
		return _initialItem;
	}
	
	/**
	 * Retrieves the probability of receiving bonus items.<br>
	 * This value represents a percentage chance for the upgrade process.
	 * @return The success chance as a {@code double}.
	 */
	public double getChanceToReceiveBonusItems()
	{
		return _chanceToReceiveBonusItems;
	}
	
	/**
	 * Retrieves the list of items associated with a specific upgrade type.<br>
	 * This method looks up the {@code UpgradeDataType} in the internal map.
	 * @param upgradeDataType The type of upgrade to filter by.
	 * @return A {@code List} of {@code ItemEnchantHolder} objects, or {@code null} if no items exist for that type.
	 */
	public List<ItemEnchantHolder> getItems(UpgradeDataType upgradeDataType)
	{
		return _items.get(upgradeDataType);
	}
	
	/**
	 * Checks if the upgrade holder contains a specific category.<br>
	 * It returns {@code true} if the category exists in the items map.<br>
	 * It returns {@code false} if the map is empty or does not contain the key.
	 * @param upgradeDataType The {@code UpgradeDataType} to check for.
	 * @return {@code true} if the category is present, otherwise {@code false}.
	 */
	public boolean isHasCategory(UpgradeDataType upgradeDataType)
	{
		if (_items.isEmpty())
		{
			return false;
		}
		
		return _items.containsKey(upgradeDataType);
	}
}
