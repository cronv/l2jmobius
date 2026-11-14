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
package org.l2jmobius.gameserver.network.holders;

import org.l2jmobius.gameserver.entity.item.holders.ItemHolder;
import org.l2jmobius.gameserver.network.enums.TryMixCubeResultType;

/**
 * Represents the result of an alchemy action performed on an item.<br>
 * This class holds data regarding the outcome of a mix cube or similar transformation.<br>
 * It extends {@code ItemHolder} to maintain information about the affected object.
 * @author UnAfraid
 */
public class AlchemyResult extends ItemHolder
{
	private final TryMixCubeResultType _type;
	
	/**
	 * Creates a new {@code AlchemyResult} object.<br>
	 * This constructor initializes the result with an ID and a count.<br>
	 * It also sets the specific type of the alchemy outcome.
	 * @param id The unique identifier for the item.
	 * @param count The quantity of the item.
	 * @param type The {@code TryMixCubeResultType} associated with this result.
	 */
	public AlchemyResult(int id, long count, TryMixCubeResultType type)
	{
		super(id, count);
		_type = type;
	}
	
	/**
	 * Retrieves the result type of the alchemy attempt.<br>
	 * This method returns the {@code TryMixCubeResultType} associated with this result.
	 * @return the {@code TryMixCubeResultType} of the alchemy result.
	 */
	public TryMixCubeResultType getType()
	{
		return _type;
	}
}
