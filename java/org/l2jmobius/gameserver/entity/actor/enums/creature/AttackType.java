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
 * Defines the different types of attacks that a {@code creature} can perform.<br>
 * This enumeration is used to categorize combat actions for mechanics and animations.
 * @author UnAfraid
 */
public enum AttackType
{
	MISSED(1),
	BLOCKED(2),
	CRITICAL(4),
	SHOT_USED(8);
	
	private final int _mask;
	
	/**
	 * Creates a new {@code AttackType} instance.<br>
	 * This constructor assigns the bitwise mask to the internal field.
	 * @param mask The integer value representing the specific attack type.
	 */
	private AttackType(int mask)
	{
		_mask = mask;
	}
	
	/**
	 * Returns the bitmask value associated with this {@code AttackType}.<br>
	 * This value is used to identify specific attack properties.
	 * @return The integer mask for the current attack type.
	 */
	public int getMask()
	{
		return _mask;
	}
}
