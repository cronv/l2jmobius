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
package org.l2jmobius.gameserver.mechanics.options;

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.commons.util.Rnd;

/**
 * This class serves as a container for grouping related game options.<br>
 * It allows the server to manage and organize multiple {@code OptionData} objects together.
 * @author Pere, Mobius
 */
public class OptionDataGroup
{
	private final int _order;
	private final List<OptionDataCategory> _categories;
	
	/**
	 * Creates a new {@code OptionDataGroup} instance.<br>
	 * This constructor initializes the group with a specific display order and a list of categories.
	 * @param order The numerical position of this group in the sequence.
	 * @param categories A {@code List} of {@code OptionDataCategory} objects belonging to this group.
	 */
	public OptionDataGroup(int order, List<OptionDataCategory> categories)
	{
		_order = order;
		_categories = categories;
	}
	
	/**
	 * Retrieves the display order of this {@code OptionDataGroup}.<br>
	 * This value determines where the group appears in a list.
	 * @return The integer representing the sort order.
	 */
	public int getOrder()
	{
		return _order;
	}
	
	/**
	 * Retrieves the list of categories associated with this group.<br>
	 * This method returns all {@code OptionDataCategory} objects stored in the internal list.
	 * @return a {@code List} of {@code OptionDataCategory} objects.
	 */
	public List<OptionDataCategory> getCategories()
	{
		return _categories;
	}
	
	/**
	 * Selects a random effect based on an item ID.<br>
	 * This method iterates through categories to find a valid match.<br>
	 * It ensures that the selected category is compatible with the provided ID.
	 * @param itemId The unique identifier of the item to check.
	 * @return An {@code Options} object containing the effect, or {@code null} if no effect is found.
	 */
	public Options getRandomEffect(int itemId)
	{
		final List<OptionDataCategory> exclusions = new ArrayList<>();
		Options result = null;
		
		do
		{
			double random = Rnd.nextDouble() * 100.0;
			
			for (OptionDataCategory category : _categories)
			{
				if (!category.getItemIds().isEmpty() && !category.getItemIds().contains(itemId))
				{
					if (!exclusions.contains(category))
					{
						exclusions.add(category);
					}
					continue;
				}
				
				double chance = category.getChance();
				if (random < chance)
				{
					// Return null, don't roll anything.
					if (category.isEmptyCategory())
					{
						return null;
					}
					
					result = category.getRandomOptions();
					return result;
				}
				
				random -= chance;
			}
		}
		while (exclusions.size() < _categories.size());
		
		return result;
	}
}
