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
package org.l2jmobius.gameserver.mechanics.olympiad;

/**
 * Defines the different types of competitions available in the Olympiad system.<br>
 * This enum is used to categorize and identify specific competition modes.
 * @author DS
 */
public enum CompetitionType
{
	CLASSED("classed"),
	NON_CLASSED("non-classed"),
	OTHER("other");
	
	private final String _name;
	
	/**
	 * Creates a new {@code CompetitionType} instance.<br>
	 * Sets the internal name of the competition type.
	 * @param name The string value to assign as the name.
	 */
	CompetitionType(String name)
	{
		_name = name;
	}
	
	/**
	 * Returns the internal name of the {@code CompetitionType}.<br>
	 * This is useful for identifying the type in a human-readable format.
	 * @return The string representation of the competition type.
	 */
	@Override
	public String toString()
	{
		return _name;
	}
}
