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

import java.util.List;
import java.util.Set;

import org.l2jmobius.commons.util.Rnd;

/**
 * This class serves as a data transfer object (DTO) for representing an item with a specific probability.<br>
 * It stores the {@code itemId}, the {@code count}, and the associated {@code chance} value.
 * @author xban1x, CostyKiller
 */
public class ItemChanceHolder extends ItemHolder
{
	private final double _chance;
	private final byte _enchantmentLevel;
	private final boolean _maintainIngredient;
	private Set<Integer> _targetIds = null;
	
	/**
	 * Creates a new {@code ItemChanceHolder} with a default count of {@code 1}.<br>
	 * This constructor simplifies the creation of an item holder when only the ID and chance are known.
	 * @param id The unique identifier for the item.
	 * @param chance The probability of the item being selected as a double value.
	 */
	public ItemChanceHolder(int id, double chance)
	{
		this(id, chance, 1);
	}
	
	/**
	 * Creates a new {@code ItemChanceHolder} with a specific ID, chance, and count.<br>
	 * The enchantment level is set to {@code 0}.<br>
	 * The maintain ingredient flag is set to {@code false}.
	 * @param id The unique identifier for the item.
	 * @param chance The probability of the item dropping.
	 * @param count The number of items to drop.
	 */
	public ItemChanceHolder(int id, double chance, long count)
	{
		super(id, count);
		_chance = chance;
		_enchantmentLevel = 0;
		_maintainIngredient = false;
		_targetIds = null;
	}
	
	/**
	 * Creates a new {@code ItemChanceHolder} with specific drop properties.<br>
	 * This constructor sets the item ID, count, chance, and enchantment level.<br>
	 * The ingredient maintenance flag is set to {@code false} by default.
	 * @param id The unique identifier for the item.
	 * @param chance The probability of the item dropping.
	 * @param count The number of items to drop.
	 * @param enchantmentLevel The level of enchantment applied to the item.
	 */
	public ItemChanceHolder(int id, double chance, long count, byte enchantmentLevel)
	{
		super(id, count);
		_chance = chance;
		_enchantmentLevel = enchantmentLevel;
		_maintainIngredient = false;
		_targetIds = null;
	}
	
	/**
	 * Creates a new {@code ItemChanceHolder} with specific drop properties.<br>
	 * This constructor initializes all fields including the item ID and count.
	 * @param id The unique identifier for the item.
	 * @param chance The probability of the item dropping.
	 * @param count The amount of items to give.
	 * @param enchantmentLevel The level of enchantment on the item.
	 * @param maintainIngredient Whether the ingredient is kept after use.
	 */
	public ItemChanceHolder(int id, double chance, long count, byte enchantmentLevel, boolean maintainIngredient)
	{
		super(id, count);
		_chance = chance;
		_enchantmentLevel = enchantmentLevel;
		_maintainIngredient = maintainIngredient;
		_targetIds = null;
	}
	
	/**
	 * Creates a new {@code ItemChanceHolder} with specific item properties.<br>
	 * This constructor initializes the item ID, count, and drop chance.<br>
	 * It also parses an array of target IDs into a set of integers.
	 * @param id The unique identifier for the item.
	 * @param count The quantity of the item to be granted.
	 * @param chance The probability of the item being selected.
	 * @param targetIds An array of strings representing specific target IDs to include.
	 */
	public ItemChanceHolder(int id, long count, double chance, String[] targetIds)
	{
		super(id, count);
		_chance = chance;
		_enchantmentLevel = 0;
		_maintainIngredient = false;
		
		if (targetIds != null)
		{
			for (String s : targetIds)
			{
				try
				{
					_targetIds.add(Integer.parseInt(s.trim()));
				}
				catch (NumberFormatException e)
				{
					// Ignore invalid.
				}
			}
		}
	}
	
	/**
	 * Retrieves the probability value for this item.<br>
	 * This value is stored as a {@code double}.
	 * @return The chance value.
	 */
	public double getChance()
	{
		return _chance;
	}
	
	/**
	 * Retrieves the enchantment level of the item.<br>
	 * This value is stored as a {@code byte}.
	 * @return The current enchantment level.
	 */
	public byte getEnchantmentLevel()
	{
		return _enchantmentLevel;
	}
	
	/**
	 * Checks if the item is used as a maintenance ingredient.<br>
	 * Returns {@code true} if it is an ingredient.<br>
	 * Returns {@code false} otherwise.
	 * @return The maintenance status of the item.
	 */
	public boolean isMaintainIngredient()
	{
		return _maintainIngredient;
	}
	
	/**
	 * Checks if the destroyed item matches this holder.<br>
	 * It returns {@code true} if the {@code _targetIds} set is empty.<br>
	 * Otherwise, it checks if the ID exists in the set.
	 * @param destroyedItemId The ID of the item that was destroyed.
	 * @return {@code true} if there is a match, {@code false} otherwise.
	 */
	public boolean matches(int destroyedItemId)
	{
		if (_targetIds.isEmpty())
		{
			return true; // No targetIds means match all.
		}
		
		return _targetIds.contains(destroyedItemId);
	}
	
	/**
	 * Selects a random {@code ItemChanceHolder} from a provided list.<br>
	 * It uses a weighted random system based on the chance values.<br>
	 * If no item is selected, it returns {@code null}.
	 * @param holders The list of {@code ItemChanceHolder} objects to choose from.
	 * @return A randomly selected {@code ItemChanceHolder} or {@code null}.
	 */
	public static ItemChanceHolder getRandomHolder(List<ItemChanceHolder> holders)
	{
		double itemRandom = 100 * Rnd.nextDouble();
		for (ItemChanceHolder holder : holders)
		{
			// Any mathmatical expression including NaN will result in either NaN or 0 of converted to something other than double.
			// We would usually want to skip calculating any holders that include NaN as a chance, because that ruins the overall process.
			final double chance = holder.getChance();
			if (!Double.isNaN(chance))
			{
				// Calculate chance
				if (chance > itemRandom)
				{
					return holder;
				}
				
				itemRandom -= chance;
			}
		}
		
		return null;
	}
	
	/**
	 * Returns a string representation of this {@code ItemChanceHolder}.<br>
	 * It includes the class name, item ID, count, and chance.
	 * @return A formatted string describing the holder.
	 */
	@Override
	public String toString()
	{
		return "[" + getClass().getSimpleName() + "] ID: " + getId() + ", count: " + getCount() + ", chance: " + _chance;
	}
}
