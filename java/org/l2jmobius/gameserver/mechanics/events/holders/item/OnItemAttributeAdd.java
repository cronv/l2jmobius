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
package org.l2jmobius.gameserver.mechanics.events.holders.item;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;

/**
 * This class represents an event triggered when an attribute is added to an {@code Item}.<br>
 * It holds the necessary data for processing item attribute modifications.
 * @author NightBR
 */
public class OnItemAttributeAdd implements IBaseEvent
{
	private final Player _player;
	private final Item _item;
	
	/**
	 * This constructor initializes a new {@code OnItemAttributeAdd} event.<br>
	 * It stores the {@code Player} who received the attribute.<br>
	 * It also stores the specific {@code Item} involved in the event.
	 * @param player The {@code Player} instance associated with this event.
	 * @param item The {@code Item} instance associated with this event.
	 */
	public OnItemAttributeAdd(Player player, Item item)
	{
		_player = player;
		_item = item;
	}
	
	/**
	 * Retrieves the {@code Player} associated with this event.
	 * @return the {@code Player} object.
	 */
	public Player getPlayer()
	{
		return _player;
	}
	
	/**
	 * Retrieves the {@code Item} associated with this event.<br>
	 * This method returns the specific item that triggered the attribute change.
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
		return EventType.ON_ITEM_ATTRIBUTE_ADD;
	}
}
