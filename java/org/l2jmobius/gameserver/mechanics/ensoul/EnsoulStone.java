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
package org.l2jmobius.gameserver.mechanics.ensoul;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an {@code EnsoulStone} item used within the ensoul mechanic.<br>
 * This class handles the data and logic associated with these specific stones.
 * @author UnAfraid
 */
public class EnsoulStone
{
	private final int _id;
	private final int _slotType;
	private final List<Integer> _options = new ArrayList<>();
	
	/**
	 * Creates a new instance of {@code EnsoulStone}.<br>
	 * This constructor initializes the stone with a specific ID and slot type.
	 * @param id The unique identifier for the stone.
	 * @param slotType The type of slot this stone belongs to.
	 */
	public EnsoulStone(int id, int slotType)
	{
		_id = id;
		_slotType = slotType;
	}
	
	/**
	 * Retrieves the unique identifier for this {@code EnsoulStone}.<br>
	 * This value is assigned when the object is created.
	 * @return The {@code int} ID of the stone.
	 */
	public int getId()
	{
		return _id;
	}
	
	/**
	 * Retrieves the type of slot associated with this {@code EnsoulStone}.<br>
	 * This value identifies which equipment slot the stone can be used in.
	 * @return The integer value representing the slot type.
	 */
	public int getSlotType()
	{
		return _slotType;
	}
	
	/**
	 * Retrieves the list of available options for this {@code EnsoulStone}.<br>
	 * This method returns all values stored in the internal options list.
	 * @return a {@code List<Integer>} containing the stone's options.
	 */
	public List<Integer> getOptions()
	{
		return _options;
	}
	
	/**
	 * Adds a new value to the list of options.<br>
	 * This method updates the internal {@code _options} list.
	 * @param option The integer value to add to the collection.
	 */
	public void addOption(int option)
	{
		_options.add(option);
	}
}
