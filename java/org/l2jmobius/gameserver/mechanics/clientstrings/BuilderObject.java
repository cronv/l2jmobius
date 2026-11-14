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
package org.l2jmobius.gameserver.mechanics.clientstrings;

/**
 * This class serves as a base for constructing objects that require specific client-side string data.<br>
 * It provides a structured way to build complex objects by extending the {@code Builder} functionality.
 * @author Forsaiken
 */
public class BuilderObject extends Builder
{
	private final int _index;
	
	/**
	 * Creates a new instance of {@code BuilderObject}.<br>
	 * This constructor sets the internal index based on the provided ID.<br>
	 * The input {@code id} must be between 1 and 9 inclusive.
	 * @param id The unique identifier used to set the object's index.
	 */
	BuilderObject(int id)
	{
		if ((id < 1) || (id > 9))
		{
			throw new RuntimeException("Illegal Id: " + id);
		}
		
		_index = id - 1;
	}
	
	/**
	 * Converts an {@code Object} into its string representation.<br>
	 * If the provided parameter is {@code null}, it returns the literal {@code "null"}.<br>
	 * Otherwise, it calls the {@code toString()} method on the object.
	 * @param param The object to convert to a string.
	 * @return A string representation of the given object.
	 */
	@Override
	public String toString(Object param)
	{
		return param == null ? "null" : param.toString();
	}
	
	/**
	 * Converts the provided arguments into a string representation.<br>
	 * This method returns {@code null} if the input is empty or {@code null}.<br>
	 * It specifically returns the string value of the first element in the array.
	 * @param params A variable number of objects to process.
	 * @return The string representation of the first parameter, or {@code null} if no parameters are provided.
	 */
	@Override
	public String toString(Object... params)
	{
		if ((params == null) || (params.length == 0))
		{
			return "null";
		}
		
		return params[0].toString();
	}
	
	/**
	 * Retrieves the current index of this {@code BuilderObject}.<br>
	 * This value was set during the construction of the object.
	 * @return The integer value of the index.
	 */
	@Override
	public int getIndex()
	{
		return _index;
	}
	
	/**
	 * Returns a string representation of the current object.<br>
	 * This string shows the index value inside brackets.<br>
	 * The displayed number is incremented by {@code 1}.
	 * @return A formatted string containing the adjusted index.
	 */
	@Override
	public String toString()
	{
		return "[PARAM-" + (_index + 1) + "]";
	}
}
