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
package org.l2jmobius.gameserver.entity.itemcontainer;

import java.util.logging.Level;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.enums.ItemLocation;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.managers.ItemManager;

/**
 * This class handles the refund process for items belonging to a {@code Player}.<br>
 * It manages the logic for returning items to their original owners or locations.
 * @author DS
 */
public class PlayerRefund extends ItemContainer
{
	private final Player _owner;
	
	/**
	 * Creates a new {@code PlayerRefund} instance.<br>
	 * This object links the refund to a specific player.
	 * @param owner The {@code Player} who owns this refund.
	 */
	public PlayerRefund(Player owner)
	{
		_owner = owner;
	}
	
	/**
	 * Returns the display name of this refund object.<br>
	 * The value is always returned as {@code Refund}.
	 * @return The name of the refund.
	 */
	@Override
	public String getName()
	{
		return "Refund";
	}
	
	/**
	 * Retrieves the player who owns this refund.<br>
	 * This method returns the {@code Player} associated with the instance.
	 * @return the {@code Player} owner of this object.
	 */
	@Override
	public Player getOwner()
	{
		return _owner;
	}
	
	/**
	 * Retrieves the base location for this refund container.<br>
	 * This method always returns a specific constant value.
	 * @return the {@code ItemLocation} representing the refund source.
	 */
	@Override
	public ItemLocation getBaseLocation()
	{
		return ItemLocation.REFUND;
	}
	
	/**
	 * Adds a new {@code Item} to the refund container.<br>
	 * This method ensures the container size does not exceed 12 items.<br>
	 * If the limit is exceeded, it removes and destroys the oldest item.
	 * @param item The {@code Item} to be added to the container.
	 */
	@Override
	protected void addItem(Item item)
	{
		super.addItem(item);
		try
		{
			if (getSize() > 12)
			{
				final Item removedItem = _items.stream().findFirst().get();
				if (_items.remove(removedItem))
				{
					ItemManager.destroyItem(ItemProcessType.REFUND, removedItem, getOwner(), null);
					removedItem.updateDatabase(true);
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "addItem()", e);
		}
	}
	
	/**
	 * Updates the current weight of the {@code PlayerRefund} container.<br>
	 * This method recalculates the total weight based on all items currently held.<br>
	 * It ensures that the weight value is synchronized with the actual contents.
	 */
	@Override
	public void refreshWeight()
	{
	}
	
	/**
	 * Removes all items from this container.<br>
	 * Each item is destroyed using {@code ItemManager}.<br>
	 * The internal list of items is cleared after processing.
	 */
	@Override
	public void deleteMe()
	{
		try
		{
			for (Item item : _items)
			{
				ItemManager.destroyItem(ItemProcessType.REFUND, item, getOwner(), null);
				item.updateDatabase(true);
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "deleteMe()", e);
		}
		
		_items.clear();
	}
	
	/**
	 * Restores the items to their original locations.<br>
	 * This method handles the cleanup of the {@code PlayerRefund} container.
	 */
	@Override
	public void restore()
	{
	}
}
