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
package org.l2jmobius.gameserver.network.holders;

import static org.l2jmobius.gameserver.entity.itemcontainer.Inventory.ADENA_ID;
import static org.l2jmobius.gameserver.entity.itemcontainer.Inventory.MAX_ADENA;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.PrivateStoreType;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.itemcontainer.PlayerInventory;
import org.l2jmobius.gameserver.managers.PrivateStoreHistoryManager;
import org.l2jmobius.gameserver.managers.PunishmentManager;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExPrivateStoreBuyingResult;
import org.l2jmobius.gameserver.network.serverpackets.ExPrivateStoreSellingResult;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * Manages the list of items displayed during a trade between players.<br>
 * It handles the synchronization and validation of items being exchanged.<br>
 * This class ensures that both parties agree on the contents of the {@code TradeList}.
 * @author Advi, Mobius
 */
public class TradeList
{
	private static final Logger LOGGER = Logger.getLogger(TradeList.class.getName());
	
	private final Player _owner;
	private Player _partner;
	private final Set<TradeItem> _items = ConcurrentHashMap.newKeySet();
	private String _title;
	private boolean _packaged;
	
	private boolean _confirmed = false;
	private boolean _locked = false;
	
	/**
	 * Creates a new {@code TradeList} for a specific player.<br>
	 * This initializes the trade session with an owner.
	 * @param owner The {@code Player} who starts the trade.
	 */
	public TradeList(Player owner)
	{
		_owner = owner;
	}
	
	/**
	 * Retrieves the player who owns this trade list.
	 * @return the {@code Player} who created or owns the trade.
	 */
	public Player getOwner()
	{
		return _owner;
	}
	
	/**
	 * Sets the trading partner for this {@code TradeList}.<br>
	 * This method assigns a {@code Player} to the partner field.
	 * @param partner The {@code Player} who will be participating in the trade.
	 */
	public void setPartner(Player partner)
	{
		_partner = partner;
	}
	
	/**
	 * Retrieves the player who is currently trading with the owner.<br>
	 * Returns {@code null} if no partner is assigned.
	 * @return The {@code Player} object representing the trade partner.
	 */
	public Player getPartner()
	{
		return _partner;
	}
	
	/**
	 * Sets the display name for this trade list.<br>
	 * The provided {@code String} will be stored as the new title.
	 * @param title The new title to assign to the trade list.
	 */
	public void setTitle(String title)
	{
		_title = title;
	}
	
	/**
	 * Retrieves the title of the trade list.
	 * @return The {@code String} title of the trade.
	 */
	public String getTitle()
	{
		return _title;
	}
	
	/**
	 * Checks if the trade list is currently locked.<br>
	 * Returns {@code true} if it is locked, and {@code false} otherwise.
	 * @return The lock status of this trade list.
	 */
	public boolean isLocked()
	{
		return _locked;
	}
	
	/**
	 * Checks if the trade has been confirmed.
	 * @return {@code true} if the trade is confirmed, {@code false} otherwise.
	 */
	public boolean isConfirmed()
	{
		return _confirmed;
	}
	
	/**
	 * Checks if the trade list is currently packaged.
	 * @return {@code true} if the trade is packaged, {@code false} otherwise.
	 */
	public boolean isPackaged()
	{
		return _packaged;
	}
	
	/**
	 * Sets whether the trade list is currently packaged.<br>
	 * This updates the {@code _packaged} field with the provided value.
	 * @param value The new packaging status to set.
	 */
	public void setPackaged(boolean value)
	{
		_packaged = value;
	}
	
	/**
	 * Retrieves all items currently in the trade list.
	 * @return a {@code Collection} of {@code TradeItem} objects.
	 */
	public Collection<TradeItem> getItems()
	{
		return _items;
	}
	
	/**
	 * Retrieves a list of items available for trade.<br>
	 * This method updates the provided {@code PlayerInventory} with the current items.
	 * @param inventory The {@code PlayerInventory} to update.
	 * @return A {@code Collection} of {@code TradeItem} objects.
	 */
	public Collection<TradeItem> getAvailableItems(PlayerInventory inventory)
	{
		final List<TradeItem> list = new ArrayList<>();
		for (TradeItem item : _items)
		{
			item = new TradeItem(item, item.getCount(), item.getPrice());
			inventory.adjustAvailableItem(item);
			list.add(item);
		}
		
		return list;
	}
	
	/**
	 * Returns the total number of items in this trade list.<br>
	 * This count includes all items currently stored in the {@code _items} set.
	 * @return The size of the item collection as an {@code int}.
	 */
	public int getItemCount()
	{
		return _items.size();
	}
	
	/**
	 * Updates the count of a stackable item in the trade list.<br>
	 * If the item is already present, it subtracts the existing amount from the new count.<br>
	 * It returns a new {@code TradeItem} or {@code null} if the resulting count is zero or less.
	 * @param item The {@code Item} to be adjusted in the trade list.
	 * @return A new {@code TradeItem} with the adjusted count, or {@code null}.
	 */
	public TradeItem adjustAvailableItem(Item item)
	{
		if (item.isStackable())
		{
			for (TradeItem exclItem : _items)
			{
				if (exclItem.getItem().getId() == item.getId())
				{
					return item.getCount() <= exclItem.getCount() ? null : new TradeItem(item, item.getCount() - exclItem.getCount(), item.getReferencePrice());
				}
			}
		}
		
		return new TradeItem(item, item.getCount(), item.getReferencePrice());
	}
	
	/**
	 * Adds a new item to the trade list.<br>
	 * This method uses a default price of {@code 0}.
	 * @param objectId The unique identifier for the item type.
	 * @param count The quantity of the item to add.
	 * @return The newly created {@code TradeItem} instance.
	 */
	public TradeItem addItem(int objectId, long count)
	{
		return addItem(objectId, count, 0);
	}
	
	/**
	 * Adds a new item to the trade list with a specific price.<br>
	 * This method checks if the item is valid for trading and available in the owner's inventory.<br>
	 * It also ensures that the requested count and price are within allowed limits.
	 * @param objectId The unique identifier of the world object to add.
	 * @param count The number of items to include in the trade.
	 * @param price The price assigned to this specific item entry.
	 * @return The newly created {@code TradeItem} if successful, or {@code null} if any validation fails.
	 */
	public synchronized TradeItem addItem(int objectId, long count, long price)
	{
		if (_locked)
		{
			return null;
		}
		
		final WorldObject o = World.findObject(objectId);
		if (!(o instanceof Item))
		{
			return null;
		}
		
		final Item item = (Item) o;
		if (!(item.isTradeable() || (_owner.isGM() && GeneralConfig.GM_TRADE_RESTRICTED_ITEMS)) || item.isQuestItem())
		{
			return null;
		}
		
		if (!_owner.getInventory().canManipulateWithItemId(item.getId()))
		{
			return null;
		}
		
		if ((count <= 0) || (count > item.getCount()))
		{
			return null;
		}
		
		if (!item.isStackable() && (count > 1))
		{
			return null;
		}
		
		if ((MAX_ADENA / count) < price)
		{
			return null;
		}
		
		for (TradeItem checkitem : _items)
		{
			if (checkitem.getObjectId() == objectId)
			{
				return null;
			}
		}
		
		final TradeItem titem = new TradeItem(item, count, price);
		_items.add(titem);
		
		// If Player has already confirmed this trade, invalidate the confirmation.
		invalidateConfirmation();
		return titem;
	}
	
	/**
	 * Adds a new item to the trade list using its unique ID.<br>
	 * This method checks if the item is valid and tradeable before adding it.<br>
	 * It also prevents overflows and ensures non-stackable items have a count of 1.
	 * @param itemId The unique identifier for the item template.
	 * @param count The number of items to add.
	 * @param price The adena price for the specified count.
	 * @return The created {@code TradeItem} object, or {@code null} if the addition fails.
	 */
	public synchronized TradeItem addItemByItemId(int itemId, long count, long price)
	{
		if (_locked)
		{
			LOGGER.warning(_owner.getName() + ": Attempt to modify locked TradeList!");
			return null;
		}
		
		final ItemTemplate item = ItemData.getInstance().getTemplate(itemId);
		if (item == null)
		{
			LOGGER.warning(_owner.getName() + ": Attempt to add invalid item to TradeList!");
			return null;
		}
		
		if (!item.isTradeable() || item.isQuestItem())
		{
			return null;
		}
		
		if (!item.isStackable() && (count > 1))
		{
			LOGGER.warning(_owner.getName() + ": Attempt to add non-stackable item to TradeList with count > 1!");
			return null;
		}
		
		if ((MAX_ADENA / count) < price)
		{
			LOGGER.warning(_owner.getName() + ": Attempt to overflow adena !");
			return null;
		}
		
		final TradeItem titem = new TradeItem(item, count, price);
		_items.add(titem);
		
		// If Player has already confirmed this trade, invalidate the confirmation.
		invalidateConfirmation();
		return titem;
	}
	
	/**
	 * Removes a specific quantity of an item from the trade list.<br>
	 * This method checks if the trade is locked before proceeding.<br>
	 * It also invalidates the partner's confirmation if an item is removed.
	 * @param objectId The unique identifier for the world object.
	 * @param itemId The unique identifier for the item template.
	 * @param count The amount of items to remove from the list.
	 * @return The updated {@code TradeItem} or {@code null} if the operation fails.
	 */
	private synchronized TradeItem removeItem(int objectId, int itemId, long count)
	{
		if (_locked)
		{
			LOGGER.warning(_owner.getName() + ": Attempt to modify locked TradeList!");
			return null;
		}
		
		if (count < 0)
		{
			LOGGER.warning(_owner.getName() + ": Attempt to remove " + count + " items from TradeList!");
			return null;
		}
		
		for (TradeItem titem : _items)
		{
			if ((titem.getObjectId() == objectId) || (titem.getItem().getId() == itemId))
			{
				// If Partner has already confirmed this trade, invalidate the confirmation.
				if (_partner != null)
				{
					final TradeList partnerList = _partner.getActiveTradeList();
					if (partnerList == null)
					{
						final String partnerName = _partner.getName();
						LOGGER.warning(partnerName + ": Trading partner (" + partnerName + ") is invalid in this trade!");
						return null;
					}
					
					partnerList.invalidateConfirmation();
				}
				
				// Reduce item count or complete item.
				if ((count != -1) && (titem.getCount() > count))
				{
					titem.setCount(titem.getCount() - count);
				}
				else
				{
					_items.remove(titem);
				}
				
				return titem;
			}
		}
		
		return null;
	}
	
	/**
	 * Synchronizes the trade items with the owner's inventory.<br>
	 * It removes items that no longer exist or have a count of zero.<br>
	 * It updates the item counts to match the current inventory state.
	 */
	public synchronized void updateItems()
	{
		for (TradeItem titem : _items)
		{
			final Item item = _owner.getInventory().getItemByObjectId(titem.getObjectId());
			if ((item == null) || (titem.getCount() < 1))
			{
				removeItem(titem.getObjectId(), -1, -1);
			}
			else if (item.getCount() < titem.getCount())
			{
				titem.setCount(item.getCount());
			}
		}
	}
	
	/**
	 * Sets the lock status of this trade list to {@code true}.<br>
	 * This prevents further modifications to the items during a transaction.
	 */
	public void lock()
	{
		_locked = true;
	}
	
	/**
	 * Removes all items from the trade list.<br>
	 * Resets the {@code _locked} status to {@code false}.
	 */
	public synchronized void clear()
	{
		_items.clear();
		_locked = false;
	}
	
	/**
	 * Confirms the trade and initiates the item exchange.<br>
	 * This method checks if both parties have agreed to the trade.<br>
	 * It handles synchronization to prevent deadlocks during the process.
	 * @return {@code true} if the trade was successfully confirmed or is already confirmed, {@code false} otherwise.
	 */
	public boolean confirm()
	{
		if (_confirmed)
		{
			return true; // Already confirmed
		}
		
		// If Partner has already confirmed this trade, proceed exchange.
		if (_partner != null)
		{
			final TradeList partnerList = _partner.getActiveTradeList();
			if (partnerList == null)
			{
				final String partnerName = _partner.getName();
				LOGGER.warning(partnerName + ": Trading partner (" + partnerName + ") is invalid in this trade!");
				return false;
			}
			
			// Synchronization order to avoid deadlock.
			TradeList sync1;
			TradeList sync2;
			if (getOwner().getObjectId() > partnerList.getOwner().getObjectId())
			{
				sync1 = partnerList;
				sync2 = this;
			}
			else
			{
				sync1 = this;
				sync2 = partnerList;
			}
			
			synchronized (sync1)
			{
				synchronized (sync2)
				{
					_confirmed = true;
					if (partnerList.isConfirmed())
					{
						partnerList.lock();
						lock();
						if (!partnerList.validate() || !validate())
						{
							return false;
						}
						
						doExchange(partnerList);
					}
					else
					{
						_partner.onTradeConfirm(_owner);
					}
				}
			}
		}
		else
		{
			_confirmed = true;
		}
		
		return _confirmed;
	}
	
	/**
	 * Resets the confirmation status of the trade.<br>
	 * Sets the {@code _confirmed} field to {@code false}.
	 */
	private void invalidateConfirmation()
	{
		_confirmed = false;
	}
	
	/**
	 * Checks if the trade list is currently valid.<br>
	 * It verifies that the {@code _owner} exists in the {@code World}.<br>
	 * It also ensures all items in the {@code _items} set are valid for transfer.
	 * @return {@code true} if all checks pass, otherwise {@code false}
	 */
	private boolean validate()
	{
		// Check for Owner validity.
		if ((_owner == null) || (World.getPlayer(_owner.getObjectId()) == null))
		{
			LOGGER.warning("Invalid owner of TradeList");
			return false;
		}
		
		// Check for Item validity.
		for (TradeItem titem : _items)
		{
			final Item item = _owner.checkItemManipulation(titem.getObjectId(), titem.getCount(), "transfer");
			if ((item == null) || (item.getCount() < 1))
			{
				LOGGER.warning(_owner.getName() + ": Invalid Item in TradeList");
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * Transfers all items from the owner to the partner.<br>
	 * Validates that each item exists before moving it.<br>
	 * Updates the provided inventory packets if they are not {@code null}.
	 * @param partner The {@code Player} receiving the items.
	 * @param ownerIU The {@code InventoryUpdate} packet for the owner.
	 * @param partnerIU The {@code InventoryUpdate} packet for the partner.
	 * @return {@code true} if all items were transferred successfully, otherwise {@code false}.
	 */
	private boolean TransferItems(Player partner, InventoryUpdate ownerIU, InventoryUpdate partnerIU)
	{
		for (TradeItem titem : _items)
		{
			final PlayerInventory ownerInventory = _owner.getInventory();
			final int titemObjectId = titem.getObjectId();
			final Item oldItem = ownerInventory.getItemByObjectId(titemObjectId);
			if (oldItem == null)
			{
				return false;
			}
			
			final Item newItem = ownerInventory.transferItem(ItemProcessType.TRANSFER, titemObjectId, titem.getCount(), partner.getInventory(), _owner, _partner);
			if (newItem == null)
			{
				return false;
			}
			
			// Add changes to inventory update packets.
			if (ownerIU != null)
			{
				if ((oldItem.getCount() > 0) && (oldItem != newItem))
				{
					ownerIU.addModifiedItem(oldItem);
				}
				else
				{
					ownerIU.addRemovedItem(oldItem);
				}
			}
			
			if (partnerIU != null)
			{
				if (newItem.getCount() > titem.getCount())
				{
					partnerIU.addModifiedItem(newItem);
				}
				else
				{
					partnerIU.addNewItem(newItem);
				}
			}
		}
		
		return true;
	}
	
	/**
	 * Calculates the total number of inventory slots required for trade items.<br>
	 * It checks if items are stackable and considers the partner's current inventory.
	 * @param partner The {@code Player} whose inventory is checked for existing items.
	 * @return The total count of slots needed to hold all trade items.
	 */
	private int countItemsSlots(Player partner)
	{
		int slots = 0;
		for (TradeItem item : _items)
		{
			if (item == null)
			{
				continue;
			}
			
			final ItemTemplate template = ItemData.getInstance().getTemplate(item.getItem().getId());
			if (template == null)
			{
				continue;
			}
			
			if (!template.isStackable())
			{
				slots += item.getCount();
			}
			else if (partner.getInventory().getItemByItemId(item.getItem().getId()) == null)
			{
				slots++;
			}
		}
		
		return slots;
	}
	
	/**
	 * Calculates the total weight of all items in the trade list.<br>
	 * It iterates through every {@code TradeItem} and multiplies its count by its template weight.<br>
	 * The result is capped at {@code Integer.MAX_VALUE}.
	 * @return The total calculated weight as an {@code int}.
	 */
	private int calcItemsWeight()
	{
		long weight = 0;
		for (TradeItem item : _items)
		{
			if (item == null)
			{
				continue;
			}
			
			final ItemTemplate template = ItemData.getInstance().getTemplate(item.getItem().getId());
			if (template == null)
			{
				continue;
			}
			
			weight += item.getCount() * template.getWeight();
		}
		
		return (int) Math.min(weight, Integer.MAX_VALUE);
	}
	
	/**
	 * Checks if a receiver would pass the stack limit with any item of this list.
	 * @param receiver The {@code Player} who gets the items.
	 * @return {@code true} if an item would not fit in its stack.
	 */
	private boolean overflowsStacks(Player receiver)
	{
		for (TradeItem item : _items)
		{
			if (overflowsStack(receiver, item.getItem().getId(), item.getCount()))
			{
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Checks if adding a count of a stackable item would pass the stack limit, where the excess would be lost.
	 * @param receiver The {@code Player} who gets the items.
	 * @param itemId The item id.
	 * @param count The count to add.
	 * @return {@code true} if the stack cannot take the whole count.
	 */
	private static boolean overflowsStack(Player receiver, int itemId, long count)
	{
		final ItemTemplate template = ItemData.getInstance().getTemplate(itemId);
		if ((template == null) || !template.isStackable())
		{
			return false;
		}
		
		return count > ((itemId == ADENA_ID ? MAX_ADENA : Long.MAX_VALUE) - receiver.getInventory().getInventoryItemCount(itemId, -1));
	}
	
	/**
	 * Executes the final exchange of items between two players.<br>
	 * It validates weight and capacity limits for both inventories before transferring items.<br>
	 * If validation fails, it sends an error message to both parties.<br>
	 * Finally, it updates the inventory packets and finishes the trade process.
	 * @param partnerList The {@code TradeList} object belonging to the other player in the trade.
	 */
	private void doExchange(TradeList partnerList)
	{
		boolean success = false;
		
		// Check weight and slots.
		if ((!_owner.getInventory().validateWeight(partnerList.calcItemsWeight())) || !(partnerList.getOwner().getInventory().validateWeight(calcItemsWeight())))
		{
			partnerList.getOwner().sendPacket(SystemMessageId.WEIGHT_LIMIT_IS_EXCEEDED);
			_owner.sendPacket(SystemMessageId.WEIGHT_LIMIT_IS_EXCEEDED);
		}
		else if ((!_owner.getInventory().validateCapacity(partnerList.countItemsSlots(getOwner()))) || (!partnerList.getOwner().getInventory().validateCapacity(countItemsSlots(partnerList.getOwner()))))
		{
			partnerList.getOwner().sendPacket(SystemMessageId.YOUR_INVENTORY_IS_FULL);
			_owner.sendPacket(SystemMessageId.YOUR_INVENTORY_IS_FULL);
		}
		else if (partnerList.overflowsStacks(getOwner()) || overflowsStacks(partnerList.getOwner()))
		{
			partnerList.getOwner().sendPacket(SystemMessageId.YOU_HAVE_EXCEEDED_THE_QUANTITY_THAT_CAN_BE_INPUTTED);
			_owner.sendPacket(SystemMessageId.YOU_HAVE_EXCEEDED_THE_QUANTITY_THAT_CAN_BE_INPUTTED);
		}
		else
		{
			// Prepare inventory update packet.
			final InventoryUpdate ownerIU = new InventoryUpdate();
			final InventoryUpdate partnerIU = new InventoryUpdate();
			
			// Transfer items
			partnerList.TransferItems(_owner, partnerIU, ownerIU);
			TransferItems(partnerList.getOwner(), ownerIU, partnerIU);
			
			// Send inventory update packets.
			_owner.sendInventoryUpdate(ownerIU);
			_partner.sendInventoryUpdate(partnerIU);
			
			success = true;
		}
		
		// Visual inconsistencies fix.
		_owner.sendItemList();
		_partner.sendItemList();
		
		// Finish the trade
		partnerList.getOwner().onTradeFinish(success);
		_owner.onTradeFinish(success);
	}
	
	/**
	 * Processes a purchase from a private store for the specified player.<br>
	 * It validates the items, checks for sufficient adena and inventory space, and transfers the items.<br>
	 * Returns {@code 0} on success, {@code 1} if the trade is locked or invalid, and {@code 2} for illegal actions.
	 * @param player The player attempting to buy the items.
	 * @param items The set of requested items to purchase.
	 * @return An integer status code representing the result of the transaction.
	 */
	public synchronized int privateStoreBuy(Player player, Set<RequestTrade> items)
	{
		if (_locked)
		{
			return 1;
		}
		
		if (!validate())
		{
			lock();
			return 1;
		}
		
		if (!_owner.isOnline() || !player.isOnline())
		{
			return 1;
		}
		
		int slots = 0;
		int weight = 0;
		long totalPrice = 0;
		
		final PlayerInventory ownerInventory = _owner.getInventory();
		final PlayerInventory playerInventory = player.getInventory();
		for (RequestTrade item : items)
		{
			boolean found = false;
			for (TradeItem ti : _items)
			{
				if (ti.getObjectId() == item.getObjectId())
				{
					if (ti.getPrice() == item.getPrice())
					{
						if (ti.getCount() < item.getCount())
						{
							item.setCount(ti.getCount());
						}
						
						found = true;
					}
					break;
				}
			}
			
			// item with this objectId and price not found in tradelist
			if (!found)
			{
				if (_packaged)
				{
					PunishmentManager.handleIllegalPlayerAction(player, "[TradeList.privateStoreBuy()] " + player + " tried to cheat the package sell and buy only a part of the package! Ban this player for bot usage!", GeneralConfig.DEFAULT_PUNISH);
					return 2;
				}
				
				item.setCount(0);
				continue;
			}
			
			final long itemCount = item.getCount();
			final long price = item.getPrice();
			
			// Check for overflow in the single item.
			if ((MAX_ADENA / itemCount) < price)
			{
				// private store attempting to overflow - disable it
				lock();
				return 1;
			}
			
			totalPrice += itemCount * price;
			
			// Check for overflow of the total price.
			if ((MAX_ADENA < totalPrice) || (totalPrice < 0))
			{
				// private store attempting to overflow - disable it
				lock();
				return 1;
			}
			
			// Check if requested item is available for manipulation.
			final Item oldItem = _owner.checkItemManipulation(item.getObjectId(), itemCount, "sell");
			if ((oldItem == null) || !oldItem.isTradeable())
			{
				// private store sell invalid item - disable it
				lock();
				return 2;
			}
			
			final ItemTemplate template = ItemData.getInstance().getTemplate(item.getItemId());
			if (template == null)
			{
				continue;
			}
			
			if (overflowsStack(player, item.getItemId(), itemCount))
			{
				player.sendPacket(SystemMessageId.YOU_HAVE_EXCEEDED_THE_QUANTITY_THAT_CAN_BE_INPUTTED);
				return 1;
			}
			
			weight += itemCount * template.getWeight();
			if (!template.isStackable())
			{
				slots += itemCount;
			}
			else if (playerInventory.getItemByItemId(item.getItemId()) == null)
			{
				slots++;
			}
		}
		
		if (totalPrice > playerInventory.getAdena())
		{
			player.sendPacket(SystemMessageId.NOT_ENOUGH_ADENA);
			return 1;
		}
		
		if (overflowsStack(_owner, ADENA_ID, totalPrice))
		{
			player.sendPacket(SystemMessageId.YOU_HAVE_EXCEEDED_THE_QUANTITY_THAT_CAN_BE_INPUTTED);
			return 1;
		}
		
		if (!playerInventory.validateWeight(weight))
		{
			player.sendPacket(SystemMessageId.WEIGHT_LIMIT_IS_EXCEEDED);
			return 1;
		}
		
		if (!playerInventory.validateCapacity(slots))
		{
			player.sendPacket(SystemMessageId.YOUR_INVENTORY_IS_FULL);
			return 1;
		}
		
		// Keep the item reference, paying the exact amount destroys the stack.
		final Item adenaItem = playerInventory.getAdenaInstance();
		if (!playerInventory.reduceAdena(ItemProcessType.BUY, totalPrice, player, _owner))
		{
			player.sendPacket(SystemMessageId.NOT_ENOUGH_ADENA);
			return 1;
		}
		
		// Prepare inventory update packets.
		final InventoryUpdate ownerIU = new InventoryUpdate();
		final InventoryUpdate playerIU = new InventoryUpdate();
		playerIU.addItem(adenaItem);
		ownerInventory.addAdena(ItemProcessType.SELL, totalPrice, _owner, player);
		
		// ownerIU.addItem(ownerInventory.getAdenaInstance());
		boolean ok = true;
		
		// Transfer items
		for (RequestTrade item : items)
		{
			if (item.getCount() == 0)
			{
				continue;
			}
			
			final long itemCount = item.getCount();
			final int itemObjectId = item.getObjectId();
			final long price = item.getPrice();
			
			// Check if requested item is available for manipulation.
			final Item oldItem = _owner.checkItemManipulation(itemObjectId, itemCount, "sell");
			if (oldItem == null)
			{
				// Should not happens - validation already done.
				lock();
				ok = false;
				break;
			}
			
			// Proceed with item transfer.
			final Item newItem = ownerInventory.transferItem(ItemProcessType.TRANSFER, itemObjectId, itemCount, playerInventory, _owner, player);
			if (newItem == null)
			{
				ok = false;
				break;
			}
			
			removeItem(itemObjectId, -1, itemCount);
			
			PrivateStoreHistoryManager.getInstance().registerTransaction(PrivateStoreType.SELL, newItem, itemCount, itemCount * price);
			
			// Add changes to inventory update packets.
			if ((oldItem.getCount() > 0) && (oldItem != newItem))
			{
				ownerIU.addModifiedItem(oldItem);
			}
			else
			{
				ownerIU.addRemovedItem(oldItem);
			}
			
			if (newItem.getCount() > itemCount)
			{
				playerIU.addModifiedItem(newItem);
			}
			else
			{
				playerIU.addNewItem(newItem);
			}
			
			// Send messages about the transaction to both players.
			final String playerName = player.getName();
			if (newItem.isStackable())
			{
				SystemMessage msg = new SystemMessage(SystemMessageId.C1_PURCHASED_S3_S2_S);
				msg.addString(playerName);
				msg.addItemName(newItem);
				msg.addLong(itemCount);
				_owner.sendPacket(msg);
				
				msg = new SystemMessage(SystemMessageId.YOU_HAVE_PURCHASED_S3_S2_S_FROM_C1);
				msg.addString(_owner.getName());
				msg.addItemName(newItem);
				msg.addLong(itemCount);
				player.sendPacket(msg);
			}
			else
			{
				SystemMessage msg = new SystemMessage(SystemMessageId.C1_PURCHASED_S2);
				msg.addString(playerName);
				msg.addItemName(newItem);
				_owner.sendPacket(msg);
				
				msg = new SystemMessage(SystemMessageId.YOU_HAVE_PURCHASED_S2_FROM_C1);
				msg.addString(_owner.getName());
				msg.addItemName(newItem);
				player.sendPacket(msg);
			}
			
			_owner.sendPacket(new ExPrivateStoreSellingResult(itemObjectId, itemCount, player.getAppearance().getVisibleName()));
		}
		
		// Send inventory update packet.
		_owner.sendInventoryUpdate(ownerIU);
		player.sendInventoryUpdate(playerIU);
		
		// Visual inconsistencies fix.
		_owner.sendItemList();
		player.sendItemList();
		
		return ok ? 0 : 2;
	}
	
	/**
	 * Processes a request to purchase items from the private store.<br>
	 * Validates item availability, prices, and player online status.<br>
	 * Handles the transfer of items and adena between players.<br>
	 * Updates inventories and sends system messages for successful transactions.
	 * @param player The {@code Player} attempting to buy items.
	 * @param requestedItems An array of {@code RequestTrade} objects containing the desired items.
	 * @return {@code true} if at least one item was successfully purchased, otherwise {@code false}.
	 */
	public synchronized boolean privateStoreSell(Player player, RequestTrade[] requestedItems)
	{
		if (_locked || !_owner.isOnline() || !player.isOnline())
		{
			return false;
		}
		
		boolean ok = false;
		
		final PlayerInventory ownerInventory = _owner.getInventory();
		final PlayerInventory playerInventory = player.getInventory();
		
		// Prepare inventory update packet.
		final InventoryUpdate ownerIU = new InventoryUpdate();
		final InventoryUpdate playerIU = new InventoryUpdate();
		long totalPrice = 0;
		
		final TradeItem[] sellerItems = _items.toArray(new TradeItem[0]);
		for (RequestTrade item : requestedItems)
		{
			// Searching item in tradelist using itemId.
			boolean found = false;
			for (TradeItem ti : sellerItems)
			{
				if (ti.getItem().getId() == item.getItemId())
				{
					// Price should be the same.
					if (ti.getPrice() == item.getPrice())
					{
						// If requesting more than available - decrease count.
						if (ti.getCount() < item.getCount())
						{
							item.setCount(ti.getCount());
						}
						
						found = item.getCount() > 0;
					}
					break;
				}
			}
			
			// Not found any item in the tradelist with same itemId and price.
			// Maybe another player already sold this item?
			if (!found)
			{
				continue;
			}
			
			final long itemCount = item.getCount();
			
			// Check for overflow in the single item.
			if ((MAX_ADENA / itemCount) < item.getPrice())
			{
				lock();
				break;
			}
			
			final long _totalPrice = totalPrice + (itemCount * item.getPrice());
			
			// Check for overflow of the total price.
			if ((MAX_ADENA < _totalPrice) || (_totalPrice < 0))
			{
				lock();
				break;
			}
			
			if (ownerInventory.getAdena() < _totalPrice)
			{
				continue;
			}
			
			final int objectId = item.getObjectId();
			if ((objectId < 1) || (objectId > sellerItems.length))
			{
				continue;
			}
			
			final TradeItem tradeItem = sellerItems[objectId - 1];
			if ((tradeItem == null) || (tradeItem.getItem().getId() != item.getItemId()))
			{
				continue;
			}
			
			// Neither side may lose part of a full stack.
			if (overflowsStack(_owner, item.getItemId(), item.getCount()) || overflowsStack(player, ADENA_ID, _totalPrice))
			{
				player.sendPacket(SystemMessageId.YOU_HAVE_EXCEEDED_THE_QUANTITY_THAT_CAN_BE_INPUTTED);
				continue;
			}
			
			// Check if requested item is available for manipulation.
			int currentObjectId = tradeItem.getObjectId();
			Item oldItem = player.checkItemManipulation(currentObjectId, itemCount, "sell");
			// private store - buy use same objectId for buying several non-stackable items
			if (oldItem == null)
			{
				// searching other items using same itemId and the enchant level requested by the buyer
				for (Item invItem : playerInventory.getAllItemsByItemId(item.getItemId()))
				{
					if (invItem.getEnchantLevel() == tradeItem.getEnchant())
					{
						oldItem = invItem;
						break;
					}
				}
				
				if (oldItem == null)
				{
					continue;
				}
				
				currentObjectId = oldItem.getObjectId();
				oldItem = player.checkItemManipulation(currentObjectId, itemCount, "sell");
				if (oldItem == null)
				{
					continue;
				}
			}
			
			if (oldItem.getId() != item.getItemId())
			{
				PunishmentManager.handleIllegalPlayerAction(player, player + " is cheating with sell items", GeneralConfig.DEFAULT_PUNISH);
				return false;
			}
			
			if (!oldItem.isTradeable() || (oldItem.getEnchantLevel() != tradeItem.getEnchant()))
			{
				continue;
			}
			
			// Proceed with item transfer.
			final Item newItem = playerInventory.transferItem(ItemProcessType.TRANSFER, currentObjectId, itemCount, ownerInventory, player, _owner);
			if (newItem == null)
			{
				continue;
			}
			
			removeItem(-1, item.getItemId(), itemCount);
			ok = true;
			
			PrivateStoreHistoryManager.getInstance().registerTransaction(PrivateStoreType.BUY, newItem, itemCount, itemCount * item.getPrice());
			
			// Increase total price only after successful transaction.
			totalPrice = _totalPrice;
			
			// Add changes to inventory update packets.
			if ((oldItem.getCount() > 0) && (oldItem != newItem))
			{
				playerIU.addModifiedItem(oldItem);
			}
			else
			{
				playerIU.addRemovedItem(oldItem);
			}
			
			if (newItem.getCount() > itemCount)
			{
				ownerIU.addModifiedItem(newItem);
			}
			else
			{
				ownerIU.addNewItem(newItem);
			}
			
			// Send messages about the transaction to both players.
			final String playerName = player.getName();
			if (newItem.isStackable())
			{
				SystemMessage msg = new SystemMessage(SystemMessageId.YOU_HAVE_PURCHASED_S3_S2_S_FROM_C1);
				msg.addString(playerName);
				msg.addItemName(newItem);
				msg.addLong(itemCount);
				_owner.sendPacket(msg);
				
				msg = new SystemMessage(SystemMessageId.C1_PURCHASED_S3_S2_S);
				msg.addString(_owner.getName());
				msg.addItemName(newItem);
				msg.addLong(itemCount);
				player.sendPacket(msg);
			}
			else
			{
				SystemMessage msg = new SystemMessage(SystemMessageId.YOU_HAVE_PURCHASED_S2_FROM_C1);
				msg.addString(playerName);
				msg.addItemName(newItem);
				_owner.sendPacket(msg);
				
				msg = new SystemMessage(SystemMessageId.C1_PURCHASED_S2);
				msg.addString(_owner.getName());
				msg.addItemName(newItem);
				player.sendPacket(msg);
			}
			
			_owner.sendPacket(new ExPrivateStoreBuyingResult(item.getObjectId(), itemCount, player.getAppearance().getVisibleName()));
		}
		
		if (totalPrice > 0)
		{
			// Transfer adena
			if (totalPrice > ownerInventory.getAdena())
			{
				// should not happens, just a precaution
				return false;
			}
			
			final Item adenaItem = ownerInventory.getAdenaInstance();
			ownerInventory.reduceAdena(ItemProcessType.BUY, totalPrice, _owner, player);
			ownerIU.addItem(adenaItem);
			playerInventory.addAdena(ItemProcessType.SELL, totalPrice, player, _owner);
			playerIU.addItem(playerInventory.getAdenaInstance());
		}
		
		if (ok)
		{
			// Send inventory update packet.
			_owner.sendInventoryUpdate(ownerIU);
			player.sendInventoryUpdate(playerIU);
			
			// Visual inconsistencies fix.
			_owner.sendItemList();
			player.sendItemList();
		}
		
		return ok;
	}
}
