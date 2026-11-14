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
package org.l2jmobius.gameserver.entity.itemcontainer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.config.custom.TransmogConfig;
import org.l2jmobius.gameserver.data.xml.AgathionData;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.request.EnchantItemRequest;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.entity.item.Weapon;
import org.l2jmobius.gameserver.entity.item.enums.ItemLocation;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.holders.AgathionSkillHolder;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.item.type.EtcItemType;
import org.l2jmobius.gameserver.mechanics.events.EventDispatcher;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerItemAdd;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerItemDestroy;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerItemDrop;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerItemTransfer;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.SkillConditionScope;
import org.l2jmobius.gameserver.mechanics.variables.ItemVariables;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.enums.StatusUpdateType;
import org.l2jmobius.gameserver.network.holders.TradeItem;
import org.l2jmobius.gameserver.network.holders.TradeList;
import org.l2jmobius.gameserver.network.serverpackets.ExAdenaInvenCount;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import org.l2jmobius.gameserver.network.serverpackets.StatusUpdate;
import org.l2jmobius.gameserver.network.serverpackets.limitshop.ExBloodyCoinCount;

/**
 * Represents the inventory system specifically for a {@code Player} entity.<br>
 * It manages items, equipment, and various storage slots owned by a player.<br>
 * This class handles item movements, updates, and database persistence for player-owned goods.
 */
public class PlayerInventory extends Inventory
{
	private static final Logger LOGGER = Logger.getLogger(PlayerInventory.class.getName());
	
	private final Player _owner;
	private Item _adena;
	private Item _ancientAdena;
	private Item _beautyTickets;
	private Collection<Integer> _blockItems = null;
	private InventoryBlockType _blockMode = InventoryBlockType.NONE;
	private final AtomicInteger _questItemSize = new AtomicInteger();
	private final AtomicInteger _artifactItemSize = new AtomicInteger();
	
	/**
	 * Creates a new {@code PlayerInventory} for a specific player.<br>
	 * This constructor links the inventory to its owner.
	 * @param owner The {@code Player} who owns this inventory.
	 */
	public PlayerInventory(Player owner)
	{
		_owner = owner;
	}
	
	/**
	 * Retrieves the {@code Player} who owns this container.
	 * @return the {@code Player} owner of this container.
	 */
	@Override
	public Player getOwner()
	{
		return _owner;
	}
	
	/**
	 * Retrieves the default location for items in this container.<br>
	 * This method always returns {@code ItemLocation.INVENTORY}.
	 * @return the base {@code ItemLocation} of the container.
	 */
	@Override
	protected ItemLocation getBaseLocation()
	{
		return ItemLocation.INVENTORY;
	}
	
	/**
	 * Retrieves the location where equipment items are stored.<br>
	 * This method always returns the {@code PAPERDOLL} location.
	 * @return the {@code ItemLocation} for equipped items.
	 */
	@Override
	protected ItemLocation getEquipLocation()
	{
		return ItemLocation.PAPERDOLL;
	}
	
	/**
	 * Retrieves the {@code Item} instance for Adena.<br>
	 * This method returns the specific item object representing the player's currency.
	 * @return The {@code Item} instance of Adena.
	 */
	public Item getAdenaInstance()
	{
		return _adena;
	}
	
	/**
	 * Retrieves the total amount of {@code Adena} held in this container.<br>
	 * It returns {@code 0} if no {@code Adena} is present.
	 * @return The count of {@code Adena}.
	 */
	@Override
	public long getAdena()
	{
		return _adena != null ? _adena.getCount() : 0;
	}
	
	/**
	 * Retrieves the {@code Item} instance for Ancient Adena.<br>
	 * This method returns the specific item object associated with the player's ancient currency.
	 * @return The {@code Item} instance of Ancient Adena.
	 */
	public Item getAncientAdenaInstance()
	{
		return _ancientAdena;
	}
	
	/**
	 * Retrieves the total amount of Ancient Adena held by the player.<br>
	 * It returns {@code 0} if no Ancient Adena is present.
	 * @return The count of Ancient Adena as a {@code long}.
	 */
	public long getAncientAdena()
	{
		return (_ancientAdena != null) ? _ancientAdena.getCount() : 0;
	}
	
	/**
	 * Retrieves the {@code Item} instance for beauty tickets.<br>
	 * This method returns the specific ticket item held by the player.
	 * @return The {@code Item} instance of beauty tickets.
	 */
	public Item getBeautyTicketsInstance()
	{
		return _beautyTickets;
	}
	
	/**
	 * Retrieves the total count of beauty tickets owned by the player.<br>
	 * It returns {@code 0} if no beauty tickets are present.
	 * @return The number of beauty tickets as a {@code long}.
	 */
	@Override
	public long getBeautyTickets()
	{
		return _beautyTickets != null ? _beautyTickets.getCount() : 0;
	}
	
	/**
	 * Retrieves a collection of unique {@code Item} objects from the inventory.<br>
	 * This method filters out duplicate item IDs based on the provided criteria.<br>
	 * It allows you to toggle the inclusion of specific currency types and availability status.
	 * @param allowAdena Determines if items with the {@code ADENA_ID} should be included.
	 * @param allowAncientAdena Determines if items with the {@code ANCIENT_ADENA_ID} should be included.
	 * @param onlyAvailable If set to {@code true}, only items that are currently available will be returned.
	 * @return A {@code Collection} of unique {@code Item} objects.
	 */
	public Collection<Item> getUniqueItems(boolean allowAdena, boolean allowAncientAdena, boolean onlyAvailable)
	{
		final Collection<Item> result = new ArrayDeque<>();
		for (Item item : _items)
		{
			final int itemId = item.getId();
			if (!allowAdena && (itemId == ADENA_ID))
			{
				continue;
			}
			
			if (!allowAncientAdena && (itemId == ANCIENT_ADENA_ID))
			{
				continue;
			}
			
			boolean isDuplicate = false;
			for (Item addedItem : result)
			{
				if (addedItem.getId() == itemId)
				{
					isDuplicate = true;
					break;
				}
			}
			
			if (!isDuplicate && (!onlyAvailable || item.isAvailable(_owner, false, false)))
			{
				result.add(item);
			}
		}
		
		return result;
	}
	
	/**
	 * Retrieves all items that match a specific ID from the inventory.<br>
	 * It filters the results based on whether equipped items should be included.
	 * @param itemId The unique identifier of the item to search for.
	 * @param includeEquipped Set to {@code true} to include items currently worn by the player, or {@code false} to exclude them.
	 * @return A {@code Collection} of {@code Item} objects matching the criteria.
	 */
	public Collection<Item> getAllItemsByItemId(int itemId, boolean includeEquipped)
	{
		final Collection<Item> result = new ArrayDeque<>();
		for (Item item : _items)
		{
			if ((itemId == item.getId()) && (includeEquipped || !item.isEquipped()))
			{
				result.add(item);
			}
		}
		
		return result;
	}
	
	/**
	 * Retrieves all items that match a specific ID and enchantment level.<br>
	 * This method includes both equipped and unequipped items in the result.
	 * @param itemId The unique identifier for the item type.
	 * @param enchantment The required enchantment level of the item.
	 * @return A {@code Collection} of {@code Item} objects matching the criteria.
	 */
	public Collection<Item> getAllItemsByItemId(int itemId, int enchantment)
	{
		return getAllItemsByItemId(itemId, enchantment, true);
	}
	
	/**
	 * Retrieves all items that match the specified ID and enchantment level.<br>
	 * This method filters items based on their unique identifier and enchantment status.<br>
	 * It also allows filtering by whether the item is currently equipped or not.
	 * @param itemId The unique identifier of the item to search for.
	 * @param enchantment The specific enchantment level required for a match.
	 * @param includeEquipped A boolean flag where {@code true} includes equipped items and {@code false} excludes them.
	 * @return A {@code Collection} of {@code Item} objects that meet all criteria.
	 */
	public Collection<Item> getAllItemsByItemId(int itemId, int enchantment, boolean includeEquipped)
	{
		final Collection<Item> result = new ArrayDeque<>();
		for (Item item : _items)
		{
			if ((itemId == item.getId()) && (item.getEnchantLevel() == enchantment) && (includeEquipped || !item.isEquipped()))
			{
				result.add(item);
			}
		}
		
		return result;
	}
	
	/**
	 * Retrieves a list of items that can be traded or manipulated.<br>
	 * Filters the inventory based on specific availability rules.
	 * @param allowAdena Determines if {@code Item} instances of Adena are included.
	 * @param allowNonTradeable Determines if non-tradeable items are included.
	 * @param feightable Determines if only freightable items in the inventory should be returned.
	 * @return A {@code Collection} of {@code Item} objects matching the criteria.
	 */
	public Collection<Item> getAvailableItems(boolean allowAdena, boolean allowNonTradeable, boolean feightable)
	{
		final Collection<Item> result = new ArrayDeque<>();
		for (Item item : _items)
		{
			if (!item.isAvailable(_owner, allowAdena, allowNonTradeable) || !canManipulateWithItemId(item.getId()))
			{
				continue;
			}
			else if (feightable)
			{
				if ((item.getItemLocation() == ItemLocation.INVENTORY) && item.isFreightable())
				{
					result.add(item);
				}
				continue;
			}
			
			result.add(item);
		}
		
		return result;
	}
	
	/**
	 * Retrieves a list of items that can be traded based on the provided trade list.<br>
	 * This method filters the owner's items and adjusts them according to {@code TradeList} rules.
	 * @param tradeList The {@code TradeList} used to determine item availability and adjustments.
	 * @return A {@code Collection} of {@code TradeItem} objects that are eligible for trading.
	 */
	public Collection<TradeItem> getAvailableItems(TradeList tradeList)
	{
		final Collection<TradeItem> result = new ArrayDeque<>();
		for (Item item : _items)
		{
			if ((item != null) && item.isAvailable(_owner, false, false))
			{
				final TradeItem adjItem = tradeList.adjustAvailableItem(item);
				if (adjItem != null)
				{
					result.add(adjItem);
				}
			}
		}
		
		return result;
	}
	
	/**
	 * Updates the properties of a {@code TradeItem} based on other items in the inventory.<br>
	 * This method checks if any unequipped or non-equipable versions of the item exist.<br>
	 * If found, it synchronizes the object ID, enchantment level, and count with those items.<br>
	 * If all instances are equipped, the count is set to {@code 0}.
	 * @param item The {@code TradeItem} to be adjusted.
	 */
	public void adjustAvailableItem(TradeItem item)
	{
		final int tradeItemId = item.getItem().getId();
		boolean notAllEquipped = false;
		for (Item adjItem : getAllItemsByItemId(tradeItemId))
		{
			if (adjItem.isEquipable())
			{
				if (!adjItem.isEquipped())
				{
					notAllEquipped |= true;
				}
			}
			else
			{
				notAllEquipped |= true;
				break;
			}
		}
		
		if (notAllEquipped)
		{
			final Item adjItem = getItemByItemId(tradeItemId);
			item.setObjectId(adjItem.getObjectId());
			item.setEnchant(adjItem.getEnchantLevel());
			
			final long adjCount = adjItem.getCount();
			if (adjCount < item.getCount())
			{
				item.setCount(adjCount);
			}
			
			return;
		}
		
		item.setCount(0);
	}
	
	/**
	 * Adds a specific amount of Adena to the inventory.<br>
	 * This method ensures that only positive amounts are processed.
	 * @param process The type of item process being performed.
	 * @param count The amount of Adena to add.
	 * @param actor The player performing the action.
	 * @param reference An object used as a reference for the transaction.
	 */
	public void addAdena(ItemProcessType process, long count, Player actor, Object reference)
	{
		if (count > 0)
		{
			addItem(process, ADENA_ID, count, actor, reference);
		}
	}
	
	/**
	 * Adds a specific amount of beauty tickets to the player's inventory.<br>
	 * This method ensures that only positive amounts are processed.
	 * @param process The type of process used for adding the items.
	 * @param count The number of beauty tickets to add.
	 * @param actor The {@code Player} who is receiving the items.
	 * @param reference An object providing additional context for the transaction.
	 */
	public void addBeautyTickets(ItemProcessType process, long count, Player actor, Object reference)
	{
		if (count > 0)
		{
			addItem(process, BEAUTY_TICKET_ID, count, actor, reference);
		}
	}
	
	/**
	 * Reduces the amount of Adena for a specific player.<br>
	 * This method checks if the requested count is greater than {@code 0}.<br>
	 * It then attempts to destroy the specified amount of Adena items.
	 * @param process The type of item process being performed.
	 * @param count The amount of Adena to remove.
	 * @param actor The player performing the action.
	 * @param reference An object used as a reference for the operation.
	 * @return {@code true} if the items were successfully destroyed, {@code false} otherwise.
	 */
	public boolean reduceAdena(ItemProcessType process, long count, Player actor, Object reference)
	{
		if (count > 0)
		{
			return destroyItemByItemId(process, ADENA_ID, count, actor, reference) != null;
		}
		
		return false;
	}
	
	/**
	 * Reduces the number of beauty tickets in a player's inventory.<br>
	 * This method checks if the requested count is valid before removing items.<br>
	 * It returns {@code true} if the removal was successful and {@code false} otherwise.
	 * @param process The type of process used for the item operation.
	 * @param count The number of beauty tickets to remove.
	 * @param actor The player performing the action.
	 * @param reference An object providing context for the operation.
	 * @return {@code true} if the items were successfully removed, {@code false} otherwise.
	 */
	public boolean reduceBeautyTickets(ItemProcessType process, long count, Player actor, Object reference)
	{
		if (count > 0)
		{
			return destroyItemByItemId(process, BEAUTY_TICKET_ID, count, actor, reference) != null;
		}
		
		return false;
	}
	
	/**
	 * Adds a specific amount of Ancient Adena to the inventory.<br>
	 * This method ensures that only positive counts are processed.
	 * @param process The type of item process being performed.
	 * @param count The number of Ancient Adena to add.
	 * @param actor The player who is receiving the items.
	 * @param reference An object used as a reference for the transaction.
	 */
	public void addAncientAdena(ItemProcessType process, long count, Player actor, Object reference)
	{
		if (count > 0)
		{
			addItem(process, ANCIENT_ADENA_ID, count, actor, reference);
		}
	}
	
	/**
	 * Reduces the amount of Ancient Adena from a container.<br>
	 * This method checks if the requested count is valid and attempts to remove the items.
	 * @param process The type of item process being performed.
	 * @param count The number of Ancient Adena to remove.
	 * @param actor The player performing the action.
	 * @param reference An object used as a reference for the operation.
	 * @return {@code true} if the reduction was successful, {@code false} otherwise.
	 */
	public boolean reduceAncientAdena(ItemProcessType process, long count, Player actor, Object reference)
	{
		return (count > 0) && (destroyItemByItemId(process, ANCIENT_ADENA_ID, count, actor, reference) != null);
	}
	
	/**
	 * Adds an {@code Item} to the inventory.<br>
	 * This method updates special currency instances and sends update packets to the {@code Player}.<br>
	 * It also triggers relevant events if listeners are present.
	 * @param process The type of process used to add the item.
	 * @param item The {@code Item} instance to be added.
	 * @param actor The {@code Player} who is performing the action.
	 * @param reference An object providing context for the addition.
	 * @return The successfully added {@code Item}, or {@code null} if it failed.
	 */
	@Override
	public Item addItem(ItemProcessType process, Item item, Player actor, Object reference)
	{
		final Item addedItem = super.addItem(process, item, actor, reference);
		if (addedItem != null)
		{
			final int addedItemId = addedItem.getId();
			if ((addedItemId == ADENA_ID) && !addedItem.equals(_adena))
			{
				_adena = addedItem;
			}
			else if ((addedItemId == ANCIENT_ADENA_ID) && !addedItem.equals(_ancientAdena))
			{
				_ancientAdena = addedItem;
			}
			else if ((addedItemId == BEAUTY_TICKET_ID) && !addedItem.equals(_beautyTickets))
			{
				_beautyTickets = addedItem;
			}
			
			if (actor != null)
			{
				// Send inventory update packet.
				final InventoryUpdate playerIU = new InventoryUpdate();
				playerIU.addItem(addedItem);
				actor.sendInventoryUpdate(playerIU);
				
				// Notify to scripts
				final ItemTemplate addedTemplate = addedItem.getTemplate();
				if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_ITEM_ADD, actor, addedTemplate))
				{
					EventDispatcher.getInstance().notifyEventAsync(new OnPlayerItemAdd(actor, addedItem), actor, addedTemplate);
				}
			}
		}
		
		return addedItem;
	}
	
	/**
	 * Adds a new {@code Item} to the inventory.<br>
	 * This method handles the creation and placement of items based on the provided process type.
	 * @param process The {@code ItemProcessType} used for this addition.
	 * @param itemId The unique identifier of the item template.
	 * @param count The quantity of the item to add.
	 * @param actor The {@code Player} performing the action.
	 * @param reference An optional object used as a reference for the transaction.
	 * @return The newly created {@code Item} instance.
	 */
	@Override
	public Item addItem(ItemProcessType process, int itemId, long count, Player actor, Object reference)
	{
		return addItem(process, itemId, count, actor, reference, true);
	}
	
	/**
	 * Adds a new item to the inventory.<br>
	 * This method handles special currency updates and sends network packets if requested.<br>
	 * It also triggers relevant game events for the actor.
	 * @param process The type of process used to add the item.
	 * @param itemId The unique identifier of the item template.
	 * @param count The quantity of the item to add.
	 * @param actor The player who is receiving the item.
	 * @param reference An object used as a reference for the transaction.
	 * @param update Whether to send an inventory update packet to the actor.
	 * @return The {@code Item} instance that was added, or {@code null} if the addition failed.
	 */
	public Item addItem(ItemProcessType process, int itemId, long count, Player actor, Object reference, boolean update)
	{
		final Item item = super.addItem(process, itemId, count, actor, reference);
		if (item != null)
		{
			final int addedItemId = item.getId();
			if ((addedItemId == ADENA_ID) && !item.equals(_adena))
			{
				_adena = item;
			}
			else if ((addedItemId == ANCIENT_ADENA_ID) && !item.equals(_ancientAdena))
			{
				_ancientAdena = item;
			}
			else if ((addedItemId == BEAUTY_TICKET_ID) && !item.equals(_beautyTickets))
			{
				_beautyTickets = item;
			}
			
			if (actor != null)
			{
				// Send inventory update packet.
				if (update)
				{
					final InventoryUpdate playerIU = new InventoryUpdate();
					if (item.isStackable() && (item.getCount() > count))
					{
						playerIU.addModifiedItem(item);
					}
					else
					{
						playerIU.addNewItem(item);
					}
					
					actor.sendInventoryUpdate(playerIU);
					
					// Adena UI update.
					if (addedItemId == Inventory.ADENA_ID)
					{
						actor.sendPacket(new ExAdenaInvenCount(actor));
					}
					// Einhasad coin UI update.
					else if (addedItemId == Inventory.EINHASAD_COIN_ID)
					{
						actor.sendPacket(new ExBloodyCoinCount(actor));
					}
				}
				
				// Notify to scripts.
				final ItemTemplate itemTemplate = item.getTemplate();
				if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_ITEM_ADD, actor, itemTemplate))
				{
					EventDispatcher.getInstance().notifyEventAsync(new OnPlayerItemAdd(actor, item), actor, itemTemplate);
				}
			}
		}
		
		return item;
	}
	
	/**
	 * Transfers an item from this container to a target container.<br>
	 * This method handles the movement logic and updates local currency references.<br>
	 * It also triggers any relevant transfer events for scripts.
	 * @param process The type of process being performed on the item.
	 * @param objectId The unique identifier of the item to move.
	 * @param count The amount of the item to transfer.
	 * @param target The destination {@code ItemContainer} for the item.
	 * @param actor The {@code Player} performing the action.
	 * @param reference An optional object used as a reference for the transaction.
	 * @return The {@code Item} that was transferred.
	 */
	@Override
	public Item transferItem(ItemProcessType process, int objectId, long count, ItemContainer target, Player actor, Object reference)
	{
		final Item item = super.transferItem(process, objectId, count, target, actor, reference);
		
		final int ownerId = getOwnerId();
		if ((_adena != null) && ((_adena.getCount() <= 0) || (_adena.getOwnerId() != ownerId)))
		{
			_adena = null;
		}
		
		if ((_ancientAdena != null) && ((_ancientAdena.getCount() <= 0) || (_ancientAdena.getOwnerId() != ownerId)))
		{
			_ancientAdena = null;
		}
		
		// Notify to scripts
		final ItemTemplate itemTemplate = item.getTemplate();
		if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_ITEM_TRANSFER, itemTemplate))
		{
			EventDispatcher.getInstance().notifyEventAsync(new OnPlayerItemTransfer(actor, item, target), itemTemplate);
		}
		
		return item;
	}
	
	/**
	 * Removes an item from the current container and moves it to a new location.<br>
	 * This method handles the logic for detaching items based on specific process types.<br>
	 * It also sends an updated item list to the {@code Player} if the operation succeeds.
	 * @param process The type of process being performed on the item.
	 * @param item The {@code Item} instance to be detached.
	 * @param count The amount of the item to detach.
	 * @param newLocation The {@code ItemLocation} where the item will be moved.
	 * @param actor The {@code Player} performing the action.
	 * @param reference An optional object used as a reference for the operation.
	 * @return The detached {@code Item} instance, or {@code null} if the operation failed.
	 */
	@Override
	public Item detachItem(ItemProcessType process, Item item, long count, ItemLocation newLocation, Player actor, Object reference)
	{
		final Item detachedItem = super.detachItem(process, item, count, newLocation, actor, reference);
		if ((detachedItem != null) && (actor != null))
		{
			actor.sendItemList();
		}
		
		return detachedItem;
	}
	
	/**
	 * Removes an {@code Item} from the inventory based on a specific process.<br>
	 * This method handles the destruction logic for the entire stack of the item.
	 * @param process The type of process used to destroy the item.
	 * @param item The {@code Item} instance to be removed.
	 * @param actor The {@code Player} performing the action.
	 * @param reference An optional object used as a reference for the destruction.
	 * @return The destroyed {@code Item}.
	 */
	@Override
	public Item destroyItem(ItemProcessType process, Item item, Player actor, Object reference)
	{
		return destroyItem(process, item, item.getCount(), actor, reference);
	}
	
	/**
	 * Removes an item from the inventory based on the specified process.<br>
	 * This method handles currency cleanup and triggers destruction events.
	 * @param process The type of action being performed on the item.
	 * @param item The {@code Item} instance to be destroyed.
	 * @param count The amount of the item to remove.
	 * @param actor The {@code Player} performing the action.
	 * @param reference An optional object used as a reference for the destruction.
	 * @return The {@code Item} that was destroyed, or {@code null}.
	 */
	@Override
	public Item destroyItem(ItemProcessType process, Item item, long count, Player actor, Object reference)
	{
		final Item destroyedItem = super.destroyItem(process, item, count, actor, reference);
		
		if ((_adena != null) && (_adena.getCount() <= 0))
		{
			_adena = null;
		}
		
		if ((_ancientAdena != null) && (_ancientAdena.getCount() <= 0))
		{
			_ancientAdena = null;
		}
		
		if (destroyedItem != null)
		{
			// Adena UI update.
			final int destroyedItemId = destroyedItem.getId();
			if (destroyedItemId == Inventory.ADENA_ID)
			{
				actor.sendPacket(new ExAdenaInvenCount(actor));
			}
			// Einhasad coin UI update.
			else if (destroyedItemId == Inventory.EINHASAD_COIN_ID)
			{
				actor.sendPacket(new ExBloodyCoinCount(actor));
			}
			
			// Notify to scripts
			final ItemTemplate destroyedItemTemplate = destroyedItem.getTemplate();
			if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_ITEM_DESTROY, destroyedItemTemplate))
			{
				EventDispatcher.getInstance().notifyEventAsync(new OnPlayerItemDestroy(actor, destroyedItem), destroyedItemTemplate);
			}
		}
		
		return destroyedItem;
	}
	
	/**
	 * Removes an item from the container based on its unique object ID.<br>
	 * This method finds the specific {@code Item} and then calls the destruction logic.<br>
	 * It returns {@code null} if the item cannot be found.
	 * @param process The type of process used to destroy the item.
	 * @param objectId The unique identifier of the item to remove.
	 * @param count The amount of the item to destroy.
	 * @param actor The {@code Player} performing the action.
	 * @param reference An optional object used as a reference for the destruction.
	 * @return The destroyed {@code Item} instance, or {@code null} if not found.
	 */
	@Override
	public Item destroyItem(ItemProcessType process, int objectId, long count, Player actor, Object reference)
	{
		final Item item = getItemByObjectId(objectId);
		return item == null ? null : destroyItem(process, item, count, actor, reference);
	}
	
	/**
	 * Removes a specific quantity of an item from the inventory.<br>
	 * This method searches for non-equipped items matching the provided {@code itemId}.<br>
	 * It handles both stackable and non-stackable items based on the requested {@code count}.
	 * @param process The type of process used to destroy the item.
	 * @param itemId The unique identifier of the item to be removed.
	 * @param count The amount of the item to remove.
	 * @param actor The player performing the action.
	 * @param reference An optional object used as a reference for the destruction process.
	 * @return The {@code Item} that was destroyed, or {@code null} if no items were removed.
	 */
	@Override
	public Item destroyItemByItemId(ItemProcessType process, int itemId, long count, Player actor, Object reference)
	{
		return destroyItemByItemId(process, itemId, -1, count, actor, reference);
	}
	
	/**
	 * Removes a specific quantity of an item of a given enchant level from the inventory.<br>
	 * This method prefers items that are not equipped, matching the ones the caller counted.<br>
	 * It handles both stackable and non-stackable items based on the requested {@code count}.
	 * @param process The type of process used to destroy the item.
	 * @param itemId The unique identifier of the item to be removed.
	 * @param enchantLevel The enchant level the items have to carry, or {@code -1} to accept any.
	 * @param count The amount of the item to remove.
	 * @param actor The player performing the action.
	 * @param reference An optional object used as a reference for the destruction process.
	 * @return The {@code Item} that was destroyed, or {@code null} if no items were removed.
	 */
	public Item destroyItemByItemId(ItemProcessType process, int itemId, int enchantLevel, long count, Player actor, Object reference)
	{
		// Attempt to find non equipped items.
		Item destroyItem = null;
		final Collection<Item> items = enchantLevel < 0 ? getAllItemsByItemId(itemId) : getAllItemsByItemId(itemId, enchantLevel, true);
		for (Item item : items)
		{
			destroyItem = item;
			if (!destroyItem.isEquipped())
			{
				break;
			}
		}
		
		// No item found.
		if (destroyItem == null)
		{
			return null;
		}
		
		// Support destroying multiple non stackable items.
		if (!destroyItem.isStackable() && (count > 1))
		{
			if (getInventoryItemCount(itemId, enchantLevel, false) >= count)
			{
				final InventoryUpdate iu = new InventoryUpdate();
				long destroyed = 0;
				for (Item item : items)
				{
					if (!item.isEquipped() && (destroyItem(process, item, 1, actor, reference) != null))
					{
						iu.addRemovedItem(item);
						if (++destroyed == count)
						{
							_owner.sendInventoryUpdate(iu);
							refreshWeight();
							return item;
						}
					}
				}
			}
			else
			{
				return null;
			}
		}
		
		// Single item or stackable.
		return destroyItem(process, destroyItem, count, actor, reference);
	}
	
	/**
	 * Drops an item from the inventory into the world.<br>
	 * This method handles currency cleanup and triggers drop events.
	 * @param process The type of process used to drop the item.
	 * @param item The {@code Item} instance to be dropped.
	 * @param actor The {@code Player} performing the action.
	 * @param reference An optional object used as a reference for the drop.
	 * @return The dropped {@code Item} instance, or {@code null} if the drop failed.
	 */
	@Override
	public Item dropItem(ItemProcessType process, Item item, Player actor, Object reference)
	{
		final Item droppedItem = super.dropItem(process, item, actor, reference);
		
		if ((_adena != null) && ((_adena.getCount() <= 0) || (_adena.getOwnerId() != getOwnerId())))
		{
			_adena = null;
		}
		
		if ((_ancientAdena != null) && ((_ancientAdena.getCount() <= 0) || (_ancientAdena.getOwnerId() != getOwnerId())))
		{
			_ancientAdena = null;
		}
		
		// Notify to scripts
		if (droppedItem != null)
		{
			final ItemTemplate droppedTemplate = droppedItem.getTemplate();
			if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_ITEM_DROP, droppedTemplate))
			{
				EventDispatcher.getInstance().notifyEventAsync(new OnPlayerItemDrop(actor, droppedItem, droppedItem.getLocation()), droppedTemplate);
			}
		}
		
		return droppedItem;
	}
	
	/**
	 * Drops an item from the inventory into the world.<br>
	 * This method handles currency cleanup and triggers drop events.
	 * @param process The type of process used for dropping the item.
	 * @param objectId The unique identifier of the item template.
	 * @param count The amount of items to drop.
	 * @param actor The player performing the action.
	 * @param reference An optional object used as a reference for the drop.
	 * @return The {@code Item} that was dropped, or {@code null} if none were created.
	 */
	@Override
	public Item dropItem(ItemProcessType process, int objectId, long count, Player actor, Object reference)
	{
		final Item item = super.dropItem(process, objectId, count, actor, reference);
		
		final int ownerId = getOwnerId();
		if ((_adena != null) && ((_adena.getCount() <= 0) || (_adena.getOwnerId() != ownerId)))
		{
			_adena = null;
		}
		
		if ((_ancientAdena != null) && ((_ancientAdena.getCount() <= 0) || (_ancientAdena.getOwnerId() != ownerId)))
		{
			_ancientAdena = null;
		}
		
		// Notify to scripts
		if (item != null)
		{
			final ItemTemplate itemTemplate = item.getTemplate();
			if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_ITEM_DROP, itemTemplate))
			{
				EventDispatcher.getInstance().notifyEventAsync(new OnPlayerItemDrop(actor, item, item.getLocation()), itemTemplate);
			}
		}
		
		return item;
	}
	
	/**
	 * Adds a new {@code Item} to the container.<br>
	 * This method updates the quest item count if the item is a quest item.<br>
	 * It then calls the superclass implementation to store the item.
	 * @param item The {@code Item} instance to be added.
	 */
	@Override
	protected void addItem(Item item)
	{
		if (item.isQuestItem())
		{
			_questItemSize.incrementAndGet();
		}
		else if (item.isArtifactItem())
		{
			_artifactItemSize.incrementAndGet();
		}
		
		super.addItem(item);
	}
	
	/**
	 * Removes the specified {@code Item} from the inventory.<br>
	 * This method updates shortcut bars and clears active processing requests.<br>
	 * It also resets specific currency values if applicable.
	 * @param item The {@code Item} to be removed.
	 * @return {@code true} if the removal was successful, {@code false} otherwise.
	 */
	@Override
	protected boolean removeItem(Item item)
	{
		// Removes any reference to the item from Shortcut bar.
		final int itemObjectId = item.getObjectId();
		_owner.removeItemFromShortcut(itemObjectId);
		
		// Removes active Enchant Scroll.
		if (_owner.isProcessingItem(itemObjectId))
		{
			// Prevents inventory corruption during multi-enchant with insufficient scrolls.
			// Without this check, remaining scrolls would incorrectly display 0 quantity after enchantment.
			// The scroll and the support item are consumed while the request runs, so keep the request alive until the enchant ends.
			final EnchantItemRequest enchantItemRequest = _owner.getRequest(EnchantItemRequest.class);
			if ((enchantItemRequest == null) || !enchantItemRequest.isProcessing())
			{
				_owner.removeRequestsThatProcessesItem(itemObjectId);
			}
		}
		
		final int itemId = item.getId();
		switch (itemId)
		{
			case ADENA_ID:
			{
				_adena = null;
				break;
			}
			case ANCIENT_ADENA_ID:
			{
				_ancientAdena = null;
				break;
			}
			case BEAUTY_TICKET_ID:
			{
				_beautyTickets = null;
				break;
			}
		}
		
		if (item.isQuestItem())
		{
			_questItemSize.decrementAndGet();
		}
		else if (item.isArtifactItem())
		{
			_artifactItemSize.decrementAndGet();
		}
		
		return super.removeItem(item);
	}
	
	/**
	 * Retrieves the current size of the quest item container.<br>
	 * This value is stored in an {@code AtomicInteger}.
	 * @return The total number of items currently held in the quest container.
	 */
	public int getQuestSize()
	{
		return _questItemSize.get();
	}
	
	/**
	 * Retrieves the current size of the Artifact item container.<br>
	 * This value is stored in an {@code AtomicInteger}.
	 * @return The total number of Artifacts currently held in the inventory.
	 */
	public int getArtifactSize()
	{
		return _artifactItemSize.get();
	}
	
	/**
	 * Returns the total number of items counting towards the standard inventory capacity.<br>
	 * This excludes both quest items and Artifacts, which the client shows in their own dedicated tabs.
	 * @return The count of standard items in the container.
	 */
	public int getNonQuestSize()
	{
		return _items.size() - _questItemSize.get() - _artifactItemSize.get();
	}
	
	/**
	 * Updates the weight of all items in the inventory.<br>
	 * This method also refreshes the owner's overload status.<br>
	 * Finally, it sends a packet to update the current load.
	 */
	@Override
	public void refreshWeight()
	{
		super.refreshWeight();
		
		_owner.refreshOverloaded(true);
		
		final StatusUpdate su = new StatusUpdate(_owner);
		su.addUpdate(StatusUpdateType.CUR_LOAD, _owner.getCurrentLoad());
		_owner.sendPacket(su);
	}
	
	/**
	 * Restores the currency items for this inventory.<br>
	 * This method calls the parent {@code restore()} method.<br>
	 * It then initializes the {@code _adena}, {@code _ancientAdena}, and {@code _beautyTickets} fields.
	 */
	@Override
	public void restore()
	{
		super.restore();
		_adena = getItemByItemId(ADENA_ID);
		_ancientAdena = getItemByItemId(ANCIENT_ADENA_ID);
		_beautyTickets = getItemByItemId(BEAUTY_TICKET_ID);
	}
	
	/**
	 * Retrieves the visible equipment data for a specific player.<br>
	 * This method fetches items from the database located in the {@code PAPERDOLL} location.<br>
	 * It returns a 2D array representing the paperdoll slots and their visual properties.
	 * @param objectId The unique identifier of the player owner.
	 * @return A 2D integer array containing the slot data, display IDs, enchant levels, and visual IDs.
	 */
	public static int[][] restoreVisibleInventory(int objectId)
	{
		final int[][] paperdoll = new int[Inventory.PAPERDOLL_TOTALSLOTS][4];
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT object_id,item_id,loc_data,enchant_level FROM items WHERE owner_id=? AND loc='PAPERDOLL'"))
		{
			ps.setInt(1, objectId);
			try (ResultSet invdata = ps.executeQuery())
			{
				while (invdata.next())
				{
					final int slot = invdata.getInt("loc_data");
					final ItemVariables vars = new ItemVariables(invdata.getInt("object_id"));
					paperdoll[slot][0] = invdata.getInt("object_id");
					
					final int itemId = invdata.getInt("item_id");
					final ItemTemplate template = ItemData.getInstance().getTemplate(itemId);
					paperdoll[slot][1] = template == null ? itemId : template.getDisplayId();
					
					paperdoll[slot][2] = invdata.getInt("enchant_level");
					paperdoll[slot][3] = vars.getInt(ItemVariables.VISUAL_ID, TransmogConfig.ENABLE_TRANSMOG ? vars.getInt(ItemVariables.TRANSMOG_ID, 0) : 0);
					if (paperdoll[slot][3] > 0) // Fix for hair appearance conflicting with original model.
					{
						paperdoll[slot][1] = paperdoll[slot][3];
					}
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Could not restore inventory: " + e.getMessage(), e);
		}
		
		return paperdoll;
	}
	
	/**
	 * Checks if the player has enough space and weight capacity for a list of items.<br>
	 * It calculates required slots based on stackability and total weight.<br>
	 * If the check fails, it can send notification messages to the owner.
	 * @param itemList The list of {@code ItemTemplate} objects to validate.
	 * @param sendMessage Whether to send a general inventory full message if validation fails.
	 * @param sendSkillMessage Whether to send a specific skill unavailability message if weight is exceeded.
	 * @return {@code true} if the items fit in the inventory, {@code false} otherwise.
	 */
	public boolean checkInventorySlotsAndWeight(List<ItemTemplate> itemList, boolean sendMessage, boolean sendSkillMessage)
	{
		int lootWeight = 0;
		int requiredSlots = 0;
		if (itemList != null)
		{
			for (ItemTemplate item : itemList)
			{
				// If the item is not stackable or is stackable and not present in inventory, will need a slot.
				if (!item.isStackable() || (getInventoryItemCount(item.getId(), -1) <= 0))
				{
					requiredSlots++;
				}
				
				lootWeight += item.getWeight();
			}
		}
		
		final boolean inventoryStatusOK = validateCapacity(requiredSlots) && validateWeight(lootWeight);
		if (!inventoryStatusOK && sendMessage)
		{
			_owner.sendPacket(SystemMessageId.YOUR_INVENTORY_IS_FULL);
			if (sendSkillMessage)
			{
				_owner.sendPacket(SystemMessageId.WEIGHT_AND_VOLUME_LIMIT_HAVE_BEEN_EXCEEDED_THAT_SKILL_IS_CURRENTLY_UNAVAILABLE);
			}
		}
		
		return inventoryStatusOK;
	}
	
	/**
	 * Checks if there is enough space in the inventory for an {@code Item}.<br>
	 * It determines how many slots are required based on stackability.<br>
	 * This method ensures the container can hold the item before adding it.
	 * @param item The {@code Item} to be validated.
	 * @return {@code true} if there is enough capacity, {@code false} otherwise.
	 */
	public boolean validateCapacity(Item item)
	{
		int slots = 0;
		if (!item.isStackable() || ((getInventoryItemCount(item.getId(), -1) <= 0) && !item.getTemplate().hasExImmediateEffect()))
		{
			slots++;
		}
		
		return validateCapacity(slots, item.isQuestItem(), item.isArtifactItem());
	}
	
	/**
	 * Checks if there is enough space to add an item based on its unique ID.<br>
	 * This method determines the number of slots required for the item.<br>
	 * It then validates this against the current inventory capacity.
	 * @param itemId The unique identifier of the item to check.
	 * @return {@code true} if there is enough space, {@code false} otherwise.
	 */
	public boolean validateCapacityByItemId(int itemId)
	{
		int slots = 0;
		final Item invItem = getItemByItemId(itemId);
		if ((invItem == null) || !invItem.isStackable())
		{
			slots++;
		}
		
		final ItemTemplate template = ItemData.getInstance().getTemplate(itemId);
		return validateCapacity(slots, template.isQuestItem(), template.isArtifactItem());
	}
	
	/**
	 * Checks if the provided number of slots is valid.<br>
	 * This method ensures that the capacity does not exceed allowed limits.
	 * @param slots The number of slots to check.
	 * @return {@code true} if the capacity is valid, {@code false} otherwise.
	 */
	@Override
	public boolean validateCapacity(long slots)
	{
		return validateCapacity(slots, false, false);
	}
	
	/**
	 * Checks if there is enough space in the inventory to add items.<br>
	 * It validates capacity based on whether the item is a quest item or not.<br>
	 * The check respects the player's specific inventory limits.
	 * @param slots The number of slots required for the new items.
	 * @param questItem Whether the item being added is a quest item.
	 * @return {@code true} if there is enough space, {@code false} otherwise.
	 */
	public boolean validateCapacity(long slots, boolean questItem)
	{
		return validateCapacity(slots, questItem, false);
	}
	
	/**
	 * Checks if there is enough space in the inventory to add items.<br>
	 * It validates capacity based on whether the item is a quest item or an Artifact.<br>
	 * The check respects the player's specific inventory limits.
	 * @param slots The number of slots required for the new items.
	 * @param questItem Whether the item being added is a quest item.
	 * @param artifactItem Whether the item being added is an Artifact.
	 * @return {@code true} if there is enough space, {@code false} otherwise.
	 */
	public boolean validateCapacity(long slots, boolean questItem, boolean artifactItem)
	{
		if (artifactItem)
		{
			return (getArtifactSize() + slots) <= _owner.getArtifactInventoryLimit();
		}
		
		return ((slots == 0) && !PlayerConfig.AUTO_LOOT_SLOT_LIMIT) || questItem ? (getQuestSize() + slots) <= _owner.getQuestInventoryLimit() : (getNonQuestSize() + slots) <= _owner.getInventoryLimit();
	}
	
	/**
	 * Checks if adding a specific weight will exceed the player's maximum load.<br>
	 * This method ignores the weight limit for Game Masters in certain modes.
	 * @param weight The amount of weight to add.
	 * @return {@code true} if the new total weight is within limits, {@code false} otherwise.
	 */
	@Override
	public boolean validateWeight(long weight)
	{
		// Disable weight check for GMs.
		if (_owner.isGM() && _owner.getDietMode() && _owner.getAccessLevel().allowTransaction())
		{
			return true;
		}
		
		return ((_totalWeight + weight) <= _owner.getMaxLoad());
	}
	
	/**
	 * Sets the list of blocked items and the current block mode.<br>
	 * This method updates the internal state and notifies the owner.
	 * @param items The collection of item IDs to be blocked.
	 * @param mode The {@code InventoryBlockType} to apply for blocking.
	 */
	public void setInventoryBlock(Collection<Integer> items, InventoryBlockType mode)
	{
		_blockMode = mode;
		_blockItems = items;
		_owner.sendItemList();
	}
	
	/**
	 * Removes the current block mode from the inventory.<br>
	 * Sets {@code _blockMode} to {@code InventoryBlockType.NONE}.<br>
	 * Clears the {@code _blockItems} list.<br>
	 * Sends an updated item list to the owner.
	 */
	public void unblock()
	{
		_blockMode = InventoryBlockType.NONE;
		_blockItems = null;
		_owner.sendItemList();
	}
	
	/**
	 * Checks if the inventory currently has an active block applied.<br>
	 * It returns {@code true} if a block type is set and there are items to be blocked.<br>
	 * Otherwise, it returns {@code false}.
	 * @return {@code true} if the inventory is blocked, {@code false} otherwise.
	 */
	public boolean hasInventoryBlock()
	{
		return ((_blockMode != InventoryBlockType.NONE) && (_blockItems != null) && !_blockItems.isEmpty());
	}
	
	/**
	 * This method blocks all items in the inventory.<br>
	 * It sets the block type to {@code WHITELIST} for specific IDs.<br>
	 * Use this when you need to restrict item movement or visibility.
	 */
	public void blockAllItems()
	{
		setInventoryBlock(Arrays.asList(ADENA_ID), InventoryBlockType.WHITELIST);
	}
	
	/**
	 * Retrieves the current block mode of the inventory.<br>
	 * This value determines how items are restricted or displayed in the container.
	 * @return the current {@code InventoryBlockType}
	 */
	public InventoryBlockType getBlockMode()
	{
		return _blockMode;
	}
	
	/**
	 * Retrieves the collection of items that are currently blocked.
	 * @return a {@code Collection<Integer>} containing the IDs of blocked items.
	 */
	public Collection<Integer> getBlockItems()
	{
		return _blockItems;
	}
	
	/**
	 * Checks if an item can be manipulated based on the current block settings.<br>
	 * This method evaluates the {@code itemId} against a whitelist or blacklist.<br>
	 * It returns {@code true} if the action is allowed and {@code false} otherwise.
	 * @param itemId The unique identifier of the item to check.
	 * @return {@code true} if the item can be manipulated, {@code false} if it is blocked.
	 */
	public boolean canManipulateWithItemId(int itemId)
	{
		final Collection<Integer> blockedItems = _blockItems;
		if (blockedItems != null)
		{
			switch (_blockMode)
			{
				case NONE:
				{
					return true;
				}
				case WHITELIST:
				{
					for (int id : blockedItems)
					{
						if (id == itemId)
						{
							return true;
						}
					}
					
					return false;
				}
				case BLACKLIST:
				{
					for (int id : blockedItems)
					{
						if (id == itemId)
						{
							return false;
						}
					}
					
					return true;
				}
			}
		}
		
		return true;
	}
	
	/**
	 * Returns a string representation of the current object.<br>
	 * It displays the simple name of the class and the owner.
	 * @return A string containing the class name and the {@code _owner}.
	 */
	@Override
	public String toString()
	{
		return getClass().getSimpleName() + "[" + _owner + "]";
	}
	
	/**
	 * Iterates through all items in the container.<br>
	 * Applies skills and enchantment statistics to each {@code Item}.<br>
	 * Handles special abilities for equipped items.<br>
	 * Manages Agathion skills by removing old ones and adding new ones based on enchant levels.
	 */
	public void applyItemSkills()
	{
		for (Item item : _items)
		{
			item.giveSkillsToOwner();
			item.applyEnchantStats();
			if (item.isEquipped())
			{
				item.applySpecialAbilities();
				
				final int itemLocationSlot = item.getLocationSlot();
				if (((itemLocationSlot >= Inventory.PAPERDOLL_AGATHION1) && (itemLocationSlot <= Inventory.PAPERDOLL_AGATHION5)))
				{
					final AgathionSkillHolder agathionSkills = AgathionData.getInstance().getSkills(item.getId());
					if (agathionSkills != null)
					{
						final int itemEnchantLevel = item.getEnchantLevel();
						// Remove old skills.
						for (Skill skill : agathionSkills.getMainSkills(itemEnchantLevel))
						{
							_owner.removeSkill(skill, false, skill.isPassive());
						}
						
						for (Skill skill : agathionSkills.getSubSkills(itemEnchantLevel))
						{
							_owner.removeSkill(skill, false, skill.isPassive());
						}
						
						// Add new skills.
						if (itemLocationSlot == Inventory.PAPERDOLL_AGATHION1)
						{
							for (Skill skill : agathionSkills.getMainSkills(itemEnchantLevel))
							{
								if (skill.isPassive() && !skill.checkConditions(SkillConditionScope.PASSIVE, _owner, _owner))
								{
									continue;
								}
								
								_owner.addSkill(skill, false);
							}
						}
						
						for (Skill skill : agathionSkills.getSubSkills(itemEnchantLevel))
						{
							if (skill.isPassive() && !skill.checkConditions(SkillConditionScope.PASSIVE, _owner, _owner))
							{
								continue;
							}
							
							_owner.addSkill(skill, false);
						}
					}
				}
			}
		}
	}
	
	/**
	 * Reduces the count of a specific ammunition item.<br>
	 * This method checks if the active weapon matches the provided {@code EtcItemType}.<br>
	 * It verifies that the ammunition is not infinite before decreasing the quantity by 1.
	 * @param type The type of ammunition to reduce.
	 */
	@Override
	public void reduceAmmunitionCount(EtcItemType type)
	{
		if ((type != EtcItemType.ARROW) && (type != EtcItemType.BOLT))
		{
			LOGGER.log(Level.WARNING, type.toString(), " which is not ammo type.");
			return;
		}
		
		final Weapon weapon = _owner.getActiveWeaponItem();
		if (weapon == null)
		{
			return;
		}
		
		Item ammunition = null;
		switch (weapon.getItemType())
		{
			case BOW:
			{
				ammunition = findArrowForBow(weapon);
				break;
			}
			case CROSSBOW:
			case TWOHANDCROSSBOW:
			{
				ammunition = findBoltForCrossBow(weapon);
				break;
			}
			default:
			{
				return;
			}
		}
		
		if ((ammunition == null) || (ammunition.getItemType() != type))
		{
			return;
		}
		
		if (ammunition.getEtcItem().isInfinite())
		{
			return;
		}
		
		// Reduce item count.
		updateItemCountNoDbUpdate(ItemProcessType.NONE, ammunition, -1, _owner, null);
	}
	
	/**
	 * Updates the count of an {@code Item} without saving to the database.<br>
	 * This method handles item modification, destruction, and weight refreshing.<br>
	 * It sends an inventory update to the owner regardless of success.
	 * @param process The type of process used for the change.
	 * @param item The {@code Item} instance to modify.
	 * @param countDelta The amount to add or subtract from the current count.
	 * @param creator The {@code Player} who initiated the action.
	 * @param reference An optional object used as a reference for the change.
	 * @return {@code true} if the operation was successful, {@code false} otherwise.
	 */
	public boolean updateItemCountNoDbUpdate(ItemProcessType process, Item item, long countDelta, Player creator, Object reference)
	{
		final InventoryUpdate iu = new InventoryUpdate();
		final long left = item.getCount() + countDelta;
		try
		{
			if (left > 0)
			{
				synchronized (item)
				{
					if ((process != null) && (process != ItemProcessType.NONE))
					{
						item.changeCount(process, countDelta, creator, reference);
					}
					else
					{
						item.changeCount(ItemProcessType.NONE, -1, creator, reference);
					}
					
					item.setLastChange(Item.MODIFIED);
					refreshWeight();
					iu.addModifiedItem(item);
					return true;
				}
			}
			else if (left == 0)
			{
				iu.addRemovedItem(item);
				destroyItem(process, item, _owner, null);
				return true;
			}
			else
			{
				return false;
			}
		}
		finally
		{
			_owner.sendInventoryUpdate(iu);
		}
	}
	
	/**
	 * Updates the quantity of a specific {@code Item}.<br>
	 * This method handles the logic for changing item counts based on the provided process.<br>
	 * It ensures the database is updated if the resulting count is greater than {@code 0}.
	 * @param process The type of process used to update the item.
	 * @param item The {@code Item} instance to be modified.
	 * @param countDelta The amount to change by, which can be positive or negative.
	 * @param creator The {@code Player} who initiated the action.
	 * @param reference An optional object used as a reference for the transaction.
	 * @return {@code true} if the update was successful, otherwise {@code false}.
	 */
	public boolean updateItemCount(ItemProcessType process, Item item, long countDelta, Player creator, Object reference)
	{
		if (item != null)
		{
			try
			{
				return updateItemCountNoDbUpdate(process, item, countDelta, creator, reference);
			}
			finally
			{
				if (item.getCount() > 0)
				{
					item.updateDatabase();
				}
			}
		}
		
		return false;
	}
}
