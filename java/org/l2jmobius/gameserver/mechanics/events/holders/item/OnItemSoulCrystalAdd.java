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
import org.l2jmobius.gameserver.mechanics.ensoul.EnsoulStone;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;

/**
 * This class handles the event triggered when a {@code SoulCrystal} is added to an item.<br>
 * It allows for custom logic to be executed during the soul crystal addition process.
 * @author NightBR
 */
public class OnItemSoulCrystalAdd implements IBaseEvent
{
	private final Player _player;
	private final Item _item;
	private final EnsoulStone _stone;
	
	/**
	 * Creates a new event for adding a soul crystal to an item.<br>
	 * This method initializes the required data for the {@code OnItemSoulCrystalAdd} event.
	 * @param player The {@code Player} who triggered the action.
	 * @param item The {@code Item} being modified.
	 * @param stone The {@code EnsoulStone} used in the process.
	 */
	public OnItemSoulCrystalAdd(Player player, Item item, EnsoulStone stone)
	{
		_player = player;
		_item = item;
		_stone = stone;
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
	 * Retrieves the {@code Item} associated with this event.
	 * @return the {@code Item} object.
	 */
	public Item getItem()
	{
		return _item;
	}
	
	/**
	 * Retrieves the {@code EnsoulStone} associated with this event.
	 * @return the {@code EnsoulStone} object.
	 */
	public EnsoulStone getEnsoulStone()
	{
		return _stone;
	}
	
	/**
	 * Retrieves the type of this event.<br>
	 * This method returns a constant from {@code EventType}.
	 * @return the {@code EventType} associated with this event.
	 */
	@Override
	public EventType getType()
	{
		return EventType.ON_ITEM_SOUL_CRYSTAL_ADD;
	}
}
