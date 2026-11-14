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

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.enums.ItemLocation;

/**
 * Represents a specialized warehouse used for storing items obtained from gacha systems.<br>
 * This class extends {@code Warehouse} to provide specific functionality for gacha-related inventory management.
 */
public class GachaWarehouse extends Warehouse
{
	public static final int MAXIMUM_TEMPORARY_WAREHOUSE_COUNT = 1_100;
	
	private final Creature _owner;
	
	/**
	 * Creates a new {@code GachaWarehouse} for a specific player.<br>
	 * This constructor sets the owner of the warehouse to the provided {@code Player}.
	 * @param player The {@code Player} who will own this warehouse.
	 */
	public GachaWarehouse(Player player)
	{
		_owner = player;
	}
	
	/**
	 * Retrieves the {@code Creature} that owns this warehouse.<br>
	 * This method returns the internal owner reference.
	 * @return the {@code Creature} owning this instance.
	 */
	@Override
	protected Creature getOwner()
	{
		return _owner;
	}
	
	/**
	 * Returns the default location for this warehouse.<br>
	 * This method identifies where the items are stored in the system.
	 * @return the {@code ItemLocation} representing the gacha storage.
	 */
	@Override
	protected ItemLocation getBaseLocation()
	{
		return ItemLocation.GACHA;
	}
}
