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
package org.l2jmobius.gameserver.mechanics.beautyshop;

import java.util.HashMap;
import java.util.Map;

import org.l2jmobius.gameserver.util.StatSet;

/**
 * Represents an item available in the beauty shop.<br>
 * This class stores the properties and effects associated with a specific cosmetic item.
 * @author Sdw
 */
public class BeautyItem
{
	private final int _id;
	private final int _adena;
	private final int _resetAdena;
	private final int _beautyShopTicket;
	private final Map<Integer, BeautyItem> _colors = new HashMap<>();
	
	/**
	 * Creates a new {@code BeautyItem} instance.<br>
	 * This constructor initializes the item properties from a {@code StatSet}.<br>
	 * It extracts values for id, adena, reset_adena, and beauty_shop_ticket.
	 * @param set The {@code StatSet} containing the item data.
	 */
	public BeautyItem(StatSet set)
	{
		_id = set.getInt("id");
		_adena = set.getInt("adena", 0);
		_resetAdena = set.getInt("reset_adena", 0);
		_beautyShopTicket = set.getInt("beauty_shop_ticket", 0);
	}
	
	/**
	 * Retrieves the unique identifier for this {@code BeautyItem}.<br>
	 * This value is used to distinguish between different items.
	 * @return The {@code int} ID of the item.
	 */
	public int getId()
	{
		return _id;
	}
	
	/**
	 * Retrieves the amount of {@code adena} for this item.<br>
	 * This value represents the base cost.
	 * @return The current {@code adena} value.
	 */
	public int getAdena()
	{
		return _adena;
	}
	
	/**
	 * Retrieves the amount of {@code adena} required for a reset.<br>
	 * This value is used when resetting an item's properties.
	 * @return The {@code int} value representing the reset cost.
	 */
	public int getResetAdena()
	{
		return _resetAdena;
	}
	
	/**
	 * Retrieves the number of beauty shop tickets for this item.<br>
	 * This value is stored in the {@code _beautyShopTicket} field.
	 * @return The total count of beauty shop tickets.
	 */
	public int getBeautyShopTicket()
	{
		return _beautyShopTicket;
	}
	
	/**
	 * Adds a new color variation to the current {@code BeautyItem}.<br>
	 * This method creates a new {@code BeautyItem} using the provided {@code StatSet}.<br>
	 * It stores the new item in the internal colors map.
	 * @param set The {@code StatSet} containing the data for the new color.
	 */
	public void addColor(StatSet set)
	{
		final BeautyItem color = new BeautyItem(set);
		_colors.put(set.getInt("id"), color);
	}
	
	/**
	 * Retrieves the map of color variations for this item.<br>
	 * Each entry maps a unique integer ID to its corresponding {@code BeautyItem}.
	 * @return A {@code Map<Integer, BeautyItem>} containing all available colors.
	 */
	public Map<Integer, BeautyItem> getColors()
	{
		return _colors;
	}
}
