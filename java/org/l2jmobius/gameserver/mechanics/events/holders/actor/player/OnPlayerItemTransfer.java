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
 * This class represents an event triggered when a {@code Player} transfers an {@code Item}.<br>
 * It holds the necessary data regarding the source and destination of the transferred item.
 * @author UnAfraid
 */
public class OnPlayerItemTransfer implements IBaseEvent
{
	private final Player _player;
	private final Item _item;
	private final ItemContainer _container;
	
	/**
	 * Creates a new {@code OnPlayerItemTransfer} event.<br>
	 * This constructor initializes the event with the required data.
	 * @param player The {@code Player} who is performing the action.
	 * @param item The {@code Item} being moved.
	 * @param container The {@code ItemContainer} where the item is going.
	 */
	public OnPlayerItemTransfer(Player player, Item item, ItemContainer container)
	{
		_player = player;
		_item = item;
		_container = container;
	}
	
	/**
	 * Retrieves the {@code Player} associated with this event.<br>
	 * This method returns the player who performed the item transfer.
	 * @return the {@code Player} object.
	 */
	public Player getPlayer()
	{
		return _player;
	}
	
	/**
	 * Retrieves the {@code Item} associated with this event.<br>
	 * This method returns the specific item that was transferred.
	 * @return The {@code Item} object.
	 */
	public Item getItem()
	{
		return _item;
	}
	
	/**
	 * Retrieves the {@code ItemContainer} associated with this event.<br>
	 * This is the container where the item was moved to or from.
	 * @return the {@code ItemContainer} object.
	 */
	public ItemContainer getContainer()
	{
		return _container;
	}
	
	/**
	 * Returns the type of this event.<br>
	 * This method identifies the specific {@code EventType} associated with the transfer.
	 * @return The {@code EventType} for player item transfers.
	 */
	@Override
	public EventType getType()
	{
		return EventType.ON_PLAYER_ITEM_TRANSFER;
	}
}
