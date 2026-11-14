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
package org.l2jmobius.gameserver.entity.actor.transform;

import org.l2jmobius.gameserver.entity.item.holders.ItemHolder;

/**
 * This class represents an additional item holder for a {@code transform}.<br>
 * It extends {@code ItemHolder} to manage extra items associated with the entity.
 * @author UnAfraid
 */
public class AdditionalItemHolder extends ItemHolder
{
	private final boolean _allowed;
	
	/**
	 * Creates a new instance of {@code AdditionalItemHolder}.<br>
	 * This constructor initializes the item with a specific ID and permission status.
	 * @param id The unique identifier for the item.
	 * @param allowed A boolean indicating if the item is permitted to be used.
	 */
	public AdditionalItemHolder(int id, boolean allowed)
	{
		super(id, 0);
		_allowed = allowed;
	}
	
	/**
	 * Checks if the item can be used.<br>
	 * This method returns the value of the {@code _allowed} flag.
	 * @return {@code true} if the item is allowed to be used, {@code false} otherwise.
	 */
	public boolean isAllowedToUse()
	{
		return _allowed;
	}
}
