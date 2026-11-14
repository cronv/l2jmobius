/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2jmobius.gameserver.entity.item.enchant.attribute;

import org.l2jmobius.gameserver.entity.actor.enums.creature.AttributeType;

/**
 * This class acts as a container for various {@code AttributeType} values.<br>
 * It allows the system to manage and store attributes associated with enchantments.
 * @author UnAfraid
 */
public class AttributeHolder
{
	private final AttributeType _type;
	private int _value;
	
	/**
	 * Creates a new instance of {@code AttributeHolder}.<br>
	 * This constructor initializes the attribute with a specific type and value.
	 * @param type The {@code AttributeType} to assign to this holder.
	 * @param value The initial integer value for the attribute.
	 */
	public AttributeHolder(AttributeType type, int value)
	{
		_type = type;
		_value = value;
	}
	
	/**
	 * Retrieves the type of the attribute.<br>
	 * This method returns the {@code AttributeType} associated with this holder.
	 * @return the {@code AttributeType} of this attribute.
	 */
	public AttributeType getType()
	{
		return _type;
	}
	
	/**
	 * Retrieves the current numerical value of this attribute.<br>
	 * This method returns the integer stored in the {@code _value} field.
	 * @return The current value as an {@code int}.
	 */
	public int getValue()
	{
		return _value;
	}
	
	/**
	 * Updates the current value of this attribute.<br>
	 * The new {@code int} will replace the old one.
	 * @param value The new value to set.
	 */
	public void setValue(int value)
	{
		_value = value;
	}
	
	/**
	 * Increases the current value of this attribute.<br>
	 * The new value is calculated by adding {@code with} to the existing value.
	 * @param with The amount to add to the current value.
	 */
	public void incValue(int with)
	{
		_value += with;
	}
	
	/**
	 * Returns a string representation of the attribute.<br>
	 * It combines the {@code AttributeType} name and the current value.
	 * @return A formatted string showing the type and its value.
	 */
	@Override
	public String toString()
	{
		return _type.name() + " +" + _value;
	}
}
