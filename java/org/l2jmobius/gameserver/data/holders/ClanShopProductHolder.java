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

import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.network.holders.TradeItem;

/**
 * This class acts as a data container for products sold in the clan shop.<br>
 * It stores information regarding the {@code ItemTemplate} and its associated trade properties.
 * @author Mobius
 */
public class ClanShopProductHolder
{
	private final int _clanLevel;
	private final TradeItem _item;
	private final int _count;
	private final long _adena;
	private final int _fame;
	
	/**
	 * Creates a new {@code ClanShopProductHolder} object.<br>
	 * This constructor initializes the product details for the clan shop.
	 * @param clanLevel The required level of the clan to purchase this item.
	 * @param item The {@code ItemTemplate} used to define the base item.
	 * @param count The quantity of the item provided in the shop.
	 * @param adena The amount of currency required for the purchase.
	 * @param fame The amount of fame points required for the purchase.
	 */
	public ClanShopProductHolder(int clanLevel, ItemTemplate item, int count, long adena, int fame)
	{
		_clanLevel = clanLevel;
		_item = new TradeItem(item, 0, 0);
		_count = count;
		_adena = adena;
		_fame = fame;
	}
	
	/**
	 * Retrieves the level required for this clan shop product.<br>
	 * This value determines which clan tier can purchase the item.
	 * @return the {@code int} level of the clan.
	 */
	public int getClanLevel()
	{
		return _clanLevel;
	}
	
	/**
	 * Retrieves the {@code TradeItem} associated with this product.<br>
	 * This method returns the item data used in the clan shop.
	 * @return the {@code TradeItem} object.
	 */
	public TradeItem getTradeItem()
	{
		return _item;
	}
	
	/**
	 * Retrieves the total quantity of the item.<br>
	 * This value represents how many units are available in this product holder.
	 * @return The number of items as an {@code int}.
	 */
	public int getCount()
	{
		return _count;
	}
	
	/**
	 * Retrieves the amount of {@code adena} for this product.<br>
	 * This value represents the cost in currency.
	 * @return The total {@code adena} amount.
	 */
	public long getAdena()
	{
		return _adena;
	}
	
	/**
	 * Retrieves the fame value associated with this product.<br>
	 * This value represents the amount of fame gained from a purchase.
	 * @return The current fame value as an {@code int}.
	 */
	public int getFame()
	{
		return _fame;
	}
}
