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
package org.l2jmobius.gameserver.data.holders;

import org.l2jmobius.gameserver.entity.actor.Creature;

/**
 * This class acts as a data container for information regarding warped space instances.<br>
 * It stores the state and properties of a specific {@code WarpedSpace} area.<br>
 * Use this holder to manage spatial data shared between entities.
 * @author Sdw
 */
public class WarpedSpaceHolder
{
	private final Creature _creature;
	private final int _range;
	
	/**
	 * Creates a new {@code WarpedSpaceHolder} instance.<br>
	 * This object stores the data for a specific warped space area.
	 * @param creature The {@code Creature} associated with this space.
	 * @param range The distance of the effect in units.
	 */
	public WarpedSpaceHolder(Creature creature, int range)
	{
		_creature = creature;
		_range = range;
	}
	
	/**
	 * Retrieves the {@code Creature} associated with this holder.<br>
	 * This method returns the internal creature object.
	 * @return The {@code Creature} instance.
	 */
	public Creature getCreature()
	{
		return _creature;
	}
	
	/**
	 * Retrieves the distance of the warped space.<br>
	 * This value represents how far the effect reaches.
	 * @return The range as an {@code int}.
	 */
	public int getRange()
	{
		return _range;
	}
}
