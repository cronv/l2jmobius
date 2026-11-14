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

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.enums.ItemLocation;

/**
 * Represents a player's personal warehouse storage system.<br>
 * This class handles the management of items stored within a {@code Player} account.<br>
 * It extends the base functionality provided by {@code Warehouse}.
 */
public class PlayerWarehouse extends Warehouse
{
	private final Player _owner;
	
	/**
	 * Creates a new {@code PlayerWarehouse} instance.<br>
	 * This constructor assigns the warehouse to a specific player.
	 * @param owner The {@code Player} who owns this warehouse.
	 */
	public PlayerWarehouse(Player owner)
	{
		_owner = owner;
	}
	
	/**
	 * Returns the name of this warehouse.<br>
	 * The returned value is always {@code Warehouse}.
	 * @return The name of the warehouse as a {@code String}.
	 */
	@Override
	public String getName()
	{
		return "Warehouse";
	}
	
	/**
	 * Retrieves the {@code Player} who owns this warehouse.<br>
	 * This method returns the owner assigned during initialization.
	 * @return the {@code Player} object that owns this warehouse
	 */
	@Override
	public Player getOwner()
	{
		return _owner;
	}
	
	/**
	 * Returns the default location for this warehouse.<br>
	 * This method always returns {@code ItemLocation.WAREHOUSE}.
	 * @return the base {@code ItemLocation} of the warehouse.
	 */
	@Override
	public ItemLocation getBaseLocation()
	{
		return ItemLocation.WAREHOUSE;
	}
	
	/**
	 * Checks if adding more items will exceed the warehouse limit.<br>
	 * This method calculates the total size of current and new items.<br>
	 * It compares that sum against the owner's maximum capacity.
	 * @param slots The number of additional slots to add.
	 * @return {@code true} if the new total is within the limit, {@code false} otherwise.
	 */
	@Override
	public boolean validateCapacity(long slots)
	{
		return (_items.size() + slots) <= _owner.getWareHouseLimit();
	}
}
