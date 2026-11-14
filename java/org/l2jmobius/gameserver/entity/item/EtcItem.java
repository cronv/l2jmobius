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
package org.l2jmobius.gameserver.entity.item;

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.gameserver.entity.item.holders.ExtractableProduct;
import org.l2jmobius.gameserver.entity.item.type.EtcItemType;
import org.l2jmobius.gameserver.entity.itemcontainer.Inventory;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class manages the properties and behavior of {@code EtcItem} objects.<br>
 * It handles specific logic for items that are not standard equipment or consumables.
 */
public class EtcItem extends ItemTemplate
{
	private String _handler;
	private EtcItemType _type;
	private List<ExtractableProduct> _extractableItems;
	private int _extractableCountMin;
	private int _extractableCountMax;
	private boolean _isInfinite;
	private boolean _isMineral = false;
	
	/**
	 * Creates a new {@code EtcItem} instance.<br>
	 * This constructor initializes the item using the provided statistics.
	 * @param set The {@code StatSet} containing the key-value pairs for the item description.
	 */
	public EtcItem(StatSet set)
	{
		super(set);
	}
	
	/**
	 * Updates the item properties using a {@code StatSet}.<br>
	 * This method populates fields like type, handler, and extractable counts.
	 * @param set The {@code StatSet} containing the configuration data.
	 */
	@Override
	public void set(StatSet set)
	{
		super.set(set);
		_type = set.getEnum("etcitem_type", EtcItemType.class, EtcItemType.NONE);
		_type1 = ItemTemplate.TYPE1_ITEM_QUESTITEM_ADENA;
		_type2 = ItemTemplate.TYPE2_OTHER; // default is other
		
		if (isQuestItem())
		{
			_type2 = ItemTemplate.TYPE2_QUEST;
		}
		else if ((getId() == Inventory.ADENA_ID) || (getId() == Inventory.ANCIENT_ADENA_ID))
		{
			_type2 = ItemTemplate.TYPE2_MONEY;
		}
		
		_handler = set.getString("handler", null); // ! null !
		
		_extractableCountMin = set.getInt("extractableCountMin", 0);
		_extractableCountMax = set.getInt("extractableCountMax", 0);
		if (_extractableCountMin > _extractableCountMax)
		{
			LOGGER.warning("Item " + this + " extractableCountMin is bigger than extractableCountMax!");
		}
		
		_isInfinite = set.getBoolean("is_infinite", false);
	}
	
	/**
	 * Retrieves the type of this {@code EtcItem}.<br>
	 * This helps identify how the item behaves in the game.
	 * @return the {@code EtcItemType} associated with this item.
	 */
	@Override
	public EtcItemType getItemType()
	{
		return _type;
	}
	
	/**
	 * Retrieves the bitmask associated with this item type.<br>
	 * This value is used to identify specific categories of items.
	 * @return the integer mask for the {@code EtcItemType}.
	 */
	@Override
	public int getItemMask()
	{
		return _type.mask();
	}
	
	/**
	 * Checks if the current item is an {@code EtcItem}.<br>
	 * This method always returns {@code true} because this class represents an {@code EtcItem}.
	 * @return {@code true} if the item is an {@code EtcItem}.
	 */
	@Override
	public boolean isEtcItem()
	{
		return true;
	}
	
	/**
	 * Retrieves the name of the handler associated with this {@code EtcItem}.<br>
	 * This is used to identify which logic class processes this item.
	 * @return The name of the handler as a {@code String}.
	 */
	public String getHandlerName()
	{
		return _handler;
	}
	
	/**
	 * Retrieves the list of products that can be extracted from this item.<br>
	 * This method returns all {@code ExtractableProduct} objects associated with the item.
	 * @return a {@code List} of {@code ExtractableProduct} items.
	 */
	public List<ExtractableProduct> getExtractableItems()
	{
		return _extractableItems;
	}
	
	/**
	 * Gets the minimum number of items that can be extracted.<br>
	 * This value represents the lower bound for extraction results.
	 * @return the minimum extractable count as an {@code int}.
	 */
	public int getExtractableCountMin()
	{
		return _extractableCountMin;
	}
	
	/**
	 * Returns the maximum number of items that can be extracted.<br>
	 * This value is used to limit the extraction process for an {@code EtcItem}.
	 * @return The maximum extractable count as an {@code int}.
	 */
	public int getExtractableCountMax()
	{
		return _extractableCountMax;
	}
	
	/**
	 * Checks if the item has infinite properties.<br>
	 * This is used to determine if the item can be used without being consumed.
	 * @return {@code true} if the item is infinite, {@code false} otherwise.
	 */
	public boolean isInfinite()
	{
		return _isInfinite;
	}
	
	/**
	 * Adds a new product to the list of extractable items.<br>
	 * This method initializes the internal list if it is currently {@code null}.
	 * @param extractableProduct The {@code ExtractableProduct} to be added.
	 */
	@Override
	public void addCapsuledItem(ExtractableProduct extractableProduct)
	{
		if (_extractableItems == null)
		{
			_extractableItems = new ArrayList<>();
		}
		
		_extractableItems.add(extractableProduct);
	}
	
	/**
	 * Checks if the item is classified as a mineral.
	 * @return {@code true} if the item is a mineral, {@code false} otherwise.
	 */
	public boolean isMineral()
	{
		return _isMineral;
	}
	
	/**
	 * Sets the mineral status of this {@code EtcItem}.<br>
	 * This method updates the internal flag to {@code true}.
	 */
	public void setMineral()
	{
		_isMineral = true;
	}
}
