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

import org.l2jmobius.gameserver.entity.item.enums.WorldExchangeItemStatusType;
import org.l2jmobius.gameserver.entity.item.enums.WorldExchangeItemSubType;
import org.l2jmobius.gameserver.entity.item.instance.Item;

/**
 * This class manages the data and state for items placed in a {@code WorldExchange}.<br>
 * It acts as a container to track item properties, statuses, and types within the exchange system.
 * @author Index
 */
public class WorldExchangeHolder
{
	private final long _worldExchangeId;
	private final Item _itemInstance;
	private final ItemInfo _itemInfo;
	private final long _price;
	private final int _worldExchangeType;
	private final int _oldOwnerId;
	private WorldExchangeItemStatusType _storeType;
	private final WorldExchangeItemSubType _category;
	private final long _startTime;
	private long _endTime;
	private boolean _hasChanges;
	
	/**
	 * Creates a new {@code WorldExchangeHolder} object.<br>
	 * This constructor initializes all fields for an item in the world exchange.
	 * @param worldExchangeId The unique identifier for the exchange.
	 * @param itemInstance The actual instance of the {@code Item}.
	 * @param itemInfo The information data for the {@code Item}.
	 * @param price The cost of the item in the exchange.
	 * @param worldExchangeType The specific type of the world exchange.
	 * @param oldOwnerId The ID of the previous owner of the item.
	 * @param storeType The status type of the store for this item.
	 * @param category The sub-type category of the item.
	 * @param startTime The timestamp when the exchange started.
	 * @param endTime The timestamp when the exchange expires.
	 * @param hasChanges A boolean flag indicating if any changes have occurred.
	 */
	public WorldExchangeHolder(long worldExchangeId, Item itemInstance, ItemInfo itemInfo, long price, int worldExchangeType, int oldOwnerId, WorldExchangeItemStatusType storeType, WorldExchangeItemSubType category, long startTime, long endTime, boolean hasChanges)
	{
		_worldExchangeId = worldExchangeId;
		_itemInstance = itemInstance;
		_itemInfo = itemInfo;
		_price = price;
		_worldExchangeType = worldExchangeType;
		_oldOwnerId = oldOwnerId;
		_storeType = storeType;
		_category = category;
		_startTime = startTime;
		_endTime = endTime;
		_hasChanges = hasChanges;
	}
	
	/**
	 * Retrieves the unique identifier for this world exchange.<br>
	 * This ID is used to identify a specific exchange entry in the system.
	 * @return The {@code long} value of the world exchange ID.
	 */
	public long getWorldExchangeId()
	{
		return _worldExchangeId;
	}
	
	/**
	 * Retrieves the specific {@code Item} instance associated with this holder.<br>
	 * This method returns the unique item object currently held in the exchange.
	 * @return The {@code Item} instance.
	 */
	public Item getItemInstance()
	{
		return _itemInstance;
	}
	
	/**
	 * Retrieves the information for the item. <br>
	 * This method returns the {@code ItemInfo} associated with this holder.
	 * @return the {@code ItemInfo} of the item.
	 */
	public ItemInfo getItemInfo()
	{
		return _itemInfo;
	}
	
	/**
	 * Retrieves the price of the item in the world exchange.<br>
	 * This value is stored as a {@code long}.
	 * @return The current price of the item.
	 */
	public long getPrice()
	{
		return _price;
	}
	
	/**
	 * Retrieves the type of the world exchange.<br>
	 * This value identifies how the item is categorized in the exchange system.
	 * @return The integer value representing the {@code worldExchangeType}.
	 */
	public int getWorldExchangeType()
	{
		return _worldExchangeType;
	}
	
	/**
	 * Retrieves the unique identifier of the previous owner.<br>
	 * This ID is used to track who originally held the item.
	 * @return The {@code int} value representing the old owner's ID.
	 */
	public int getOldOwnerId()
	{
		return _oldOwnerId;
	}
	
	/**
	 * Retrieves the current store type of the world exchange item.<br>
	 * This value determines how the item is categorized in the exchange.
	 * @return the {@code WorldExchangeItemStatusType} of this holder.
	 */
	public WorldExchangeItemStatusType getStoreType()
	{
		return _storeType;
	}
	
	/**
	 * Updates the store type for this exchange item.<br>
	 * This method sets the {@code _storeType} field to the provided value.
	 * @param storeType The new {@code WorldExchangeItemStatusType} to assign.
	 */
	public void setStoreType(WorldExchangeItemStatusType storeType)
	{
		_storeType = storeType;
	}
	
	/**
	 * Retrieves the category of the world exchange item.<br>
	 * This value identifies the specific sub-type classification.
	 * @return the {@code WorldExchangeItemSubType} of this holder.
	 */
	public WorldExchangeItemSubType getCategory()
	{
		return _category;
	}
	
	/**
	 * Retrieves the start time of the world exchange.<br>
	 * The value is returned as a {@code long}.
	 * @return The starting timestamp.
	 */
	public long getStartTime()
	{
		return _startTime;
	}
	
	/**
	 * Retrieves the end time of the world exchange.<br>
	 * This value is returned as a {@code long}.
	 * @return The end time of the exchange.
	 */
	public long getEndTime()
	{
		return _endTime;
	}
	
	/**
	 * Sets the end time for the world exchange.<br>
	 * This updates the {@code _endTime} field with a new value.
	 * @param endTime The new end time as a {@code long}.
	 */
	public void setEndTime(long endTime)
	{
		_endTime = endTime;
	}
	
	/**
	 * Checks if the holder has pending changes.<br>
	 * This method resets the internal change flag to {@code false}.
	 * @return {@code true} if there were changes and they have been cleared, otherwise {@code false}.
	 */
	public boolean hasChanges()
	{
		if (_hasChanges) // TODO: Fix logic.
		{
			_hasChanges = false;
			return true;
		}
		
		return false;
	}
	
	/**
	 * Updates the change status of this holder.<br>
	 * This method sets whether any modifications have occurred.
	 * @param hasChanges The new value to set for the change flag.
	 */
	public void setHasChanges(boolean hasChanges)
	{
		_hasChanges = hasChanges;
	}
}
