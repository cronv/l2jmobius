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
 * This event is triggered when an item is destroyed within a player's clan warehouse.<br>
 * It allows for custom logic to be executed during the destruction of such items.
 * @author UnAfraid
 */
public class OnPlayerClanWHItemDestroy implements IBaseEvent
{
	private final Player _player;
	private final Item _item;
	private final long _count;
	private final ItemContainer _container;
	
	/**
	 * Creates a new event for when an item is destroyed in a clan warehouse.<br>
	 * This constructor initializes the required data for the destruction event.
	 * @param player The {@code Player} who triggered the action.
	 * @param item The {@code Item} that was destroyed.
	 * @param count The amount of items destroyed.
	 * @param container The {@code ItemContainer} where the item was located.
	 */
	public OnPlayerClanWHItemDestroy(Player player, Item item, long count, ItemContainer container)
	{
		_player = player;
		_item = item;
		_count = count;
		_container = container;
	}
	
	/**
	 * Retrieves the {@code Player} associated with this event.<br>
	 * This method returns the player who triggered the action.
	 * @return the {@code Player} object.
	 */
	public Player getPlayer()
	{
		return _player;
	}
	
	/**
	 * Retrieves the {@code Item} associated with this event.<br>
	 * This method returns the specific item that was destroyed.
	 * @return The {@code Item} object.
	 */
	public Item getItem()
	{
		return _item;
	}
	
	/**
	 * Returns the number of items destroyed.<br>
	 * This value corresponds to the {@code count} field.
	 * @return The total count of the destroyed items.
	 */
	public long getCount()
	{
		return _count;
	}
	
	/**
	 * Retrieves the {@code ItemContainer} associated with this event.<br>
	 * This is the container where the item was destroyed.
	 * @return the {@code ItemContainer} instance.
	 */
	public ItemContainer getContainer()
	{
		return _container;
	}
	
	/**
	 * Retrieves the type of this event.<br>
	 * This method returns a constant from the {@code EventType} enum.
	 * @return the {@code EventType} associated with this event.
	 */
	@Override
	public EventType getType()
	{
		return EventType.ON_PLAYER_CLAN_WH_ITEM_DESTROY;
	}
}
