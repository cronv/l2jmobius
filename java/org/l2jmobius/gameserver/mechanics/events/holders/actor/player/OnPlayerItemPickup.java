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
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;

/**
 * This class represents an event triggered when a {@code Player} picks up an {@code Item}.<br>
 * It holds the necessary data for handling item collection logic within the game engine.
 * @author UnAfraid
 */
public class OnPlayerItemPickup implements IBaseEvent
{
	private final Player _player;
	private final Item _item;
	
	/**
	 * This constructor initializes a new {@code OnPlayerItemPickup} event.<br>
	 * It stores the {@code Player} who picked up an item.<br>
	 * It also stores the specific {@code Item} that was collected.
	 * @param player The {@code Player} instance involved in the pickup.
	 * @param item The {@code Item} instance that was picked up.
	 */
	public OnPlayerItemPickup(Player player, Item item)
	{
		_player = player;
		_item = item;
	}
	
	/**
	 * Retrieves the {@code Player} associated with this event.<br>
	 * This method returns the player who picked up an item.
	 * @return the {@code Player} object.
	 */
	public Player getPlayer()
	{
		return _player;
	}
	
	/**
	 * Retrieves the {@code Item} that was picked up.<br>
	 * This method returns the specific item instance associated with this event.
	 * @return the {@code Item} object.
	 */
	public Item getItem()
	{
		return _item;
	}
	
	/**
	 * Retrieves the type of this event.<br>
	 * This method returns a constant from the {@code EventType} enum.
	 * @return The {@code EventType} associated with this event.
	 */
	@Override
	public EventType getType()
	{
		return EventType.ON_PLAYER_ITEM_PICKUP;
	}
}
