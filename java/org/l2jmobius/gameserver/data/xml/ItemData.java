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
package org.l2jmobius.gameserver.data.xml;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Logger;

import org.l2jmobius.commons.config.ThreadConfig;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.ServerConfig;
import org.l2jmobius.gameserver.entity.item.Armor;
import org.l2jmobius.gameserver.entity.item.EtcItem;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.entity.item.Weapon;
import org.l2jmobius.gameserver.entity.item.holders.ItemSkillHolder;
import org.l2jmobius.gameserver.entity.item.type.EtcItemType;
import org.l2jmobius.gameserver.entity.item.type.ItemType;
import org.l2jmobius.gameserver.mechanics.skill.AmmunitionSkillList;
import org.l2jmobius.gameserver.util.DocumentItem;

/**
 * This class serves as a container for all item templates in the game.<br>
 * It manages the collection of {@code ItemTemplate} objects loaded from XML data.
 */
public class ItemData
{
	private static final Logger LOGGER = Logger.getLogger(ItemData.class.getName());
	
	private ItemTemplate[] _allTemplates;
	private final Map<Integer, EtcItem> _etcItems = new HashMap<>();
	private final Map<Integer, Armor> _armors = new HashMap<>();
	private final Map<Integer, Weapon> _weapons = new HashMap<>();
	private final List<File> _itemFiles = new ArrayList<>();
	
	/**
	 * Initializes the {@code ItemData} instance.<br>
	 * This constructor loads all item templates from the default and custom directories.<br>
	 * It triggers the internal {@code load()} process to populate the data maps.
	 */
	protected ItemData()
	{
		processDirectory("data/stats/items", _itemFiles);
		if (GeneralConfig.CUSTOM_ITEMS_LOAD)
		{
			processDirectory("data/stats/items/custom", _itemFiles);
		}
		
		load();
	}
	
	/**
	 * Scans a specific directory for XML files.<br>
	 * Adds valid files to the provided list.<br>
	 * Logs a warning if the directory is missing.
	 * @param dirName The name of the directory to scan.
	 * @param list The list where found {@code File} objects will be added.
	 */
	private void processDirectory(String dirName, List<File> list)
	{
		final File dir = new File(ServerConfig.DATAPACK_ROOT, dirName);
		if (!dir.exists())
		{
			LOGGER.warning("Directory " + dir.getAbsolutePath() + " does not exist.");
			return;
		}
		
		final File[] files = dir.listFiles();
		if (files != null)
		{
			for (File file : files)
			{
				if (file.isFile() && file.getName().toLowerCase().endsWith(".xml"))
				{
					list.add(file);
				}
			}
		}
	}
	
	/**
	 * Loads all item templates from the files listed in {@code _itemFiles}.<br>
	 * It clears existing collections and parses data into categories.<br>
	 * The method supports multithreaded loading based on {@code ThreadConfig.THREADS_FOR_LOADING}.<br>
	 * Finally, it populates a fast lookup array using the highest item ID found.
	 */
	private void load()
	{
		final Collection<ItemTemplate> items = ConcurrentHashMap.newKeySet();
		int highestId = 0;
		_armors.clear();
		_etcItems.clear();
		_weapons.clear();
		
		// If multithreading is enabled, use a thread pool to parse files.
		if (ThreadConfig.THREADS_FOR_LOADING)
		{
			final Collection<ScheduledFuture<?>> tasks = ConcurrentHashMap.newKeySet();
			for (File file : _itemFiles)
			{
				tasks.add(ThreadPool.schedule(() ->
				{
					final DocumentItem document = new DocumentItem(file);
					document.parse();
					items.addAll(document.getItemList());
				}, 0));
			}
			
			// Wait for all scheduled tasks to complete.
			while (!tasks.isEmpty())
			{
				for (ScheduledFuture<?> task : tasks)
				{
					if ((task == null) || task.isDone() || task.isCancelled())
					{
						tasks.remove(task);
					}
				}
			}
		}
		else // Parse files sequentially if multithreading is not enabled.
		{
			for (File file : _itemFiles)
			{
				final DocumentItem document = new DocumentItem(file);
				document.parse();
				items.addAll(document.getItemList());
			}
		}
		
		// Process each loaded item and organize them into their respective collections.
		for (ItemTemplate item : items)
		{
			final Integer itemId = item.getId();
			if (highestId < itemId)
			{
				highestId = itemId;
			}
			
			if (item instanceof EtcItem)
			{
				_etcItems.put(itemId, (EtcItem) item);
				
				final ItemType itemType = item.getItemType();
				if ((itemType == EtcItemType.ARROW) || (itemType == EtcItemType.BOLT))
				{
					final List<ItemSkillHolder> skills = item.getAllSkills();
					if (skills != null)
					{
						AmmunitionSkillList.add(skills);
					}
				}
			}
			else if (item instanceof Armor)
			{
				_armors.put(itemId, (Armor) item);
			}
			else
			{
				_weapons.put(itemId, (Weapon) item);
			}
		}
		
		// Log the highest item ID and create a fast lookup array based on this value.
		final String simpleName = getClass().getSimpleName();
		LOGGER.info(simpleName + ": Highest item id used: " + highestId);
		_allTemplates = new ItemTemplate[highestId + 1];
		
		// Populate the fast lookup table with items from each category.
		for (Armor item : _armors.values())
		{
			_allTemplates[item.getId()] = item;
		}
		
		for (Weapon item : _weapons.values())
		{
			_allTemplates[item.getId()] = item;
		}
		
		for (EtcItem item : _etcItems.values())
		{
			_allTemplates[item.getId()] = item;
		}
		
		final int etcSize = _etcItems.size();
		final int armorSize = _armors.size();
		final int weaponSize = _weapons.size();
		LOGGER.info(simpleName + ": Loaded " + etcSize + " etc items.");
		LOGGER.info(simpleName + ": Loaded " + armorSize + " armor items.");
		LOGGER.info(simpleName + ": Loaded " + weaponSize + " weapon items.");
		LOGGER.info(simpleName + ": Loaded " + (etcSize + armorSize + weaponSize) + " items in total.");
	}
	
	/**
	 * Reloads the item data from the configuration files.<br>
	 * This method calls {@code load()} to refresh all templates.<br>
	 * It also refreshes the {@code EnchantItemHPBonusData} instance.
	 */
	public void reload()
	{
		load();
		EnchantItemHPBonusData.getInstance().load();
	}
	
	/**
	 * Retrieves an {@code ItemTemplate} based on its unique identifier.<br>
	 * This method checks if the provided {@code id} is within the valid range of the internal array.<br>
	 * It returns {@code null} if the ID is out of bounds.
	 * @param id The unique integer ID of the item template to retrieve.
	 * @return The corresponding {@code ItemTemplate} object, or {@code null} if not found.
	 */
	public ItemTemplate getTemplate(int id)
	{
		if ((id >= _allTemplates.length) || (id < 0))
		{
			return null;
		}
		
		return _allTemplates[id];
	}
	
	/**
	 * Retrieves all unique IDs for armor items.<br>
	 * This method returns the keys from the internal armor map.
	 * @return a {@code Set<Integer>} containing every armor ID.
	 */
	public Set<Integer> getAllArmorsId()
	{
		return _armors.keySet();
	}
	
	/**
	 * Retrieves all armor items from the data container.<br>
	 * This method returns a collection of {@code Armor} objects.
	 * @return A {@code Collection} containing all loaded {@code Armor} instances.
	 */
	public Collection<Armor> getAllArmors()
	{
		return _armors.values();
	}
	
	/**
	 * Retrieves all unique IDs for weapons.<br>
	 * This method returns the keys from the internal weapon map.
	 * @return A {@code Set<Integer>} containing every weapon ID.
	 */
	public Set<Integer> getAllWeaponsId()
	{
		return _weapons.keySet();
	}
	
	/**
	 * Retrieves all weapon templates from the data. <br>
	 * This method returns a collection of {@code Weapon} objects.
	 * @return A {@code Collection} containing all loaded {@code Weapon} items.
	 */
	public Collection<Weapon> getAllWeapons()
	{
		return _weapons.values();
	}
	
	/**
	 * Retrieves all unique IDs for items categorized as {@code EtcItem}.<br>
	 * This method returns the keys from the internal collection of miscellaneous items.
	 * @return A {@code Set} containing all {@code Integer} IDs for {@code EtcItem} objects.
	 */
	public Set<Integer> getAllEtcItemsId()
	{
		return _etcItems.keySet();
	}
	
	/**
	 * Retrieves all items categorized as {@code EtcItem}.<br>
	 * This method returns the values stored in the internal map.
	 * @return A {@code Collection} of all {@code EtcItem} objects.
	 */
	public Collection<EtcItem> getAllEtcItems()
	{
		return _etcItems.values();
	}
	
	/**
	 * Retrieves all loaded item templates.<br>
	 * This method returns the internal array of {@code ItemTemplate} objects.
	 * @return an array containing every {@code ItemTemplate} in the game.
	 */
	public ItemTemplate[] getAllItems()
	{
		return _allTemplates;
	}
	
	/**
	 * Returns the total number of items in the template array.<br>
	 * This value corresponds to the length of {@code _allTemplates}.
	 * @return The size of the item template array as an {@code int}.
	 */
	public int getArraySize()
	{
		return _allTemplates.length;
	}
	
	/**
	 * Retrieves the singleton instance of {@code ItemData}.<br>
	 * This method provides global access to the item data container.
	 * @return The single instance of {@code ItemData}.
	 */
	public static ItemData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final ItemData INSTANCE = new ItemData();
	}
}
