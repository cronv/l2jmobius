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
 * This class serves as a data template for the creation of homunculi.<br>
 * It defines the required properties and conditions needed to spawn a new homunculus entity.
 */
public class HomunculusCreationTemplate
{
	private final int _slotId;
	private final boolean _isEnabled;
	private final int _grade;
	private final boolean _isEvent;
	private final List<ItemHolder> _itemsFee;
	private final Integer[] _hpFee;
	private final Long[] _spFee;
	private final Integer[] _vpFee;
	private final long _time;
	private final List<Double[]> _createChances;
	
	/**
	 * Creates a new template for homunculus creation.<br>
	 * This constructor initializes all required fees and settings.
	 * @param slotId The unique identifier for the slot.
	 * @param isEnabled Whether this template is active.
	 * @param grade The quality level of the homunculus.
	 * @param isEvent Indicates if this is a special event creation.
	 * @param itemsFee A list of {@code ItemHolder} objects required as payment.
	 * @param hpFee An array of {@code Integer} values for HP costs.
	 * @param spFee An array of {@code Long} values for SP costs.
	 * @param vpFee An array of {@code Integer} values for VP costs.
	 * @param time The duration associated with the creation.
	 * @param createChances A list of {@code Double[]} representing success probabilities.
	 */
	public HomunculusCreationTemplate(int slotId, boolean isEnabled, int grade, boolean isEvent, List<ItemHolder> itemsFee, Integer[] hpFee, Long[] spFee, Integer[] vpFee, long time, List<Double[]> createChances)
	{
		_slotId = slotId;
		_isEnabled = isEnabled;
		_grade = grade;
		_isEvent = isEvent;
		_itemsFee = itemsFee;
		_hpFee = hpFee;
		_spFee = spFee;
		_vpFee = vpFee;
		_time = time;
		_createChances = createChances;
	}
	
	/**
	 * Retrieves the unique identifier for this creation slot.<br>
	 * This value identifies where the template is located in the configuration.
	 * @return the {@code int} slot ID.
	 */
	public int getSlotId()
	{
		return _slotId;
	}
	
	/**
	 * Checks if this template is currently active.<br>
	 * Returns {@code true} if enabled.<br>
	 * Returns {@code false} if disabled.
	 * @return the current status of the template
	 */
	public boolean isEnabled()
	{
		return _isEnabled;
	}
	
	/**
	 * Retrieves the grade of the homunculus template.<br>
	 * This value represents the quality level assigned to the creation.
	 * @return The integer value of the {@code grade}.
	 */
	public int getGrade()
	{
		return _grade;
	}
	
	/**
	 * Checks if this template belongs to a special event.<br>
	 * Returns {@code true} if it is an event item.<br>
	 * Returns {@code false} otherwise.
	 * @return the event status of the template.
	 */
	public boolean isEvent()
	{
		return _isEvent;
	}
	
	/**
	 * Retrieves the list of items required as a fee.<br>
	 * This method returns all {@code ItemHolder} objects associated with the creation cost.
	 * @return A {@code List} of {@code ItemHolder} objects representing the item fees.
	 */
	public List<ItemHolder> getItemFee()
	{
		return _itemsFee;
	}
	
	/**
	 * Checks if there are any items required as a fee.<br>
	 * Returns {@code true} if the {@code _itemsFee} list is not empty.<br>
	 * Returns {@code false} if no items are required.
	 * @return {@code true} if an item fee exists, {@code false} otherwise.
	 */
	public boolean haveAnotherFee()
	{
		return !_itemsFee.isEmpty();
	}
	
	/**
	 * Returns the amount of {@code HP} fee required for use.<br>
	 * This value is retrieved from the second element of the {@code _hpFee} array.
	 * @return The count of {@code HP} fee for use.
	 */
	public int getHPFeeCountByUse()
	{
		return _hpFee[1];
	}
	
	/**
	 * Returns the number of {@code HP} fees required.<br>
	 * This value is retrieved from the first element of the {@code _hpFee} array.
	 * @return The count of {@code HP} fees.
	 */
	public int getHPFeeCount()
	{
		return _hpFee[0];
	}
	
	/**
	 * Retrieves the amount of {@code SP} required for use.<br>
	 * This value is taken from the second element of the {@code _spFee} array.
	 * @return The number of {@code SP} points required for use.
	 */
	public long getSPFeeCountByUse()
	{
		return _spFee[1];
	}
	
	/**
	 * Retrieves the amount of {@code SP} required for creation.<br>
	 * This value is taken from the first element of the {@code _spFee} array.
	 * @return The total {@code SP} fee as a {@code long}.
	 */
	public long getSPFeeCount()
	{
		return _spFee[0];
	}
	
	/**
	 * Retrieves the {@code VP} fee required for use.<br>
	 * This value is taken from the second element of the internal {@code _vpFee} array.
	 * @return The amount of {@code VP} needed.
	 */
	public int getVPFeeByUse()
	{
		return _vpFee[1];
	}
	
	/**
	 * Returns the amount of {@code VP} required for creation.<br>
	 * This value is retrieved from the first element of the {@code _vpFee} array.
	 * @return The total {@code VP} fee count.
	 */
	public int getVPFeeCount()
	{
		return _vpFee[0];
	}
	
	/**
	 * Calculates the total maximum creation chance.<br>
	 * It sums all values from the second index of each array in {@code _createChances}.
	 * @return The sum of all maximum chances as a {@code double}.
	 */
	public double getMaxChance()
	{
		double result = 0;
		for (int i = 0; i < _createChances.size(); i++)
		{
			Double[] chance = _createChances.get(i);
			result = result + chance[1];
		}
		
		return result;
	}
	
	/**
	 * Checks if a specific item is required for the creation fee.<br>
	 * It searches through the list of items associated with this template.
	 * @param itemId The unique identifier of the item to check.
	 * @return {@code true} if the item is found in the fee, otherwise {@code false}.
	 */
	public boolean isInstanceHaveCoupon(int itemId)
	{
		for (ItemHolder humu : _itemsFee)
		{
			if (humu.getId() == itemId)
			{
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Returns the time when this template was created.<br>
	 * The value is stored as a {@code long}.
	 * @return The creation time.
	 */
	public long getCreationTime()
	{
		return _time;
	}
	
	/**
	 * Retrieves the list of creation chances for this template.<br>
	 * Each {@code Double[]} represents a specific chance value.
	 * @return A {@code List} of {@code Double[]} containing the creation chances.
	 */
	public List<Double[]> getCreationChance()
	{
		return _createChances;
	}
}
