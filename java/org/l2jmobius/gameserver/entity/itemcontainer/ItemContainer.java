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
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.entity.item.enums.ItemLocation;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.managers.ItemManager;
import org.l2jmobius.gameserver.mechanics.variables.ItemVariables;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import org.l2jmobius.gameserver.network.serverpackets.ShortcutInit;

/**
 * Represents a generic container that can hold {@code Item} instances.<br>
 * This abstract class provides the base functionality for inventories, bags, and other storage types.
 * @author Advi
 */
public abstract class ItemContainer
{
	protected static final Logger LOGGER = Logger.getLogger(ItemContainer.class.getName());
	
	protected final Set<Item> _items = ConcurrentHashMap.newKeySet(1);
	
	/**
	 * Creates a new instance of an {@code ItemContainer}.<br>
	 * This constructor is protected for internal use by subclasses.
	 */
	protected ItemContainer()
	{
	}
	
	protected abstract Creature getOwner();
	
	protected abstract ItemLocation getBaseLocation();
	
	/**
	 * Returns the name of the container type.<br>
	 * This method provides a constant string identifier for the {@code ItemContainer}.
	 * @return The name of the container as a {@code String}.
	 */
	public String getName()
	{
		return "ItemContainer";
	}
	
	/**
	 * Retrieves the unique identifier of the owner.<br>
	 * Returns {@code 0} if there is no owner.
	 * @return The {@code int} object ID of the owner.
	 */
	public int getOwnerId()
	{
		final Creature owner = getOwner();
		if (owner == null)
		{
			return 0;
		}
		
		return owner.getObjectId();
	}
	
	/**
	 * Returns the total number of items currently held in this container.<br>
	 * This method counts all entries within the {@code _items} set.
	 * @return The size of the item collection as an {@code int}.
	 */
	public int getSize()
	{
		return _items.size();
	}
	
	/**
	 * Retrieves all items currently stored in this container.<br>
	 * This method returns the internal collection of {@code Item} objects.
	 * @return a {@code Collection} of {@code Item} objects.
	 */
	public Collection<Item> getItems()
	{
		return _items;
	}
	
	/**
	 * Finds an {@code Item} within this container based on its unique ID.<br>
	 * It searches through all stored items and returns the first match found.<br>
	 * If no item with the specified ID exists, it returns {@code null}.
	 * @param itemId The unique identifier of the item to search for.
	 * @return The matching {@code Item} object or {@code null} if not found.
	 */
	public Item getItemByItemId(int itemId)
	{
		for (Item item : _items)
		{
			if (item.getId() == itemId)
			{
				return item;
			}
		}
		
		return null;
	}
	
	/**
	 * Retrieves all items that match a specific ID.<br>
	 * This method searches through the container's collection.<br>
	 * It returns a new {@code Collection} containing the matching {@code Item} objects.
	 * @param itemId The unique identifier of the item to search for.
	 * @return A {@code Collection} of items that match the provided {@code itemId}.
	 */
	public Collection<Item> getAllItemsByItemId(int itemId)
	{
		final Collection<Item> result = new ArrayDeque<>();
		for (Item item : _items)
		{
			if (itemId == item.getId())
			{
				result.add(item);
			}
		}
		
		return result;
	}
	
	/**
	 * Finds an {@code Item} within this container using its unique object ID.<br>
	 * It searches through all stored items and returns the first match found.<br>
	 * If no item matches the provided ID, it returns {@code null}.
	 * @param objectId The unique identifier of the item to find.
	 * @return The matching {@code Item} instance or {@code null} if not found.
	 */
	public Item getItemByObjectId(int objectId)
	{
		for (Item item : _items)
		{
			if (objectId == item.getObjectId())
			{
				return item;
			}
		}
		
		return null;
	}
	
	/**
	 * Calculates the total number of items in this container.<br>
	 * It filters by a specific {@code itemId} and {@code enchantLevel}.<br>
	 * This method includes all equipped items in the count.
	 * @param itemId The unique identifier for the item type.
	 * @param enchantLevel The level of enchantment to match.
	 * @return The total count of matching items as a {@code long}.
	 */
	public long getInventoryItemCount(int itemId, int enchantLevel)
	{
		return getInventoryItemCount(itemId, enchantLevel, true);
	}
	
	/**
	 * Calculates the total number of items in this container.<br>
	 * It filters by a specific ID and enchantment level.<br>
	 * You can choose whether to include currently equipped items.
	 * @param itemId The unique identifier for the item type.
	 * @param enchantLevel The required enchantment level. Use -1 to ignore this check.
	 * @param includeEquipped Set to {@code true} to count equipped items, or {@code false} to exclude them.
	 * @return The total count of matching items found in the container.
	 */
	public long getInventoryItemCount(int itemId, int enchantLevel, boolean includeEquipped)
	{
		long count = 0;
		for (Item item : _items)
		{
			if ((item.getId() == itemId) && ((item.getEnchantLevel() == enchantLevel) || (enchantLevel < 0)) && (includeEquipped || !item.isEquipped()))
			{
				if (item.isStackable())
				{
					return item.getCount();
				}
				
				count++;
			}
		}
		
		return count;
	}
	
	/**
	 * Checks if the container holds any item that allows self-resurrection.<br>
	 * It iterates through all items in the {@code _items} set.<br>
	 * Returns {@code true} if at least one valid item is found.
	 * @return {@code true} if a self-resurrection item exists, otherwise {@code false}.
	 */
	public boolean haveItemForSelfResurrection()
	{
		for (Item item : _items)
		{
			if (item.getTemplate().isAllowSelfResurrection())
			{
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Adds an {@code Item} to this container.<br>
	 * If the item is stackable and already exists, it updates the count of the existing item.<br>
	 * Otherwise, it sets the owner and location for a new item instance.<br>
	 * <p>
	 * @param process The type of operation being performed.
	 * @param item The {@code Item} to be added or merged.
	 * @param actor The {@code Player} performing the action.
	 * @param reference An optional object used for tracking the source of the change.
	 * @return The resulting {@code Item} after it has been added or updated.
	 */
	public Item addItem(ItemProcessType process, Item item, Player actor, Object reference)
	{
		Item newItem = item;
		final Item olditem = getItemByItemId(newItem.getId());
		
		// If stackable item is found in inventory just add to current quantity.
		if ((olditem != null) && olditem.isStackable())
		{
			final long count = newItem.getCount();
			olditem.changeCount(process, count, actor, reference);
			olditem.setLastChange(Item.MODIFIED);
			
			// And destroys the item
			ItemManager.destroyItem(process, newItem, actor, reference);
			newItem.updateDatabase();
			newItem = olditem;
		}
		else // If item hasn't be found in inventory, create new one.
		{
			newItem.setOwnerId(process, getOwnerId(), actor, reference);
			newItem.setItemLocation(getBaseLocation());
			newItem.setLastChange((Item.ADDED));
			
			// Add item in inventory.
			addItem(newItem);
		}
		
		refreshWeight();
		return newItem;
	}
	
	/**
	 * Adds an item to this container based on the provided ID and count.<br>
	 * If the item is stackable, it updates the existing quantity.<br>
	 * Otherwise, it creates a new item instance.
	 * @param process The type of process being performed.
	 * @param itemId The unique identifier for the item template.
	 * @param count The amount of the item to add.
	 * @param actor The player performing the action.
	 * @param reference An optional object used as a reference for the operation.
	 * @return The resulting {@code Item} instance, or {@code null} if the item is invalid.
	 */
	public Item addItem(ItemProcessType process, int itemId, long count, Player actor, Object reference)
	{
		Item item = getItemByItemId(itemId);
		
		// If stackable item is found in inventory just add to current quantity.
		if ((item != null) && item.isStackable())
		{
			item.changeCount(process, count, actor, reference);
			item.setLastChange(Item.MODIFIED);
		}
		else // If item hasn't be found in inventory, create new one.
		{
			final InventoryUpdate iu = new InventoryUpdate();
			for (int i = 0; i < count; i++)
			{
				final ItemTemplate template = ItemData.getInstance().getTemplate(itemId);
				if (template == null)
				{
					LOGGER.warning("Invalid ItemId (" + itemId + ") requested by " + (actor != null ? actor : process));
					return null;
				}
				
				final boolean templateStackable = template.isStackable();
				item = ItemManager.createItem(process, itemId, templateStackable ? count : 1, actor, reference);
				item.setOwnerId(getOwnerId());
				item.setItemLocation(getBaseLocation());
				item.setLastChange(Item.ADDED);
				
				// Add item in inventory.
				addItem(item);
				
				// Add additional items to InventoryUpdate.
				if ((count > 1) && (i < (count - 1)))
				{
					iu.addNewItem(item);
				}
				
				// If stackable, end loop as entire count is included in 1 instance of item.
				if (templateStackable || !GeneralConfig.MULTIPLE_ITEM_DROP)
				{
					break;
				}
			}
			
			// If additional items where created send InventoryUpdate.
			if ((count > 1) && (item != null) && !item.isStackable() && (item.getItemLocation() == ItemLocation.INVENTORY))
			{
				actor.sendInventoryUpdate(iu);
			}
		}
		
		refreshWeight();
		return item;
	}
	
	/**
	 * Transfers a specific quantity of an item from this container to another.<br>
	 * It handles both stackable and non-stackable items automatically.<br>
	 * The method updates the database and refreshes weights for both containers.
	 * @param process The type of process used for the transfer.
	 * @param objectId The unique identifier of the item to move.
	 * @param countValue The amount of the item to transfer.
	 * @param target The {@code ItemContainer} where the item will be moved.
	 * @param actor The {@code Player} performing the action.
	 * @param reference An optional object used for additional context.
	 * @return The resulting {@code Item} in the target container, or {@code null} if the transfer fails.
	 */
	public Item transferItem(ItemProcessType process, int objectId, long countValue, ItemContainer target, Player actor, Object reference)
	{
		if (target == null)
		{
			return null;
		}
		
		final Item sourceitem = getItemByObjectId(objectId);
		if (sourceitem == null)
		{
			return null;
		}
		
		final boolean sourceIsStackable = sourceitem.isStackable();
		Item targetitem = sourceIsStackable ? target.getItemByItemId(sourceitem.getId()) : null;
		synchronized (sourceitem)
		{
			// Check if this item still present in this container.
			if (getItemByObjectId(objectId) != sourceitem)
			{
				return null;
			}
			
			// Check if requested quantity is available.
			final long sourceCount = sourceitem.getCount();
			long count = countValue;
			if (count > sourceCount)
			{
				count = sourceCount;
			}
			
			// If possible, move entire item object.
			if ((sourceCount == count) && (targetitem == null) && !sourceIsStackable)
			{
				removeItem(sourceitem);
				target.addItem(process, sourceitem, actor, reference);
				targetitem = sourceitem;
			}
			else
			{
				if (sourceCount > count) // If possible, only update counts.
				{
					sourceitem.changeCount(process, -count, actor, reference);
				}
				else // Otherwise destroy old item.
				{
					removeItem(sourceitem);
					ItemManager.destroyItem(process, sourceitem, actor, reference);
				}
				
				if (targetitem != null) // If possible, only update counts.
				{
					targetitem.changeCount(process, count, actor, reference);
				}
				else // Otherwise add new item.
				{
					targetitem = target.addItem(process, sourceitem.getId(), count, actor, reference);
				}
			}
			
			// Updates database
			sourceitem.updateDatabase(true);
			if ((targetitem != sourceitem) && (targetitem != null))
			{
				targetitem.updateDatabase();
			}
			
			if ((actor != null) && sourceitem.isAugmented())
			{
				sourceitem.getAugmentation().removeBonus(actor);
			}
			
			refreshWeight();
			target.refreshWeight();
		}
		
		return targetitem;
	}
	
	/**
	 * Removes an item from this container and moves it to a new location.<br>
	 * If the count is equal to the total amount, the original item is removed.<br>
	 * If the count is less than the total, the remaining amount stays here and a new item is created.
	 * @param process The type of process being performed on the item.
	 * @param item The {@code Item} to be detached from this container.
	 * @param count The quantity of the item to remove.
	 * @param newLocation The {@code ItemLocation} where the removed items will be placed.
	 * @param actor The {@code Player} performing the action.
	 * @param reference An optional object used as a reference for the operation.
	 * @return The detached {@code Item} or {@code null} if the operation fails.
	 */
	public Item detachItem(ItemProcessType process, Item item, long count, ItemLocation newLocation, Player actor, Object reference)
	{
		if (item == null)
		{
			return null;
		}
		
		synchronized (item)
		{
			if (!_items.contains(item))
			{
				return null;
			}
			
			final long itemCount = item.getCount();
			if (count > itemCount)
			{
				return null;
			}
			
			if (count == itemCount)
			{
				removeItem(item);
				
				item.setItemLocation(newLocation);
				item.updateDatabase(true);
				refreshWeight();
				
				return item;
			}
			
			item.changeCount(process, -count, actor, reference);
			item.updateDatabase(true);
			
			final Item newItem = ItemManager.createItem(process, item.getId(), count, actor, reference);
			newItem.setOwnerId(getOwnerId());
			newItem.setItemLocation(newLocation);
			newItem.updateDatabase(true);
			refreshWeight();
			
			return newItem;
		}
	}
	
	/**
	 * Removes a specific quantity of an item from this container based on its unique object ID.<br>
	 * This method finds the {@code Item} by its {@code itemObjectId} and then performs the detachment.<br>
	 * It returns the resulting {@code Item} or {@code null} if the item is not found.
	 * @param process The type of process being performed on the item.
	 * @param itemObjectId The unique identifier of the item to detach.
	 * @param count The amount of the item to remove.
	 * @param newLocation The new {@code ItemLocation} for the detached items.
	 * @param actor The {@code Player} performing the action.
	 * @param reference An optional object used as a reference for the operation.
	 * @return The resulting {@code Item} after detachment, or {@code null} if not found.
	 */
	public Item detachItem(ItemProcessType process, int itemObjectId, long count, ItemLocation newLocation, Player actor, Object reference)
	{
		final Item item = getItemByObjectId(itemObjectId);
		if (item == null)
		{
			return null;
		}
		
		return detachItem(process, item, count, newLocation, actor, reference);
	}
	
	/**
	 * Removes an {@code Item} from the container.<br>
	 * This method handles the destruction logic based on the provided {@code ItemProcessType}.<br>
	 * It returns the resulting {@code Item} after the process is complete.
	 * @param process The type of process used to destroy the item.
	 * @param item The {@code Item} instance to be destroyed.
	 * @param actor The {@code Player} who initiated the destruction.
	 * @param reference An optional object used as a reference for the action.
	 * @return The resulting {@code Item} after destruction.
	 */
	public Item destroyItem(ItemProcessType process, Item item, Player actor, Object reference)
	{
		return destroyItem(process, item, item.getCount(), actor, reference);
	}
	
	/**
	 * Removes or reduces the quantity of an {@code Item} from the container.<br>
	 * If the count is greater than the requested amount, it only decreases the quantity.<br>
	 * If the count matches the requested amount, the item is destroyed.<br>
	 * Returns {@code null} if the operation fails or the item is insufficient.
	 * @param process The type of process being performed on the item.
	 * @param item The {@code Item} instance to be modified or removed.
	 * @param count The quantity to remove from the item.
	 * @param actor The {@code Player} performing the action.
	 * @param reference An optional object used for additional context during the process.
	 * @return The modified {@code Item} if successful, or {@code null} otherwise.
	 */
	public Item destroyItem(ItemProcessType process, Item item, long count, Player actor, Object reference)
	{
		synchronized (item)
		{
			// Adjust item quantity
			final long itemCount = item.getCount();
			if (itemCount > count)
			{
				item.changeCount(process, -count, actor, reference);
				item.setLastChange(Item.MODIFIED);
				refreshWeight();
			}
			else
			{
				if (itemCount < count)
				{
					return null;
				}
				
				final boolean removed = removeItem(item);
				if (!removed)
				{
					return null;
				}
				
				ItemManager.destroyItem(process, item, actor, reference);
				item.updateDatabase();
				refreshWeight();
				
				item.stopAllTasks();
			}
			
			// Tempfix for black shortcut icon when consumed item count reaches 0.
			if (item.getCount() < 1)
			{
				actor.sendPacket(new ShortcutInit(actor));
			}
		}
		
		return item;
	}
	
	/**
	 * Removes an item from the container based on its unique object ID.<br>
	 * This method first retrieves the {@code Item} and then calls the internal destruction logic.<br>
	 * It returns {@code null} if the specified {@code objectId} does not exist.
	 * @param process The type of process being performed to destroy the item.
	 * @param objectId The unique identifier of the item to remove.
	 * @param count The amount of the item to destroy.
	 * @param actor The {@code Player} performing the action.
	 * @param reference An optional object used as a context reference for the operation.
	 * @return The destroyed {@code Item} instance, or {@code null} if not found.
	 */
	public Item destroyItem(ItemProcessType process, int objectId, long count, Player actor, Object reference)
	{
		final Item item = getItemByObjectId(objectId);
		return item == null ? null : destroyItem(process, item, count, actor, reference);
	}
	
	/**
	 * Removes an item from the container based on its unique ID.<br>
	 * This method first finds the {@code Item} by its {@code itemId}.<br>
	 * If the item exists, it calls {@code destroyItem} to remove it.
	 * @param process The type of process used for destruction.
	 * @param itemId The unique ID of the item to be removed.
	 * @param count The amount of the item to destroy.
	 * @param actor The {@code Player} performing the action.
	 * @param reference An optional object used as a context reference.
	 * @return The destroyed {@code Item} object, or {@code null} if no item was found.
	 */
	public Item destroyItemByItemId(ItemProcessType process, int itemId, long count, Player actor, Object reference)
	{
		final Item item = getItemByItemId(itemId);
		return item == null ? null : destroyItem(process, item, count, actor, reference);
	}
	
	/**
	 * Removes all items from this container.<br>
	 * It iterates through every {@code Item} in the internal set.<br>
	 * Each item is destroyed using the provided process and references.
	 * @param process The type of process used to destroy the items.
	 * @param actor The {@code Player} who initiated the destruction.
	 * @param reference An object used as a reference for the action.
	 */
	public void destroyAllItems(ItemProcessType process, Player actor, Object reference)
	{
		for (Item item : _items)
		{
			destroyItem(process, item, actor, reference);
		}
	}
	
	/**
	 * Retrieves the total amount of {@code Adena} currency in this container.<br>
	 * It searches for an item with the ID matching {@code Inventory.ADENA_ID}.<br>
	 * If no such item is found, it returns {@code 0}.
	 * @return The count of {@code Adena} items as a {@code long}.
	 */
	public long getAdena()
	{
		for (Item item : _items)
		{
			if (item.getId() == Inventory.ADENA_ID)
			{
				return item.getCount();
			}
		}
		
		return 0;
	}
	
	/**
	 * Retrieves the total count of beauty tickets in this container.<br>
	 * It searches through all items for a specific ID.<br>
	 * Returns 0 if no beauty tickets are found.
	 * @return The number of beauty tickets held.
	 */
	public long getBeautyTickets()
	{
		for (Item item : _items)
		{
			if (item.getId() == Inventory.BEAUTY_TICKET_ID)
			{
				return item.getCount();
			}
		}
		
		return 0;
	}
	
	/**
	 * Adds a new {@code Item} to the container.<br>
	 * This method updates the internal collection of items.
	 * @param item The {@code Item} object to be added.
	 */
	protected void addItem(Item item)
	{
		_items.add(item);
	}
	
	/**
	 * Removes a specific {@code Item} from the container.<br>
	 * This method updates the internal collection of items.
	 * @param item The {@code Item} object to be removed.
	 * @return {@code true} if the item was successfully removed, {@code false} otherwise.
	 */
	protected boolean removeItem(Item item)
	{
		return _items.remove(item);
	}
	
	/**
	 * Updates the weight of the {@code ItemContainer}.<br>
	 * This method recalculates the total weight based on the current items.
	 */
	protected void refreshWeight()
	{
	}
	
	/**
	 * Removes all items from this container.<br>
	 * This method saves the current state of each item to the database before removal.<br>
	 * It also stops any active tasks and removes the objects from the {@code World}.<br>
	 * Finally, it clears the internal item collection.
	 */
	public void deleteMe()
	{
		if ((this instanceof PlayerInventory) || (this instanceof PlayerWarehouse) || (getOwner() != null))
		{
			for (Item item : _items)
			{
				item.updateDatabase(true);
				item.stopAllTasks();
				
				final ItemVariables vars = item.getScript(ItemVariables.class);
				if (vars != null)
				{
					vars.saveNow();
				}
			}
		}
		
		for (Item item : _items)
		{
			World.removeObject(item);
		}
		
		_items.clear();
	}
	
	/**
	 * Updates the database for all items in this container.<br>
	 * This method checks if the container has an owner before proceeding.<br>
	 * It iterates through every {@code Item} and calls its update method.
	 */
	public void updateDatabase()
	{
		if (getOwner() != null)
		{
			for (Item item : _items)
			{
				item.updateDatabase(true);
			}
		}
	}
	
	/**
	 * Restores items from the database to this container.<br>
	 * It fetches all items belonging to the owner at the base location.<br>
	 * Stackable items are merged into existing quantities.<br>
	 * Non-stackable items are added as new entries.
	 */
	public void restore()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT * FROM items WHERE owner_id=? AND (loc=?)"))
		{
			ps.setInt(1, getOwnerId());
			ps.setString(2, getBaseLocation().name());
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					final Item item = new Item(rs);
					World.addObject(item);
					
					final Player owner = getOwner() != null ? getOwner().asPlayer() : null;
					
					// If stackable item is found in inventory just add to current quantity.
					if (item.isStackable() && (getItemByItemId(item.getId()) != null))
					{
						addItem(ItemProcessType.RESTORE, item, owner, null);
					}
					else
					{
						addItem(item);
					}
				}
			}
			
			refreshWeight();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Could not restore container:", e);
		}
	}
	
	/**
	 * Checks if the provided capacity is valid.<br>
	 * This method currently always returns {@code true}.
	 * @param slots The number of slots to validate.
	 * @return {@code true} if the capacity is valid, otherwise {@code false}.
	 */
	public boolean validateCapacity(long slots)
	{
		return true;
	}
	
	/**
	 * Checks if the provided weight is valid.<br>
	 * This method currently always returns {@code true}.
	 * @param weight The weight value to validate.
	 * @return {@code true} if the weight is valid.
	 */
	public boolean validateWeight(long weight)
	{
		return true;
	}
	
	/**
	 * Checks if there is enough space to add an item based on its ID.<br>
	 * It determines the required capacity by checking if the item is stackable.<br>
	 * If it is stackable, it validates for a count of 1.<br>
	 * Otherwise, it validates for the full {@code count}.
	 * @param itemId The unique identifier of the item to check.
	 * @param count The amount of the item being added.
	 * @return {@code true} if there is enough capacity or the item template is missing, {@code false} otherwise.
	 */
	public boolean validateCapacityByItemId(int itemId, long count)
	{
		final ItemTemplate template = ItemData.getInstance().getTemplate(itemId);
		return (template == null) || (template.isStackable() ? validateCapacity(1) : validateCapacity(count));
	}
	
	/**
	 * Checks if the total weight of a specific item quantity is valid.<br>
	 * This method calculates weight based on the {@code ItemTemplate} data.<br>
	 * It returns {@code true} if the template does not exist or if the weight is within limits.
	 * @param itemId The unique identifier for the item type.
	 * @param count The number of items to calculate weight for.
	 * @return {@code true} if the weight is valid, otherwise {@code false}.
	 */
	public boolean validateWeightByItemId(int itemId, long count)
	{
		final ItemTemplate template = ItemData.getInstance().getTemplate(itemId);
		return (template == null) || validateWeight(template.getWeight() * count);
	}
}
