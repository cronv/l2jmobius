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
package org.l2jmobius.gameserver.mechanics.commission;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ScheduledFuture;

import org.l2jmobius.gameserver.entity.item.holders.ItemInfo;
import org.l2jmobius.gameserver.entity.item.instance.Item;

/**
 * Represents an item required to complete a specific commission.<br>
 * This class handles the logic for checking and validating items used in commissions.
 * @author NosBit, Ren
 */
public class CommissionItem
{
	private final long _commissionId;
	private final Item _itemInstance;
	private final ItemInfo _itemInfo;
	private final long _pricePerUnit;
	private final Instant _startTime;
	private final byte _durationInDays;
	private final byte _discountInPercentage;
	private ScheduledFuture<?> _saleEndTask;
	
	/**
	 * Creates a new {@code CommissionItem} instance.<br>
	 * This constructor initializes the item with its price, timing, and discount details.
	 * @param commissionId The unique identifier for the commission.
	 * @param itemInstance The {@code Item} object associated with this commission.
	 * @param pricePerUnit The cost of a single unit of the item.
	 * @param startTime The date and time when the commission begins.
	 * @param durationInDays The number of days the commission remains active.
	 * @param discountInPercentage The percentage value used to reduce the item price.
	 */
	public CommissionItem(long commissionId, Item itemInstance, long pricePerUnit, Instant startTime, byte durationInDays, byte discountInPercentage)
	{
		_commissionId = commissionId;
		_itemInstance = itemInstance;
		_itemInfo = new ItemInfo(_itemInstance);
		_pricePerUnit = pricePerUnit;
		_startTime = startTime;
		_durationInDays = durationInDays;
		_discountInPercentage = discountInPercentage;
	}
	
	/**
	 * Retrieves the unique identifier for this commission.<br>
	 * This ID is used to distinguish between different commissions.
	 * @return The {@code long} value of the commission ID.
	 */
	public long getCommissionId()
	{
		return _commissionId;
	}
	
	/**
	 * Retrieves the specific {@code Item} instance associated with this commission.<br>
	 * This method returns the unique object for the item being sold.
	 * @return The {@code Item} instance.
	 */
	public Item getItemInstance()
	{
		return _itemInstance;
	}
	
	/**
	 * Retrieves the information for this commission item.<br>
	 * This method returns the {@code ItemInfo} object associated with the current instance.
	 * @return The {@code ItemInfo} of the commission item.
	 */
	public ItemInfo getItemInfo()
	{
		return _itemInfo;
	}
	
	/**
	 * Retrieves the price for a single unit of this commission item.<br>
	 * This value is stored as a {@code long}.
	 * @return The price per unit.
	 */
	public long getPricePerUnit()
	{
		return _pricePerUnit;
	}
	
	/**
	 * Retrieves the start time of this commission.<br>
	 * The value is returned as an {@code Instant}.
	 * @return the {@code Instant} representing when the commission started.
	 */
	public Instant getStartTime()
	{
		return _startTime;
	}
	
	/**
	 * Returns the duration of the commission in days.<br>
	 * This value is stored as a {@code byte}.
	 * @return The number of days for the commission.
	 */
	public byte getDurationInDays()
	{
		return _durationInDays;
	}
	
	/**
	 * Retrieves the discount rate for this commission.<br>
	 * The value represents a percentage.
	 * @return the discount as a {@code byte}.
	 */
	public byte getDiscountInPercentage()
	{
		return _discountInPercentage;
	}
	
	/**
	 * Calculates the end time of the commission.<br>
	 * It adds the duration in days to the start time.
	 * @return the {@code Instant} representing when the commission ends.
	 */
	public Instant getEndTime()
	{
		return _startTime.plus(_durationInDays, ChronoUnit.DAYS);
	}
	
	/**
	 * Retrieves the scheduled task for the end of a sale.<br>
	 * >* This task is used to manage when a commission item's discount expires.
	 * @return the {@code ScheduledFuture<?>} representing the sale end task.
	 */
	public ScheduledFuture<?> getSaleEndTask()
	{
		return _saleEndTask;
	}
	
	/**
	 * Sets the scheduled task for when a sale ends.<br>
	 * This method updates the {@code _saleEndTask} field.
	 * @param saleEndTask The {@code ScheduledFuture<?>} to be assigned.
	 */
	public void setSaleEndTask(ScheduledFuture<?> saleEndTask)
	{
		_saleEndTask = saleEndTask;
	}
}
