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

import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;

/**
 * Represents a single bid placed by a {@code Player} on an auction item.<br>
 * This class stores the details of the offer, such as the amount and the bidder.<br>
 * It is used to manage the bidding process within the auction system.
 * @author Forsaiken
 */
public class ItemAuctionBid
{
	private final int _playerObjId;
	private long _lastBid;
	
	/**
	 * Creates a new {@code ItemAuctionBid} instance.<br>
	 * This constructor initializes the bid with a specific player and amount.
	 * @param playerObjId The unique identifier for the player making the bid.
	 * @param lastBid The current value of the bid.
	 */
	public ItemAuctionBid(int playerObjId, long lastBid)
	{
		_playerObjId = playerObjId;
		_lastBid = lastBid;
	}
	
	/**
	 * Retrieves the unique identifier of the player who placed the bid.<br>
	 * This ID corresponds to the {@code playerObjId} field.
	 * @return The {@code int} object ID of the player.
	 */
	public int getPlayerObjId()
	{
		return _playerObjId;
	}
	
	/**
	 * Retrieves the current highest bid for this auction.<br>
	 * This value represents the most recent amount offered.
	 * @return The {@code long} value of the last bid.
	 */
	public long getLastBid()
	{
		return _lastBid;
	}
	
	/**
	 * Updates the current highest bid for this auction.<br>
	 * This method sets the {@code _lastBid} field to a new value.
	 * @param lastBid The new amount of the bid.
	 */
	public void setLastBid(long lastBid)
	{
		_lastBid = lastBid;
	}
	
	/**
	 * Cancels the current bid for this auction.<br>
	 * Sets the {@code _lastBid} value to {@code -1}.<br>
	 * This action marks the bid as no longer active.
	 */
	public void cancelBid()
	{
		_lastBid = -1;
	}
	
	/**
	 * Checks if the bid has been canceled.<br>
	 * Returns {@code true} if the last bid is less than or equal to {@code 0}.<br>
	 * Otherwise, it returns {@code false}.
	 * @return The cancellation status of the bid.
	 */
	public boolean isCanceled()
	{
		return _lastBid <= 0;
	}
	
	/**
	 * Retrieves the {@code Player} object associated with this bid.<br>
	 * It uses the internal player ID to find the character in the world.
	 * @return the {@code Player} instance, or {@code null} if not found.
	 */
	public Player getPlayer()
	{
		return World.getPlayer(_playerObjId);
	}
}
