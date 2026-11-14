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

import org.l2jmobius.gameserver.interfaces.ILocational;

/**
 * Represents the different types of positions a {@code creature} can occupy.<br>
 * This enumeration is used to categorize spatial states within the game world.
 * @author Sdw
 */
public enum Position
{
	FRONT,
	SIDE,
	BACK;
	
	/**
	 * Calculates the relative position of one location compared to another.<br>
	 * This method uses retail-like formulas to determine if a target is in front, side, or back.
	 * @param from The starting location used as the reference point.
	 * @param to The destination location to check against the reference.
	 * @return The calculated {@code Position} relative to the source.
	 */
	public static Position getPosition(ILocational from, ILocational to)
	{
		final int heading = Math.abs(to.getHeading() - from.calculateHeadingTo(to));
		if (((heading >= 0x2000) && (heading <= 0x6000)) || (Integer.toUnsignedLong(heading - 0xA000) <= 0x4000))
		{
			return SIDE;
		}
		else if (Integer.toUnsignedLong(heading - 0x2000) <= 0xC000)
		{
			return FRONT;
		}
		else
		{
			return BACK;
		}
	}
}
