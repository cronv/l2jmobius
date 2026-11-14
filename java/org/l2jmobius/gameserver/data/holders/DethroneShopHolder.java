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
 * Holds a Dethrone shop entry with its requirements and production rewards.
 * @author Liamxroy
 */
public class DethroneShopHolder
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
	private final int _productionId2;
	private final long _count2;
	private final float _chance2;
	private final int _productionId3;
	private final long _count3;
	private final float _chance3;
	private final int _productionId4;
	private final long _count4;
	private final float _chance4;
	private final int _productionId5;
	private final long _count5;
	private final int _accountDailyLimit;
	private final int _accountBuyLimit;
	private final boolean _announce;
	
	/**
	 * Creates a holder with the shop requirements and production rewards.
	 * @param id The shop item id.
	 * @param category The category.
	 * @param minLevel The minimum level required.
	 * @param maxLevel The maximum level allowed.
	 * @param ingredientIds The required ingredient ids.
	 * @param ingredientQuantities The required ingredient quantities.
	 * @param ingredientEnchants The required ingredient enchants.
	 * @param productionId The first production result id.
	 * @param count The first production quantity.
	 * @param chance The first production chance.
	 * @param productionId2 The second production result id.
	 * @param count2 The second production quantity.
	 * @param chance2 The second production chance.
	 * @param productionId3 The third production result id.
	 * @param count3 The third production quantity.
	 * @param chance3 The third production chance.
	 * @param productionId4 The fourth production result id.
	 * @param count4 The fourth production quantity.
	 * @param chance4 The fourth production chance.
	 * @param productionId5 The fifth production result id.
	 * @param count5 The fifth production quantity.
	 * @param accountDailyLimit The daily purchase limit per account.
	 * @param accountBuyLimit The total purchase limit per account.
	 * @param announce {@code true} to broadcast the purchase.
	 */
	public DethroneShopHolder(int id, int category, int minLevel, int maxLevel, int[] ingredientIds, long[] ingredientQuantities, int[] ingredientEnchants, int productionId, long count, float chance, int productionId2, long count2, float chance2, int productionId3, long count3, float chance3, int productionId4, long count4, float chance4, int productionId5, long count5, int accountDailyLimit, int accountBuyLimit, boolean announce)
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
		_productionId2 = productionId2;
		_count2 = count2;
		_chance2 = chance2;
		_productionId3 = productionId3;
		_count3 = count3;
		_chance3 = chance3;
		_productionId4 = productionId4;
		_count4 = count4;
		_chance4 = chance4;
		_productionId5 = productionId5;
		_count5 = count5;
		_accountDailyLimit = accountDailyLimit;
		_accountBuyLimit = accountBuyLimit;
		_announce = announce;
	}
	
	/**
	 * Retrieves the shop item id.
	 * @return The id as an {@code int}.
	 */
	public int getId()
	{
		return _id;
	}
	
	/**
	 * Retrieves the category.
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
	 * Retrieves the required ingredient quantities.
	 * @return A {@code long[]} of ingredient quantities.
	 */
	public long[] getIngredientQuantities()
	{
		return _ingredientQuantities;
	}
	
	/**
	 * Retrieves the required ingredient enchants.
	 * @return An {@code int[]} of ingredient enchants.
	 */
	public int[] getIngredientEnchants()
	{
		return _ingredientEnchants;
	}
	
	/**
	 * Retrieves the first production result id.
	 * @return The production id as an {@code int}.
	 */
	public int getProductionId()
	{
		return _productionId;
	}
	
	/**
	 * Retrieves the first production quantity.
	 * @return The count as a {@code long}.
	 */
	public long getCount()
	{
		return _count;
	}
	
	/**
	 * Retrieves the first production chance.
	 * @return The chance as a {@code float}.
	 */
	public float getChance()
	{
		return _chance;
	}
	
	/**
	 * Retrieves the second production result id.
	 * @return The production id as an {@code int}.
	 */
	public int getProductionId2()
	{
		return _productionId2;
	}
	
	/**
	 * Retrieves the second production quantity.
	 * @return The count as a {@code long}.
	 */
	public long getCount2()
	{
		return _count2;
	}
	
	/**
	 * Retrieves the second production chance.
	 * @return The chance as a {@code float}.
	 */
	public float getChance2()
	{
		return _chance2;
	}
	
	/**
	 * Retrieves the third production result id.
	 * @return The production id as an {@code int}.
	 */
	public int getProductionId3()
	{
		return _productionId3;
	}
	
	/**
	 * Retrieves the third production quantity.
	 * @return The count as a {@code long}.
	 */
	public long getCount3()
	{
		return _count3;
	}
	
	/**
	 * Retrieves the third production chance.
	 * @return The chance as a {@code float}.
	 */
	public float getChance3()
	{
		return _chance3;
	}
	
	/**
	 * Retrieves the fourth production result id.
	 * @return The production id as an {@code int}.
	 */
	public int getProductionId4()
	{
		return _productionId4;
	}
	
	/**
	 * Retrieves the fourth production quantity.
	 * @return The count as a {@code long}.
	 */
	public long getCount4()
	{
		return _count4;
	}
	
	/**
	 * Retrieves the fourth production chance.
	 * @return The chance as a {@code float}.
	 */
	public float getChance4()
	{
		return _chance4;
	}
	
	/**
	 * Retrieves the fifth production result id.
	 * @return The production id as an {@code int}.
	 */
	public int getProductionId5()
	{
		return _productionId5;
	}
	
	/**
	 * Retrieves the fifth production quantity.
	 * @return The count as a {@code long}.
	 */
	public long getCount5()
	{
		return _count5;
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
	 * Retrieves the total purchase limit per account.
	 * @return The buy limit as an {@code int}.
	 */
	public int getAccountBuyLimit()
	{
		return _accountBuyLimit;
	}
	
	/**
	 * Indicates whether the purchase is broadcast.
	 * @return {@code true} if announced, {@code false} otherwise.
	 */
	public boolean isAnnounce()
	{
		return _announce;
	}
}
