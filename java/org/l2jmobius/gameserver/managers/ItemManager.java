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
package org.l2jmobius.gameserver.managers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Attackable;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.instance.EventMonster;
import org.l2jmobius.gameserver.entity.groups.CommandChannel;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.entity.item.enums.ItemLocation;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.itemcontainer.Inventory;
import org.l2jmobius.gameserver.mechanics.events.EventDispatcher;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.item.OnItemCreate;
import org.l2jmobius.gameserver.util.GMAudit;

/**
 * This class manages the lifecycle of items within the game world.<br>
 * It handles the creation and destruction of {@code Item} instances.
 * @author Mobius
 */
public class ItemManager
{
	private static final Logger LOGGER = Logger.getLogger(ItemManager.class.getName());
	private static final Logger LOGGER_ITEMS = Logger.getLogger("item");
	
	/**
	 * Private constructor for the {@code ItemManager} class.<br>
	 * This prevents other classes from creating new instances of this manager.
	 */
	private ItemManager()
	{
	}
	
	/**
	 * Creates a new {@code Item} instance based on the provided parameters.<br>
	 * This method handles the logic for generating an item into the world or inventory.
	 * @param process The type of process used to create the item.
	 * @param itemId The unique identifier of the item template.
	 * @param count The quantity of the item to create.
	 * @param actor The {@code Player} who is performing the action.
	 * @return The newly created {@code Item} object.
	 */
	public static Item createItem(ItemProcessType process, int itemId, long count, Player actor)
	{
		return createItem(process, itemId, count, actor, null);
	}
	
	/**
	 * Creates and initializes a new {@code Item} instance in the world.<br>
	 * This method handles loot logic, stack counts, logging, and event notifications.
	 * @param process The type of process used to create the item.
	 * @param itemId The unique identifier for the item template.
	 * @param count The quantity of the item to create.
	 * @param actor The {@code Creature} performing the action.
	 * @param reference An object providing context for the creation, such as a monster or world object.
	 * @return The newly created {@code Item} instance.
	 */
	public static Item createItem(ItemProcessType process, int itemId, long count, Creature actor, Object reference)
	{
		// Create and Init the Item corresponding to the Item Identifier.
		final Item item = new Item(IdManager.getInstance().getNextId(), itemId);
		if ((process == ItemProcessType.LOOT) && !PlayerConfig.AUTO_LOOT_ITEM_IDS.contains(itemId))
		{
			ScheduledFuture<?> itemLootShedule;
			if ((reference instanceof Attackable) && ((Attackable) reference).isRaid()) // Loot privilege for raids.
			{
				// If in CommandChannel and was killing a World/RaidBoss.
				final Attackable raid = (Attackable) reference;
				final CommandChannel firstCC = raid.getFirstCommandChannelAttacked();
				if ((firstCC != null) && !PlayerConfig.AUTO_LOOT_RAIDS)
				{
					item.setOwnerId(firstCC.getLeaderObjectId());
					itemLootShedule = ThreadPool.schedule(new ResetOwner(item), PlayerConfig.LOOT_RAIDS_PRIVILEGE_INTERVAL);
					item.setItemLootShedule(itemLootShedule);
				}
			}
			else if (!PlayerConfig.AUTO_LOOT || ((reference instanceof EventMonster) && ((EventMonster) reference).eventDropOnGround()))
			{
				item.setOwnerId(actor.getObjectId());
				itemLootShedule = ThreadPool.schedule(new ResetOwner(item), 15000);
				item.setItemLootShedule(itemLootShedule);
			}
		}
		
		// Add the Item object to the World.
		World.addObject(item);
		
		// Set Item parameters.
		if (item.isStackable() && (count > 1))
		{
			item.setCount(count);
		}
		
		final ItemTemplate itemTemplate = item.getTemplate();
		if ((GeneralConfig.LOG_ITEMS && ((!GeneralConfig.LOG_ITEMS_SMALL_LOG) && (!GeneralConfig.LOG_ITEMS_IDS_ONLY))) || (GeneralConfig.LOG_ITEMS_SMALL_LOG && (item.isEquipable() || (item.getId() == Inventory.ADENA_ID))) || (GeneralConfig.LOG_ITEMS_IDS_ONLY && GeneralConfig.LOG_ITEMS_IDS_LIST.contains(item.getId())))
		{
			if (item.isEnchanted())
			{
				LOGGER_ITEMS.info(StringUtil.concat("CREATE:", String.valueOf(process), ", item ", String.valueOf(item.getObjectId()), ":+", String.valueOf(item.getEnchantLevel()), " ", itemTemplate.getName(), "(", String.valueOf(item.getCount()), "), ", String.valueOf(actor), ", ", String.valueOf(reference)));
			}
			else
			{
				LOGGER_ITEMS.info(StringUtil.concat("CREATE:", String.valueOf(process), ", item ", String.valueOf(item.getObjectId()), ":", itemTemplate.getName(), "(", String.valueOf(item.getCount()), "), ", String.valueOf(actor), ", ", String.valueOf(reference)));
			}
		}
		
		if ((actor != null) && actor.isGM() && GeneralConfig.GMAUDIT)
		{
			String referenceName = "no-reference";
			if (reference instanceof WorldObject)
			{
				final String woName = ((WorldObject) reference).getName();
				referenceName = (woName != null ? woName : "no-name");
			}
			else if (reference instanceof String)
			{
				referenceName = (String) reference;
			}
			
			final String targetName = (actor.getTarget() != null ? actor.getTarget().getName() : "no-target");
			GMAudit.logAction(String.valueOf(actor), StringUtil.concat(String.valueOf(process), "(id: ", String.valueOf(itemId), " count: ", String.valueOf(count), " name: ", item.getItemName(), " objId: ", String.valueOf(item.getObjectId()), ")"), targetName, StringUtil.concat("Object referencing this action is: ", referenceName));
		}
		
		// Notify to scripts.
		final EventDispatcher eventDispatcher = EventDispatcher.getInstance();
		if (eventDispatcher.hasListener(EventType.ON_ITEM_CREATE, itemTemplate))
		{
			eventDispatcher.notifyEventAsync(new OnItemCreate(item, actor, reference), itemTemplate);
		}
		
		return item;
	}
	
	/**
	 * Removes an {@code Item} from the game world and database.<br>
	 * This method updates the item status to {@code VOID} and releases its unique ID.<br>
	 * It also handles logging for specific processes and deletes associated pets if applicable.
	 * @param process The type of process used to destroy the item.
	 * @param item The {@code Item} instance to be removed.
	 * @param actor The {@code Player} who performed the action.
	 * @param reference An optional object providing context for the destruction.
	 */
	public static void destroyItem(ItemProcessType process, Item item, Player actor, Object reference)
	{
		synchronized (item)
		{
			final long old = item.getCount();
			final ItemTemplate itemTemplate = item.getTemplate();
			item.setCount(0);
			item.setOwnerId(0);
			item.setItemLocation(ItemLocation.VOID);
			item.setLastChange(Item.REMOVED);
			
			World.removeObject(item);
			final int objectId = item.getObjectId();
			IdManager.getInstance().releaseId(objectId);
			
			if ((process != null) && (process != ItemProcessType.NONE))
			{
				if ((GeneralConfig.LOG_ITEMS && ((!GeneralConfig.LOG_ITEMS_SMALL_LOG) && (!GeneralConfig.LOG_ITEMS_IDS_ONLY))) || (GeneralConfig.LOG_ITEMS_SMALL_LOG && (item.isEquipable() || (item.getId() == Inventory.ADENA_ID))) || (GeneralConfig.LOG_ITEMS_IDS_ONLY && GeneralConfig.LOG_ITEMS_IDS_LIST.contains(item.getId())))
				{
					if (item.isEnchanted())
					{
						LOGGER_ITEMS.info(StringUtil.concat("DELETE:", String.valueOf(process), ", item ", String.valueOf(objectId), ":+", String.valueOf(item.getEnchantLevel()), " ", itemTemplate.getName(), "(", String.valueOf(old), "), PrevCount(", String.valueOf(old), "), ", String.valueOf(actor), ", ", String.valueOf(reference)));
					}
					else
					{
						LOGGER_ITEMS.info(StringUtil.concat("DELETE:", String.valueOf(process), ", item ", String.valueOf(objectId), ":", itemTemplate.getName(), "(", String.valueOf(old), "), PrevCount(", String.valueOf(old), "), ", String.valueOf(actor), ", ", String.valueOf(reference)));
					}
				}
				
				if ((actor != null) && actor.isGM() && GeneralConfig.GMAUDIT)
				{
					String referenceName = "no-reference";
					if (reference instanceof WorldObject)
					{
						referenceName = (((WorldObject) reference).getName() != null ? ((WorldObject) reference).getName() : "no-name");
					}
					else if (reference instanceof String)
					{
						referenceName = (String) reference;
					}
					
					final String targetName = (actor.getTarget() != null ? actor.getTarget().getName() : "no-target");
					GMAudit.logAction(String.valueOf(actor), StringUtil.concat(String.valueOf(process), "(id: ", String.valueOf(item.getId()), " count: ", String.valueOf(old), " itemObjId: ", String.valueOf(objectId), ")"), targetName, StringUtil.concat("Object referencing this action is: ", referenceName));
				}
			}
			
			// If it's a pet control item, delete the pet as well.
			if (itemTemplate.isPetItem())
			{
				try (Connection con = DatabaseFactory.getConnection();
					PreparedStatement statement = con.prepareStatement("DELETE FROM pets WHERE item_obj_id=?"))
				{
					// Delete the pet in database.
					statement.setInt(1, objectId);
					statement.execute();
				}
				catch (Exception e)
				{
					LOGGER.log(Level.WARNING, "ItemManager: Could not delete pet objectid:", e);
				}
			}
		}
	}
	
	/**
	 * Runnable class that resets the owner of an item to 0 after a specified time interval. <br>
	 * This is typically used for loot items that need a temporary owner before becoming free to collect.
	 */
	protected static class ResetOwner implements Runnable
	{
		Item _item;
		
		public ResetOwner(Item item)
		{
			_item = item;
		}
		
		@Override
		public void run()
		{
			// Set owner id to 0 only when location is VOID.
			if (_item.getItemLocation() == ItemLocation.VOID)
			{
				_item.setOwnerId(0);
			}
			
			_item.setItemLootShedule(null);
		}
	}
}
