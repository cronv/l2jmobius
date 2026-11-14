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
package org.l2jmobius.gameserver.entity.itemcontainer;

import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.enums.ItemLocation;

/**
 * Represents the freight container associated with a {@code Player}.<br>
 * This class manages items stored in the player's cargo space.<br>
 * It extends {@code ItemContainer} to provide specific functionality for player-owned goods.
 */
public class PlayerFreight extends ItemContainer
{
	private final Player _owner;
	private final int _ownerId;
	
	/**
	 * Creates a new {@code PlayerFreight} instance.<br>
	 * This constructor initializes the freight with a specific owner ID.<br>
	 * It also calls the {@code restore()} method to load existing data.
	 * @param objectId The unique identifier of the player who owns this freight.
	 */
	public PlayerFreight(int objectId)
	{
		_owner = null;
		_ownerId = objectId;
		restore();
	}
	
	/**
	 * Creates a new {@code PlayerFreight} instance for a specific player.<br>
	 * This constructor sets the owner and their unique object ID.
	 * @param owner The {@code Player} who owns this freight container.
	 */
	public PlayerFreight(Player owner)
	{
		_owner = owner;
		_ownerId = owner.getObjectId();
	}
	
	/**
	 * Retrieves the unique identifier of the owner.<br>
	 * This ID belongs to the {@code Player} who owns this freight.
	 * @return The {@code int} ID of the owner.
	 */
	@Override
	public int getOwnerId()
	{
		return _ownerId;
	}
	
	/**
	 * Retrieves the {@code Player} who owns this freight.<br>
	 * This method returns the owner associated with the container.
	 * @return the {@code Player} owner of this freight.
	 */
	@Override
	public Player getOwner()
	{
		return _owner;
	}
	
	/**
	 * Returns the default location for this freight container.<br>
	 * This method always returns the {@code ItemLocation.FREIGHT} value.
	 * @return the base {@code ItemLocation} of the freight.
	 */
	@Override
	public ItemLocation getBaseLocation()
	{
		return ItemLocation.FREIGHT;
	}
	
	/**
	 * Returns the display name of this freight container.<br>
	 * The value returned is always {@code "Freight"}.
	 * @return The name of the freight.
	 */
	@Override
	public String getName()
	{
		return "Freight";
	}
	
	/**
	 * Checks if adding more items will exceed the maximum allowed capacity.<br>
	 * It compares the current size plus the new {@code slots} against a configuration limit.
	 * @param slots The number of additional slots to check.
	 * @return {@code true} if the total fits within the limit, otherwise {@code false}.
	 */
	@Override
	public boolean validateCapacity(long slots)
	{
		return ((getSize() + slots) <= PlayerConfig.ALT_FREIGHT_SLOTS);
	}
	
	/**
	 * Recalculates the total weight of all items in this container.<br>
	 * This method updates the internal weight value based on current contents.
	 */
	@Override
	public void refreshWeight()
	{
	}
}
