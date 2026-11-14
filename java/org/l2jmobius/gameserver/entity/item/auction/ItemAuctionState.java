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

/**
 * Represents the possible states of an item auction.<br>
 * This enum is used to track whether an auction is active, completed, or failed.
 * @author Forsaiken
 */
public enum ItemAuctionState
{
	CREATED((byte) 0),
	STARTED((byte) 1),
	FINISHED((byte) 2);
	
	private final byte _stateId;
	
	/**
	 * Creates a new {@code ItemAuctionState} instance.<br>
	 * This constructor sets the internal state identifier.
	 * @param stateId The unique byte ID for the auction state.
	 */
	ItemAuctionState(byte stateId)
	{
		_stateId = stateId;
	}
	
	/**
	 * Retrieves the unique identifier for the current auction state.<br>
	 * This value is used to identify the status in the database.
	 * @return The {@code byte} ID of the {@code ItemAuctionState}.
	 */
	public byte getStateId()
	{
		return _stateId;
	}
	
	/**
	 * Converts a numeric ID into its corresponding {@code ItemAuctionState}.<br>
	 * This method searches through all available states.<br>
	 * It returns the matching state if found.
	 * @param stateId The byte value representing the auction state.
	 * @return The matching {@code ItemAuctionState} or {@code null} if no match exists.
	 */
	public static ItemAuctionState stateForStateId(byte stateId)
	{
		for (ItemAuctionState state : ItemAuctionState.values())
		{
			if (state.getStateId() == stateId)
			{
				return state;
			}
		}
		
		return null;
	}
}
