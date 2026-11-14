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
package org.l2jmobius.gameserver.mechanics.homunculus;

import java.util.List;

import org.l2jmobius.gameserver.entity.item.holders.ItemHolder;

/**
 * Represents the configuration data for a specific homunculus slot.<br>
 * This template defines the properties and constraints associated with a slot in the homunculus system.
 */
public class HomunculusSlotTemplate
{
	private final int _slotId;
	private final List<ItemHolder> _price;
	private final boolean _isEnabled;
	
	/**
	 * Creates a new {@code HomunculusSlotTemplate} object.<br>
	 * This constructor initializes the slot ID, the required price list, and the active status.
	 * @param slotId The unique identifier for the slot.
	 * @param price A {@code List} of {@code ItemHolder} objects representing the cost.
	 * @param isEnabled Determines if the slot is currently active.
	 */
	public HomunculusSlotTemplate(int slotId, List<ItemHolder> price, boolean isEnabled)
	{
		_slotId = slotId;
		_price = price;
		_isEnabled = isEnabled;
	}
	
	/**
	 * Retrieves the unique identifier for this slot.<br>
	 * This value is used to identify which slot a {@code HomunculusSlotTemplate} belongs to.
	 * @return The integer ID of the slot.
	 */
	public int getSlotId()
	{
		return _slotId;
	}
	
	/**
	 * Retrieves the list of items required as a price.<br>
	 * This method returns the {@code List} of {@code ItemHolder} objects associated with this slot.
	 * @return A {@code List} containing the {@code ItemHolder} objects for the price.
	 */
	public List<ItemHolder> getPrice()
	{
		return _price;
	}
	
	/**
	 * Checks if the current slot is active.<br>
	 * Returns {@code true} if enabled.<br>
	 * Returns {@code false} if disabled.
	 * @return The status of the slot.
	 */
	public boolean getSlotEnabled()
	{
		return _isEnabled;
	}
}
