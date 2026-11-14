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

import java.util.List;

import org.l2jmobius.gameserver.entity.item.holders.ItemEnchantHolder;

/**
 * This class acts as a data container for collection-related information.<br>
 * It stores and manages the state of various collections within the game world.
 * @author Berezkin Nikolay
 */
public class CollectionDataHolder
{
	private final int _collectionId;
	private final String _name;
	private final int _optionId;
	private final int _category;
	private final int _completeCount;
	private final List<ItemEnchantHolder> _items;
	
	/**
	 * Creates a new instance of {@code CollectionDataHolder}.<br>
	 * This constructor initializes all data fields for a collection.
	 * @param collectionId The unique identifier for the collection.
	 * @param name The display name of the collection.
	 * @param optionId The specific option ID associated with this entry.
	 * @param category The category group the collection belongs to.
	 * @param completeCount The number of items required to complete the collection.
	 * @param items A list of {@code ItemEnchantHolder} objects belonging to this collection.
	 */
	public CollectionDataHolder(int collectionId, String name, int optionId, int category, int completeCount, List<ItemEnchantHolder> items)
	{
		_collectionId = collectionId;
		_name = name;
		_optionId = optionId;
		_category = category;
		_completeCount = completeCount;
		_items = items;
	}
	
	/**
	 * Retrieves the unique identifier for this collection.<br>
	 * This ID is used to identify specific data within the {@code CollectionDataHolder}.
	 * @return The integer ID of the collection.
	 */
	public int getCollectionId()
	{
		return _collectionId;
	}
	
	/**
	 * Retrieves the name of the collection.<br>
	 * This value is stored in the {@code _name} field.
	 * @return The name of the collection as a {@code String}.
	 */
	public String getName()
	{
		return _name;
	}
	
	/**
	 * Retrieves the unique identifier for this specific option.<br>
	 * This value is used to distinguish between different options within a collection.
	 * @return The {@code int} ID of the option.
	 */
	public int getOptionId()
	{
		return _optionId;
	}
	
	/**
	 * Retrieves the category of the collection.<br>
	 * This value identifies which group the data belongs to.
	 * @return The {@code int} category value.
	 */
	public int getCategory()
	{
		return _category;
	}
	
	/**
	 * Retrieves the total number of completed items.<br>
	 * This value represents how many requirements have been met.
	 * @return The current count of completed items as an {@code int}.
	 */
	public int getCompleteCount()
	{
		return _completeCount;
	}
	
	/**
	 * Retrieves the list of items associated with this collection.<br>
	 * This method returns all {@code ItemEnchantHolder} objects stored in the holder.
	 * @return a {@code List} of {@code ItemEnchantHolder} objects.
	 */
	public List<ItemEnchantHolder> getItems()
	{
		return _items;
	}
}
