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
package org.l2jmobius.gameserver.entity.item.auction;

import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.managers.IdManager;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Represents an item currently listed in the auction system.<br>
 * This class manages the data and state of a specific {@code Item} instance while it is being sold.
 * @author Forsaiken
 */
public class AuctionItem
{
	private final int _auctionItemId;
	private final int _auctionLength;
	private final long _auctionInitBid;
	
	private final int _itemId;
	private final long _itemCount;
	@SuppressWarnings("unused")
	private final StatSet _itemExtra;
	
	/**
	 * Creates a new {@code AuctionItem} instance.<br>
	 * This constructor initializes all required fields for an auction listing.
	 * @param auctionItemId The unique identifier for the auction.
	 * @param auctionLength The duration of the auction in seconds.
	 * @param auctionInitBid The starting bid amount for the auction.
	 * @param itemId The base item ID being auctioned.
	 * @param itemCount The quantity of the item available.
	 * @param itemExtra Additional statistics or properties for the item.
	 */
	public AuctionItem(int auctionItemId, int auctionLength, long auctionInitBid, int itemId, long itemCount, StatSet itemExtra)
	{
		_auctionItemId = auctionItemId;
		_auctionLength = auctionLength;
		_auctionInitBid = auctionInitBid;
		_itemId = itemId;
		_itemCount = itemCount;
		_itemExtra = itemExtra;
	}
	
	/**
	 * Checks if the item template exists in the game data.<br>
	 * It verifies that {@code _itemId} is valid.
	 * @return {@code true} if the item exists, {@code false} otherwise.
	 */
	public boolean checkItemExists()
	{
		return ItemData.getInstance().getTemplate(_itemId) != null;
	}
	
	/**
	 * Retrieves the unique identifier for this auction item.<br>
	 * This ID is used to distinguish different auctions in the system.
	 * @return The {@code int} value of the auction item ID.
	 */
	public int getAuctionItemId()
	{
		return _auctionItemId;
	}
	
	/**
	 * Retrieves the duration of the auction.<br>
	 * This value represents how long the auction will last.
	 * @return The length of the auction as an {@code int}.
	 */
	public int getAuctionLength()
	{
		return _auctionLength;
	}
	
	/**
	 * Retrieves the starting bid for this auction.<br>
	 * This value is used to determine the minimum price required to place a bid.
	 * @return The initial bid amount as a {@code long}.
	 */
	public long getAuctionInitBid()
	{
		return _auctionInitBid;
	}
	
	/**
	 * Retrieves the unique identifier for the item.<br>
	 * This value corresponds to the {@code itemId} defined in the data.
	 * @return The integer ID of the item.
	 */
	public int getItemId()
	{
		return _itemId;
	}
	
	/**
	 * Retrieves the total number of items in this auction.<br>
	 * This value represents the quantity associated with the {@code itemId}.
	 * @return The count of items as a {@code long}.
	 */
	public long getItemCount()
	{
		return _itemCount;
	}
	
	/**
	 * Creates a new {@code Item} instance based on the auction data.<br>
	 * This method generates a unique ID and adds the item to the world.<br>
	 * It also sets the correct count and default enchantment level.
	 * @return The newly created {@code Item} object.
	 */
	public Item createNewItemInstance()
	{
		final Item item = new Item(IdManager.getInstance().getNextId(), _itemId);
		World.addObject(item);
		item.setCount(_itemCount);
		item.setEnchantLevel(item.getTemplate().getDefaultEnchantLevel());
		return item;
	}
}
