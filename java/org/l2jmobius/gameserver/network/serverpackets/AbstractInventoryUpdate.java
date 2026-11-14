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
package org.l2jmobius.gameserver.network.serverpackets;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.item.holders.ItemInfo;
import org.l2jmobius.gameserver.entity.item.instance.Item;

/**
 * This class serves as a base for all packets that update an inventory.<br>
 * It handles the common logic required to synchronize item changes between the server and the client.
 * @author Mobius
 */
public abstract class AbstractInventoryUpdate extends AbstractItemPacket
{
	private final Map<Integer, ItemInfo> _items = new HashMap<>();
	
	/**
	 * Creates a new instance of the {@code AbstractInventoryUpdate} class.<br>
	 * This is the default constructor for inventory update packets.
	 */
	public AbstractInventoryUpdate()
	{
	}
	
	/**
	 * Creates a new update for an inventory change.<br>
	 * This constructor adds the specified {@code Item} to the internal list of updates.
	 * @param item The {@code Item} to be included in the inventory update.
	 */
	public AbstractInventoryUpdate(Item item)
	{
		addItem(item);
	}
	
	/**
	 * Creates a new update containing a list of items.<br>
	 * This method populates the internal map with all provided {@code ItemInfo} objects.<br>
	 * It ensures thread safety during the population process.
	 * @param items The list of {@code ItemInfo} objects to include in the update.
	 */
	public AbstractInventoryUpdate(List<ItemInfo> items)
	{
		synchronized (_items)
		{
			for (ItemInfo item : items)
			{
				_items.put(item.getObjectId(), item);
			}
		}
	}
	
	/**
	 * Adds a new {@code Item} to the internal collection.<br>
	 * This method updates the map using the unique object ID of the item.<br>
	 * It ensures thread safety by synchronizing on the internal map.
	 * @param item The {@code Item} instance to be added.
	 */
	public void addItem(Item item)
	{
		synchronized (_items)
		{
			_items.put(item.getObjectId(), new ItemInfo(item));
		}
	}
	
	/**
	 * Adds a new {@code Item} to the inventory update list.<br>
	 * This method ensures that the item is stored using its unique object ID.<br>
	 * It uses a synchronized block to keep the internal map thread-safe.
	 * @param item The {@code Item} instance to be added to the collection.
	 */
	public void addNewItem(Item item)
	{
		synchronized (_items)
		{
			_items.put(item.getObjectId(), new ItemInfo(item, 1));
		}
	}
	
	/**
	 * Adds an {@code Item} to the internal collection.<br>
	 * This method updates the entry using the unique object ID of the item.<br>
	 * It ensures thread safety by synchronizing on the internal map.
	 * @param item The {@code Item} to be added to the update packet.
	 */
	public void addModifiedItem(Item item)
	{
		synchronized (_items)
		{
			_items.put(item.getObjectId(), new ItemInfo(item, 2));
		}
	}
	
	/**
	 * Adds an item to the list of removed items.<br>
	 * This method updates the internal map with the provided {@code Item}.
	 * @param item The {@code Item} object to be added.
	 */
	public void addRemovedItem(Item item)
	{
		synchronized (_items)
		{
			_items.put(item.getObjectId(), new ItemInfo(item, 3));
		}
	}
	
	/**
	 * Adds multiple {@code Item} objects to the inventory.<br>
	 * This method updates the internal map with information from each item.<br>
	 * It ensures thread safety by using a synchronized block.
	 * @param items The collection of {@code Item} objects to add.
	 */
	public void addItems(Collection<Item> items)
	{
		synchronized (_items)
		{
			for (Item item : items)
			{
				_items.put(item.getObjectId(), new ItemInfo(item));
			}
		}
	}
	
	/**
	 * Adds all entries from a map to the internal collection.<br>
	 * This method updates the current inventory with multiple {@code ItemInfo} objects.<br>
	 * It uses the keys from the provided map as identifiers.
	 * @param items The map of integers and {@code ItemInfo} objects to add.
	 */
	public void putAll(Map<Integer, ItemInfo> items)
	{
		synchronized (_items)
		{
			_items.putAll(items);
		}
	}
	
	/**
	 * Retrieves all items currently stored in this update.<br>
	 * The map uses the item index as the key.
	 * @return a {@code Map<Integer, ItemInfo>} containing the inventory entries.
	 */
	public Map<Integer, ItemInfo> getItemEntries()
	{
		return _items;
	}
	
	/**
	 * Retrieves all items currently in the inventory update.<br>
	 * This method returns a collection of {@code ItemInfo} objects.
	 * @return A {@code Collection} containing all {@code ItemInfo} entries.
	 */
	public Collection<ItemInfo> getItems()
	{
		return _items.values();
	}
	
	/**
	 * Writes all items in the internal map to the provided {@code WriteBuffer}.<br>
	 * This method clears the internal item collection after writing.<br>
	 * It ensures thread safety by synchronizing on the internal map.
	 * @param buffer The {@code WriteBuffer} used to store the serialized data.
	 */
	protected void writeItems(WriteBuffer buffer)
	{
		synchronized (_items)
		{
			buffer.writeByte(0); // 140
			buffer.writeInt(0); // 140
			buffer.writeInt(_items.size()); // 140
			for (ItemInfo item : _items.values())
			{
				buffer.writeShort(item.getChange()); // Update type : 01-add, 02-modify, 03-remove
				writeItem(item, buffer);
			}
			
			_items.clear();
		}
	}
}
