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
package org.l2jmobius.gameserver.entity.item.holders;

import java.util.Objects;

import org.l2jmobius.gameserver.util.StatSet;

/**
 * Simple holder for an item id and count.
 * @author UnAfraid
 */
public class ItemHolder
{
	private final int _id;
	private final long _count;
	
	/**
	 * Creates a holder from a {@code StatSet}, reading the id and count.
	 * @param set The {@code StatSet} with the item properties.
	 */
	public ItemHolder(StatSet set)
	{
		_id = set.getInt("id");
		_count = set.getLong("count");
	}
	
	/**
	 * Creates a holder for the given item id and count.
	 * @param id The item id.
	 * @param count The item count.
	 */
	public ItemHolder(int id, long count)
	{
		_id = id;
		_count = count;
	}
	
	/**
	 * Retrieves the item id.
	 * @return The id as an {@code int}.
	 */
	public int getId()
	{
		return _id;
	}
	
	/**
	 * Retrieves the item count.
	 * @return The count as a {@code long}.
	 */
	public long getCount()
	{
		return _count;
	}
	
	/**
	 * Indicates whether the given object is an {@code ItemHolder} with the same id and count.
	 * @param obj The object to compare against.
	 * @return {@code true} if equal, {@code false} otherwise.
	 */
	@Override
	public boolean equals(Object obj)
	{
		if (!(obj instanceof ItemHolder))
		{
			return false;
		}
		else if (obj == this)
		{
			return true;
		}
		
		final ItemHolder objInstance = (ItemHolder) obj;
		return (_id == objInstance.getId()) && (_count == objInstance.getCount());
	}
	
	/**
	 * Computes the hash code from the id and count.
	 * @return The hash code as an {@code int}.
	 */
	@Override
	public int hashCode()
	{
		return Objects.hash(_id, _count);
	}
	
	/**
	 * Returns a string with the class name, id and count.
	 * @return The string representation.
	 */
	@Override
	public String toString()
	{
		return "[" + getClass().getSimpleName() + "] ID: " + _id + ", count: " + _count;
	}
}
