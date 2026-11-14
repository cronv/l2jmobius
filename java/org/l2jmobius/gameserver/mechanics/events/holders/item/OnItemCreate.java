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

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;

/**
 * This class represents an event triggered when a new {@code Item} instance is created.<br>
 * It holds the necessary data for listeners to react to item creation in the game world.
 * @author UnAfraid
 */
public class OnItemCreate implements IBaseEvent
{
	private final Item _item;
	private final Creature _creature;
	private final Object _reference;
	
	/**
	 * This constructor initializes a new {@code OnItemCreate} event.<br>
	 * It sets the item, the creature who created it, and an optional reference.
	 * @param item The {@code Item} instance that was created.
	 * @param actor The {@code Creature} that performed the action.
	 * @param reference An optional {@code Object} used for additional context.
	 */
	public OnItemCreate(Item item, Creature actor, Object reference)
	{
		_item = item;
		_creature = actor;
		_reference = reference;
	}
	
	/**
	 * Retrieves the {@code Item} associated with this event.<br>
	 * This method returns the specific item instance that was created.
	 * @return The {@code Item} object.
	 */
	public Item getItem()
	{
		return _item;
	}
	
	/**
	 * Retrieves the {@code Creature} associated with this event.<br>
	 * This is the character that triggered the action.
	 * @return the {@code Creature} instance.
	 */
	public Creature getActiveChar()
	{
		return _creature;
	}
	
	/**
	 * Retrieves the reference object associated with this event.<br>
	 * This is used to identify the specific context of the item creation.
	 * @return The {@code Object} reference.
	 */
	public Object getReference()
	{
		return _reference;
	}
	
	/**
	 * Returns the type of this event.<br>
	 * This method identifies the specific {@code EventType} associated with this instance.
	 * @return The {@code EventType} for this event.
	 */
	@Override
	public EventType getType()
	{
		return EventType.ON_ITEM_CREATE;
	}
}
