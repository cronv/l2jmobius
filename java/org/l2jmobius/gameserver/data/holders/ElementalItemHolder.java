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
package org.l2jmobius.gameserver.data.holders;

import org.l2jmobius.gameserver.entity.actor.enums.creature.AttributeType;
import org.l2jmobius.gameserver.entity.item.enums.ElementalItemType;

/**
 * This class acts as a data holder for items that possess elemental properties.<br>
 * It stores information related to {@code ElementalItemType} and associated attributes.
 * @author Mobius
 */
public class ElementalItemHolder
{
	private final int _itemId;
	private final AttributeType _element;
	private final ElementalItemType _type;
	private final int _power;
	
	/**
	 * Creates a new {@code ElementalItemHolder} instance.<br>
	 * This constructor initializes the item properties.
	 * @param itemId The unique identifier for the item.
	 * @param element The type of elemental attribute.
	 * @param type The category of the elemental item.
	 * @param power The strength value of the element.
	 */
	public ElementalItemHolder(int itemId, AttributeType element, ElementalItemType type, int power)
	{
		_itemId = itemId;
		_element = element;
		_type = type;
		_power = power;
	}
	
	/**
	 * Retrieves the unique identifier for this item.<br>
	 * This value is used to identify the specific item in the game database.
	 * @return The {@code int} ID of the item.
	 */
	public int getItemId()
	{
		return _itemId;
	}
	
	/**
	 * Retrieves the element type associated with this item.<br>
	 * This method returns the {@code AttributeType} assigned to the holder.
	 * @return The {@code AttributeType} of the element.
	 */
	public AttributeType getElement()
	{
		return _element;
	}
	
	/**
	 * Retrieves the elemental type of this item.<br>
	 * This method returns the {@code ElementalItemType} associated with the holder.
	 * @return the {@code ElementalItemType} of the item.
	 */
	public ElementalItemType getType()
	{
		return _type;
	}
	
	/**
	 * Retrieves the power level of the elemental item.<br>
	 * This value is stored in the {@code _power} field.
	 * @return The integer value representing the item's power.
	 */
	public int getPower()
	{
		return _power;
	}
}
