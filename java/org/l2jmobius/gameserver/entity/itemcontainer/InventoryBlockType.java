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
package org.l2jmobius.gameserver.entity.itemcontainer;

/**
 * Defines the different types of blocks available within an inventory.<br>
 * This enum is used to categorize how items are organized or displayed in a container.
 * @author UnAfraid
 */
public enum InventoryBlockType
{
	NONE(-1),
	BLACKLIST(0),
	WHITELIST(1);
	
	private final int _clientId;
	
	/**
	 * Creates a new instance of {@code InventoryBlockType}.<br>
	 * This constructor initializes the internal ID for the block type.
	 * @param clientId The unique identifier associated with this block type.
	 */
	private InventoryBlockType(int clientId)
	{
		_clientId = clientId;
	}
	
	/**
	 * Retrieves the unique identifier for this {@code InventoryBlockType}.<br>
	 * This value is used to identify the block type in the client.
	 * @return The integer ID of the block type.
	 */
	public int getClientId()
	{
		return _clientId;
	}
}
