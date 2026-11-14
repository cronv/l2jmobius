/*
 * Copyright (c) 2013 L2jMobius
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.l2jmobius.gameserver.entity.item.holders;

/**
 * This class manages enchantments specifically for unique items.<br>
 * It extends {@code ItemEnchantHolder} to provide specialized handling for these types of objects.
 * @author Index, Mobius
 */
public class UniqueItemEnchantHolder extends ItemEnchantHolder
{
	private final int _objectId;
	
	/**
	 * Creates a new {@code UniqueItemEnchantHolder} instance.<br>
	 * This constructor sets the default count to {@code 1}.
	 * @param id The unique identifier for the enchantment.
	 * @param objectId The identifier of the associated object.
	 */
	public UniqueItemEnchantHolder(int id, int objectId)
	{
		this(id, objectId, 1);
	}
	
	/**
	 * Creates a new instance of {@code UniqueItemEnchantHolder}.<br>
	 * This constructor initializes the holder with an ID, an object ID, and a count.
	 * @param id The unique identifier for the enchantment.
	 * @param objectId The identifier for the specific object.
	 * @param count The quantity of the item.
	 */
	public UniqueItemEnchantHolder(int id, int objectId, long count)
	{
		super(id, count);
		_objectId = objectId;
	}
	
	/**
	 * Creates a new {@code UniqueItemEnchantHolder} from an existing {@code ItemEnchantHolder}.<br>
	 * This constructor copies the ID, count, and enchant level from the provided holder.<br>
	 * It also assigns a specific object identifier to this unique instance.
	 * @param itemHolder The source {@code ItemEnchantHolder} to copy data from.
	 * @param objectId The unique identifier for this specific object.
	 */
	public UniqueItemEnchantHolder(ItemEnchantHolder itemHolder, int objectId)
	{
		super(itemHolder.getId(), itemHolder.getCount(), itemHolder.getEnchantLevel());
		_objectId = objectId;
	}
	
	/**
	 * Retrieves the unique identifier for this object.<br>
	 * This value is used to identify specific instances in the game world.
	 * @return The {@code int} object ID.
	 */
	public int getObjectId()
	{
		return _objectId;
	}
	
	/**
	 * Returns a string representation of this {@code UniqueItemEnchantHolder}.<br>
	 * This includes the class name, ID, object ID, count, and enchant level.
	 * @return A formatted string containing the holder details.
	 */
	@Override
	public String toString()
	{
		return "[" + getClass().getSimpleName() + "] ID: " + getId() + ", object ID: " + _objectId + ", count: " + getCount() + ", enchant level: " + getEnchantLevel();
	}
}
