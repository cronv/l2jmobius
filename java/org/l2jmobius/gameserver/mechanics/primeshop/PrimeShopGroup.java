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

import java.util.List;

import org.l2jmobius.gameserver.util.StatSet;

/**
 * Represents a collection of items available within a specific {@code PrimeShop}.<br>
 * This class manages the grouping and organization of shop contents for easier access.
 * @author UnAfraid
 */
public class PrimeShopGroup
{
	private final int _brId;
	private final int _category;
	private final int _paymentType;
	private final int _price;
	private final int _panelType;
	private final int _recommended;
	private final int _start;
	private final int _end;
	private final int _daysOfWeek;
	private final int _startHour;
	private final int _startMinute;
	private final int _stopHour;
	private final int _stopMinute;
	private final int _stock;
	private final int _maxStock;
	private final int _salePercent;
	private final int _minLevel;
	private final int _maxLevel;
	private final int _minBirthday;
	private final int _maxBirthday;
	private final int _accountDailyLimit;
	private final int _accountBuyLimit;
	private final List<PrimeShopItem> _items;
	
	/**
	 * Creates a new {@code PrimeShopGroup} using the provided data.<br>
	 * This constructor initializes all shop properties from a {@code StatSet}.
	 * @param set The {@code StatSet} containing the configuration values.
	 * @param items A {@code List} of {@code PrimeShopItem} objects to include in this group.
	 */
	public PrimeShopGroup(StatSet set, List<PrimeShopItem> items)
	{
		_brId = set.getInt("id");
		_category = set.getInt("cat", 0);
		_paymentType = set.getInt("paymentType", 0);
		_price = set.getInt("price");
		_panelType = set.getInt("panelType", 0);
		_recommended = set.getInt("recommended", 0);
		_start = set.getInt("startSale", 0);
		_end = set.getInt("endSale", 0);
		_daysOfWeek = set.getInt("daysOfWeek", 127);
		_startHour = set.getInt("startHour", 0);
		_startMinute = set.getInt("startMinute", 0);
		_stopHour = set.getInt("stopHour", 0);
		_stopMinute = set.getInt("stopMinute", 0);
		_stock = set.getInt("stock", 0);
		_maxStock = set.getInt("maxStock", -1);
		_salePercent = set.getInt("salePercent", 0);
		_minLevel = set.getInt("minLevel", 0);
		_maxLevel = set.getInt("maxLevel", 0);
		_minBirthday = set.getInt("minBirthday", 0);
		_maxBirthday = set.getInt("maxBirthday", 0);
		_accountDailyLimit = set.getInt("accountDailyLimit", 0);
		_accountBuyLimit = set.getInt("accountBuyLimit", 0);
		_items = items;
	}
	
	/**
	 * Retrieves the unique identifier for this shop group.<br>
	 * This value is used to identify the specific {@code PrimeShopGroup}.
	 * @return The unique ID of the shop group.
	 */
	public int getBrId()
	{
		return _brId;
	}
	
	/**
	 * Retrieves the category identifier for this group.<br>
	 * This value is used to organize items within the shop.
	 * @return The {@code int} category ID.
	 */
	public int getCat()
	{
		return _category;
	}
	
	/**
	 * Retrieves the payment type for this shop group.<br>
	 * This value determines how the transaction is processed.
	 * @return The {@code int} representing the payment type.
	 */
	public int getPaymentType()
	{
		return _paymentType;
	}
	
	/**
	 * Retrieves the price of the shop group.<br>
	 * This value is stored as an {@code int}.
	 * @return The current price.
	 */
	public int getPrice()
	{
		return _price;
	}
	
	/**
	 * Calculates the total count of all items in this group.<br>
	 * It sums the {@code getCount()} value for every {@code PrimeShopItem}.
	 * @return The total sum of item counts as a {@code long}.
	 */
	public long getCount()
	{
		return _items.stream().mapToLong(PrimeShopItem::getCount).sum();
	}
	
	/**
	 * Calculates the total weight of all items in this group.<br>
	 * It sums the individual weights from the {@code _items} list.
	 * @return The total sum of weights as an {@code int}.
	 */
	public int getWeight()
	{
		return _items.stream().mapToInt(PrimeShopItem::getWeight).sum();
	}
	
	/**
	 * Retrieves the type of the panel.<br>
	 * This value identifies how the shop group is displayed.
	 * @return the {@code int} value representing the panel type.
	 */
	public int getPanelType()
	{
		return _panelType;
	}
	
	/**
	 * Retrieves the recommendation level for this shop group.<br>
	 * This value determines how often the group is suggested to players.
	 * @return The {@code int} value representing the recommendation priority.
	 */
	public int getRecommended()
	{
		return _recommended;
	}
	
	/**
	 * Retrieves the starting index of the sale group.<br>
	 * This value is used to determine the position within a list.
	 * @return the {@code int} representing the start position.
	 */
	public int getStartSale()
	{
		return _start;
	}
	
	/**
	 * Retrieves the end value for the sale.<br>
	 * This value is stored in the {@code _end} field.
	 * @return The integer value representing the end of the sale.
	 */
	public int getEndSale()
	{
		return _end;
	}
	
	/**
	 * Retrieves the days of the week for this shop group.<br>
	 * This value represents which days the shop is active.
	 * @return the number of days in the week.
	 */
	public int getDaysOfWeek()
	{
		return _daysOfWeek;
	}
	
	/**
	 * Retrieves the starting hour for this shop group.<br>
	 * The value is returned as an {@code int}.
	 * @return the start hour of the shop group.
	 */
	public int getStartHour()
	{
		return _startHour;
	}
	
	/**
	 * Retrieves the starting minute for this shop group.<br>
	 * This value is used to determine when the sale begins.
	 * @return the start minute as an {@code int}.
	 */
	public int getStartMinute()
	{
		return _startMinute;
	}
	
	/**
	 * Returns the hour when the shop closes.<br>
	 * This value is used to determine the end of the sale period.
	 * @return the stop hour as an {@code int}.
	 */
	public int getStopHour()
	{
		return _stopHour;
	}
	
	/**
	 * Retrieves the minute value for when the shop stops.<br>
	 * This represents the end time of the sale period.
	 * @return the stop minute as an {@code int}.
	 */
	public int getStopMinute()
	{
		return _stopMinute;
	}
	
	/**
	 * Retrieves the current stock amount.<br>
	 * This value represents how many items are currently available.
	 * @return The current number of items in stock as an {@code int}.
	 */
	public int getStock()
	{
		return _stock;
	}
	
	/**
	 * Retrieves the maximum stock capacity for this group.<br>
	 * This value represents the upper limit of items available.
	 * @return The {@code int} value of the maximum stock.
	 */
	public int getTotal()
	{
		return _maxStock;
	}
	
	/**
	 * Retrieves the sale percentage for this shop group.<br>
	 * This value represents how much of a discount is applied.
	 * @return The current sale percentage as an {@code int}.
	 */
	public int getSalePercent()
	{
		return _salePercent;
	}
	
	/**
	 * Returns the minimum level required to access this shop group.<br>
	 * This value is stored in the {@code _minLevel} field.
	 * @return the minimum level as an {@code int}.
	 */
	public int getMinLevel()
	{
		return _minLevel;
	}
	
	/**
	 * Returns the maximum level allowed for this shop group.<br>
	 * This value is used to restrict access based on player level.
	 * @return the maximum level as an {@code int}.
	 */
	public int getMaxLevel()
	{
		return _maxLevel;
	}
	
	/**
	 * Retrieves the minimum birthday value for this shop group.<br>
	 * This value is used to filter items based on a player's birthday.
	 * @return the minimum birthday as an {@code int}.
	 */
	public int getMinBirthday()
	{
		return _minBirthday;
	}
	
	/**
	 * Returns the maximum birthday value for this group.<br>
	 * This value is used to filter eligible birthdays.
	 * @return the {@code int} representing the maximum birthday.
	 */
	public int getMaxBirthday()
	{
		return _maxBirthday;
	}
	
	/**
	 * Retrieves the maximum number of times an account can purchase from this group daily.<br>
	 * This value is stored in the {@code _accountDailyLimit} field.
	 * @return The daily limit for accounts as an {@code int}.
	 */
	public int getAccountDailyLimit()
	{
		return _accountDailyLimit;
	}
	
	/**
	 * Retrieves the maximum number of times an account can purchase from this group.<br>
	 * This value is used to restrict purchases per user.
	 * @return the total buy limit for the account.
	 */
	public int getAccountBuyLimit()
	{
		return _accountBuyLimit;
	}
	
	/**
	 * Retrieves the list of items associated with this shop group.
	 * @return a {@code List} containing all {@code PrimeShopItem} objects.
	 */
	public List<PrimeShopItem> getItems()
	{
		return _items;
	}
}
