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
import org.l2jmobius.gameserver.mechanics.options.AugmentationInstance;

/**
 * Handles requests related to the augmentation of an item.<br>
 * This class processes actions involving {@code AugmentationInstance} mechanics for players.
 * @author Index
 */
public class AugmentationRequest extends AbstractRequest
{
	private Item _augmented;
	private Item _mineral;
	private AugmentationInstance _augmentation;
	
	/**
	 * Creates a new {@code AugmentationRequest} for a specific player.<br>
	 * This constructor initializes the request with the provided {@code Player} object.
	 * @param player The {@code Player} who is initiating the augmentation request.
	 */
	public AugmentationRequest(Player player)
	{
		super(player);
	}
	
	/**
	 * Sets the augmented item for this request.<br>
	 * This method retrieves an {@code Item} from the player's inventory using the provided ID.
	 * @param objectId The unique identifier of the item to set.
	 */
	public synchronized void setAugmentedItem(int objectId)
	{
		_augmented = getPlayer().getInventory().getItemByObjectId(objectId);
	}
	
	/**
	 * Retrieves the {@code Item} that was selected for augmentation.<br>
	 * This method is thread-safe because it is synchronized.
	 * @return the {@code Item} object associated with this request.
	 */
	public synchronized Item getAugmentedItem()
	{
		return _augmented;
	}
	
	/**
	 * Sets the mineral item for this request.<br>
	 * This method retrieves the {@code Item} from the player's inventory using the provided ID.
	 * @param objectId The unique identifier of the mineral item.
	 */
	public synchronized void setMineralItem(int objectId)
	{
		_mineral = getPlayer().getInventory().getItemByObjectId(objectId);
	}
	
	/**
	 * Retrieves the mineral item associated with this request.<br>
	 * This method is thread-safe because it is synchronized.
	 * @return the {@code Item} object representing the mineral.
	 */
	public synchronized Item getMineralItem()
	{
		return _mineral;
	}
	
	/**
	 * Sets the {@code AugmentationInstance} for this request.<br>
	 * This method updates the internal augmentation data.
	 * @param augment The {@code AugmentationInstance} to set.
	 */
	public synchronized void setAugment(AugmentationInstance augment)
	{
		_augmentation = augment;
	}
	
	/**
	 * Retrieves the current {@code AugmentationInstance} associated with this request.<br>
	 * This method is thread-safe because it is synchronized.
	 * @return The {@code AugmentationInstance} object or {@code null} if none exists.
	 */
	public synchronized AugmentationInstance getAugment()
	{
		return _augmentation;
	}
	
	/**
	 * Checks if the request is currently using a specific object.<br>
	 * This method always returns {@code false}.
	 * @param objectId The unique identifier of the object to check.
	 * @return {@code false} regardless of the input.
	 */
	@Override
	public boolean isUsing(int objectId)
	{
		return false;
	}
}
