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
 * Represents an item that is associated with a premium status.<br>
 * This class handles the logic for items granted to players who have active premium subscriptions.
 * @author Gnacik
 */
public class PremiumItem
{
	private final int _itemId;
	private long _count;
	private final String _sender;
	
	/**
	 * Creates a new instance of {@code PremiumItem}.<br>
	 * This constructor initializes the item with its ID, quantity, and owner.
	 * @param itemid The unique identifier for the item.
	 * @param count The total number of items.
	 * @param sender The name of the player who sent the item.
	 */
	public PremiumItem(int itemid, long count, String sender)
	{
		_itemId = itemid;
		_count = count;
		_sender = sender;
	}
	
	/**
	 * Updates the current count of the item.<br>
	 * Sets the {@code _count} field to a new value.
	 * @param newcount The new quantity for the item.
	 */
	public void updateCount(long newcount)
	{
		_count = newcount;
	}
	
	/**
	 * Retrieves the unique identifier for this item.<br>
	 * This value is assigned when the {@code PremiumItem} is created.
	 * @return The {@code int} ID of the item.
	 */
	public int getItemId()
	{
		return _itemId;
	}
	
	/**
	 * Retrieves the current quantity of the item.<br>
	 * This value represents how many units are held.
	 * @return The total count as a {@code long}.
	 */
	public long getCount()
	{
		return _count;
	}
	
	/**
	 * Retrieves the name of the sender.<br>
	 * This identifies who provided the {@code PremiumItem}.
	 * @return The {@code String} representing the sender's name.
	 */
	public String getSender()
	{
		return _sender;
	}
}
