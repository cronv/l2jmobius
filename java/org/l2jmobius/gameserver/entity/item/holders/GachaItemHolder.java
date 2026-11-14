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
package org.l2jmobius.gameserver.entity.item.holders;

import org.l2jmobius.gameserver.entity.item.enums.UniqueGachaRank;

/**
 * This class represents a container for items obtained through the gacha system.<br>
 * It extends {@code ItemHolder} to provide specific functionality for gacha-related rewards.
 */
public class GachaItemHolder extends ItemHolder
{
	private final int _itemChance;
	private final int _enchantLevel;
	private final UniqueGachaRank _rank;
	
	/**
	 * Creates a new instance of {@code GachaItemHolder}.<br>
	 * This constructor initializes the gacha item properties.
	 * @param itemId The unique identifier for the item.
	 * @param itemCount The total quantity of the item.
	 * @param itemChance The probability chance for this item to appear.
	 * @param enchantLevel The level of enchantment applied to the item.
	 * @param rank The {@code UniqueGachaRank} assigned to this item.
	 */
	public GachaItemHolder(int itemId, long itemCount, int itemChance, int enchantLevel, UniqueGachaRank rank)
	{
		super(itemId, itemCount);
		_itemChance = itemChance;
		_enchantLevel = enchantLevel;
		_rank = rank;
	}
	
	/**
	 * Retrieves the chance of obtaining this item.<br>
	 * This value is stored during the initialization of the {@code GachaItemHolder}.
	 * @return The integer value representing the item chance.
	 */
	public int getItemChance()
	{
		return _itemChance;
	}
	
	/**
	 * Retrieves the current enchantment level of the item.<br>
	 * This value is stored in the {@code _enchantLevel} field.
	 * @return The integer value representing the enchantment level.
	 */
	public int getEnchantLevel()
	{
		return _enchantLevel;
	}
	
	/**
	 * Retrieves the unique gacha rank of this item.<br>
	 * This method returns the {@code UniqueGachaRank} assigned to the holder.
	 * @return The current {@code UniqueGachaRank}.
	 */
	public UniqueGachaRank getRank()
	{
		return _rank;
	}
}
