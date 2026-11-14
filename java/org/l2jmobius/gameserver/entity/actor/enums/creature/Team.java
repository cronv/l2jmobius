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
package org.l2jmobius.gameserver.entity.actor.enums.creature;

/**
 * Defines the different teams that creatures can belong to.<br>
 * This enumeration is used to manage group affiliations and interactions between entities.
 * @author NosBit
 */
public enum Team
{
	NONE(0),
	BLUE(1),
	RED(2);
	
	private int _id;
	
	/**
	 * Creates a new instance of the {@code Team} enum.<br>
	 * This constructor assigns an internal identifier to the team.
	 * @param id The unique integer ID for the team.
	 */
	private Team(int id)
	{
		_id = id;
	}
	
	/**
	 * Retrieves the unique identifier for this {@code Team}.<br>
	 * This value is used to distinguish between different team types.
	 * @return The integer ID of the team.
	 */
	public int getId()
	{
		return _id;
	}
}
