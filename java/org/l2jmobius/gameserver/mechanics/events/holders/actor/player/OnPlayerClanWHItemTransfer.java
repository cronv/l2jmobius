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
package org.l2jmobius.gameserver.mechanics.events.holders.actor.player;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.itemcontainer.ItemContainer;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;

/**
 * Handles the event triggered when a player transfers an {@code Item} to or from a clan warehouse.<br>
 * This class manages the logic for validating and processing these specific inventory movements.
 * @author UnAfraid
 */
public class OnPlayerClanWHItemTransfer implements IBaseEvent
{
	private final Player _player;
	private final Item _item;
	private final long _count;
	private final ItemContainer _container;
	
	/**
	 * Creates a new event for transferring items to a clan warehouse.<br>
	 * This constructor initializes the required data for the transfer process.
	 * @param player The {@code Player} who is performing the action.
	 * @param item The {@code Item} being moved.
	 * @param count The amount of the {@code Item} to be transferred.
	 * @param container The destination {@code ItemContainer}.
	 */
	public OnPlayerClanWHItemTransfer(Player player, Item item, long count, ItemContainer container)
	{
		_player = player;
		_item = item;
		_count = count;
		_container = container;
	}
	
	/**
	 * Retrieves the {@code Player} associated with this event.<br>
	 * This method returns the player who initiated the item transfer.
	 * @return the {@code Player} object.
	 */
	public Player getPlayer()
	{
		return _player;
	}
	
	/**
	 * Retrieves the {@code Item} associated with this event.<br>
	 * This method returns the specific item being transferred.
	 * @return The {@code Item} object.
	 */
	public Item getItem()
	{
		return _item;
	}
	
	/**
	 * Retrieves the number of items involved in this transfer.<br>
	 * This value represents the quantity of the {@code Item}.
	 * @return The total count of items.
	 */
	public long getCount()
	{
		return _count;
	}
	
	/**
	 * Retrieves the {@code ItemContainer} associated with this event.<br>
	 * This is the container where the item transfer occurs.
	 * @return the {@code ItemContainer} object.
	 */
	public ItemContainer getContainer()
	{
		return _container;
	}
	
	/**
	 * Returns the type of this event.<br>
	 * This identifies the specific action being performed.
	 * @return the {@code EventType} associated with this event.
	 */
	@Override
	public EventType getType()
	{
		return EventType.ON_PLAYER_CLAN_WH_ITEM_TRANSFER;
	}
}
