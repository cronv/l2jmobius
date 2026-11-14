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

/**
 * This class serves as a Data Transfer Object (DTO) for unique items.<br>
 * It stores the item {@code id}, its specific object {@code id}, and the total count.
 * @author xban1x
 */
public class UniqueItemHolder extends ItemHolder
{
	private final int _objectId;
	
	/**
	 * Creates a new {@code UniqueItemHolder} with a default count of {@code 1}.<br>
	 * This constructor initializes the item using the provided IDs.
	 * @param id The unique identifier for the item.
	 * @param objectId The specific object ID associated with this item.
	 */
	public UniqueItemHolder(int id, int objectId)
	{
		this(id, objectId, 1);
	}
	
	/**
	 * Creates a new {@code UniqueItemHolder} instance.<br>
	 * This constructor initializes the item with a specific ID, object ID, and count.
	 * @param id The unique identifier for the item.
	 * @param objectId The specific object ID associated with this item.
	 * @param count The total quantity of the item.
	 */
	public UniqueItemHolder(int id, int objectId, long count)
	{
		super(id, count);
		_objectId = objectId;
	}
	
	/**
	 * Retrieves the unique identifier for this item.<br>
	 * This value is used to distinguish specific instances of an item.
	 * @return The {@code int} object ID.
	 */
	public int getObjectId()
	{
		return _objectId;
	}
	
	/**
	 * Returns a string representation of this {@code UniqueItemHolder}.<br>
	 * It includes the class name, item ID, object ID, and count.
	 * @return A formatted string describing the current holder.
	 */
	@Override
	public String toString()
	{
		return "[" + getClass().getSimpleName() + "] ID: " + getId() + ", object ID: " + _objectId + ", count: " + getCount();
	}
}
