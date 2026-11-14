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

import java.util.Collection;
import java.util.List;

/**
 * This class acts as a data container for relic collection information.<br>
 * It stores and manages the state of relics associated with a specific entity.
 * @author CostyKiller
 */
public class RelicCollectionDataHolder
{
	private final int _relicCollectionId;
	private final String _name;
	private final int _optionId;
	private final int _category;
	private final int _completeCount;
	private final List<RelicCollectionInfoHolder> _relics;
	
	/**
	 * Creates a new instance of {@code RelicCollectionDataHolder}.<br>
	 * This constructor initializes all data fields for a relic collection.
	 * @param relicCollectionId The unique identifier for the collection.
	 * @param name The display name of the collection.
	 * @param optionId The specific option ID associated with this collection.
	 * @param category The category group the collection belongs to.
	 * @param completeCount The number of items required to complete the collection.
	 * @param relics A {@code List} containing the individual {@code RelicCollectionInfoHolder} objects.
	 */
	public RelicCollectionDataHolder(int relicCollectionId, String name, int optionId, int category, int completeCount, List<RelicCollectionInfoHolder> relics)
	{
		_relicCollectionId = relicCollectionId;
		_name = name;
		_optionId = optionId;
		_category = category;
		_completeCount = completeCount;
		_relics = relics;
	}
	
	/**
	 * Retrieves the unique identifier for this relic collection.<br>
	 * This ID is used to identify specific collections in the system.
	 * @return The {@code int} value of the collection ID.
	 */
	public int getCollectionId()
	{
		return _relicCollectionId;
	}
	
	/**
	 * Retrieves the name of the relic collection.<br>
	 * This value is stored in the {@code _name} field.
	 * @return The name of the collection as a {@code String}.
	 */
	public String getName()
	{
		return _name;
	}
	
	/**
	 * Retrieves the unique identifier for the option.<br>
	 * This value is used to distinguish between different options within a collection.
	 * @return The {@code int} ID of the option.
	 */
	public int getOptionId()
	{
		return _optionId;
	}
	
	/**
	 * Retrieves the category identifier for this collection.<br>
	 * This value helps group different types of relics together.
	 * @return The {@code int} category ID.
	 */
	public int getCategory()
	{
		return _category;
	}
	
	/**
	 * Retrieves the total number of completed items in this collection.<br>
	 * This value represents how many relics have been finished.
	 * @return The current count of completed relics as an {@code int}.
	 */
	public int getCompleteCount()
	{
		return _completeCount;
	}
	
	/**
	 * Retrieves the list of relics associated with this collection.
	 * @return a {@code Collection} of {@code RelicCollectionInfoHolder} objects.
	 */
	public Collection<RelicCollectionInfoHolder> getRelics()
	{
		return _relics;
	}
	
	/**
	 * Retrieves a specific relic from the collection.<br>
	 * This method uses the provided position to find the item.
	 * @param index The position of the relic in the list.
	 * @return The {@code RelicCollectionInfoHolder} at the specified index.
	 */
	public RelicCollectionInfoHolder getRelic(int index)
	{
		return _relics.get(index);
	}
}
