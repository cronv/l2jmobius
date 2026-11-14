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

import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.entity.item.enums.ItemLocation;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.events.EventDispatcher;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerClanWHItemAdd;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerClanWHItemDestroy;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerClanWHItemTransfer;

/**
 * Represents a shared storage container for items belonging to a {@code Clan}.<br>
 * It allows members of the same clan to store and manage items collectively.<br>
 * This class extends the base functionality provided by {@code Warehouse}.
 */
public class ClanWarehouse extends Warehouse
{
	private final Clan _clan;
	
	/**
	 * Creates a new {@code ClanWarehouse} instance.<br>
	 * This constructor links the warehouse to a specific {@code Clan}.
	 * @param clan The {@code Clan} that owns this warehouse.
	 */
	public ClanWarehouse(Clan clan)
	{
		_clan = clan;
	}
	
	/**
	 * Returns the name of the warehouse.<br>
	 * This method always returns the string {@code ClanWarehouse}.
	 * @return The name of this warehouse as a {@code String}.
	 */
	@Override
	public String getName()
	{
		return "ClanWarehouse";
	}
	
	/**
	 * Retrieves the unique identifier of the owner.<br>
	 * This value corresponds to the {@code Clan} ID associated with this warehouse.
	 * @return The unique integer ID of the owner.
	 */
	@Override
	public int getOwnerId()
	{
		return _clan.getId();
	}
	
	/**
	 * Retrieves the {@code Player} who owns this warehouse.<br>
	 * This is determined by the leader of the associated clan.
	 * @return the {@code Player} object representing the owner.
	 */
	@Override
	public Player getOwner()
	{
		return _clan.getLeader().getPlayer();
	}
	
	/**
	 * Returns the base location for this warehouse.<br>
	 * This value is used to identify the storage type as {@code CLANWH}.
	 * @return the {@code ItemLocation} of the clan warehouse.
	 */
	@Override
	public ItemLocation getBaseLocation()
	{
		return ItemLocation.CLANWH;
	}
	
	/**
	 * Checks if adding more items will exceed the maximum allowed capacity.<br>
	 * It compares the current item count plus the new {@code slots} against the limit defined in {@code PlayerConfig}.
	 * @param slots The number of additional slots to check.
	 * @return {@code true} if the total fits within the limit, {@code false} otherwise.
	 */
	@Override
	public boolean validateCapacity(long slots)
	{
		return (_items.size() + slots) <= PlayerConfig.WAREHOUSE_SLOTS_CLAN;
	}
	
	/**
	 * Adds a new item to the clan warehouse.<br>
	 * This method handles the addition process and notifies relevant scripts.
	 * @param process The type of process used to add the item.
	 * @param itemId The unique identifier of the item template.
	 * @param count The amount of the item to add.
	 * @param actor The player performing the action.
	 * @param reference An optional object used as a reference for the operation.
	 * @return The {@code Item} that was added to the warehouse.
	 */
	@Override
	public Item addItem(ItemProcessType process, int itemId, long count, Player actor, Object reference)
	{
		final Item item = super.addItem(process, itemId, count, actor, reference);
		
		// Notify to scripts
		final ItemTemplate template = item.getTemplate();
		if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_CLAN_WH_ITEM_ADD, template))
		{
			EventDispatcher.getInstance().notifyEventAsync(new OnPlayerClanWHItemAdd(actor, item, this), template);
		}
		
		return item;
	}
	
	/**
	 * Adds an {@code Item} to the clan warehouse.<br>
	 * This method notifies relevant scripts when a new item is added.<br>
	 * It then calls the base class logic to complete the addition.
	 * @param process The type of process being performed.
	 * @param item The {@code Item} instance to add.
	 * @param actor The {@code Player} performing the action.
	 * @param reference An optional object used as a reference for the operation.
	 * @return The added {@code Item} instance.
	 */
	@Override
	public Item addItem(ItemProcessType process, Item item, Player actor, Object reference)
	{
		// Notify to scripts
		final ItemTemplate template = item.getTemplate();
		if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_CLAN_WH_ITEM_ADD, template))
		{
			EventDispatcher.getInstance().notifyEventAsync(new OnPlayerClanWHItemAdd(actor, item, this), template);
		}
		
		return super.addItem(process, item, actor, reference);
	}
	
	/**
	 * Removes an item from the clan warehouse.<br>
	 * This method notifies listeners of the destruction event before calling the base class logic.
	 * @param process The type of process used to destroy the item.
	 * @param item The {@code Item} instance to be removed.
	 * @param count The amount of the item to remove.
	 * @param actor The {@code Player} performing the action.
	 * @param reference An optional object used as a reference for the operation.
	 * @return The resulting {@code Item} after the destruction process.
	 */
	@Override
	public Item destroyItem(ItemProcessType process, Item item, long count, Player actor, Object reference)
	{
		// Notify to scripts
		final ItemTemplate template = item.getTemplate();
		if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_CLAN_WH_ITEM_DESTROY, template))
		{
			EventDispatcher.getInstance().notifyEventAsync(new OnPlayerClanWHItemDestroy(actor, item, count, this), template);
		}
		
		return super.destroyItem(process, item, count, actor, reference);
	}
	
	/**
	 * Moves an item from the current warehouse to a different container.<br>
	 * This method handles event notifications for clan warehouse transfers.<br>
	 * It uses the base transfer logic of the {@code Warehouse} class.
	 * @param process The type of operation being performed.
	 * @param objectId The unique identifier of the item to move.
	 * @param count The amount of the item to transfer.
	 * @param target The destination container for the item.
	 * @param actor The player performing the action.
	 * @param reference An optional object used as a context reference.
	 * @return The {@code Item} that was transferred.
	 */
	@Override
	public Item transferItem(ItemProcessType process, int objectId, long count, ItemContainer target, Player actor, Object reference)
	{
		final Item item = getItemByObjectId(objectId);
		
		// Notify to scripts
		final ItemTemplate template = item.getTemplate();
		if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_CLAN_WH_ITEM_TRANSFER, template))
		{
			EventDispatcher.getInstance().notifyEventAsync(new OnPlayerClanWHItemTransfer(actor, item, count, target), template);
		}
		
		return super.transferItem(process, objectId, count, target, actor, reference);
	}
}
