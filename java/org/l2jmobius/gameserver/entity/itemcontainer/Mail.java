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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Level;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.enums.ItemLocation;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.instance.Item;

/**
 * Represents a player's mail system for receiving and managing items.<br>
 * This class handles the storage and retrieval of messages sent between players.<br>
 * It extends {@code ItemContainer} to allow for item management within the mailbox.
 * @author DS
 */
public class Mail extends ItemContainer
{
	private final int _ownerId;
	private int _messageId;
	
	/**
	 * Creates a new {@code Mail} instance.<br>
	 * This constructor initializes the mail with an owner and a specific message.
	 * @param objectId The unique identifier for the owner of the mail.
	 * @param messageId The unique identifier for the message content.
	 */
	public Mail(int objectId, int messageId)
	{
		_ownerId = objectId;
		_messageId = messageId;
	}
	
	/**
	 * Retrieves the display name of the {@code Mail} object.<br>
	 * This method returns a constant string value.
	 * @return The name of the mail item as a {@code String}.
	 */
	@Override
	public String getName()
	{
		return "Mail";
	}
	
	/**
	 * Retrieves the {@code Player} who owns this mail.<br>
	 * This method returns {@code null} if no owner is assigned.
	 * @return the {@code Player} owning the mail, or {@code null}.
	 */
	@Override
	public Player getOwner()
	{
		return null;
	}
	
	/**
	 * Retrieves the default location for this mail item.<br>
	 * This method returns a constant value from {@code ItemLocation}.
	 * @return the {@code ItemLocation} representing the mail system.
	 */
	@Override
	public ItemLocation getBaseLocation()
	{
		return ItemLocation.MAIL;
	}
	
	/**
	 * Retrieves the unique identifier for the message.<br>
	 * This ID is used to identify specific mail content.
	 * @return The {@code int} value of the message ID.
	 */
	public int getMessageId()
	{
		return _messageId;
	}
	
	/**
	 * Updates the unique identifier for the mail message.<br>
	 * This method also updates the location of all items inside the mail.<br>
	 * It saves the changes to the database automatically.
	 * @param messageId The new {@code int} ID to assign to this message.
	 */
	public void setNewMessageId(int messageId)
	{
		_messageId = messageId;
		for (Item item : _items)
		{
			item.setItemLocation(getBaseLocation(), messageId);
		}
		
		updateDatabase();
	}
	
	/**
	 * Moves all items from this mail container to a warehouse.<br>
	 * If the provided {@code ItemContainer} is {@code null}, items are set to the default warehouse location.<br>
	 * Otherwise, each item is transferred to the specified container.
	 * @param wh The destination {@code ItemContainer}.
	 */
	public void returnToWh(ItemContainer wh)
	{
		for (Item item : _items)
		{
			if (wh == null)
			{
				item.setItemLocation(ItemLocation.WAREHOUSE);
			}
			else
			{
				transferItem(ItemProcessType.TRANSFER, item.getObjectId(), item.getCount(), wh, null, null);
			}
		}
	}
	
	/**
	 * Adds a new {@code Item} to this mail container.<br>
	 * It sets the item location based on the current message ID.<br>
	 * The database is updated for the added item.
	 * @param item The {@code Item} to be added to the container.
	 */
	@Override
	protected void addItem(Item item)
	{
		super.addItem(item);
		item.setItemLocation(getBaseLocation(), _messageId);
		item.updateDatabase(true);
	}
	
	/*
	 * Allow saving of the items without owner
	 */
	/**
	 * Updates all items inside this mail container in the database.<br>
	 * It iterates through every {@code Item} in the internal list.<br>
	 * Each item is updated with a {@code true} flag.
	 */
	@Override
	public void updateDatabase()
	{
		for (Item item : _items)
		{
			item.updateDatabase(true);
		}
	}
	
	/**
	 * Restores items from the database to this mail container.<br>
	 * It searches for items belonging to the owner at the base location.<br>
	 * Stackable items are merged into existing stacks if they already exist.
	 */
	@Override
	public void restore()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT * FROM items WHERE owner_id=? AND loc=? AND loc_data=?"))
		{
			statement.setInt(1, _ownerId);
			statement.setString(2, getBaseLocation().name());
			statement.setInt(3, _messageId);
			try (ResultSet inv = statement.executeQuery())
			{
				while (inv.next())
				{
					final Item item = new Item(inv);
					World.addObject(item);
					
					// If stackable item is found just add to current quantity.
					if (item.isStackable() && (getItemByItemId(item.getId()) != null))
					{
						addItem(ItemProcessType.RESTORE, item, null, null);
					}
					else
					{
						addItem(item);
					}
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "could not restore container:", e);
		}
	}
	
	/**
	 * Removes all items from this {@code Mail} container.<br>
	 * It updates the database for each item and removes them from the world.<br>
	 * The internal list of items is cleared after processing.
	 */
	@Override
	public void deleteMe()
	{
		for (Item item : _items)
		{
			item.updateDatabase(true);
			item.stopAllTasks();
			World.removeObject(item);
		}
		
		_items.clear();
	}
	
	/**
	 * Retrieves the unique identifier of the owner.<br>
	 * This ID belongs to the {@code Player} who owns this mail.
	 * @return The {@code int} value representing the owner's ID.
	 */
	@Override
	public int getOwnerId()
	{
		return _ownerId;
	}
}
