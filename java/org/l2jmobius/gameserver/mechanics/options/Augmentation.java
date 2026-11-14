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
package org.l2jmobius.gameserver.mechanics.options;

/**
 * Augmentation template of a single mineral.<br>
 * Holds the option groups that can be rolled for each weapon discipline.
 * @author Pere, Mobius
 */
public class Augmentation
{
	private final int _mineralId;
	private final OptionDataGroup[] _effects = new OptionDataGroup[3];
	private int _itemGroup = -1;
	
	/**
	 * Creates a new instance of {@code Augmentation}.<br>
	 * This constructor initializes the mineral and group identifiers.
	 * @param mineralId The unique identifier for the mineral.
	 * @param itemGroup The group ID associated with this augmentation.
	 */
	public Augmentation(int mineralId, int itemGroup)
	{
		_mineralId = mineralId;
		_itemGroup = itemGroup;
	}
	
	/**
	 * Retrieves the unique identifier for the mineral.<br>
	 * This value is used to identify which mineral this {@code Augmentation} belongs to.
	 * @return The {@code int} ID of the mineral.
	 */
	public int getMineralId()
	{
		return _mineralId;
	}
	
	/**
	 * Retrieves the unique identifier for the item group.<br>
	 * This value is assigned during the creation of a {@code Augmentation}.
	 * @return The integer ID of the item group.
	 */
	public int getItemGroup()
	{
		return _itemGroup;
	}
	
	/**
	 * Retrieves the collection of effect groups associated with this augmentation.<br>
	 * This method returns the internal array of {@code OptionDataGroup} objects.
	 * @return an array of {@code OptionDataGroup} objects.
	 */
	public OptionDataGroup[] getOptionDataGroup()
	{
		return _effects;
	}
	
	/**
	 * Assigns a specific effect group to a augmentation.<br>
	 * The {@code order} determines the index in the internal array.<br>
	 * This method updates the data for the specified position.
	 * @param order The index where the group should be stored.
	 * @param group The {@code OptionDataGroup} to assign to this order.
	 */
	public void setEffectGroup(int order, OptionDataGroup group)
	{
		_effects[order] = group;
	}
	
	/**
	 * Retrieves a random effect for a specific item.<br>
	 * This method checks if the effect group exists at the given {@code order}.<br>
	 * It then returns an {@code Options} object based on the provided item ID.
	 * @param order The index of the effect group to use.
	 * @param targetItemId The unique identifier of the item being processed.
	 * @return The random {@code Options} for the item, or {@code null} if no effect is found.
	 */
	public Options getRandomEffect(int order, int targetItemId)
	{
		if (_effects[order] == null)
		{
			return null;
		}
		
		return _effects[order].getRandomEffect(targetItemId);
	}
}
