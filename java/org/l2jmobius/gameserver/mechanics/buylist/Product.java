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
package org.l2jmobius.gameserver.mechanics.buylist;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.config.RatesConfig;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.entity.item.type.EtcItemType;
import org.l2jmobius.gameserver.taskmanagers.BuyListTaskManager;

/**
 * Represents an individual item available for purchase in the buy list.<br>
 * This class stores the necessary data and configuration for a specific {@code ItemTemplate}.
 * @author NosBit
 */
public class Product
{
	private static final Logger LOGGER = Logger.getLogger(Product.class.getName());
	
	private final int _buyListId;
	private final ItemTemplate _item;
	private final long _price;
	private final long _restockDelay;
	private final long _maxCount;
	private final double _baseTax;
	private AtomicLong _count = null;
	
	/**
	 * Creates a new {@code Product} instance.<br>
	 * This constructor initializes the product with its base properties.<br>
	 * It handles price defaults and converts time units for restock delays.
	 * @param buyListId The unique identifier for the buy list.
	 * @param item The {@code ItemTemplate} associated with this product.
	 * @param price The selling price of the item.
	 * @param restockDelay The delay in seconds before the stock can be replenished.
	 * @param maxCount The maximum number of items available in stock.
	 * @param baseTax The base tax percentage used to calculate final costs.
	 */
	public Product(int buyListId, ItemTemplate item, long price, long restockDelay, long maxCount, int baseTax)
	{
		Objects.requireNonNull(item);
		_buyListId = buyListId;
		_item = item;
		_price = (price < 0) ? item.getReferencePrice() : price;
		_restockDelay = restockDelay * 60000;
		_maxCount = maxCount;
		_baseTax = baseTax / 100.0;
		if (hasLimitedStock())
		{
			_count = new AtomicLong(maxCount);
		}
	}
	
	/**
	 * Retrieves the {@code ItemTemplate} associated with this product.<br>
	 * This method returns the base item data used for the buy list.
	 * @return The {@code ItemTemplate} of the product.
	 */
	public ItemTemplate getItem()
	{
		return _item;
	}
	
	/**
	 * Retrieves the unique identifier for the item associated with this product.<br>
	 * This value is obtained from the {@code ItemTemplate}.
	 * @return The {@code int} ID of the item.
	 */
	public int getItemId()
	{
		return _item.getId();
	}
	
	/**
	 * Retrieves the current price of this product.<br>
	 * It applies a special multiplier if the item is a {@code CASTLE_GUARD}.
	 * @return The final calculated price as a {@code long}.
	 */
	public long getPrice()
	{
		long price = _price;
		if (_item.getItemType().equals(EtcItemType.CASTLE_GUARD))
		{
			price *= RatesConfig.RATE_SIEGE_GUARDS_PRICE;
		}
		
		return price;
	}
	
	/**
	 * Retrieves the base tax rate for this product.<br>
	 * This value is used to calculate final costs.
	 * @return The {@code double} value of the base tax.
	 */
	public double getBaseTaxRate()
	{
		return _baseTax;
	}
	
	/**
	 * Returns the time delay for restocking this product.<br>
	 * This value is stored in milliseconds.
	 * @return The restock delay as a {@code long}.
	 */
	public long getRestockDelay()
	{
		return _restockDelay;
	}
	
	/**
	 * Retrieves the maximum number of items allowed for this product.<br>
	 * This value represents the upper limit of the stock.
	 * @return The maximum count as a {@code long}.
	 */
	public long getMaxCount()
	{
		return _maxCount;
	}
	
	/**
	 * Retrieves the current number of items available.<br>
	 * It returns {@code 0} if the count is null or less than zero.
	 * @return The current stock count as a {@code long}.
	 */
	public long getCount()
	{
		if (_count == null)
		{
			return 0;
		}
		
		final long count = _count.get();
		return count > 0 ? count : 0;
	}
	
	/**
	 * Updates the current stock count of this product.<br>
	 * This method sets the value of the internal {@code _count} variable.<br>
	 * It ensures that the {@code AtomicLong} is initialized if it is currently {@code null}.
	 * @param currentCount The new quantity to set for the product.
	 */
	public void setCount(long currentCount)
	{
		if (_count == null)
		{
			_count = new AtomicLong();
		}
		
		_count.set(currentCount);
	}
	
	/**
	 * Reduces the current stock count of this product.<br>
	 * It checks if the {@code _count} is initialized before proceeding.<br>
	 * This method schedules a restock task and saves the new value to the database.
	 * @param value The amount to subtract from the current count.
	 * @return {@code true} if the resulting count is zero or greater, {@code false} otherwise.
	 */
	public boolean decreaseCount(long value)
	{
		if (_count == null)
		{
			return false;
		}
		
		BuyListTaskManager.getInstance().add(this, System.currentTimeMillis() + _restockDelay);
		
		final boolean result = _count.addAndGet(-value) >= 0;
		save();
		return result;
	}
	
	/**
	 * Checks if the product has a restricted quantity.<br>
	 * It returns {@code true} if the maximum count is not set to {@code -1}.<br>
	 * If the value is {@code -1}, it means the stock is unlimited.
	 * @return {@code true} if the stock is limited, {@code false} otherwise.
	 */
	public boolean hasLimitedStock()
	{
		return _maxCount > -1;
	}
	
	/**
	 * Updates the restock timer for this product.<br>
	 * If the time has passed, it triggers a {@code restock()}.<br>
	 * Otherwise, it updates the remaining time in the {@code BuyListTaskManager}.
	 * @param nextRestockTime The timestamp when the next restock should occur.
	 */
	public void restartRestockTask(long nextRestockTime)
	{
		final long remainTime = nextRestockTime - System.currentTimeMillis();
		if (remainTime > 0)
		{
			BuyListTaskManager.getInstance().update(this, remainTime);
		}
		else
		{
			restock();
		}
	}
	
	/**
	 * Refills the current stock of this product.<br>
	 * Sets the count to {@code _maxCount}.<br>
	 * Saves the updated value to the database.
	 */
	public void restock()
	{
		setCount(_maxCount);
		save();
	}
	
	/**
	 * Saves the current state of this product to the database.<br>
	 * It updates the count and next restock time in the {@code buylists} table.<br>
	 * If an error occurs during the save process, it logs a warning.
	 */
	private void save()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement("INSERT INTO `buylists`(`buylist_id`, `item_id`, `count`, `next_restock_time`) VALUES(?, ?, ?, ?) ON DUPLICATE KEY UPDATE `count` = ?, `next_restock_time` = ?"))
		{
			statement.setInt(1, _buyListId);
			statement.setInt(2, _item.getId());
			final long count = getCount();
			statement.setLong(3, count);
			statement.setLong(5, count);
			
			final long nextRestockTime = BuyListTaskManager.getInstance().getRestockDelay(this);
			if (nextRestockTime > 0)
			{
				statement.setLong(4, nextRestockTime);
				statement.setLong(6, nextRestockTime);
			}
			else
			{
				statement.setLong(4, 0);
				statement.setLong(6, 0);
			}
			
			statement.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Failed to save Product buylist_id:" + _buyListId + " item_id:" + _item.getId(), e);
		}
	}
}
