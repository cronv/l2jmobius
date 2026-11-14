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
package org.l2jmobius.gameserver.entity.actor.request;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.instance.Item;

/**
 * Handles the request for an actor to automatically peel a target.<br>
 * This request is used when a player wants the system to manage aggro on their behalf.
 * @author Mobius
 */
public class AutoPeelRequest extends AbstractRequest
{
	private final Item _item;
	private long _totalPeelCount;
	private long _remainingPeelCount;
	
	/**
	 * Creates a new {@code AutoPeelRequest} for a specific player and item.<br>
	 * This request handles the logic for automatically peeling an item.
	 * @param player The {@code Player} who is initiating the request.
	 * @param item The {@code Item} that will be peeled.
	 */
	public AutoPeelRequest(Player player, Item item)
	{
		super(player);
		_item = item;
	}
	
	/**
	 * Retrieves the {@code Item} associated with this request.<br>
	 * This method returns the item that is being processed for peeling.
	 * @return the {@code Item} object.
	 */
	public Item getItem()
	{
		return _item;
	}
	
	/**
	 * Retrieves the total number of peels for this request.<br>
	 * This value represents the maximum amount allowed.
	 * @return the total peel count as a {@code long}.
	 */
	public long getTotalPeelCount()
	{
		return _totalPeelCount;
	}
	
	/**
	 * Sets the total number of peels for this request.<br>
	 * This value determines how many times an item can be peeled.
	 * @param count The new total peel count to set.
	 */
	public void setTotalPeelCount(long count)
	{
		_totalPeelCount = count;
	}
	
	/**
	 * Returns the number of peels left to perform.<br>
	 * This value tracks the remaining progress for the current {@code AutoPeelRequest}.
	 * @return The count of remaining peels as a {@code long}.
	 */
	public long getRemainingPeelCount()
	{
		return _remainingPeelCount;
	}
	
	/**
	 * Updates the number of peels remaining for this request.<br>
	 * This value tracks how many more times an item can be peeled.
	 * @param count The new number of remaining peels to set.
	 */
	public void setRemainingPeelCount(long count)
	{
		_remainingPeelCount = count;
	}
	
	/**
	 * Checks if this request involves an {@code Item}.<br>
	 * This method always returns {@code true} for {@code AutoPeelRequest}.
	 * @return {@code true} if the request is related to an item.
	 */
	@Override
	public boolean isItemRequest()
	{
		return true;
	}
	
	/**
	 * Checks if this request can be processed alongside another request.<br>
	 * It returns {@code true} if the other request is not an item request.
	 * @param request The {@code AbstractRequest} to check.
	 * @return {@code true} if the requests are compatible, {@code false} otherwise.
	 */
	@Override
	public boolean canWorkWith(AbstractRequest request)
	{
		return !request.isItemRequest();
	}
	
	/**
	 * Checks if the current request is associated with a specific object.<br>
	 * It compares the {@code objectId} of the item to the provided value.
	 * @param objectId The unique identifier of the object to check.
	 * @return {@code true} if the IDs match, otherwise {@code false}.
	 */
	@Override
	public boolean isUsing(int objectId)
	{
		return _item.getObjectId() == objectId;
	}
}
