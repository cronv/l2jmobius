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
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.instance.CommissionManager;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.entity.item.enums.ItemLocation;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.itemcontainer.Inventory;
import org.l2jmobius.gameserver.entity.itemcontainer.Mail;
import org.l2jmobius.gameserver.entity.itemcontainer.PlayerInventory;
import org.l2jmobius.gameserver.mechanics.commission.CommissionItem;
import org.l2jmobius.gameserver.mechanics.events.EventDispatcher;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerAuctionWin;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.enums.MailType;
import org.l2jmobius.gameserver.network.holders.MailMessage;
import org.l2jmobius.gameserver.network.serverpackets.commission.ExResponseCommissionBuyItem;
import org.l2jmobius.gameserver.network.serverpackets.commission.ExResponseCommissionDelete;
import org.l2jmobius.gameserver.network.serverpackets.commission.ExResponseCommissionInfo;
import org.l2jmobius.gameserver.network.serverpackets.commission.ExResponseCommissionList;
import org.l2jmobius.gameserver.network.serverpackets.commission.ExResponseCommissionList.CommissionListReplyType;
import org.l2jmobius.gameserver.network.serverpackets.commission.ExResponseCommissionRegister;

/**
 * Manages the logic for items associated with commissions.<br>
 * This class handles item registration, deletion, and information retrieval for commission tasks.
 * @author NosBit, Ren
 */
public class ItemCommissionManager
{
	private static final Logger LOGGER = Logger.getLogger(ItemCommissionManager.class.getName());
	
	private static final int INTERACTION_DISTANCE = 250;
	private static final int ITEMS_LIMIT_PER_REQUEST = 999;
	private static final int MAX_ITEMS_REGISTRED_PER_PLAYER = 10;
	private static final long MIN_REGISTRATION_AND_SALE_FEE = 1000;
	private static final double REGISTRATION_FEE_PER_DAY = 0.0001;
	private static final double SALE_FEE_PER_DAY = 0.005;
	private static final int[] DURATION =
	{
		1,
		3,
		5,
		7,
		15,
		30
	};
	
	private static final String SELECT_ALL_ITEMS = "SELECT * FROM `items` WHERE `loc` = ?";
	private static final String SELECT_ALL_COMMISSION_ITEMS = "SELECT * FROM `commission_items`";
	private static final String INSERT_COMMISSION_ITEM = "INSERT INTO `commission_items`(`item_object_id`, `price_per_unit`, `start_time`, `duration_in_days`, `discount_in_percentage`) VALUES (?, ?, ?, ?, ?)";
	private static final String DELETE_COMMISSION_ITEM = "DELETE FROM `commission_items` WHERE `commission_id` = ?";
	
	private final Map<Long, CommissionItem> _commissionItems = new ConcurrentSkipListMap<>();
	
	/**
	 * Initializes the {@code ItemCommissionManager}.<br>
	 * This constructor loads all commission items from the database.<br>
	 * It also schedules expiration tasks for active sales.
	 */
	protected ItemCommissionManager()
	{
		final Map<Integer, Item> itemInstances = new HashMap<>();
		try (Connection con = DatabaseFactory.getConnection())
		{
			try (PreparedStatement ps = con.prepareStatement(SELECT_ALL_ITEMS))
			{
				ps.setString(1, ItemLocation.COMMISSION.name());
				try (ResultSet rs = ps.executeQuery())
				{
					while (rs.next())
					{
						final Item itemInstance = new Item(rs);
						itemInstances.put(itemInstance.getObjectId(), itemInstance);
					}
				}
			}
			
			try (Statement st = con.createStatement();
				ResultSet rs = st.executeQuery(SELECT_ALL_COMMISSION_ITEMS))
			{
				while (rs.next())
				{
					final long commissionId = rs.getLong("commission_id");
					final Item itemInstance = itemInstances.get(rs.getInt("item_object_id"));
					if (itemInstance == null)
					{
						LOGGER.warning(getClass().getSimpleName() + ": Failed loading commission item with commission id " + commissionId + " because item instance does not exist or failed to load.");
						continue;
					}
					
					final CommissionItem commissionItem = new CommissionItem(commissionId, itemInstance, rs.getLong("price_per_unit"), rs.getTimestamp("start_time").toInstant(), rs.getByte("duration_in_days"), rs.getByte("discount_in_percentage"));
					_commissionItems.put(commissionItem.getCommissionId(), commissionItem);
					final Instant now = Instant.now();
					if (commissionItem.getEndTime().isBefore(now))
					{
						expireSale(commissionItem);
					}
					else
					{
						commissionItem.setSaleEndTask(ThreadPool.schedule(() -> expireSale(commissionItem), Duration.between(now, commissionItem.getEndTime()).toMillis()));
					}
				}
			}
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Failed loading commission items.", e);
		}
	}
	
	/**
	 * Displays a list of available auctions to a specific player.<br>
	 * The list is filtered based on the provided criteria.<br>
	 * If no items match the filter, an empty response is sent.<br>
	 * Results are sent in chunks if they exceed the maximum size.
	 * @param player The {@code Player} who will receive the auction list.
	 * @param filter A {@code Predicate} used to determine which {@code ItemTemplate} objects to include.
	 */
	public void showAuctions(Player player, Predicate<ItemTemplate> filter)
	{
		final List<CommissionItem> commissionItems = new ArrayList<>();
		for (CommissionItem item : _commissionItems.values())
		{
			if (filter.test(item.getItemInfo().getItem()))
			{
				commissionItems.add(item);
				if (commissionItems.size() >= ITEMS_LIMIT_PER_REQUEST)
				{
					break;
				}
			}
		}
		
		if (commissionItems.isEmpty())
		{
			player.sendPacket(new ExResponseCommissionList(CommissionListReplyType.ITEM_DOES_NOT_EXIST));
			return;
		}
		
		final int size = commissionItems.size();
		int chunks = size / ExResponseCommissionList.MAX_CHUNK_SIZE;
		if (size > (chunks * ExResponseCommissionList.MAX_CHUNK_SIZE))
		{
			chunks++;
		}
		
		for (int i = chunks - 1; i >= 0; i--)
		{
			player.sendPacket(new ExResponseCommissionList(CommissionListReplyType.AUCTIONS, commissionItems, i, i * ExResponseCommissionList.MAX_CHUNK_SIZE));
		}
	}
	
	/**
	 * Displays the auction items owned by a specific player.<br>
	 * This method finds all active commissions belonging to the {@code Player}.<br>
	 * It sends a packet to the player containing their listed items or an empty list message.
	 * @param player The {@code Player} who will receive the auction information.
	 */
	public void showPlayerAuctions(Player player)
	{
		final List<CommissionItem> commissionItems = new ArrayList<>();
		for (CommissionItem c : _commissionItems.values())
		{
			if (c.getItemInstance().getOwnerId() == player.getObjectId())
			{
				commissionItems.add(c);
				if (commissionItems.size() == MAX_ITEMS_REGISTRED_PER_PLAYER)
				{
					break;
				}
			}
		}
		
		if (!commissionItems.isEmpty())
		{
			player.sendPacket(new ExResponseCommissionList(CommissionListReplyType.PLAYER_AUCTIONS, commissionItems));
		}
		else
		{
			player.sendPacket(new ExResponseCommissionList(CommissionListReplyType.PLAYER_AUCTIONS_EMPTY));
		}
	}
	
	/**
	 * Registers an item for sale in the auction house.<br>
	 * This method validates the player's inventory, checks registration fees, and handles option consumption.<br>
	 * It then saves the commission to the database and schedules its expiration.
	 * @param player The {@code Player} who is registering the item.
	 * @param itemObjectId The unique ID of the item template.
	 * @param itemCount The number of items to register.
	 * @param pricePerUnit The cost for a single unit of the item.
	 * @param durationType The type of duration selected by the player.
	 * @param discountInPercentage The percentage discount applied to the sale.
	 */
	public void registerItem(Player player, int itemObjectId, long itemCount, long pricePerUnit, int durationType, byte discountInPercentage)
	{
		if (itemCount < 1)
		{
			player.sendPacket(SystemMessageId.THE_ITEM_HAS_FAILED_TO_BE_REGISTERED);
			player.sendPacket(ExResponseCommissionRegister.FAILED);
			return;
		}
		
		final long totalPrice = itemCount * pricePerUnit;
		if (totalPrice <= MIN_REGISTRATION_AND_SALE_FEE)
		{
			player.sendPacket(SystemMessageId.THE_ITEM_CANNOT_BE_REGISTERED_BECAUSE_REQUIREMENTS_ARE_NOT_MET);
			player.sendPacket(ExResponseCommissionRegister.FAILED);
			return;
		}
		
		Item itemInstance = player.getInventory().getItemByObjectId(itemObjectId);
		if ((itemInstance == null) || !itemInstance.isAvailable(player, false, false) || (itemInstance.getCount() < itemCount))
		{
			player.sendPacket(SystemMessageId.THE_ITEM_HAS_FAILED_TO_BE_REGISTERED);
			player.sendPacket(ExResponseCommissionRegister.FAILED);
			return;
		}
		
		final byte durationInDays = (byte) DURATION[durationType];
		
		synchronized (this)
		{
			long playerRegisteredItems = 0;
			for (CommissionItem item : _commissionItems.values())
			{
				if (item.getItemInstance().getOwnerId() == player.getObjectId())
				{
					playerRegisteredItems++;
				}
			}
			
			if (playerRegisteredItems >= MAX_ITEMS_REGISTRED_PER_PLAYER)
			{
				player.sendPacket(SystemMessageId.THE_MAXIMUM_NUMBER_OF_WORLD_TRADE_ITEMS_FOR_REGISTRATION_IS_10);
				player.sendPacket(ExResponseCommissionRegister.FAILED);
				return;
			}
			
			final long registrationFee = (long) Math.max(MIN_REGISTRATION_AND_SALE_FEE, (totalPrice * REGISTRATION_FEE_PER_DAY) * Math.min(durationInDays, 7));
			final PlayerInventory playerInventory = player.getInventory();
			if (!playerInventory.reduceAdena(ItemProcessType.FEE, registrationFee, player, null))
			{
				player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_ENOUGH_ADENA_TO_REGISTER_THE_ITEM);
				player.sendPacket(ExResponseCommissionRegister.FAILED);
				return;
			}
			
			itemInstance = playerInventory.detachItem(ItemProcessType.TRANSFER, itemInstance, itemCount, ItemLocation.COMMISSION, player, null);
			if (itemInstance == null)
			{
				playerInventory.addAdena(ItemProcessType.REFUND, registrationFee, player, null);
				player.sendPacket(SystemMessageId.THE_ITEM_HAS_FAILED_TO_BE_REGISTERED);
				player.sendPacket(ExResponseCommissionRegister.FAILED);
				return;
			}
			
			// Only one option item can be used per registration, so a single item is consumed.
			// Duration types (4, 5) and discount percentages (30, 100) do not overlap, so Math.max resolves the selected option.
			switch (Math.max(durationType, discountInPercentage))
			{
				case 4:
				{
					player.destroyItemByItemId(null, 22353, 1, player, true); // 15 days
					break;
				}
				case 5:
				{
					player.destroyItemByItemId(null, 22354, 1, player, true); // 30 days
					break;
				}
				case 30:
				{
					player.destroyItemByItemId(null, 22351, 1, player, true); // 30% discount
					break;
				}
				case 100:
				{
					player.destroyItemByItemId(null, 22352, 1, player, true); // 100% discount
					break;
				}
			}
			
			try (Connection con = DatabaseFactory.getConnection();
				PreparedStatement ps = con.prepareStatement(INSERT_COMMISSION_ITEM, Statement.RETURN_GENERATED_KEYS))
			{
				final Instant startTime = Instant.now();
				ps.setInt(1, itemInstance.getObjectId());
				ps.setLong(2, pricePerUnit);
				ps.setTimestamp(3, Timestamp.from(startTime));
				ps.setByte(4, durationInDays);
				ps.setByte(5, discountInPercentage);
				ps.executeUpdate();
				try (ResultSet rs = ps.getGeneratedKeys())
				{
					if (rs.next())
					{
						final CommissionItem commissionItem = new CommissionItem(rs.getLong(1), itemInstance, pricePerUnit, startTime, durationInDays, discountInPercentage);
						final ScheduledFuture<?> saleEndTask = ThreadPool.schedule(() -> expireSale(commissionItem), Duration.between(Instant.now(), commissionItem.getEndTime()).toMillis());
						commissionItem.setSaleEndTask(saleEndTask);
						_commissionItems.put(commissionItem.getCommissionId(), commissionItem);
						player.getLastCommissionInfos().put(itemInstance.getId(), new ExResponseCommissionInfo(itemInstance.getId(), pricePerUnit, itemCount, (byte) ((durationInDays - 1) / 2)));
						player.sendPacket(SystemMessageId.THE_ITEM_HAS_BEEN_REGISTERED);
						player.sendPacket(ExResponseCommissionRegister.SUCCEED);
					}
				}
			}
			catch (SQLException e)
			{
				LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Failed inserting commission item. ItemInstance: " + itemInstance, e);
				player.sendPacket(SystemMessageId.THE_ITEM_HAS_FAILED_TO_BE_REGISTERED);
				player.sendPacket(ExResponseCommissionRegister.FAILED);
			}
		}
	}
	
	/**
	 * Removes a commission item from the auction.<br>
	 * This method checks if the {@code Player} owns the item and meets weight requirements.<br>
	 * If successful, the item is returned to the player's inventory.
	 * @param player The {@code Player} attempting to delete the item.
	 * @param commissionId The unique identifier of the commission to remove.
	 */
	public void deleteItem(Player player, long commissionId)
	{
		final CommissionItem commissionItem = getCommissionItem(commissionId);
		if (commissionItem == null)
		{
			player.sendPacket(SystemMessageId.FAILED_TO_CANCEL_THE_SALE);
			player.sendPacket(ExResponseCommissionDelete.FAILED);
			return;
		}
		
		if (commissionItem.getItemInstance().getOwnerId() != player.getObjectId())
		{
			player.sendPacket(ExResponseCommissionDelete.FAILED);
			return;
		}
		
		if (!player.isInventoryUnder80(false) || (player.getWeightPenalty() >= 3))
		{
			player.sendPacket(SystemMessageId.TO_BUY_CANCEL_YOU_NEED_TO_FREE_20_OF_WEIGHT_AND_10_OF_SLOTS_IN_YOUR_INVENTORY);
			player.sendPacket(SystemMessageId.FAILED_TO_CANCEL_THE_SALE);
			player.sendPacket(ExResponseCommissionDelete.FAILED);
			return;
		}
		
		if ((_commissionItems.remove(commissionId) == null) || !commissionItem.getSaleEndTask().cancel(false))
		{
			player.sendPacket(SystemMessageId.FAILED_TO_CANCEL_THE_SALE);
			player.sendPacket(ExResponseCommissionDelete.FAILED);
			return;
		}
		
		if (deleteItemFromDB(commissionId))
		{
			player.getInventory().addItem(ItemProcessType.TRANSFER, commissionItem.getItemInstance(), player, null);
			player.sendPacket(SystemMessageId.THE_SALE_IS_CANCELLED);
			player.sendPacket(ExResponseCommissionDelete.SUCCEED);
		}
		else
		{
			player.sendPacket(SystemMessageId.FAILED_TO_CANCEL_THE_SALE);
			player.sendPacket(ExResponseCommissionDelete.FAILED);
		}
	}
	
	/**
	 * Allows a {@code Player} to purchase an item from a commission.<br>
	 * The method checks for sufficient weight and adena before completing the sale.<br>
	 * It handles price calculations, fee deductions, and mail notifications for the seller.
	 * @param player The {@code Player} attempting to buy the item.
	 * @param commissionId The unique identifier of the commission item to purchase.
	 */
	public void buyItem(Player player, long commissionId)
	{
		final CommissionItem commissionItem = getCommissionItem(commissionId);
		if (commissionItem == null)
		{
			player.sendPacket(SystemMessageId.ITEM_PURCHASE_HAS_FAILED);
			player.sendPacket(ExResponseCommissionBuyItem.FAILED);
			return;
		}
		
		final Item itemInstance = commissionItem.getItemInstance();
		if (itemInstance.getOwnerId() == player.getObjectId())
		{
			player.sendPacket(SystemMessageId.ITEM_PURCHASE_HAS_FAILED);
			player.sendPacket(ExResponseCommissionBuyItem.FAILED);
			return;
		}
		
		if (!player.isInventoryUnder80(false) || (player.getWeightPenalty() >= 3))
		{
			player.sendPacket(SystemMessageId.TO_BUY_CANCEL_YOU_NEED_TO_FREE_20_OF_WEIGHT_AND_10_OF_SLOTS_IN_YOUR_INVENTORY);
			player.sendPacket(ExResponseCommissionBuyItem.FAILED);
			return;
		}
		
		final long totalPrice = itemInstance.getCount() * commissionItem.getPricePerUnit();
		final PlayerInventory playerInventory = player.getInventory();
		if (!playerInventory.reduceAdena(ItemProcessType.FEE, totalPrice, player, null))
		{
			player.sendPacket(SystemMessageId.NOT_ENOUGH_ADENA);
			player.sendPacket(ExResponseCommissionBuyItem.FAILED);
			return;
		}
		
		if ((_commissionItems.remove(commissionId) == null) || !commissionItem.getSaleEndTask().cancel(false))
		{
			playerInventory.addAdena(ItemProcessType.REFUND, totalPrice, player, null);
			player.sendPacket(SystemMessageId.ITEM_PURCHASE_HAS_FAILED);
			player.sendPacket(ExResponseCommissionBuyItem.FAILED);
			return;
		}
		
		if (deleteItemFromDB(commissionId))
		{
			final float discountFee = (float) commissionItem.getDiscountInPercentage() / 100;
			
			final long saleFee = (long) Math.max(MIN_REGISTRATION_AND_SALE_FEE, (totalPrice * SALE_FEE_PER_DAY) * Math.min(commissionItem.getDurationInDays(), 7));
			final long addDiscount = (long) (saleFee * discountFee);
			final MailMessage mail = new MailMessage(itemInstance.getOwnerId(), itemInstance, MailType.COMMISSION_ITEM_SOLD);
			final Mail attachement = mail.createAttachments();
			attachement.addItem(ItemProcessType.SELL, Inventory.ADENA_ID, (totalPrice - saleFee) + addDiscount, player, null);
			MailManager.getInstance().sendMessage(mail);
			
			player.sendPacket(new ExResponseCommissionBuyItem(commissionItem));
			playerInventory.addItem(ItemProcessType.BUY, itemInstance, player, null);
			
			// Notify to scripts.
			if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_AUCTION_WIN))
			{
				EventDispatcher.getInstance().notifyEventAsync(new OnPlayerAuctionWin(player, itemInstance));
			}
		}
		else
		{
			playerInventory.addAdena(ItemProcessType.REFUND, totalPrice, player, null);
			player.sendPacket(ExResponseCommissionBuyItem.FAILED);
		}
	}
	
	/**
	 * Removes a specific commission item from the database.<br>
	 * This method executes a delete query using the provided unique identifier.
	 * @param commissionId The unique ID of the commission item to remove.
	 * @return {@code true} if the deletion was successful, otherwise {@code false}.
	 */
	private boolean deleteItemFromDB(long commissionId)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(DELETE_COMMISSION_ITEM))
		{
			ps.setLong(1, commissionId);
			if (ps.executeUpdate() > 0)
			{
				return true;
			}
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Failed deleting commission item. Commission ID: " + commissionId, e);
		}
		
		return false;
	}
	
	/**
	 * Removes a sale from the active list and deletes it from the database.<br>
	 * If successful, it sends a return mail to the original owner of the item.
	 * @param commissionItem The {@code CommissionItem} that has expired.
	 */
	private void expireSale(CommissionItem commissionItem)
	{
		final long commissionId = commissionItem.getCommissionId();
		if ((_commissionItems.remove(commissionId) != null) && deleteItemFromDB(commissionId))
		{
			final MailMessage mail = new MailMessage(commissionItem.getItemInstance().getOwnerId(), commissionItem.getItemInstance(), MailType.COMMISSION_ITEM_RETURNED);
			MailManager.getInstance().sendMessage(mail);
		}
	}
	
	/**
	 * Retrieves a {@code CommissionItem} based on its unique identifier.<br>
	 * This method looks up the item in the internal cache.
	 * @param commissionId The unique ID of the commission item to find.
	 * @return The {@code CommissionItem} associated with the provided ID, or {@code null} if not found.
	 */
	public CommissionItem getCommissionItem(long commissionId)
	{
		return _commissionItems.get(commissionId);
	}
	
	/**
	 * Checks if any commission items match the given ID.<br>
	 * It searches through all active commission items.
	 * @param objectId The unique identifier of the item to check.
	 * @return {@code true} if a matching item is found, otherwise {@code false}.
	 */
	public boolean hasCommissionItems(int objectId)
	{
		for (CommissionItem item : _commissionItems.values())
		{
			if (item.getItemInstance().getObjectId() == objectId)
			{
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Checks if a specific player has an item currently listed in the commission system.<br>
	 * It searches through all active commissions to find a match for the given {@code itemId}.
	 * @param player The {@code Player} object to check.
	 * @param itemId The unique identifier of the item template.
	 * @return {@code true} if the player owns an item with that ID in a commission, otherwise {@code false}.
	 */
	public boolean hasCommissionedItemId(Player player, int itemId)
	{
		final int playerId = player.getObjectId();
		for (CommissionItem item : _commissionItems.values())
		{
			final Item itemInstance = item.getItemInstance();
			if ((itemInstance.getOwnerId() == playerId) && (itemInstance.getTemplate().getId() == itemId))
			{
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Checks if a {@code Player} is close enough to interact with an NPC.<br>
	 * This method verifies the distance between the player and the last interacted folk NPC.<br>
	 * It returns {@code true} only if the NPC is a {@code CommissionManager}.
	 * @param player The {@code Player} to check.
	 * @return {@code true} if the player can interact, otherwise {@code false}.
	 */
	public static boolean isPlayerAllowedToInteract(Player player)
	{
		final Npc npc = player.getLastFolkNPC();
		if (npc instanceof CommissionManager)
		{
			return npc.calculateDistance3D(player) <= INTERACTION_DISTANCE;
		}
		
		return false;
	}
	
	/**
	 * Retrieves the singleton instance of {@code ItemCommissionManager}.<br>
	 * This method provides a global access point to the manager.
	 * @return The active {@code ItemCommissionManager} instance.
	 */
	public static ItemCommissionManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final ItemCommissionManager INSTANCE = new ItemCommissionManager();
	}
}
