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
 * This event is triggered when an item is added to a player's clan warehouse.<br>
 * It provides the necessary context for handling warehouse inventory updates.
 * @author UnAfraid
 */
public class OnPlayerClanWHItemAdd implements IBaseEvent
{
	private final Player _player;
	private final Item _item;
	private final ItemContainer _container;
	
	/**
	 * This constructor initializes a new {@code OnPlayerClanWHItemAdd} event.<br>
	 * It stores the player, the item added, and the target container.
	 * @param player The {@code Player} who triggered the action.
	 * @param item The {@code Item} that was added to the container.
	 * @param container The {@code ItemContainer} where the item was placed.
	 */
	public OnPlayerClanWHItemAdd(Player player, Item item, ItemContainer container)
	{
		_player = player;
		_item = item;
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
	 * This method returns the specific item that was added to the container.
	 * @return the {@code Item} object.
	 */
	public Item getItem()
	{
		return _item;
	}
	
	/**
	 * Retrieves the {@code ItemContainer} associated with this event.<br>
	 * This is the container where the item was added.
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
		return EventType.ON_PLAYER_CLAN_WH_ITEM_ADD;
	}
}
