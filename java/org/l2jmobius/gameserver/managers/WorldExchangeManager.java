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

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.w3c.dom.Document;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.config.WorldExchangeConfig;
import org.l2jmobius.gameserver.config.custom.MultilingualSupportConfig;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.enums.ItemLocation;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.enums.WorldExchangeItemStatusType;
import org.l2jmobius.gameserver.entity.item.enums.WorldExchangeItemSubType;
import org.l2jmobius.gameserver.entity.item.enums.WorldExchangeSortType;
import org.l2jmobius.gameserver.entity.item.holders.ItemInfo;
import org.l2jmobius.gameserver.entity.item.holders.WorldExchangeHolder;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.itemcontainer.Inventory;
import org.l2jmobius.gameserver.mechanics.options.AugmentationInstance;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.network.serverpackets.worldexchange.WorldExchangeBuyItem;
import org.l2jmobius.gameserver.network.serverpackets.worldexchange.WorldExchangeInfo;
import org.l2jmobius.gameserver.network.serverpackets.worldexchange.WorldExchangeRegisterItem;
import org.l2jmobius.gameserver.network.serverpackets.worldexchange.WorldExchangeSellCompleteAlarm;
import org.l2jmobius.gameserver.network.serverpackets.worldexchange.WorldExchangeSettleList;
import org.l2jmobius.gameserver.network.serverpackets.worldexchange.WorldExchangeSettleRecvResult;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Manages the global world exchange system for trading items between players.<br>
 * This class handles item registration, listing, and settlement processes.<br>
 * It coordinates interactions between {@code Player} entities and the database.
 * @author Index
 */
public class WorldExchangeManager implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(WorldExchangeManager.class.getName());
	
	private static final String SELECT_ALL_ITEMS = "SELECT * FROM `items` WHERE `loc`=?";
	private static final String RESTORE_INFO = "SELECT * FROM world_exchange_items";
	private static final String INSERT_WORLD_EXCHANGE = "REPLACE INTO world_exchange_items (`world_exchange_id`, `item_object_id`, `item_status`, `category_id`, `price`, `we_type`, `old_owner_id`, `start_time`, `end_time`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
	private static final String CHECK_WORLD_EXCHANGE_TYPE = "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'world_exchange_items' AND COLUMN_NAME = 'we_type'";
	private static final String ADD_WORLD_EXCHANGE_TYPE = "ALTER TABLE `world_exchange_items` ADD COLUMN `we_type` TINYINT NOT NULL DEFAULT 0 AFTER `price`"; // Protocol 557.
	private static final int WORLD_EXCHANGE_TYPE_LCOIN = 0;
	
	private final Map<Long, WorldExchangeHolder> _itemBids = new ConcurrentHashMap<>();
	private final Map<Integer, WorldExchangeItemSubType> _itemCategories = new ConcurrentHashMap<>();
	private final Map<String, Map<Integer, String>> _localItemNames = new HashMap<>(new HashMap<>());
	private long _lastWorldExchangeId = 0;
	
	private ScheduledFuture<?> _checkStatus = null;
	
	/**
	 * Constructs a new {@code WorldExchangeManager}.<br>
	 * This constructor initializes the manager and starts the background task for checking bid statuses.<br>
	 * It only proceeds if world exchange is enabled in the configuration.
	 */
	public WorldExchangeManager()
	{
		if (!WorldExchangeConfig.ENABLE_WORLD_EXCHANGE)
		{
			return;
		}
		
		firstLoad();
		if (_checkStatus == null)
		{
			_checkStatus = ThreadPool.scheduleAtFixedRate(this::checkBidStatus, WorldExchangeConfig.WORLD_EXCHANGE_SAVE_INTERVAL, WorldExchangeConfig.WORLD_EXCHANGE_SAVE_INTERVAL);
		}
	}
	
	/**
	 * Loads the item name localizations from the data files.<br>
	 * This method checks if multilingual support is enabled.<br>
	 * It parses the required XML files for all allowed languages.<br>
	 * It also ensures the default language is loaded if it is missing.
	 */
	@Override
	public void load()
	{
		if (MultilingualSupportConfig.MULTILANG_ENABLE)
		{
			_localItemNames.clear();
			for (String lang : MultilingualSupportConfig.MULTILANG_ALLOWED)
			{
				final File file = new File("data/lang/" + lang + "/ItemNameLocalisation.xml");
				if (!file.isFile())
				{
					continue;
				}
				
				parseDatapackFile("data/lang/" + lang + "/ItemNameLocalisation.xml");
				final int size = _localItemNames.get(lang).size();
				if (size == 0)
				{
					_localItemNames.remove(lang);
				}
				else
				{
					LOGGER.log(Level.INFO, getClass().getSimpleName() + ": Loaded ItemName localisations for [" + lang + "].");
				}
			}
		}
		
		if (!MultilingualSupportConfig.MULTILANG_DEFAULT.equals(WorldExchangeConfig.WORLD_EXCHANGE_DEFAULT_LANG) && !_localItemNames.containsKey(WorldExchangeConfig.WORLD_EXCHANGE_DEFAULT_LANG))
		{
			parseDatapackFile("data/lang/" + WorldExchangeConfig.WORLD_EXCHANGE_DEFAULT_LANG + "/ItemNameLocalisation.xml");
		}
	}
	
	/**
	 * This method parses a {@code Document} to extract item names.<br>
	 * It maps item IDs to their corresponding names from the XML structure.<br>
	 * The results are stored in the internal language map based on the file path.
	 * @param document The {@code Document} object containing the item data.
	 * @param file The {@code File} representing the source of the configuration.
	 */
	@Override
	public void parseDocument(Document document, File file)
	{
		final ConcurrentHashMap<Integer, String> local = new ConcurrentHashMap<>();
		forEach(document, "list", listNode ->
		{
			forEach(listNode, "item", itemNode ->
			{
				final StatSet itemSet = new StatSet(parseAttributes(itemNode));
				local.put(itemSet.getInt("id"), itemSet.getString("name"));
			});
		});
		_localItemNames.put(document.getDocumentURI().split("data/lang/")[1].split("/")[0], local);
	}
	
	/**
	 * Retrieves the localized names for items based on a specific language.<br>
	 * This method looks up the translation map using the provided {@code lang} code.
	 * @param lang The language code to use for localization.
	 * @return A {@code Map} where the key is the item ID and the value is its localized name.
	 */
	public Map<Integer, String> getItemLocalByLang(String lang)
	{
		return _localItemNames.get(lang);
	}
	
	/**
	 * Performs the initial loading sequence for the manager.<br>
	 * This method loads item instances and bids before calling {@code load()}.
	 */
	synchronized void firstLoad()
	{
		ensureDatabaseSchema();
		final Map<Integer, Item> itemInstances = loadItemInstances();
		loadItemBids(itemInstances);
		load();
	}
	
	/**
	 * Verifies that the database has the required tables.<br>
	 * Creates the necessary schema if it is missing.<br>
	 * Logs a warning if an {@code SQLException} occurs during this process.
	 */
	private void ensureDatabaseSchema()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement check = con.prepareStatement(CHECK_WORLD_EXCHANGE_TYPE))
		{
			try (ResultSet rs = check.executeQuery())
			{
				if (rs.next() && (rs.getInt(1) == 0))
				{
					try (PreparedStatement ps = con.prepareStatement(ADD_WORLD_EXCHANGE_TYPE))
					{
						ps.executeUpdate();
					}
				}
			}
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Failed updating world exchange schema.", e);
		}
	}
	
	/**
	 * Periodically checks the status of all active bids.<br>
	 * It identifies expired items and updates their statuses based on configuration.<br>
	 * Items that are sold or timed out are removed from the exchange and updated in the database.
	 */
	private void checkBidStatus()
	{
		if (!WorldExchangeConfig.ENABLE_WORLD_EXCHANGE)
		{
			return;
		}
		
		for (Entry<Long, WorldExchangeHolder> entry : _itemBids.entrySet())
		{
			final WorldExchangeHolder holder = entry.getValue();
			final long currentTime = System.currentTimeMillis();
			final long endTime = holder.getEndTime();
			if (endTime > currentTime)
			{
				continue;
			}
			
			switch (holder.getStoreType())
			{
				case WORLD_EXCHANGE_NONE:
				{
					_itemBids.remove(entry.getKey());
					continue;
				}
				case WORLD_EXCHANGE_REGISTERED:
				{
					holder.setEndTime(calculateDate(WorldExchangeConfig.WORLD_EXCHANGE_ITEM_BACK_PERIOD));
					holder.setStoreType(WorldExchangeItemStatusType.WORLD_EXCHANGE_OUT_TIME);
					_itemBids.replace(entry.getKey(), holder);
					insert(entry.getKey(), false);
					break;
				}
				case WORLD_EXCHANGE_SOLD:
				case WORLD_EXCHANGE_OUT_TIME:
				{
					holder.setStoreType(WorldExchangeItemStatusType.WORLD_EXCHANGE_NONE);
					insert(entry.getKey(), true);
					Item item = holder.getItemInstance();
					item.setItemLocation(ItemLocation.VOID);
					item.updateDatabase(!WorldExchangeConfig.WORLD_EXCHANGE_LAZY_UPDATE);
					break;
				}
			}
		}
	}
	
	/**
	 * Loads all item instances from the database for the world exchange.<br>
	 * This method checks if the world exchange is enabled before querying.<br>
	 * It returns an empty map if the feature is disabled or if a database error occurs.
	 * @return A {@code Map} containing the object IDs and their corresponding {@code Item} instances.
	 */
	synchronized Map<Integer, Item> loadItemInstances()
	{
		if (!WorldExchangeConfig.ENABLE_WORLD_EXCHANGE)
		{
			return Collections.emptyMap();
		}
		
		final Map<Integer, Item> itemInstances = new HashMap<>();
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(SELECT_ALL_ITEMS))
		{
			ps.setString(1, ItemLocation.EXCHANGE.name());
			
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					final Item itemInstance = new Item(rs);
					itemInstances.put(itemInstance.getObjectId(), itemInstance);
				}
			}
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Failed loading items instances.", e);
		}
		
		return itemInstances;
	}
	
	/**
	 * Loads active bids from the database into memory.<br>
	 * This method updates the status of world exchange items and populates the {@code _itemBids} map.<br>
	 * It ensures that expired items are handled according to the configuration settings.
	 * @param itemInstances A map containing the {@code Item} instances indexed by their object IDs.
	 */
	private synchronized void loadItemBids(Map<Integer, Item> itemInstances)
	{
		if (!WorldExchangeConfig.ENABLE_WORLD_EXCHANGE)
		{
			return;
		}
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(RESTORE_INFO);
			ResultSet rs = ps.executeQuery())
		{
			while (rs.next())
			{
				boolean needChange = false;
				final long worldExchangeId = rs.getLong("world_exchange_id");
				_lastWorldExchangeId = Math.max(worldExchangeId, _lastWorldExchangeId);
				WorldExchangeItemStatusType storeType = WorldExchangeItemStatusType.getWorldExchangeItemStatusType(rs.getInt("item_status"));
				if (storeType == WorldExchangeItemStatusType.WORLD_EXCHANGE_NONE)
				{
					continue;
				}
				
				final Item itemInstance = itemInstances.get(rs.getInt("item_object_id"));
				if (itemInstance == null)
				{
					LOGGER.warning(getClass().getSimpleName() + ": Failed loading commission item with world exchange id " + worldExchangeId + " because item instance does not exist or failed to load.");
					continue;
				}
				
				final WorldExchangeItemSubType categoryId = WorldExchangeItemSubType.getWorldExchangeItemSubType(rs.getInt("category_id"));
				final long price = rs.getLong("price");
				final int worldExchangeType = rs.getInt("we_type");
				final int bidPlayerObjectId = rs.getInt("old_owner_id");
				final long startTime = rs.getLong("start_time");
				long endTime = rs.getLong("end_time");
				if (endTime < System.currentTimeMillis())
				{
					if ((storeType == WorldExchangeItemStatusType.WORLD_EXCHANGE_OUT_TIME) || (storeType == WorldExchangeItemStatusType.WORLD_EXCHANGE_SOLD))
					{
						itemInstance.setItemLocation(ItemLocation.VOID);
						itemInstance.updateDatabase(true);
						continue;
					}
					
					endTime = calculateDate(WorldExchangeConfig.WORLD_EXCHANGE_ITEM_BACK_PERIOD);
					storeType = WorldExchangeItemStatusType.WORLD_EXCHANGE_OUT_TIME;
					needChange = true;
				}
				
				_itemBids.put(worldExchangeId, new WorldExchangeHolder(worldExchangeId, itemInstance, new ItemInfo(itemInstance), price, worldExchangeType, bidPlayerObjectId, storeType, categoryId, startTime, endTime, needChange));
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Failed loading bid items.", e);
		}
	}
	
	/**
	 * Calculates the registration fee for a world exchange bid.<br>
	 * This method determines the cost based on the exchange type and configuration settings.
	 * @param player The {@code Player} performing the action.
	 * @param objectId The unique identifier of the item being registered.
	 * @param amount The quantity of the item.
	 * @param priceForEach The price assigned to each individual unit of the item.
	 * @param weType The type of world exchange being used.
	 * @return The calculated fee as a {@code long}.
	 */
	private long calculateFeeForRegister(Player player, int objectId, long amount, long priceForEach, int weType)
	{
		if (isLcoinExchangeType(weType))
		{
			return Math.round(priceForEach * WorldExchangeConfig.WORLD_EXCHANGE_ADENA_FEE);
		}
		
		return Math.max(1, Math.round(priceForEach * WorldExchangeConfig.WORLD_EXCHANGE_LCOIN_TAX));
	}
	
	/**
	 * Checks if the given exchange type is an {@code Lcoin} exchange.
	 * @param weType The exchange type to check.
	 * @return {@code true} if it matches {@code WORLD_EXCHANGE_TYPE_LCOIN}, otherwise {@code false}.
	 */
	private boolean isLcoinExchangeType(int weType)
	{
		return weType == WORLD_EXCHANGE_TYPE_LCOIN;
	}
	
	/**
	 * Determines the currency ID based on the exchange type.<br>
	 * It checks if the type is an Lcoin exchange.<br>
	 * If it is, it returns the {@code EINHASAD_COIN_ID}.<br>
	 * Otherwise, it returns the {@code ADENA_ID}.
	 * @param weType The type of the world exchange.
	 * @return The integer ID of the currency used for the exchange.
	 */
	private int getExchangeCurrencyId(int weType)
	{
		return isLcoinExchangeType(weType) ? Inventory.EINHASAD_COIN_ID : Inventory.ADENA_ID;
	}
	
	/**
	 * Registers a new item bid in the world exchange.<br>
	 * Validates player inventory and checks for sufficient adena to pay fees.<br>
	 * Removes the item from the player's inventory upon successful registration.
	 * @param player The {@code Player} attempting to register the bid.
	 * @param itemObjectId The unique identifier of the item being registered.
	 * @param amount The quantity of the item to register.
	 * @param priceForEach The price per unit for the item.
	 * @param weType The type of world exchange used for this registration.
	 */
	public synchronized void registerItemBid(Player player, int itemObjectId, long amount, long priceForEach, int weType)
	{
		if (!WorldExchangeConfig.ENABLE_WORLD_EXCHANGE)
		{
			return;
		}
		
		final int objectId = player.getObjectId();
		final Map<WorldExchangeItemStatusType, List<WorldExchangeHolder>> playerBids = getPlayerBids(objectId);
		if (playerBids.size() >= 10)
		{
			player.sendPacket(new SystemMessage(SystemMessageId.NO_SLOTS_AVAILABLE));
			player.sendPacket(WorldExchangeRegisterItem.FAIL);
			return;
		}
		
		final Item item = player.getInventory().getItemByObjectId(itemObjectId);
		if (item == null)
		{
			player.sendPacket(new SystemMessage(SystemMessageId.NO_ITEM_FOUND));
			player.sendPacket(WorldExchangeRegisterItem.FAIL);
			return;
		}
		
		if ((amount < 1) || (priceForEach < 1) || ((amount * priceForEach) < 1))
		{
			player.sendPacket(new SystemMessage(SystemMessageId.INCORRECT_ITEM_COUNT_2));
			player.sendPacket(WorldExchangeRegisterItem.FAIL);
			return;
		}
		
		long feePrice = calculateFeeForRegister(player, itemObjectId, amount, priceForEach, weType);
		if ((WorldExchangeConfig.WORLD_EXCHANGE_MAX_ADENA_FEE != -1) && (feePrice > WorldExchangeConfig.WORLD_EXCHANGE_MAX_ADENA_FEE))
		{
			feePrice = WorldExchangeConfig.WORLD_EXCHANGE_MAX_ADENA_FEE;
		}
		
		if (feePrice > player.getAdena())
		{
			player.sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_ADENA));
			player.sendPacket(WorldExchangeRegisterItem.FAIL);
			return;
		}
		
		if (feePrice < 1)
		{
			player.sendPacket(new SystemMessage(SystemMessageId.INCORRECT_ITEM_COUNT_2));
			player.sendPacket(WorldExchangeRegisterItem.FAIL);
			return;
		}
		
		final long freeId = getNextId();
		final InventoryUpdate iu = new InventoryUpdate();
		if (item.isStackable() && (player.getInventory().getInventoryItemCount(item.getId(), -1) > amount))
		{
			iu.addModifiedItem(item);
		}
		else
		{
			iu.addRemovedItem(item);
		}
		
		final Item itemInstance = player.getInventory().detachItem(ItemProcessType.TRANSFER, item, amount, ItemLocation.EXCHANGE, player, null);
		if (itemInstance == null)
		{
			player.sendPacket(new SystemMessage(SystemMessageId.NO_ITEM_FOUND));
			player.sendPacket(WorldExchangeRegisterItem.FAIL);
			return;
		}
		
		final WorldExchangeItemSubType category = _itemCategories.get(itemInstance.getId());
		if (category == null)
		{
			player.sendPacket(new SystemMessage(SystemMessageId.THE_ITEM_YOU_REGISTERED_HAS_BEEN_SOLD));
			player.sendPacket(WorldExchangeRegisterItem.FAIL);
			return;
		}
		
		player.sendInventoryUpdate(iu);
		player.getInventory().reduceAdena(ItemProcessType.FEE, feePrice, player, null);
		final long endTime = calculateDate(WorldExchangeConfig.WORLD_EXCHANGE_ITEM_SELL_PERIOD);
		_itemBids.put(freeId, new WorldExchangeHolder(freeId, itemInstance, new ItemInfo(itemInstance), priceForEach, weType, objectId, WorldExchangeItemStatusType.WORLD_EXCHANGE_REGISTERED, category, System.currentTimeMillis(), endTime, true));
		player.sendPacket(new WorldExchangeInfo(weType));
		player.sendPacket(new WorldExchangeRegisterItem(itemObjectId, amount, 1, weType));
		if (!WorldExchangeConfig.WORLD_EXCHANGE_LAZY_UPDATE)
		{
			insert(freeId, false);
		}
	}
	
	/**
	 * Generates a unique identifier for the next exchange entry.<br>
	 * This method increments the internal counter safely.
	 * @return The next available {@code long} ID.
	 */
	private synchronized long getNextId()
	{
		return _lastWorldExchangeId++;
	}
	
	/**
	 * Calculates a timestamp based on the current date.<br>
	 * It adds the specified number of days to the current time.
	 * @param days The number of days to add to the current date.
	 * @return The resulting time in milliseconds as a {@code long}.
	 */
	private long calculateDate(int days)
	{
		final Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.DATE, days);
		return calendar.getTimeInMillis();
	}
	
	/**
	 * Checks the status of a specific world exchange item and performs the corresponding action.<br>
	 * This method handles logic for registered, sold, or expired items based on their current state.<br>
	 * It ensures that actions like canceling bids or returning items are executed correctly.
	 * @param player The {@code Player} who is interacting with the world exchange.
	 * @param worldExchangeIndex The unique identifier of the item in the world exchange.
	 * @param weType The type of the world exchange transaction.
	 */
	public synchronized void getItemStatusAndMakeAction(Player player, long worldExchangeIndex, int weType)
	{
		if (!WorldExchangeConfig.ENABLE_WORLD_EXCHANGE)
		{
			return;
		}
		
		final WorldExchangeHolder worldExchangeItem = _itemBids.get(worldExchangeIndex);
		if (worldExchangeItem == null)
		{
			player.sendPacket(new SystemMessage(SystemMessageId.NO_ITEM_FOUND));
			player.sendPacket(WorldExchangeSettleRecvResult.FAIL);
			return;
		}
		
		final WorldExchangeItemStatusType storeType = worldExchangeItem.getStoreType();
		switch (storeType)
		{
			case WORLD_EXCHANGE_REGISTERED:
			{
				cancelBid(player, worldExchangeItem, weType);
				break;
			}
			case WORLD_EXCHANGE_SOLD:
			{
				takeBidMoney(player, worldExchangeItem, weType);
				break;
			}
			case WORLD_EXCHANGE_OUT_TIME:
			{
				returnItem(player, worldExchangeItem, weType);
				break;
			}
		}
	}
	
	/**
	 * Cancels an active bid for a specific world exchange item.<br>
	 * This method validates the ownership and status of the item before processing.<br>
	 * It returns the item to the original owner if all conditions are met.
	 * @param player The {@code Player} who is performing the action.
	 * @param worldExchangeItem The {@code WorldExchangeHolder} representing the item being bid on.
	 * @param weType The type of the world exchange.
	 */
	private void cancelBid(Player player, WorldExchangeHolder worldExchangeItem, int weType)
	{
		if (!WorldExchangeConfig.ENABLE_WORLD_EXCHANGE)
		{
			return;
		}
		
		if (worldExchangeItem.getStoreType() == WorldExchangeItemStatusType.WORLD_EXCHANGE_NONE)
		{
			player.sendPacket(new WorldExchangeSettleList(player, weType));
			player.sendPacket(new SystemMessage(SystemMessageId.NO_ITEM_FOUND));
			player.sendPacket(WorldExchangeSettleRecvResult.FAIL);
			return;
		}
		
		final long worldExchangeId = worldExchangeItem.getWorldExchangeId();
		if (!_itemBids.containsKey(worldExchangeId))
		{
			player.sendPacket(new WorldExchangeSettleList(player, weType));
			player.sendPacket(new SystemMessage(SystemMessageId.NO_ITEM_FOUND));
			player.sendPacket(WorldExchangeSettleRecvResult.FAIL);
			return;
		}
		
		if (_itemBids.get(worldExchangeId) != worldExchangeItem)
		{
			player.sendPacket(new WorldExchangeSettleList(player, weType));
			player.sendPacket(new SystemMessage(SystemMessageId.NO_ITEM_FOUND));
			player.sendPacket(WorldExchangeSettleRecvResult.FAIL);
			return;
		}
		
		if (player.getObjectId() != worldExchangeItem.getOldOwnerId())
		{
			player.sendPacket(new SystemMessage(SystemMessageId.ITEM_OUT_OF_STOCK));
			player.sendPacket(WorldExchangeSettleRecvResult.FAIL);
			return;
		}
		
		if (worldExchangeItem.getStoreType() == WorldExchangeItemStatusType.WORLD_EXCHANGE_SOLD)
		{
			player.sendPacket(new WorldExchangeSettleList(player, weType));
			player.sendPacket(new SystemMessage(SystemMessageId.THE_ITEM_YOU_REGISTERED_HAS_BEEN_SOLD));
			player.sendPacket(WorldExchangeSettleRecvResult.FAIL);
			return;
		}
		
		final Item itemInstance = worldExchangeItem.getItemInstance();
		player.sendPacket(new WorldExchangeSettleRecvResult(itemInstance.getObjectId(), itemInstance.getCount(), 1, weType));
		player.getInventory().addItem(ItemProcessType.TRANSFER, worldExchangeItem.getItemInstance(), player, player);
		player.sendPacket(new WorldExchangeInfo(weType));
		worldExchangeItem.setStoreType(WorldExchangeItemStatusType.WORLD_EXCHANGE_NONE);
		worldExchangeItem.setHasChanges(true);
		_itemBids.replace(worldExchangeId, worldExchangeItem);
		player.sendPacket(new WorldExchangeSettleList(player, weType));
		if (!WorldExchangeConfig.WORLD_EXCHANGE_LAZY_UPDATE)
		{
			insert(worldExchangeId, true);
		}
	}
	
	/**
	 * Processes the final payment and item delivery for a world exchange bid.<br>
	 * This method validates the auction status, owner, and expiration time.<br>
	 * It calculates the tax fee and adds the remaining currency to the player's inventory.<br>
	 * Finally, it updates the item status and removes it from the active bids list.
	 * @param player The {@code Player} who is receiving the items and currency.
	 * @param worldExchangeItem The {@code WorldExchangeHolder} representing the specific auction item.
	 * @param weType The type of the world exchange.
	 */
	private void takeBidMoney(Player player, WorldExchangeHolder worldExchangeItem, int weType)
	{
		if (!WorldExchangeConfig.ENABLE_WORLD_EXCHANGE)
		{
			return;
		}
		
		final WorldExchangeItemStatusType storeType = worldExchangeItem.getStoreType();
		if (storeType == WorldExchangeItemStatusType.WORLD_EXCHANGE_NONE)
		{
			player.sendPacket(new WorldExchangeSettleList(player, weType));
			player.sendPacket(new SystemMessage(SystemMessageId.NO_ITEM_FOUND));
			player.sendPacket(WorldExchangeSettleRecvResult.FAIL);
			return;
		}
		
		final long worldExchangeId = worldExchangeItem.getWorldExchangeId();
		if (!_itemBids.containsKey(worldExchangeId))
		{
			player.sendPacket(new WorldExchangeSettleList(player, weType));
			player.sendPacket(new SystemMessage(SystemMessageId.NO_ITEM_FOUND));
			player.sendPacket(WorldExchangeSettleRecvResult.FAIL);
			return;
		}
		
		if (_itemBids.get(worldExchangeId) != worldExchangeItem)
		{
			player.sendPacket(new WorldExchangeSettleList(player, weType));
			player.sendPacket(new SystemMessage(SystemMessageId.NO_ITEM_FOUND));
			player.sendPacket(WorldExchangeSettleRecvResult.FAIL);
			return;
		}
		
		if (player.getObjectId() != worldExchangeItem.getOldOwnerId())
		{
			player.sendPacket(new SystemMessage(SystemMessageId.NO_ITEM_FOUND));
			player.sendPacket(WorldExchangeSettleRecvResult.FAIL);
			return;
		}
		
		if (storeType != WorldExchangeItemStatusType.WORLD_EXCHANGE_SOLD)
		{
			player.sendPacket(new WorldExchangeSettleList(player, weType));
			player.sendPacket(new SystemMessage(SystemMessageId.THE_ITEM_YOU_REGISTERED_HAS_BEEN_SOLD));
			player.sendPacket(WorldExchangeSettleRecvResult.FAIL);
			return;
		}
		
		if (worldExchangeItem.getEndTime() < System.currentTimeMillis())
		{
			player.sendPacket(new WorldExchangeSettleList(player, weType));
			player.sendPacket(new SystemMessage(SystemMessageId.THE_REGISTRATION_PERIOD_FOR_THE_ITEM_YOU_REGISTERED_HAS_EXPIRED));
			player.sendPacket(WorldExchangeSettleRecvResult.FAIL);
			return;
		}
		
		Item item = worldExchangeItem.getItemInstance();
		player.sendPacket(new WorldExchangeSettleRecvResult(item.getObjectId(), item.getCount(), 1, weType));
		final long fee = Math.max(1, Math.round((worldExchangeItem.getPrice() * WorldExchangeConfig.WORLD_EXCHANGE_LCOIN_TAX * 100) / 100));
		final long returnPrice = worldExchangeItem.getPrice() - Math.min(fee, (WorldExchangeConfig.WORLD_EXCHANGE_MAX_LCOIN_TAX != -1 ? WorldExchangeConfig.WORLD_EXCHANGE_MAX_LCOIN_TAX : Long.MAX_VALUE)); // floating-point accuracy workaround :D
		player.getInventory().addItem(ItemProcessType.FEE, getExchangeCurrencyId(worldExchangeItem.getWorldExchangeType()), (returnPrice), player, null);
		player.sendPacket(new WorldExchangeInfo(weType));
		worldExchangeItem.setStoreType(WorldExchangeItemStatusType.WORLD_EXCHANGE_NONE);
		item.setItemLocation(ItemLocation.VOID);
		item.updateDatabase(!WorldExchangeConfig.WORLD_EXCHANGE_LAZY_UPDATE);
		worldExchangeItem.setHasChanges(true);
		_itemBids.replace(worldExchangeId, worldExchangeItem);
		if (!WorldExchangeConfig.WORLD_EXCHANGE_LAZY_UPDATE)
		{
			insert(worldExchangeId, true);
		}
	}
	
	/**
	 * Processes the return of a registered item to the player.<br>
	 * Validates if the exchange is active and the item belongs to the player.<br>
	 * Updates the item status and adds it back to the player inventory.
	 * @param player The {@code Player} receiving the item.
	 * @param worldExchangeItem The {@code WorldExchangeHolder} representing the traded item.
	 * @param weType The type of the world exchange.
	 */
	private void returnItem(Player player, WorldExchangeHolder worldExchangeItem, int weType)
	{
		if (!WorldExchangeConfig.ENABLE_WORLD_EXCHANGE)
		{
			return;
		}
		
		final WorldExchangeItemStatusType storeType = worldExchangeItem.getStoreType();
		if (storeType == WorldExchangeItemStatusType.WORLD_EXCHANGE_NONE)
		{
			player.sendPacket(new WorldExchangeSettleList(player, weType));
			player.sendPacket(new SystemMessage(SystemMessageId.NO_ITEM_FOUND));
			player.sendPacket(WorldExchangeSettleRecvResult.FAIL);
			return;
		}
		
		final long worldExchangeId = worldExchangeItem.getWorldExchangeId();
		if (!_itemBids.containsKey(worldExchangeId))
		{
			player.sendPacket(new WorldExchangeSettleList(player, weType));
			player.sendPacket(new SystemMessage(SystemMessageId.NO_ITEM_FOUND));
			player.sendPacket(WorldExchangeSettleRecvResult.FAIL);
			return;
		}
		
		if (_itemBids.get(worldExchangeId) != worldExchangeItem)
		{
			player.sendPacket(new WorldExchangeSettleList(player, weType));
			player.sendPacket(new SystemMessage(SystemMessageId.ITEM_OUT_OF_STOCK));
			player.sendPacket(WorldExchangeSettleRecvResult.FAIL);
			return;
		}
		
		if (player.getObjectId() != worldExchangeItem.getOldOwnerId())
		{
			player.sendPacket(new SystemMessage(SystemMessageId.ITEM_TO_BE_TRADED_DOES_NOT_EXIST));
			player.sendPacket(WorldExchangeSettleRecvResult.FAIL);
			return;
		}
		
		if (storeType != WorldExchangeItemStatusType.WORLD_EXCHANGE_OUT_TIME)
		{
			player.sendPacket(new WorldExchangeSettleList(player, weType));
			player.sendPacket(new SystemMessage(SystemMessageId.ITEM_OUT_OF_STOCK));
			player.sendPacket(WorldExchangeSettleRecvResult.FAIL);
			return;
		}
		
		if (worldExchangeItem.getEndTime() < System.currentTimeMillis())
		{
			player.sendPacket(new WorldExchangeSettleList(player, weType));
			player.sendPacket(new SystemMessage(SystemMessageId.THE_REGISTRATION_PERIOD_FOR_THE_ITEM_YOU_REGISTERED_HAS_EXPIRED));
			player.sendPacket(WorldExchangeSettleRecvResult.FAIL);
			return;
		}
		
		final Item itemInstance = worldExchangeItem.getItemInstance();
		player.sendPacket(new WorldExchangeSettleRecvResult(itemInstance.getObjectId(), itemInstance.getCount(), 1, weType));
		player.getInventory().addItem(ItemProcessType.TRANSFER, itemInstance, player, null);
		player.sendPacket(new WorldExchangeInfo(weType));
		worldExchangeItem.setStoreType(WorldExchangeItemStatusType.WORLD_EXCHANGE_NONE);
		worldExchangeItem.setHasChanges(true);
		_itemBids.replace(worldExchangeId, worldExchangeItem);
		if (!WorldExchangeConfig.WORLD_EXCHANGE_LAZY_UPDATE)
		{
			insert(worldExchangeId, true);
		}
	}
	
	/**
	 * Processes the purchase of an item from the world exchange.<br>
	 * This method checks if the item exists and is available for sale.<br>
	 * It verifies that the player has enough currency to complete the transaction.<br>
	 * If successful, it removes the currency, gives the item to the player, and updates the status.
	 * @param player The {@code Player} attempting to buy the item.
	 * @param worldExchangeId The unique identifier of the exchange listing.
	 * @param weType The type of the world exchange.
	 */
	public synchronized void buyItem(Player player, long worldExchangeId, int weType)
	{
		if (!WorldExchangeConfig.ENABLE_WORLD_EXCHANGE)
		{
			return;
		}
		
		if (!_itemBids.containsKey(worldExchangeId))
		{
			player.sendPacket(new SystemMessage(SystemMessageId.NO_ITEM_FOUND));
			player.sendPacket(WorldExchangeBuyItem.FAIL);
			return;
		}
		
		final WorldExchangeHolder worldExchangeItem = _itemBids.get(worldExchangeId);
		final WorldExchangeItemStatusType storeType = worldExchangeItem.getStoreType();
		if (storeType == WorldExchangeItemStatusType.WORLD_EXCHANGE_NONE)
		{
			player.sendPacket(new SystemMessage(SystemMessageId.NO_ITEM_FOUND));
			player.sendPacket(WorldExchangeBuyItem.FAIL);
			return;
		}
		
		if (storeType != WorldExchangeItemStatusType.WORLD_EXCHANGE_REGISTERED)
		{
			player.sendPacket(new SystemMessage(SystemMessageId.ITEM_OUT_OF_STOCK));
			player.sendPacket(WorldExchangeBuyItem.FAIL);
			return;
		}
		
		final int worldExchangeType = worldExchangeItem.getWorldExchangeType();
		if (worldExchangeType != weType)
		{
			player.sendPacket(new SystemMessage(SystemMessageId.ITEM_OUT_OF_STOCK));
			player.sendPacket(WorldExchangeBuyItem.FAIL);
			return;
		}
		
		final int currencyId = getExchangeCurrencyId(worldExchangeType);
		final Item currency = player.getInventory().getItemByItemId(currencyId);
		final long price = worldExchangeItem.getPrice();
		if ((currency == null) || (currency.getCount() < price))
		{
			player.sendPacket(isLcoinExchangeType(worldExchangeType) ? new SystemMessage(SystemMessageId.YOU_DO_NOT_HAVE_ENOUGH_L2_COINS_ADD_MORE_L2_COINS_AND_TRY_AGAIN) : new SystemMessage(SystemMessageId.NOT_ENOUGH_ADENA));
			player.sendPacket(WorldExchangeBuyItem.FAIL);
			return;
		}
		
		player.getInventory().destroyItem(ItemProcessType.BUY, currency, price, player, null);
		player.sendPacket(new WorldExchangeInfo(weType));
		final Item newItem = createItem(worldExchangeItem.getItemInstance(), player);
		final long destroyTime = calculateDate(WorldExchangeConfig.WORLD_EXCHANGE_PAYMENT_TAKE_PERIOD);
		WorldExchangeHolder newHolder = new WorldExchangeHolder(worldExchangeId, newItem, new ItemInfo(newItem), price, worldExchangeType, worldExchangeItem.getOldOwnerId(), WorldExchangeItemStatusType.WORLD_EXCHANGE_SOLD, worldExchangeItem.getCategory(), worldExchangeItem.getStartTime(), destroyTime, true);
		_itemBids.replace(worldExchangeId, worldExchangeItem, newHolder);
		if (!WorldExchangeConfig.WORLD_EXCHANGE_LAZY_UPDATE)
		{
			insert(worldExchangeItem.getWorldExchangeId(), false);
		}
		
		final Item receivedItem = player.getInventory().addItem(ItemProcessType.BUY, worldExchangeItem.getItemInstance(), player, null);
		final long receivedCount = receivedItem.getCount();
		player.sendPacket(new WorldExchangeBuyItem(receivedItem.getObjectId(), receivedCount, 1, weType));
		final SystemMessage sm;
		if (receivedItem.isEnchanted())
		{
			if (receivedCount < 2)
			{
				sm = new SystemMessage(SystemMessageId.YOU_HAVE_OBTAINED_S1_X_S2_6);
				sm.addByte(receivedItem.getEnchantLevel());
				sm.addItemName(receivedItem);
			}
			else
			{
				sm = new SystemMessage(SystemMessageId.YOU_HAVE_OBTAINED_S1_S2_X_S3);
				sm.addItemName(receivedItem);
				sm.addLong(receivedCount);
				sm.addByte(receivedItem.getEnchantLevel());
			}
		}
		else
		{
			sm = new SystemMessage(SystemMessageId.YOU_HAVE_OBTAINED_S1_X_S2);
			sm.addItemName(receivedItem);
			sm.addLong(receivedCount);
		}
		
		player.sendPacket(sm);
		for (Player oldOwner : World.getPlayers())
		{
			if (oldOwner.getObjectId() == newHolder.getOldOwnerId())
			{
				oldOwner.sendPacket(new WorldExchangeSellCompleteAlarm(newItem.getId(), newItem.getCount(), weType));
				break;
			}
		}
	}
	
	/**
	 * Creates a new {@code Item} instance based on an existing one.<br>
	 * This method copies properties from the old item to the new one.<br>
	 * It sets the location to {@code ItemLocation.EXCHANGE}.<br>
	 * The new item is then removed from the requestor's inventory via {@code InventoryUpdate}.
	 * @param oldItem The original {@code Item} to copy data from.
	 * @param requestor The {@code Player} who is requesting the creation of the item.
	 * @return The newly created {@code Item} object.
	 */
	private Item createItem(Item oldItem, Player requestor)
	{
		final Item newItem = new Item(oldItem.getId());
		newItem.setOwnerId(requestor.getObjectId());
		final int enchantLevel = oldItem.getEnchantLevel();
		newItem.setEnchantLevel(enchantLevel < 1 ? 0 : enchantLevel);
		newItem.setItemLocation(ItemLocation.EXCHANGE);
		newItem.setCount(oldItem.getCount());
		newItem.setVisualId(oldItem.getVisualId());
		newItem.setOwnerId(oldItem.getOwnerId());
		newItem.updateDatabase(true); // In any case it will be store in database.
		final AugmentationInstance vi = oldItem.getAugmentation();
		if (vi != null)
		{
			newItem.setAugmentation(vi, true);
		}
		
		final InventoryUpdate iu = new InventoryUpdate();
		iu.addRemovedItem(newItem);
		requestor.sendInventoryUpdate(iu);
		return newItem;
	}
	
	/**
	 * Retrieves a list of registered item bids for a specific owner.<br>
	 * The results are filtered by the specified item subtype and sorted accordingly.<br>
	 * This method returns an empty list if the world exchange is disabled.
	 * @param ownerId The unique identifier of the player who owns the items.
	 * @param type The category or subtype of the items to filter by.
	 * @param sortType The criteria used to sort the resulting list.
	 * @param lang The language code for localized sorting and display.
	 * @return A sorted {@code List} of {@code WorldExchangeHolder} objects matching the criteria.
	 */
	public List<WorldExchangeHolder> getItemBids(int ownerId, WorldExchangeItemSubType type, WorldExchangeSortType sortType, String lang)
	{
		if (!WorldExchangeConfig.ENABLE_WORLD_EXCHANGE)
		{
			return Collections.emptyList();
		}
		
		final List<WorldExchangeHolder> returnList = new ArrayList<>();
		for (WorldExchangeHolder holder : _itemBids.values())
		{
			final WorldExchangeItemStatusType storeType = holder.getStoreType();
			if (storeType == WorldExchangeItemStatusType.WORLD_EXCHANGE_NONE)
			{
				continue;
			}
			
			if ((holder.getOldOwnerId() == ownerId) || (holder.getCategory() != type))
			{
				continue;
			}
			
			if (storeType == WorldExchangeItemStatusType.WORLD_EXCHANGE_REGISTERED)
			{
				returnList.add(holder);
			}
		}
		
		return sortList(returnList, sortType, lang);
	}
	
	/**
	 * Retrieves a list of item bids based on the owner, type, and sorting criteria.<br>
	 * This method filters the results to match a specific exchange type.
	 * @param ownerId The unique identifier of the owner.
	 * @param type The sub-type of the world exchange item.
	 * @param sortType The type of sorting to apply to the results.
	 * @param lang The language code for localization.
	 * @param weType The specific world exchange type identifier.
	 * @return A {@code List} of {@code WorldExchangeHolder} objects matching the criteria.
	 */
	public List<WorldExchangeHolder> getItemBids(int ownerId, WorldExchangeItemSubType type, WorldExchangeSortType sortType, String lang, int weType)
	{
		final List<WorldExchangeHolder> filtered = new ArrayList<>();
		for (WorldExchangeHolder holder : getItemBids(ownerId, type, sortType, lang))
		{
			if (holder.getWorldExchangeType() == weType)
			{
				filtered.add(holder);
			}
		}
		
		return filtered;
	}
	
	/**
	 * Retrieves a list of registered item bids based on specific IDs.<br>
	 * The method filters out items that are not currently registered in the exchange.<br>
	 * It returns an empty list if the world exchange feature is disabled.
	 * @param ids A list of {@code Integer} item IDs to filter by.
	 * @param sortType The {@code WorldExchangeSortType} used to order the results.
	 * @param lang The language code for localization.
	 * @return A sorted {@code List} of {@code WorldExchangeHolder} objects.
	 */
	public List<WorldExchangeHolder> getItemBids(List<Integer> ids, WorldExchangeSortType sortType, String lang)
	{
		if (!WorldExchangeConfig.ENABLE_WORLD_EXCHANGE)
		{
			return Collections.emptyList();
		}
		
		final List<WorldExchangeHolder> returnList = new ArrayList<>();
		for (WorldExchangeHolder holder : _itemBids.values())
		{
			final WorldExchangeItemStatusType storeType = holder.getStoreType();
			if (storeType == WorldExchangeItemStatusType.WORLD_EXCHANGE_NONE)
			{
				continue;
			}
			
			if (ids.contains(holder.getItemInstance().getId()) && (storeType == WorldExchangeItemStatusType.WORLD_EXCHANGE_REGISTERED))
			{
				returnList.add(holder);
			}
		}
		
		return sortList(returnList, sortType, lang);
	}
	
	/**
	 * Retrieves a list of item bids based on specific IDs and sorting rules.<br>
	 * This method filters the results to match a specific exchange type.
	 * @param ids The list of item IDs to search for.
	 * @param sortType The criteria used to sort the resulting items.
	 * @param lang The language code for localization.
	 * @param weType The specific world exchange type to filter by.
	 * @return A list of {@code WorldExchangeHolder} objects matching the criteria.
	 */
	public List<WorldExchangeHolder> getItemBids(List<Integer> ids, WorldExchangeSortType sortType, String lang, int weType)
	{
		final List<WorldExchangeHolder> filtered = new ArrayList<>();
		for (WorldExchangeHolder holder : getItemBids(ids, sortType, lang))
		{
			if (holder.getWorldExchangeType() == weType)
			{
				filtered.add(holder);
			}
		}
		
		return filtered;
	}
	
	/**
	 * Retrieves the lowest price for a specific item in a world exchange.<br>
	 * It filters bids by the provided {@code itemId} and {@code weType}.<br>
	 * If no valid bids are found, it returns {@code 0}.
	 * @param itemId The unique identifier of the item.
	 * @param weType The type of world exchange to filter by.
	 * @return The minimum price as a {@code long}, or {@code 0} if none exist.
	 */
	public long getMinPriceForItem(int itemId, int weType)
	{
		long minPrice = Long.MAX_VALUE;
		for (WorldExchangeHolder holder : _itemBids.values())
		{
			if ((holder.getStoreType() != WorldExchangeItemStatusType.WORLD_EXCHANGE_REGISTERED) || (holder.getWorldExchangeType() != weType) || (holder.getItemInstance().getId() != itemId))
			{
				continue;
			}
			
			minPrice = Math.min(minPrice, holder.getPrice());
		}
		
		return minPrice == Long.MAX_VALUE ? 0 : minPrice;
	}
	
	/**
	 * Calculates the total quantity of a specific item currently registered in the world exchange.<br>
	 * This method sums up the counts from all valid bids that match the provided criteria.
	 * @param itemId The unique identifier of the item to count.
	 * @param weType The type of the world exchange to filter by.
	 * @return The total sum of the item quantities as a {@code long}.
	 */
	public long getTotalAmountForItem(int itemId, int weType)
	{
		long amount = 0;
		for (WorldExchangeHolder holder : _itemBids.values())
		{
			if ((holder.getStoreType() != WorldExchangeItemStatusType.WORLD_EXCHANGE_REGISTERED) || (holder.getWorldExchangeType() != weType) || (holder.getItemInstance().getId() != itemId))
			{
				continue;
			}
			
			amount += holder.getItemInstance().getCount();
		}
		
		return amount;
	}
	
	/**
	 * Sorts a list of {@code WorldExchangeHolder} objects based on the specified criteria.<br>
	 * The method handles sorting by price or item name in both ascending and descending orders.<br>
	 * It also limits the resulting list to a maximum of 399 items.
	 * @param unsortedList The original list of exchange holders to be sorted.
	 * @param sortType The {@code WorldExchangeSortType} defining the sorting logic.
	 * @param lang The language code used for localized name lookups.
	 * @return A new {@code List} containing up to 399 sorted {@code WorldExchangeHolder} objects.
	 */
	private List<WorldExchangeHolder> sortList(List<WorldExchangeHolder> unsortedList, WorldExchangeSortType sortType, String lang)
	{
		final List<WorldExchangeHolder> sortedList = new ArrayList<>(unsortedList);
		switch (sortType)
		{
			case PRICE_ASCE:
			{
				sortedList.sort(Comparator.comparingLong(WorldExchangeHolder::getPrice));
				break;
			}
			case PRICE_DESC:
			{
				sortedList.sort(Comparator.comparingLong(WorldExchangeHolder::getPrice));
				Collections.reverse(sortedList);
				break;
			}
			case ITEM_NAME_ASCE:
			{
				if ((lang == null) || (!lang.equals("en") && _localItemNames.containsKey(lang)))
				{
					sortedList.sort(Comparator.comparing(o -> getItemName(lang, o.getItemInstance().getId())));
				}
				else
				{
					sortedList.sort(Comparator.comparing(o -> o.getItemInstance().getItemName()));
				}
				break;
			}
			case ITEM_NAME_DESC:
			{
				if ((lang == null) || (!lang.equals("en") && _localItemNames.containsKey(lang)))
				{
					sortedList.sort(Comparator.comparing(o -> getItemName(lang, o.getItemInstance().getId())));
				}
				else
				{
					sortedList.sort(Comparator.comparing(o -> o.getItemInstance().getItemName()));
				}
				
				Collections.reverse(sortedList);
				break;
			}
			case PRICE_PER_PIECE_ASCE:
			{
				sortedList.sort(Comparator.comparingLong(WorldExchangeHolder::getPrice));
				break;
			}
			case PRICE_PER_PIECE_DESC:
			{
				sortedList.sort(Comparator.comparingLong(WorldExchangeHolder::getPrice).reversed());
				break;
			}
		}
		
		if (sortedList.size() > 399)
		{
			return sortedList.subList(0, 399);
		}
		
		return sortedList;
	}
	
	/**
	 * Retrieves the display name of an item based on a specific language and ID.<br>
	 * It checks if the language exists in the local names map.<br>
	 * If the item ID is not found, it returns an empty string.
	 * @param lang The language code to use for the translation.
	 * @param id The unique identifier of the item.
	 * @return The name of the item as a {@code String}, or an empty string if not found.
	 */
	private String getItemName(String lang, int id)
	{
		if (!_localItemNames.containsKey(lang))
		{
			return "";
		}
		
		final Map<Integer, String> names = _localItemNames.get(lang);
		final String name = names.get(id);
		if (name == null)
		{
			return "";
		}
		
		return name;
	}
	
	/**
	 * Retrieves all bids associated with a specific player.<br>
	 * The results are grouped by their current status type.<br>
	 * This method returns an empty map if the world exchange is disabled.
	 * @param ownerId The unique identifier of the player who owns the items.
	 * @return A map where keys are {@code WorldExchangeItemStatusType} and values are lists of {@code WorldExchangeHolder}.
	 */
	public Map<WorldExchangeItemStatusType, List<WorldExchangeHolder>> getPlayerBids(int ownerId)
	{
		if (!WorldExchangeConfig.ENABLE_WORLD_EXCHANGE)
		{
			return Collections.emptyMap();
		}
		
		final List<WorldExchangeHolder> registered = new ArrayList<>();
		final List<WorldExchangeHolder> sold = new ArrayList<>();
		final List<WorldExchangeHolder> outTime = new ArrayList<>();
		for (WorldExchangeHolder holder : _itemBids.values())
		{
			final WorldExchangeItemStatusType storeType = holder.getStoreType();
			if (storeType == WorldExchangeItemStatusType.WORLD_EXCHANGE_NONE)
			{
				continue;
			}
			
			if (holder.getOldOwnerId() != ownerId)
			{
				continue;
			}
			
			switch (storeType)
			{
				case WORLD_EXCHANGE_REGISTERED:
				{
					registered.add(holder);
					break;
				}
				case WORLD_EXCHANGE_SOLD:
				{
					sold.add(holder);
					break;
				}
				case WORLD_EXCHANGE_OUT_TIME:
				{
					outTime.add(holder);
					break;
				}
			}
		}
		
		final EnumMap<WorldExchangeItemStatusType, List<WorldExchangeHolder>> returnMap = new EnumMap<>(WorldExchangeItemStatusType.class);
		returnMap.put(WorldExchangeItemStatusType.WORLD_EXCHANGE_REGISTERED, registered);
		returnMap.put(WorldExchangeItemStatusType.WORLD_EXCHANGE_SOLD, sold);
		returnMap.put(WorldExchangeItemStatusType.WORLD_EXCHANGE_OUT_TIME, outTime);
		return returnMap;
	}
	
	/**
	 * Retrieves the bids made by a specific player for a given exchange type.<br>
	 * This method filters all active bids belonging to the owner based on the provided {@code weType}.<br>
	 * It returns an empty map if the world exchange system is disabled.
	 * @param ownerId The unique identifier of the player who placed the bids.
	 * @param weType The specific type of world exchange to filter by.
	 * @return A map containing lists of {@code WorldExchangeHolder} objects grouped by their status.
	 */
	public Map<WorldExchangeItemStatusType, List<WorldExchangeHolder>> getPlayerBids(int ownerId, int weType)
	{
		if (!WorldExchangeConfig.ENABLE_WORLD_EXCHANGE)
		{
			return Collections.emptyMap();
		}
		
		final Map<WorldExchangeItemStatusType, List<WorldExchangeHolder>> playerBids = getPlayerBids(ownerId);
		final EnumMap<WorldExchangeItemStatusType, List<WorldExchangeHolder>> returnMap = new EnumMap<>(WorldExchangeItemStatusType.class);
		for (Entry<WorldExchangeItemStatusType, List<WorldExchangeHolder>> entry : playerBids.entrySet())
		{
			final List<WorldExchangeHolder> filtered = new ArrayList<>();
			for (WorldExchangeHolder holder : entry.getValue())
			{
				if (holder.getWorldExchangeType() == weType)
				{
					filtered.add(holder);
				}
			}
			
			returnMap.put(entry.getKey(), filtered);
		}
		
		return returnMap;
	}
	
	/**
	 * Assigns a specific category type to a list of items.<br>
	 * This method checks if the world exchange is enabled before processing.<br>
	 * It maps each item ID to its corresponding subtype based on the provided category.
	 * @param itemIds The list of {@code Integer} IDs for the items to update.
	 * @param category The integer value representing the category type.
	 */
	public void addCategoryType(List<Integer> itemIds, int category)
	{
		if (!WorldExchangeConfig.ENABLE_WORLD_EXCHANGE)
		{
			return;
		}
		
		for (int itemId : itemIds)
		{
			final WorldExchangeItemSubType type = WorldExchangeItemSubType.getWorldExchangeItemSubType(category);
			if (type == null)
			{
				LOGGER.warning(getClass().getSimpleName() + ": Non existent category type [" + category + "] for item id " + itemId + "!");
				continue;
			}
			
			_itemCategories.putIfAbsent(itemId, type);
		}
	}
	
	/**
	 * Checks if the player has any items that were recently sold or expired.<br>
	 * This method sends a sell completion packet to the {@code Player}.<br>
	 * It only runs if world exchange is enabled in the configuration.
	 * @param player The {@code Player} object to check for sale alarms.
	 */
	public void checkPlayerSellAlarm(Player player)
	{
		if (!WorldExchangeConfig.ENABLE_WORLD_EXCHANGE)
		{
			return;
		}
		
		for (WorldExchangeHolder holder : _itemBids.values())
		{
			if ((holder.getOldOwnerId() == player.getObjectId()) && ((holder.getStoreType() == WorldExchangeItemStatusType.WORLD_EXCHANGE_SOLD) || (holder.getStoreType() == WorldExchangeItemStatusType.WORLD_EXCHANGE_OUT_TIME)))
			{
				player.sendPacket(new WorldExchangeSellCompleteAlarm(holder.getItemInstance().getId(), holder.getItemInstance().getCount(), holder.getWorldExchangeType()));
				break;
			}
		}
	}
	
	/**
	 * Saves the current world exchange item bids to the database.<br>
	 * This method only runs if {@code WORLD_EXCHANGE_ENABLE} and {@code WORLD_EXCHANGE_LAZY_UPDATE} are true.<br>
	 * It updates records for all holders that have pending changes.
	 */
	public void storeMe()
	{
		if (!WorldExchangeConfig.ENABLE_WORLD_EXCHANGE || !WorldExchangeConfig.WORLD_EXCHANGE_LAZY_UPDATE)
		{
			return;
		}
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(INSERT_WORLD_EXCHANGE))
		{
			for (WorldExchangeHolder holder : _itemBids.values())
			{
				if (!holder.hasChanges())
				{
					continue;
				}
				
				statement.setLong(1, holder.getWorldExchangeId());
				statement.setLong(2, holder.getItemInstance().getObjectId());
				statement.setInt(3, holder.getStoreType().getId());
				statement.setInt(4, holder.getCategory().getId());
				statement.setLong(5, holder.getPrice());
				statement.setInt(6, holder.getWorldExchangeType());
				statement.setInt(7, holder.getOldOwnerId());
				statement.setLong(8, holder.getStartTime());
				statement.setLong(9, holder.getEndTime());
				statement.addBatch();
			}
			
			statement.executeBatch();
			statement.closeOnCompletion();
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.SEVERE, "Error while saving World Exchange item bids:\n", e);
		}
	}
	
	/**
	 * Saves a specific world exchange item bid to the database.<br>
	 * This method handles the persistence of bid data for a given ID.<br>
	 * If {@code remove} is {@code true}, the bid is also removed from the internal cache.
	 * @param worldExchangeId The unique identifier for the world exchange entry.
	 * @param remove Whether to remove the item from the memory map after saving.
	 */
	public void insert(long worldExchangeId, boolean remove)
	{
		if (WorldExchangeConfig.WORLD_EXCHANGE_LAZY_UPDATE)
		{
			return;
		}
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(INSERT_WORLD_EXCHANGE))
		{
			WorldExchangeHolder holder = _itemBids.get(worldExchangeId);
			statement.setLong(1, holder.getWorldExchangeId());
			statement.setLong(2, holder.getItemInstance().getObjectId());
			statement.setInt(3, holder.getStoreType().getId());
			statement.setInt(4, holder.getCategory().getId());
			statement.setLong(5, holder.getPrice());
			statement.setInt(6, holder.getWorldExchangeType());
			statement.setInt(7, holder.getOldOwnerId());
			statement.setString(8, String.valueOf(holder.getStartTime()));
			statement.setString(9, String.valueOf(holder.getEndTime()));
			statement.execute();
			if (remove)
			{
				_itemBids.remove(worldExchangeId);
			}
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.SEVERE, "Error while saving World Exchange item bid " + worldExchangeId + "\n", e);
		}
	}
	
	/**
	 * Calculates the average price of a specific item based on current bids.<br>
	 * It sums the prices of all matching items and divides by the count.<br>
	 * If no items are found, it returns {@code 0}.
	 * @param itemId The unique identifier of the item to check.
	 * @return The calculated average price as a {@code long}.
	 */
	public long getAveragePriceOfItem(int itemId)
	{
		long totalPrice = 0;
		long totalItemCount = 0;
		for (WorldExchangeHolder holder : _itemBids.values())
		{
			if (holder.getItemInstance().getTemplate().getId() != itemId)
			{
				continue;
			}
			
			totalItemCount++;
			totalPrice += holder.getPrice();
		}
		
		return totalItemCount == 0 ? 0 : totalPrice / totalItemCount;
	}
	
	/**
	 * Retrieves the singleton instance of the {@code WorldExchangeManager}.<br>
	 * This method provides a global access point to the manager.
	 * @return The active {@code WorldExchangeManager} instance.
	 */
	public static WorldExchangeManager getInstance()
	{
		return WorldExchangeManager.SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final WorldExchangeManager INSTANCE = new WorldExchangeManager();
	}
}
