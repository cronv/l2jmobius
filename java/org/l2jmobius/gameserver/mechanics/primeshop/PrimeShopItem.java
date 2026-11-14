/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2jmobius.gameserver.mechanics.primeshop;

import org.l2jmobius.gameserver.entity.item.holders.ItemHolder;

/**
 * Represents an item available within the {@code PrimeShop}.<br>
 * This class extends {@code ItemHolder} to provide specific data for shop listings.
 * @author UnAfraid
 */
public class PrimeShopItem extends ItemHolder
{
	private final int _weight;
	private final int _isTradable;
	
	/**
	 * Creates a new instance of {@code PrimeShopItem}.<br>
	 * This constructor initializes the item with its basic properties.
	 * @param itemId The unique identifier for the item.
	 * @param count The number of items in the stack.
	 * @param weight The weight value of the item.
	 * @param isTradable A flag indicating if the item can be traded. Use {@code 1} for true and {@code 0} for false.
	 */
	public PrimeShopItem(int itemId, int count, int weight, int isTradable)
	{
		super(itemId, count);
		_weight = weight;
		_isTradable = isTradable;
	}
	
	/**
	 * Retrieves the weight of this {@code PrimeShopItem}.<br>
	 * This value represents the item's mass.
	 * @return The weight as an {@code int}.
	 */
	public int getWeight()
	{
		return _weight;
	}
	
	/**
	 * Checks if the item can be traded by players.<br>
	 * Returns the tradability status of this {@code PrimeShopItem}.
	 * @return The value of {@code _isTradable}, where {@code 1} means tradable and {@code 0} means non-tradable.
	 */
	public int isTradable()
	{
		return _isTradable;
	}
}
