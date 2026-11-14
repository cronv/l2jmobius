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

/**
 * Holds a limited-shop product with its requirements and production options.
 * @author Mobius
 */
public class LimitShopProductHolder
{
	private final int _id;
	private final int _category;
	private final int _minLevel;
	private final int _maxLevel;
	private final int[] _ingredientIds;
	private final long[] _ingredientQuantities;
	private final int[] _ingredientEnchants;
	private final int _productionId;
	private final long _count;
	private final float _chance;
	private final boolean _announce;
	private final int _enchant;
	private final int _productionId2;
	private final int _productionEnchant2;
	private final long _count2;
	private final float _chance2;
	private final boolean _announce2;
	private final int _productionId3;
	private final int _productionEnchant3;
	private final long _count3;
	private final float _chance3;
	private final boolean _announce3;
	private final int _productionId4;
	private final int _productionEnchant4;
	private final long _count4;
	private final float _chance4;
	private final boolean _announce4;
	private final int _productionId5;
	private final int _productionEnchant5;
	private final long _count5;
	private final float _chance5;
	private final boolean _announce5;
	private final int _accountDailyLimit;
	private final int _accountWeeklyLimit;
	private final int _accountMonthlyLimit;
	private final int _accountBuyLimit;
	private final boolean _keepOption;
	private final long _keepOptionFeeAmount;
	private final boolean[] _isMultiChoice; // Flag: Which slots are multi-choice (cost1-5).
	private final int[] _multiChoiceSlotMapping; // Maps each ingredient to its slot (0-4 or -1 if normal).
	
	/**
	 * Creates a holder with the product data, ingredient requirements and five production options.
	 * @param id The product id.
	 * @param category The product category.
	 * @param minLevel The minimum level required.
	 * @param maxLevel The maximum level allowed.
	 * @param ingredientIds The required ingredient ids.
	 * @param ingredientQuantities The required ingredient quantities.
	 * @param ingredientEnchants The required ingredient enchants.
	 * @param productionId The first production option id.
	 * @param count The first production quantity.
	 * @param chance The first production chance.
	 * @param announce {@code true} to broadcast the first result.
	 * @param enchant The first production enchant level.
	 * @param productionId2 The second production option id.
	 * @param productionEnchant2 The second production enchant level.
	 * @param count2 The second production quantity.
	 * @param chance2 The second production chance.
	 * @param announce2 {@code true} to broadcast the second result.
	 * @param productionId3 The third production option id.
	 * @param productionEnchant3 The third production enchant level.
	 * @param count3 The third production quantity.
	 * @param chance3 The third production chance.
	 * @param announce3 {@code true} to broadcast the third result.
	 * @param productionId4 The fourth production option id.
	 * @param productionEnchant4 The fourth production enchant level.
	 * @param count4 The fourth production quantity.
	 * @param chance4 The fourth production chance.
	 * @param announce4 {@code true} to broadcast the fourth result.
	 * @param productionId5 The fifth production option id.
	 * @param productionEnchant5 The fifth production enchant level.
	 * @param count5 The fifth production quantity.
	 * @param chance5 The fifth production chance.
	 * @param announce5 {@code true} to broadcast the fifth result.
	 * @param accountDailyLimit The daily purchase limit per account.
	 * @param accountWeeklyLimit The weekly purchase limit per account.
	 * @param accountMonthlyLimit The monthly purchase limit per account.
	 * @param accountBuyLimit The total purchase limit per account.
	 * @param keepOption {@code true} if the keep option is enabled.
	 * @param keepOptionFeeAmount The keep-option fee amount.
	 * @param isMultiChoice The per-slot multi-choice flags.
	 * @param multiChoiceSlotMapping The ingredient-to-slot mapping, or {@code -1} if normal.
	 */
	public LimitShopProductHolder(int id, int category, int minLevel, int maxLevel, int[] ingredientIds, long[] ingredientQuantities, int[] ingredientEnchants, int productionId, long count, float chance, boolean announce, int enchant, int productionId2, int productionEnchant2, long count2, float chance2, boolean announce2, int productionId3, int productionEnchant3, long count3, float chance3, boolean announce3, int productionId4, int productionEnchant4, long count4, float chance4, boolean announce4, int productionId5, int productionEnchant5, long count5, float chance5, boolean announce5, int accountDailyLimit, int accountWeeklyLimit, int accountMonthlyLimit, int accountBuyLimit, boolean keepOption, long keepOptionFeeAmount, boolean[] isMultiChoice, int[] multiChoiceSlotMapping)
	{
		_id = id;
		_category = category;
		_minLevel = minLevel;
		_maxLevel = maxLevel;
		_ingredientIds = ingredientIds;
		_ingredientQuantities = ingredientQuantities;
		_ingredientEnchants = ingredientEnchants;
		_productionId = productionId;
		_count = count;
		_chance = chance;
		_announce = announce;
		_enchant = enchant;
		_productionId2 = productionId2;
		_productionEnchant2 = productionEnchant2;
		_count2 = count2;
		_chance2 = chance2;
		_announce2 = announce2;
		_productionId3 = productionId3;
		_productionEnchant3 = productionEnchant3;
		_count3 = count3;
		_chance3 = chance3;
		_announce3 = announce3;
		_productionId4 = productionId4;
		_productionEnchant4 = productionEnchant4;
		_count4 = count4;
		_chance4 = chance4;
		_announce4 = announce4;
		_productionId5 = productionId5;
		_productionEnchant5 = productionEnchant5;
		_count5 = count5;
		_chance5 = chance5;
		_announce5 = announce5;
		_accountDailyLimit = accountDailyLimit;
		_accountWeeklyLimit = accountWeeklyLimit;
		_accountMonthlyLimit = accountMonthlyLimit;
		_accountBuyLimit = accountBuyLimit;
		_keepOption = keepOption;
		_keepOptionFeeAmount = keepOptionFeeAmount;
		_isMultiChoice = isMultiChoice;
		_multiChoiceSlotMapping = multiChoiceSlotMapping;
	}
	
	/**
	 * Retrieves the product id.
	 * @return The id as an {@code int}.
	 */
	public int getId()
	{
		return _id;
	}
	
	/**
	 * Retrieves the product category.
	 * @return The category as an {@code int}.
	 */
	public int getCategory()
	{
		return _category;
	}
	
	/**
	 * Retrieves the minimum level required.
	 * @return The minimum level as an {@code int}.
	 */
	public int getMinLevel()
	{
		return _minLevel;
	}
	
	/**
	 * Retrieves the maximum level allowed.
	 * @return The maximum level as an {@code int}.
	 */
	public int getMaxLevel()
	{
		return _maxLevel;
	}
	
	/**
	 * Retrieves the required ingredient ids.
	 * @return An {@code int[]} of ingredient ids.
	 */
	public int[] getIngredientIds()
	{
		return _ingredientIds;
	}
	
	/**
	 * Retrieves the required quantity for each ingredient.
	 * @return A {@code long[]} of ingredient amounts.
	 */
	public long[] getIngredientQuantities()
	{
		return _ingredientQuantities;
	}
	
	/**
	 * Retrieves the enchant level for each required ingredient.
	 * @return An {@code int[]} of ingredient enchants.
	 */
	public int[] getIngredientEnchants()
	{
		return _ingredientEnchants;
	}
	
	/**
	 * Retrieves the first production option id.
	 * @return The production id as an {@code int}.
	 */
	public int getProductionId()
	{
		return _productionId;
	}
	
	/**
	 * Retrieves the quantity for the first production option.
	 * @return The count as a {@code long}.
	 */
	public long getCount()
	{
		return _count;
	}
	
	/**
	 * Retrieves the success chance for the first production option.
	 * @return The chance as a {@code float}.
	 */
	public float getChance()
	{
		return _chance;
	}
	
	/**
	 * Indicates whether the first production result is broadcast.
	 * @return {@code true} if announced, {@code false} otherwise.
	 */
	public boolean isAnnounce()
	{
		return _announce;
	}
	
	/**
	 * Retrieves the enchant level for the first production option.
	 * @return The enchant level as an {@code int}.
	 */
	public int getEnchant()
	{
		return _enchant;
	}
	
	/**
	 * Retrieves the second production option id.
	 * @return The production id as an {@code int}.
	 */
	public int getProductionId2()
	{
		return _productionId2;
	}
	
	/**
	 * Retrieves the enchant level for the second production option.
	 * @return The enchant level as an {@code int}.
	 */
	public int getProductionEnchant2()
	{
		return _productionEnchant2;
	}
	
	/**
	 * Retrieves the quantity for the second production option.
	 * @return The count as a {@code long}.
	 */
	public long getCount2()
	{
		return _count2;
	}
	
	/**
	 * Retrieves the success chance for the second production option.
	 * @return The chance as a {@code float}.
	 */
	public float getChance2()
	{
		return _chance2;
	}
	
	/**
	 * Indicates whether the second production result is broadcast.
	 * @return {@code true} if announced, {@code false} otherwise.
	 */
	public boolean isAnnounce2()
	{
		return _announce2;
	}
	
	/**
	 * Retrieves the third production option id.
	 * @return The production id as an {@code int}.
	 */
	public int getProductionId3()
	{
		return _productionId3;
	}
	
	/**
	 * Retrieves the enchant level for the third production option.
	 * @return The enchant level as an {@code int}.
	 */
	public int getProductionEnchant3()
	{
		return _productionEnchant3;
	}
	
	/**
	 * Retrieves the quantity for the third production option.
	 * @return The count as a {@code long}.
	 */
	public long getCount3()
	{
		return _count3;
	}
	
	/**
	 * Retrieves the success chance for the third production option.
	 * @return The chance as a {@code float}.
	 */
	public float getChance3()
	{
		return _chance3;
	}
	
	/**
	 * Indicates whether the third production result is broadcast.
	 * @return {@code true} if announced, {@code false} otherwise.
	 */
	public boolean isAnnounce3()
	{
		return _announce3;
	}
	
	/**
	 * Retrieves the fourth production option id.
	 * @return The production id as an {@code int}.
	 */
	public int getProductionId4()
	{
		return _productionId4;
	}
	
	/**
	 * Retrieves the enchant level for the fourth production option.
	 * @return The enchant level as an {@code int}.
	 */
	public int getProductionEnchant4()
	{
		return _productionEnchant4;
	}
	
	/**
	 * Retrieves the quantity for the fourth production option.
	 * @return The count as a {@code long}.
	 */
	public long getCount4()
	{
		return _count4;
	}
	
	/**
	 * Retrieves the success chance for the fourth production option.
	 * @return The chance as a {@code float}.
	 */
	public float getChance4()
	{
		return _chance4;
	}
	
	/**
	 * Retrieves the success chance for the fifth production option.
	 * @return The chance as a {@code float}.
	 */
	public float getChance5()
	{
		return _chance5;
	}
	
	/**
	 * Indicates whether the fourth production result is broadcast.
	 * @return {@code true} if announced, {@code false} otherwise.
	 */
	public boolean isAnnounce4()
	{
		return _announce4;
	}
	
	/**
	 * Retrieves the fifth production option id.
	 * @return The production id as an {@code int}.
	 */
	public int getProductionId5()
	{
		return _productionId5;
	}
	
	/**
	 * Retrieves the enchant level for the fifth production option.
	 * @return The enchant level as an {@code int}.
	 */
	public int getProductionEnchant5()
	{
		return _productionEnchant5;
	}
	
	/**
	 * Retrieves the quantity for the fifth production option.
	 * @return The count as a {@code long}.
	 */
	public long getCount5()
	{
		return _count5;
	}
	
	/**
	 * Indicates whether the fifth production result is broadcast.
	 * @return {@code true} if announced, {@code false} otherwise.
	 */
	public boolean isAnnounce5()
	{
		return _announce5;
	}
	
	/**
	 * Retrieves the daily purchase limit per account.
	 * @return The daily limit as an {@code int}.
	 */
	public int getAccountDailyLimit()
	{
		return _accountDailyLimit;
	}
	
	/**
	 * Retrieves the weekly purchase limit per account.
	 * @return The weekly limit as an {@code int}.
	 */
	public int getAccountWeeklyLimit()
	{
		return _accountWeeklyLimit;
	}
	
	/**
	 * Retrieves the monthly purchase limit per account.
	 * @return The monthly limit as an {@code int}.
	 */
	public int getAccountMonthlyLimit()
	{
		return _accountMonthlyLimit;
	}
	
	/**
	 * Retrieves the total purchase limit per account.
	 * @return The buy limit as an {@code int}.
	 */
	public int getAccountBuyLimit()
	{
		return _accountBuyLimit;
	}
	
	/**
	 * Indicates whether the keep option is enabled.
	 * @return {@code true} if enabled, {@code false} otherwise.
	 */
	public boolean hasKeepOption()
	{
		return _keepOption;
	}
	
	/**
	 * Retrieves the fee charged for the keep option.
	 * @return The fee amount as a {@code long}.
	 */
	public long getKeepOptionFeeAmount()
	{
		return _keepOptionFeeAmount;
	}
	
	/**
	 * Retrieves the multi-choice flags for all slots.
	 * @return A {@code boolean[]} of multi-choice settings.
	 */
	public boolean[] getIsMultiChoice()
	{
		return _isMultiChoice;
	}
	
	/**
	 * Indicates whether the given slot is multi-choice.
	 * @param slot The slot index.
	 * @return {@code true} if multi-choice, {@code false} otherwise.
	 */
	public boolean isMultiChoice(int slot)
	{
		return (_isMultiChoice != null) && (slot >= 0) && (slot < _isMultiChoice.length) && _isMultiChoice[slot];
	}
	
	/**
	 * Retrieves the ingredient-to-slot mapping for multi-choice slots.
	 * @return An {@code int[]} of slot mappings.
	 */
	public int[] getMultiChoiceSlotMapping()
	{
		return _multiChoiceSlotMapping;
	}
	
	/**
	 * Retrieves the mapped slot for the given ingredient.
	 * @param ingredientIndex The ingredient index.
	 * @return The mapped slot as an {@code int}, or {@code -1} if invalid.
	 */
	public int getIngredientSlot(int ingredientIndex)
	{
		if ((_multiChoiceSlotMapping == null) || (ingredientIndex < 0) || (ingredientIndex >= _multiChoiceSlotMapping.length))
		{
			return -1;
		}
		return _multiChoiceSlotMapping[ingredientIndex];
	}
}