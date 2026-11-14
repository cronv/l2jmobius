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
 * This class represents text specifically used for the {@code Builder} mechanic.<br>
 * It handles the formatting and display of strings related to building actions.
 * @author Forsaiken
 */
public class BuilderText extends Builder
{
	private final String _text;
	
	/**
	 * Creates a new instance of {@code BuilderText}.<br>
	 * It sets the internal text content.
	 * @param text The string to be stored in the builder.
	 */
	BuilderText(String text)
	{
		_text = text;
	}
	
	/**
	 * Converts the builder text into a string representation.<br>
	 * This method calls the standard {@code toString()} method.<br>
	 * It accepts an {@code Object} parameter but does not use it.
	 * @param param The object to be passed to the method.
	 * @return The resulting string representation of the text.
	 */
	@Override
	public String toString(Object param)
	{
		return toString();
	}
	
	/**
	 * Converts the builder content into a formatted string.<br>
	 * This method accepts multiple {@code Object} arguments to be included in the output.<br>
	 * It internally calls the standard {@code toString()} method.
	 * @param params A variable number of objects to include in the resulting string.
	 * @return The final formatted string representation.
	 */
	@Override
	public String toString(Object... params)
	{
		return toString();
	}
	
	/**
	 * Returns the current index of the text.<br>
	 * This method always returns {@code -1}.
	 * @return The current index as an {@code int}.
	 */
	@Override
	public int getIndex()
	{
		return -1;
	}
	
	/**
	 * Returns the text content of this {@code BuilderText} object.<br>
	 * This method provides a string representation of the stored value.
	 * @return The underlying {@code String} value.
	 */
	@Override
	public String toString()
	{
		return _text;
	}
}
