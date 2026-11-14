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
import org.l2jmobius.gameserver.mechanics.options.AugmentationInstance;

/**
 * This class represents an event triggered when a {@code Player} performs an augment action.<br>
 * It holds the necessary data for processing augmentation mechanics within the game engine.
 * @author UnAfraid
 */
public class OnPlayerAugment implements IBaseEvent
{
	private final Player _player;
	private final Item _item;
	private final AugmentationInstance _augmentation;
	private final boolean _isAugment; // true = is being augmented // false = augment is being removed
	
	/**
	 * Creates a new {@code OnPlayerAugment} event.<br>
	 * This event handles the addition or removal of an augmentation from an item.
	 * @param player The {@code Player} who performed the action.
	 * @param item The {@code Item} being modified.
	 * @param augment The {@code AugmentationInstance} representing the augmentation.
	 * @param isAugment Set to {@code true} if adding an augmentation, or {@code false} if removing one.
	 */
	public OnPlayerAugment(Player player, Item item, AugmentationInstance augment, boolean isAugment)
	{
		_player = player;
		_item = item;
		_augmentation = augment;
		_isAugment = isAugment;
	}
	
	/**
	 * Retrieves the {@code Player} associated with this event.<br>
	 * This method returns the player who triggered the augmentation action.
	 * @return the {@code Player} object.
	 */
	public Player getPlayer()
	{
		return _player;
	}
	
	/**
	 * Retrieves the {@code Item} associated with this event.<br>
	 * This method returns the specific item being modified.
	 * @return the {@code Item} object.
	 */
	public Item getItem()
	{
		return _item;
	}
	
	/**
	 * Retrieves the {@code AugmentationInstance} associated with this event.<br>
	 * This represents the specific augmentation being added or removed.
	 * @return the {@code AugmentationInstance} of the augmentation.
	 */
	public AugmentationInstance getAugmentation()
	{
		return _augmentation;
	}
	
	/**
	 * Checks if the event represents an augmentation being added.<br>
	 * Returns {@code true} if the player is being augmented.<br>
	 * Returns {@code false} if an augmentation is being removed.
	 * @return The status of the augmentation action.
	 */
	public boolean isAugment()
	{
		return _isAugment;
	}
	
	/**
	 * Retrieves the type of this event.<br>
	 * It returns a constant from the {@code EventType} enum.
	 * @return the {@code EventType} associated with this event.
	 */
	@Override
	public EventType getType()
	{
		return EventType.ON_PLAYER_AUGMENT;
	}
}
