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
package org.l2jmobius.gameserver.interfaces;

import org.l2jmobius.gameserver.entity.actor.enums.creature.Position;
import org.l2jmobius.gameserver.util.LocationUtil;

/**
 * This interface defines the methods for storing and retrieving an object's world location.<br>
 * It allows entities to manage their {@code Position} data within the game world.
 * @author xban1x
 */
public interface ILocational
{
	/**
	 * Gets the X coordinate of this object.
	 * @return the X coordinate
	 */
	int getX();
	
	/**
	 * Gets the Y coordinate of this object.
	 * @return the current Y coordinate
	 */
	int getY();
	
	/**
	 * Gets the Z coordinate of this object.
	 * @return the current Z coordinate
	 */
	int getZ();
	
	/**
	 * Gets the heading of this object.
	 * @return the current heading
	 */
	int getHeading();
	
	/**
	 * Gets this object's location.
	 * @return a {@link ILocational} object containing the current position of this object
	 */
	ILocational getLocation();
	
	/**
	 * Calculates the heading required to face a specific location.<br>
	 * This method uses the current coordinates of this object and the target's coordinates.
	 * @param to The {@code ILocational} destination to calculate the heading toward.
	 * @return The calculated heading as an {@code int}.
	 */
	default int calculateHeadingTo(ILocational to)
	{
		return LocationUtil.calculateHeadingFrom(getX(), getY(), to.getX(), to.getY());
	}
	
	/**
	 * Checks if the {@code target} is located in front of this object.<br>
	 * Returns {@code false} if the {@code target} is {@code null}.
	 * @param target The location to check against.
	 * @return {@code true} if the target is in front, otherwise {@code false}.
	 */
	default boolean isInFrontOf(ILocational target)
	{
		if (target == null)
		{
			return false;
		}
		
		return Position.FRONT == Position.getPosition(this, target);
	}
	
	/**
	 * Checks if this object is positioned to the side of a target.<br>
	 * It compares the relative position between {@code this} and the {@code target}.
	 * @param target The location to check against.
	 * @return {@code true} if the object is on the side, {@code false} otherwise.
	 */
	default boolean isOnSideOf(ILocational target)
	{
		if (target == null)
		{
			return false;
		}
		
		return Position.SIDE == Position.getPosition(this, target);
	}
	
	/**
	 * Checks if the {@code target} is located behind this object.<br>
	 * It compares the relative position of the two objects.
	 * @param target The {@code ILocational} object to check against.
	 * @return {@code true} if the target is behind, {@code false} otherwise.
	 */
	default boolean isBehind(ILocational target)
	{
		if (target == null)
		{
			return false;
		}
		
		return Position.BACK == Position.getPosition(this, target);
	}
}
