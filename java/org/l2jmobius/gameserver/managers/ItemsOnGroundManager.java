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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.WorldRegion;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.taskmanagers.ItemsAutoDestroyTaskManager;

/**
 * This class manages all {@code Item} instances currently dropped on the ground.<br>
 * It handles the lifecycle and persistence of these items within the game world.
 * @author Enforcer, BazookaRpm
 */
public class ItemsOnGroundManager implements Runnable
{
	private static final Logger LOGGER = Logger.getLogger(ItemsOnGroundManager.class.getName());
	
	private final Set<Item> _items = ConcurrentHashMap.newKeySet();
	
	/**
	 * Initializes the {@code ItemsOnGroundManager}.<br>
	 * It schedules periodic database saves if configured.<br>
	 * It also calls the {@code load()} method to populate items.
	 */
	protected ItemsOnGroundManager()
	{
		if (GeneralConfig.SAVE_DROPPED_ITEM_INTERVAL > 0)
		{
			ThreadPool.scheduleAtFixedRate(this, GeneralConfig.SAVE_DROPPED_ITEM_INTERVAL, GeneralConfig.SAVE_DROPPED_ITEM_INTERVAL);
		}
		
		load();
	}
	
	/**
	 * Loads all items from the database into the game world.<br>
	 * This method checks configuration settings to determine if items should be loaded or cleared.<br>
	 * It also handles updating drop times and registering items with the {@code ItemsAutoDestroyTaskManager}.
	 */
	private void load()
	{
		// If SaveDroppedItem is false, may want to delete all items previously stored to avoid add old items on reactivate.
		if (!GeneralConfig.SAVE_DROPPED_ITEM && GeneralConfig.CLEAR_DROPPED_ITEM_TABLE)
		{
			emptyTable();
		}
		
		if (!GeneralConfig.SAVE_DROPPED_ITEM)
		{
			return;
		}
		
		// If DestroyPlayerDroppedItem was previously false, items currently protected will be added to ItemsAutoDestroy.
		if (GeneralConfig.DESTROY_DROPPED_PLAYER_ITEM)
		{
			final String str;
			if (!GeneralConfig.DESTROY_EQUIPABLE_PLAYER_ITEM)
			{
				// Recycle misc. items only.
				str = "UPDATE itemsonground SET drop_time = ? WHERE drop_time = -1 AND equipable = 0";
			}
			else
			{
				// Recycle all items including equip-able.
				str = "UPDATE itemsonground SET drop_time = ? WHERE drop_time = -1";
			}
			
			try (Connection con = DatabaseFactory.getConnection();
				PreparedStatement ps = con.prepareStatement(str))
			{
				ps.setLong(1, System.currentTimeMillis());
				ps.execute();
			}
			catch (Exception e)
			{
				LOGGER.log(Level.SEVERE, getClass().getSimpleName() + ": Error while updating table ItemsOnGround " + e.getMessage(), e);
			}
		}
		
		// Add items to world.
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT object_id,item_id,count,enchant_level,x,y,z,drop_time,equipable FROM itemsonground"))
		{
			int count = 0;
			try (ResultSet rs = ps.executeQuery())
			{
				Item item;
				while (rs.next())
				{
					item = new Item(rs.getInt(1), rs.getInt(2));
					World.addObject(item);
					
					// this check and..
					if (item.isStackable())
					{
						final long countVal = rs.getLong(3);
						if (countVal > 1)
						{
							item.setCount(countVal);
						}
					}
					
					// This, are really necessary?
					final int enchantLevel = rs.getInt(4);
					if (enchantLevel > 0)
					{
						item.setEnchantLevel(enchantLevel);
					}
					
					item.setXYZ(rs.getInt(5), rs.getInt(6), rs.getInt(7));
					
					final WorldRegion itemRegion = World.getRegion(item);
					item.setWorldRegion(itemRegion);
					itemRegion.addVisibleObject(item);
					
					final long dropTime = rs.getLong(8);
					item.setDropTime(dropTime);
					item.setProtected(dropTime == -1);
					item.setSpawned(true);
					World.addVisibleObject(item, itemRegion);
					_items.add(item);
					count++;
					
					// Add to ItemsAutoDestroy only items not protected.
					if (!GeneralConfig.LIST_PROTECTED_ITEMS.contains(item.getId()) && (dropTime > -1) && (((GeneralConfig.AUTODESTROY_ITEM_AFTER > 0) && !item.getTemplate().hasExImmediateEffect()) || ((GeneralConfig.HERB_AUTO_DESTROY_TIME > 0) && item.getTemplate().hasExImmediateEffect())))
					{
						ItemsAutoDestroyTaskManager.getInstance().addItem(item);
					}
				}
			}
			
			LOGGER.info(getClass().getSimpleName() + ": Loaded " + count + " items.");
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, getClass().getSimpleName() + ": Error while loading ItemsOnGround " + e.getMessage(), e);
		}
		
		if (GeneralConfig.EMPTY_DROPPED_ITEM_TABLE_AFTER_LOAD)
		{
			emptyTable();
		}
	}
	
	/**
	 * Adds an {@code Item} to the set of items on the ground.<br>
	 * This action only occurs if {@code GeneralConfig.SAVE_DROPPED_ITEM} is true.
	 * @param item The {@code Item} object to be saved.
	 */
	public void save(Item item)
	{
		if (GeneralConfig.SAVE_DROPPED_ITEM)
		{
			_items.add(item);
		}
	}
	
	/**
	 * Removes a specific {@code Item} from the manager.<br>
	 * This action only occurs if {@code GeneralConfig.SAVE_DROPPED_ITEM} is set to {@code true}.
	 * @param item The {@code Item} object to be removed.
	 */
	public void removeObject(Item item)
	{
		if (GeneralConfig.SAVE_DROPPED_ITEM)
		{
			_items.remove(item);
		}
	}
	
	/**
	 * Saves all current items to the database.<br>
	 * This method triggers the {@code run()} process.<br>
	 * It ensures that data is persisted correctly.
	 */
	public void saveInDb()
	{
		run();
	}
	
	/**
	 * Clears all items currently managed by this class.<br>
	 * This method empties the internal {@code _items} set.<br>
	 * Use this to reset the state of ground items.
	 */
	public void cleanUp()
	{
		_items.clear();
	}
	
	/**
	 * Deletes all records from the {@code itemsonground} database table.<br>
	 * This method is used to clear out all ground items in the database.<br>
	 * It logs an error if the SQL execution fails.
	 */
	public void emptyTable()
	{
		try (Connection con = DatabaseFactory.getConnection();
			Statement s = con.createStatement())
		{
			s.executeUpdate("DELETE FROM itemsonground");
		}
		catch (Exception e1)
		{
			LOGGER.log(Level.SEVERE, getClass().getSimpleName() + ": Error while cleaning table ItemsOnGround " + e1.getMessage(), e1);
		}
	}
	
	@Override
	public synchronized void run()
	{
		if (!GeneralConfig.SAVE_DROPPED_ITEM)
		{
			return;
		}
		
		try (Connection con = DatabaseFactory.getConnection();
			Statement delete = con.createStatement();
			PreparedStatement statement = con.prepareStatement("INSERT INTO itemsonground(object_id,item_id,count,enchant_level,x,y,z,drop_time,equipable) VALUES(?,?,?,?,?,?,?,?,?)"))
		{
			// The pool rolls back and restores auto-commit on close when the commit is never reached.
			con.setAutoCommit(false);
			delete.executeUpdate("DELETE FROM itemsonground");
			for (Item item : _items)
			{
				if (item == null)
				{
					continue;
				}
				
				final int itemId = item.getId();
				if (CursedWeaponsManager.getInstance().isCursed(itemId))
				{
					continue; // Cursed Items not saved to ground, prevent double save.
				}
				
				try
				{
					statement.setInt(1, item.getObjectId());
					statement.setInt(2, itemId);
					statement.setLong(3, item.getCount());
					statement.setInt(4, item.getEnchantLevel());
					statement.setInt(5, item.getX());
					statement.setInt(6, item.getY());
					statement.setInt(7, item.getZ());
					statement.setLong(8, (item.isProtected() ? -1 : item.getDropTime())); // Item is protected or AutoDestroyed.
					statement.setLong(9, (item.isEquipable() ? 1 : 0)); // set equip-able
					statement.execute();
					statement.clearParameters();
				}
				catch (Exception e)
				{
					LOGGER.log(Level.SEVERE, getClass().getSimpleName() + ": Error while inserting into table ItemsOnGround: " + e.getMessage(), e);
				}
			}
			
			con.commit();
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.SEVERE, getClass().getSimpleName() + ": SQL error while storing items on ground: " + e.getMessage(), e);
		}
	}
	
	/**
	 * Retrieves the singleton instance of the {@code ItemsOnGroundManager}.<br>
	 * Use this method to access the manager from anywhere in the code.
	 * @return The global instance of {@code ItemsOnGroundManager}.
	 */
	public static ItemsOnGroundManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final ItemsOnGroundManager INSTANCE = new ItemsOnGroundManager();
	}
}
