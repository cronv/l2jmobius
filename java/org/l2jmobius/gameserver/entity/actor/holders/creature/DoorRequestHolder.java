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
package org.l2jmobius.gameserver.entity.actor.holders.creature;

import org.l2jmobius.gameserver.entity.actor.instance.Door;

/**
 * This class holds the request data for interacting with a {@code Door}.<br>
 * It stores information needed to process door-related actions.<br>
 * Use this holder to manage state during door interactions.
 * @author UnAfraid
 */
public class DoorRequestHolder
{
	private final Door _target;
	
	/**
	 * Creates a new {@code DoorRequestHolder} instance.<br>
	 * This constructor initializes the holder with a specific {@code Door}.
	 * @param door The {@code Door} object to be stored in this holder.
	 */
	public DoorRequestHolder(Door door)
	{
		_target = door;
	}
	
	/**
	 * Retrieves the {@code Door} associated with this holder.<br>
	 * This method returns the target door object.
	 * @return The {@code Door} instance.
	 */
	public Door getDoor()
	{
		return _target;
	}
}
