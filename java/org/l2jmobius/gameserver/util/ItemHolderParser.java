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
package org.l2jmobius.gameserver.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.l2jmobius.commons.util.ConfigReader;
import org.l2jmobius.gameserver.entity.item.holders.ItemHolder;

/**
 * Parses lists of item holders from configuration files.
 * @author Mobius
 */
public class ItemHolderParser
{
	private static final Logger LOGGER = Logger.getLogger(ItemHolderParser.class.getName());
	
	/**
	 * Parses a semicolon-separated list of itemId,count pairs into item holders, skipping and logging invalid entries.
	 * @param config the config reader to read from
	 * @param key the property name to read
	 * @param defaultValue the fallback value when the key is absent
	 * @return the parsed item holders, or an empty list if none are valid
	 */
	public static List<ItemHolder> parseItemHolders(ConfigReader config, String key, String defaultValue)
	{
		final String line = config.getString(key, defaultValue);
		if ((line == null) || line.trim().isEmpty() || line.equalsIgnoreCase("none"))
		{
			return Collections.emptyList();
		}
		
		final String[] propertySplit = line.trim().split(";");
		final List<ItemHolder> result = new ArrayList<>(propertySplit.length);
		for (String value : propertySplit)
		{
			if (value.trim().isEmpty())
			{
				continue;
			}
			
			final String[] valueSplit = value.split(",");
			if (valueSplit.length != 2)
			{
				LOGGER.warning("ItemHolderParser: invalid entry -> " + value + ", should be itemId,itemCount. Skipping to the next entry in the list.");
				continue;
			}
			
			int itemId = -1;
			try
			{
				itemId = Integer.parseInt(valueSplit[0].trim());
			}
			catch (NumberFormatException e)
			{
				LOGGER.warning("ItemHolderParser: invalid itemId -> " + valueSplit[0] + ", value must be an integer. Skipping to the next entry in the list.");
				continue;
			}
			
			long count = -1;
			try
			{
				count = Long.parseLong(valueSplit[1].trim());
			}
			catch (NumberFormatException e)
			{
				LOGGER.warning("ItemHolderParser: invalid item count -> " + valueSplit[1] + ", value must be a number. Skipping to the next entry in the list.");
				continue;
			}
			
			if ((itemId > 0) && (count > 0))
			{
				result.add(new ItemHolder(itemId, count));
			}
		}
		
		return result;
	}
	
	/**
	 * Parses comma- or semicolon-separated item IDs into item holders each with a count of one.
	 * @param config the config reader to read from
	 * @param key the property name to read
	 * @param defaultValue the fallback value when the key is absent
	 * @return the parsed item holders, or an empty list if none are valid
	 */
	public static List<ItemHolder> parseSingleItemHolders(ConfigReader config, String key, String defaultValue)
	{
		final String line = config.getString(key, defaultValue);
		if ((line == null) || line.trim().isEmpty() || line.equalsIgnoreCase("none"))
		{
			return Collections.emptyList();
		}
		
		final String[] propertySplit = line.trim().split("[;,]");
		final List<ItemHolder> result = new ArrayList<>(propertySplit.length);
		for (String value : propertySplit)
		{
			final String trimmedValue = value.trim();
			if (trimmedValue.isEmpty())
			{
				continue;
			}
			
			int itemId = -1;
			try
			{
				itemId = Integer.parseInt(trimmedValue);
			}
			catch (NumberFormatException e)
			{
				LOGGER.warning("ItemHolderParser: invalid itemId -> " + value + ", value must be an integer. Skipping to the next entry in the list.");
				continue;
			}
			
			if (itemId > 0)
			{
				result.add(new ItemHolder(itemId, 1));
			}
		}
		
		return result;
	}
	
	/**
	 * Parses comma- or semicolon-separated positive item IDs into a set, ignoring invalid values.
	 * @param config the config reader to read from
	 * @param key the property name to read
	 * @param defaultValue the fallback value when the key is absent
	 * @return the set of valid item IDs
	 */
	public static Set<Integer> parseItemIds(ConfigReader config, String key, String defaultValue)
	{
		final String line = config.getString(key, defaultValue);
		if ((line == null) || line.trim().isEmpty() || line.equalsIgnoreCase("none"))
		{
			return new HashSet<>();
		}
		
		final String[] propertySplit = line.trim().split("[;,]");
		final Set<Integer> result = new HashSet<>(propertySplit.length);
		for (String value : propertySplit)
		{
			final String trimmedValue = value.trim();
			if (trimmedValue.isEmpty())
			{
				continue;
			}
			
			int itemId = -1;
			try
			{
				itemId = Integer.parseInt(trimmedValue);
			}
			catch (NumberFormatException e)
			{
				LOGGER.warning("ItemHolderParser: invalid itemId -> " + value + ", value must be an integer. Skipping to the next entry in the list.");
				continue;
			}
			
			if (itemId > 0)
			{
				result.add(itemId);
			}
		}
		
		return result;
	}
	
	/**
	 * Parses a single itemId,count pair into an item holder.
	 * @param config the config reader to read from
	 * @param key the property name to read
	 * @param defaultValue the fallback value when the key is absent
	 * @return the parsed item holder, or {@code null} if invalid
	 */
	public static ItemHolder parseItemHolder(ConfigReader config, String key, String defaultValue)
	{
		final String value = config.getString(key, defaultValue);
		if ((value == null) || value.trim().isEmpty() || value.equalsIgnoreCase("none"))
		{
			return null;
		}
		
		final String[] valueSplit = value.trim().split(",");
		if (valueSplit.length != 2)
		{
			LOGGER.warning("ItemHolderParser: invalid entry -> " + value + ", should be itemId,itemCount.");
			return null;
		}
		
		int itemId = -1;
		try
		{
			itemId = Integer.parseInt(valueSplit[0].trim());
		}
		catch (NumberFormatException e)
		{
			LOGGER.warning("ItemHolderParser: invalid itemId -> " + valueSplit[0] + ", value must be an integer.");
			return null;
		}
		
		long count = -1;
		try
		{
			count = Long.parseLong(valueSplit[1].trim());
		}
		catch (NumberFormatException e)
		{
			LOGGER.warning("ItemHolderParser: invalid item count -> " + valueSplit[1] + ", value must be a number.");
			return null;
		}
		
		if ((itemId > 0) && (count > 0))
		{
			return new ItemHolder(itemId, count);
		}
		
		return null;
	}
	
	/**
	 * Parses a single positive item ID into an item holder with a count of one.
	 * @param config the config reader to read from
	 * @param key the property name to read
	 * @param defaultValue the fallback value when the key is absent
	 * @return the parsed item holder, or {@code null} if invalid
	 */
	public static ItemHolder parseSingleItemHolder(ConfigReader config, String key, String defaultValue)
	{
		final String value = config.getString(key, defaultValue);
		if ((value == null) || value.trim().isEmpty() || value.equalsIgnoreCase("none"))
		{
			return null;
		}
		
		int itemId = -1;
		try
		{
			itemId = Integer.parseInt(value.trim());
		}
		catch (NumberFormatException e)
		{
			LOGGER.warning("ItemHolderParser: invalid itemId -> " + value + ", value must be an integer.");
			return null;
		}
		
		if (itemId > 0)
		{
			return new ItemHolder(itemId, 1);
		}
		
		return null;
	}
}
