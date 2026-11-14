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
package org.l2jmobius.gameserver.mechanics.stats.functions;

/**
 * Defines the types of mathematical functions used to calculate statistics.<br>
 * These functions determine how base values are modified by various modifiers.<br>
 * Use {@code StatFunction} to specify whether a stat should be added, multiplied, or handled via other logic.
 * @author Zealar
 */
public enum StatFunction
{
	ADD("Add", 30),
	DIV("Div", 20),
	ENCHANT("Enchant", 0),
	ENCHANTHP("EnchantHp", 40),
	MUL("Mul", 20),
	SET("Set", 0),
	SUB("Sub", 30);
	
	private final String _name;
	private final int _order;
	
	/**
	 * Creates a new instance of the {@code StatFunction} enum.<br>
	 * This constructor sets the display name and calculation priority.
	 * @param name The display name for the function.
	 * @param order The numerical order used to determine calculation sequence.
	 */
	StatFunction(String name, int order)
	{
		_name = name;
		_order = order;
	}
	
	/**
	 * Returns the display name of the {@code StatFunction}.<br>
	 * This name is used for identification in the user interface.
	 * @return The string representation of the function name.
	 */
	public String getName()
	{
		return _name;
	}
	
	/**
	 * Retrieves the execution priority of this {@code StatFunction}.<br>
	 * Lower values are processed before higher values.
	 * @return The integer value representing the order.
	 */
	public int getOrder()
	{
		return _order;
	}
}
