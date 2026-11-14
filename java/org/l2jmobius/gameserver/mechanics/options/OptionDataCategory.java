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

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.l2jmobius.commons.util.Rnd;

/**
 * This class defines categories for grouping different game mechanics options.<br>
 * It helps organize configuration data into logical sets for easier management.<br>
 * Use {@code OptionDataCategory} to classify various server-side settings.
 * @author Pere, Mobius
 */
public class OptionDataCategory
{
	private final Map<Options, Double> _options;
	private final Set<Integer> _itemIds;
	private final double _chance;
	private final boolean _isEmpty;
	
	/**
	 * Creates a new {@code OptionDataCategory} instance.<br>
	 * This constructor initializes the category with specific options, items, and a probability.
	 * @param options A map containing {@code Options} keys and their corresponding double values.
	 * @param itemIds A set of unique identifiers for the items in this category.
	 * @param chance The probability value used to determine if these options are applied.
	 */
	public OptionDataCategory(Map<Options, Double> options, Set<Integer> itemIds, double chance)
	{
		_options = options;
		_itemIds = itemIds;
		_chance = chance;
		_isEmpty = ((options == null) || options.isEmpty());
	}
	
	/**
	 * Checks if the category is empty.<br>
	 * This method returns the internal state of the {@code _isEmpty} flag.
	 * @return {@code true} if the category has no data, otherwise {@code false}.
	 */
	public boolean isEmptyCategory()
	{
		return _isEmpty;
	}
	
	/**
	 * Selects a random option from the available categories.<br>
	 * This method uses {@code Rnd.nextDouble()} to determine the result.<br>
	 * It ensures that a valid {@code Options} value is always returned.
	 * @return The randomly selected {@code Options} object.
	 */
	Options getRandomOptions()
	{
		if (_isEmpty)
		{
			return null;
		}
		
		double random = Rnd.nextDouble() * 100.0;
		for (Entry<Options, Double> entry : _options.entrySet())
		{
			final double value = entry.getValue();
			if (value >= random)
			{
				return entry.getKey();
			}
			
			random -= value;
		}
		
		return null;
	}
	
	/**
	 * Retrieves the map of options associated with this category.<br>
	 * Each {@code Options} key is mapped to its corresponding probability value.
	 * @return a {@code Map} containing all {@code Options} and their double values.
	 */
	public Map<Options, Double> getOptions()
	{
		return _options;
	}
	
	/**
	 * Retrieves the set of unique item identifiers associated with this category.<br>
	 * Each ID corresponds to an item that can be affected by these options.
	 * @return a {@code Set<Integer>} containing all relevant item IDs.
	 */
	public Set<Integer> getItemIds()
	{
		return _itemIds;
	}
	
	/**
	 * Retrieves the probability value for this category.<br>
	 * This value represents the likelihood of an option being selected.
	 * @return The chance as a {@code double}.
	 */
	public double getChance()
	{
		return _chance;
	}
}
