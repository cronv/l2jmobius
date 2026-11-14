/*
 * Copyright (c) 2013 L2jMobius
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.l2jmobius.gameserver.mechanics.events.holders.actor.player;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;

/**
 * This event is triggered when a {@code Player} removes an item from their equipment.<br>
 * It provides the necessary context for handling logic related to unequipping items.
 * @author Mobius
 */
public class OnPlayerItemUnequip implements IBaseEvent
{
	private final Player _player;
	private final Item _item;
	
	/**
	 * This event is triggered when a {@code Player} removes an {@code Item} from their equipment.<br>
	 * It stores the specific player and the item involved in the action.
	 * @param player The {@code Player} who unequipped the item.
	 * @param item The {@code Item} that was removed from the player's equipment.
	 */
	public OnPlayerItemUnequip(Player player, Item item)
	{
		_player = player;
		_item = item;
	}
	
	/**
	 * Retrieves the {@code Player} associated with this event.<br>
	 * This method returns the player who unequipped an item.
	 * @return the {@code Player} object.
	 */
	public Player getPlayer()
	{
		return _player;
	}
	
	/**
	 * Retrieves the {@code Item} that was unequipped.<br>
	 * This method returns the specific item instance associated with this event.
	 * @return the {@code Item} object.
	 */
	public Item getItem()
	{
		return _item;
	}
	
	/**
	 * Retrieves the type of this event.<br>
	 * This method returns a constant value from {@code EventType}.
	 * @return the {@code EventType} associated with this event.
	 */
	@Override
	public EventType getType()
	{
		return EventType.ON_PLAYER_ITEM_UNEQUIP;
	}
}
