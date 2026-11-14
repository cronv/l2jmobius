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

import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;

/**
 * This class represents an event triggered when a {@code Player} drops an {@code Item}.<br>
 * It holds the necessary data for handling item drop mechanics within the game engine.
 * @author UnAfraid
 */
public class OnPlayerItemDrop implements IBaseEvent
{
	private final Player _player;
	private final Item _item;
	private final Location _loc;
	
	/**
	 * This constructor initializes a new {@code OnPlayerItemDrop} event.<br>
	 * It stores the player, the dropped item, and the location of the drop.
	 * @param player The {@code Player} who dropped the item.
	 * @param item The {@code Item} that was dropped.
	 * @param loc The {@code Location} where the item landed.
	 */
	public OnPlayerItemDrop(Player player, Item item, Location loc)
	{
		_player = player;
		_item = item;
		_loc = loc;
	}
	
	/**
	 * Retrieves the {@code Player} associated with this event.<br>
	 * This method returns the player who dropped an item.
	 * @return the {@code Player} object.
	 */
	public Player getPlayer()
	{
		return _player;
	}
	
	/**
	 * Retrieves the {@code Item} associated with this event.<br>
	 * This method returns the specific item that was dropped by the player.
	 * @return The {@code Item} instance.
	 */
	public Item getItem()
	{
		return _item;
	}
	
	/**
	 * Retrieves the location where the item was dropped.<br>
	 * This method returns the {@code Location} object stored in this event.
	 * @return the {@code Location} of the drop
	 */
	public Location getLocation()
	{
		return _loc;
	}
	
	/**
	 * Returns the type of this event.<br>
	 * This method identifies the specific action that triggered the event.
	 * @return the {@code EventType} associated with this event.
	 */
	@Override
	public EventType getType()
	{
		return EventType.ON_PLAYER_ITEM_DROP;
	}
}
