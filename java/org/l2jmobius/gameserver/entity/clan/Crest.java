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
package org.l2jmobius.gameserver.entity.clan;

import org.l2jmobius.gameserver.entity.clan.enums.CrestType;

/**
 * Represents a clan crest within the game world.<br>
 * This class manages the visual and functional data associated with a specific {@code CrestType}.
 * @author NosBit
 */
public class Crest
{
	private final int _id;
	private final byte[] _data;
	private final CrestType _type;
	
	/**
	 * Creates a new instance of the {@code Crest} class.<br>
	 * This constructor initializes all required fields for a crest object.
	 * @param id The unique identifier for the crest.
	 * @param data The raw byte array containing the crest information.
	 * @param type The {@code CrestType} category of this crest.
	 */
	public Crest(int id, byte[] data, CrestType type)
	{
		_id = id;
		_data = data;
		_type = type;
	}
	
	/**
	 * Retrieves the unique identifier for this {@code Crest}.<br>
	 * This value is assigned when the object is created.
	 * @return The {@code int} ID of the crest.
	 */
	public int getId()
	{
		return _id;
	}
	
	/**
	 * Retrieves the raw data associated with this {@code Crest}.<br>
	 * This method returns the internal byte array.
	 * @return a {@code byte[]} containing the crest data.
	 */
	public byte[] getData()
	{
		return _data;
	}
	
	/**
	 * Retrieves the type of the {@code Crest}.<br>
	 * This method returns the specific category assigned to this crest.
	 * @return the {@code CrestType} of the current crest.
	 */
	public CrestType getType()
	{
		return _type;
	}
}
