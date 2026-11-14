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
 * This class acts as a container for virtual items within the game world.<br>
 * It manages data related to items that do not exist as physical entities in the database.<br>
 * Use {@code VirtualItemHolder} to handle temporary or non-persistent item objects.
 * @author CostyKiller
 */
public class VirtualItemHolder
{
	private final int _indexMain;
	private final long _slot;
	private final int _indexSub;
	private final int _itemId;
	private final int _enchant;
	private final int _costVISPoint;
	
	/**
	 * Creates a new instance of {@code VirtualItemHolder}.<br>
	 * This constructor initializes all required fields for a virtual item.
	 * @param indexMain The main index of the item.
	 * @param slot The unique slot identifier.
	 * @param indexSub The sub index of the item.
	 * @param itemId The unique identification number for the item.
	 * @param enchant The enchantment level of the item.
	 * @param costVISPoint The cost in VIS points required for this item.
	 */
	public VirtualItemHolder(int indexMain, long slot, int indexSub, int itemId, int enchant, int costVISPoint)
	{
		_indexMain = indexMain;
		_slot = slot;
		_indexSub = indexSub;
		_itemId = itemId;
		_enchant = enchant;
		_costVISPoint = costVISPoint;
	}
	
	/**
	 * Retrieves the main index of the virtual item.<br>
	 * This value is used to identify the primary category of the item.
	 * @return The {@code int} value representing the main index.
	 */
	public int getIndexMain()
	{
		return _indexMain;
	}
	
	/**
	 * Retrieves the unique slot identifier for this virtual item.<br>
	 * This value is used to locate the item in the inventory system.
	 * @return The {@code long} value representing the item's slot.
	 */
	public long getSlot()
	{
		return _slot;
	}
	
	/**
	 * Retrieves the secondary index of the virtual item.<br>
	 * This value is stored in the {@code _indexSub} field.
	 * @return The integer value of the secondary index.
	 */
	public int getIndexSub()
	{
		return _indexSub;
	}
	
	/**
	 * Retrieves the unique identifier for this virtual item.<br>
	 * This value corresponds to the {@code _itemId} field.
	 * @return The integer ID of the item.
	 */
	public int getItemId()
	{
		return _itemId;
	}
	
	/**
	 * Retrieves the enchantment level of the virtual item.<br>
	 * This value is stored in the {@code _enchant} field.
	 * @return The current enchantment level as an {@code int}.
	 */
	public int getEnchant()
	{
		return _enchant;
	}
	
	/**
	 * Retrieves the cost in {@code VISPoint} for this virtual item.<br>
	 * This value is used to determine how many points are required to obtain the item.
	 * @return The cost of the virtual item as an {@code int}.
	 */
	public int getCostVISPoint()
	{
		return _costVISPoint;
	}
}
