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
package org.l2jmobius.gameserver.mechanics.events.holders.item;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;

/**
 * This class represents an event triggered when a player interacts with an item.<br>
 * It holds the necessary data for processing {@code Item} talk actions.
 * @author UnAfraid
 */
public class OnItemTalk implements IBaseEvent
{
	private final Item _item;
	private final Player _player;
	
	/**
	 * Creates a new {@code OnItemTalk} event.<br>
	 * This event is triggered when a {@code Player} interacts with an {@code Item}.<br>
	 * It stores both the item and the player involved in the interaction.
	 * @param item The {@code Item} that was interacted with.
	 * @param player The {@code Player} who initiated the interaction.
	 */
	public OnItemTalk(Item item, Player player)
	{
		_item = item;
		_player = player;
	}
	
	/**
	 * Retrieves the {@code Item} associated with this event.<br>
	 * This method returns the specific item that triggered the talk action.
	 * @return The {@code Item} object.
	 */
	public Item getItem()
	{
		return _item;
	}
	
	/**
	 * Retrieves the {@code Player} associated with this event.<br>
	 * This method returns the player who interacted with the item.
	 * @return the {@code Player} object.
	 */
	public Player getPlayer()
	{
		return _player;
	}
	
	/**
	 * Returns the type of this event.<br>
	 * This method identifies the specific {@code EventType} associated with the item talk action.
	 * @return The {@code EventType} for this event.
	 */
	@Override
	public EventType getType()
	{
		return EventType.ON_ITEM_TALK;
	}
}
