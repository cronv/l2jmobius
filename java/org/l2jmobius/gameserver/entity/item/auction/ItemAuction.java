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
package org.l2jmobius.gameserver.entity.item.auction;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.holders.ItemInfo;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * Represents an individual auction listing for a specific item.<br>
 * This class manages the data and state of items currently listed in the auction house.
 * @author Forsaiken, Mobius
 */
public class ItemAuction
{
	private static final Logger LOGGER = Logger.getLogger(ItemAuction.class.getName());
	private static final long ENDING_TIME_EXTEND_5 = TimeUnit.MILLISECONDS.convert(5, TimeUnit.MINUTES);
	private static final long ENDING_TIME_EXTEND_3 = TimeUnit.MILLISECONDS.convert(3, TimeUnit.MINUTES);
	
	private final int _auctionId;
	private final int _instanceId;
	private final long _startingTime;
	private volatile long _endingTime;
	private final AuctionItem _auctionItem;
	private final List<ItemAuctionBid> _auctionBids;
	private final Object _auctionStateLock;
	
	private ItemAuctionState _auctionState;
	private ItemAuctionExtendState _scheduledAuctionEndingExtendState;
	private ItemAuctionExtendState _auctionEndingExtendState;
	
	private final ItemInfo _itemInfo;
	
	private ItemAuctionBid _highestBid;
	private int _lastBidPlayerObjId;
	
	// SQL
	private static final String DELETE_ITEM_AUCTION_BID = "DELETE FROM item_auction_bid WHERE auctionId = ? AND playerObjId = ?";
	private static final String INSERT_ITEM_AUCTION_BID = "INSERT INTO item_auction_bid (auctionId, playerObjId, playerBid) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE playerBid = ?";
	
	/**
	 * Creates a new {@code ItemAuction} with default values.<br>
	 * This constructor initializes the bid list as an empty {@code ArrayList}.<br>
	 * The initial state is set to {@code ItemAuctionState.CREATED}.
	 * @param auctionId The unique identifier for the auction.
	 * @param instanceId The unique identifier for the item instance.
	 * @param startingTime The timestamp when the auction begins.
	 * @param endingTime The timestamp when the auction ends.
	 * @param auctionItem The {@code AuctionItem} being sold.
	 */
	public ItemAuction(int auctionId, int instanceId, long startingTime, long endingTime, AuctionItem auctionItem)
	{
		this(auctionId, instanceId, startingTime, endingTime, auctionItem, new ArrayList<>(), ItemAuctionState.CREATED);
	}
	
	/**
	 * Creates a new {@code ItemAuction} instance with all required data.<br>
	 * This constructor initializes the auction state and identifies the highest bid from the provided list.<br>
	 * It also creates a new item instance for the auction.
	 * @param auctionId The unique identifier for the auction.
	 * @param instanceId The unique identifier for the item instance.
	 * @param startingTime The timestamp when the auction begins.
	 * @param endingTime The timestamp when the auction ends.
	 * @param auctionItem The {@code AuctionItem} data associated with this auction.
	 * @param auctionBids A list of all bids placed on this auction.
	 * @param auctionState The current state of the auction.
	 */
	public ItemAuction(int auctionId, int instanceId, long startingTime, long endingTime, AuctionItem auctionItem, List<ItemAuctionBid> auctionBids, ItemAuctionState auctionState)
	{
		_auctionId = auctionId;
		_instanceId = instanceId;
		_startingTime = startingTime;
		_endingTime = endingTime;
		_auctionItem = auctionItem;
		_auctionBids = auctionBids;
		_auctionState = auctionState;
		_auctionStateLock = new Object();
		_scheduledAuctionEndingExtendState = ItemAuctionExtendState.INITIAL;
		_auctionEndingExtendState = ItemAuctionExtendState.INITIAL;
		
		final Item item = _auctionItem.createNewItemInstance();
		_itemInfo = new ItemInfo(item);
		World.removeObject(item);
		
		for (ItemAuctionBid bid : _auctionBids)
		{
			if ((_highestBid == null) || (_highestBid.getLastBid() < bid.getLastBid()))
			{
				_highestBid = bid;
			}
		}
	}
	
	/**
	 * Retrieves the current state of the auction.<br>
	 * This method uses a lock to ensure thread safety while accessing the data.
	 * @return the current {@code ItemAuctionState} object.
	 */
	public ItemAuctionState getAuctionState()
	{
		final ItemAuctionState auctionState;
		
		synchronized (_auctionStateLock)
		{
			auctionState = _auctionState;
		}
		
		return auctionState;
	}
	
	/**
	 * Updates the current state of the auction.<br>
	 * This method uses a thread-safe check to ensure the state only changes if it matches the expected value.<br>
	 * It saves the new state to the database upon success.
	 * @param expected The {@code ItemAuctionState} that the auction is currently in.
	 * @param wanted The new {@code ItemAuctionState} to set.
	 * @return {@code true} if the state was successfully updated, or {@code false} if the current state did not match {@code expected}.
	 */
	public boolean setAuctionState(ItemAuctionState expected, ItemAuctionState wanted)
	{
		synchronized (_auctionStateLock)
		{
			if (_auctionState != expected)
			{
				return false;
			}
			
			_auctionState = wanted;
			storeMe();
			return true;
		}
	}
	
	/**
	 * Retrieves the unique identifier for this auction.
	 * @return the {@code int} ID of the auction.
	 */
	public int getAuctionId()
	{
		return _auctionId;
	}
	
	/**
	 * Retrieves the unique identifier for this specific auction instance.<br>
	 * This value is used to distinguish between different auctions of the same item type.
	 * @return The {@code int} instance ID.
	 */
	public int getInstanceId()
	{
		return _instanceId;
	}
	
	/**
	 * Retrieves the information for the item being auctioned.
	 * @return the {@code ItemInfo} object associated with this auction.
	 */
	public ItemInfo getItemInfo()
	{
		return _itemInfo;
	}
	
	/**
	 * Creates a new instance of an {@code Item}.<br>
	 * This method delegates the creation to the underlying {@code AuctionItem}.
	 * @return A new {@code Item} object.
	 */
	public Item createNewItemInstance()
	{
		return _auctionItem.createNewItemInstance();
	}
	
	/**
	 * Retrieves the initial bid amount for this auction.<br>
	 * This value is fetched from the {@code AuctionItem} object.
	 * @return The starting bid as a {@code long}.
	 */
	public long getAuctionInitBid()
	{
		return _auctionItem.getAuctionInitBid();
	}
	
	/**
	 * Retrieves the current highest bid for this auction.
	 * @return the {@code ItemAuctionBid} object representing the highest bid.
	 */
	public ItemAuctionBid getHighestBid()
	{
		return _highestBid;
	}
	
	/**
	 * Retrieves the current state of the auction ending extension.<br>
	 * This value represents the status of the time extension for an auction.
	 * @return the {@code ItemAuctionExtendState} of the auction ending extension.
	 */
	public ItemAuctionExtendState getAuctionEndingExtendState()
	{
		return _auctionEndingExtendState;
	}
	
	/**
	 * Retrieves the state of the scheduled auction ending extension.<br>
	 * This is used to track extensions that are planned for the future.
	 * @return the current {@code ItemAuctionExtendState} for the scheduled extension.
	 */
	public ItemAuctionExtendState getScheduledAuctionEndingExtendState()
	{
		return _scheduledAuctionEndingExtendState;
	}
	
	/**
	 * Updates the scheduled extension state for the auction ending.<br>
	 * This method sets the {@code _scheduledAuctionEndingExtendState} field.
	 * @param state The new {@code ItemAuctionExtendState} to apply.
	 */
	public void setScheduledAuctionEndingExtendState(ItemAuctionExtendState state)
	{
		_scheduledAuctionEndingExtendState = state;
	}
	
	/**
	 * Retrieves the time when the auction started.<br>
	 * The value is returned as a {@code long}.
	 * @return The starting timestamp of the auction.
	 */
	public long getStartingTime()
	{
		return _startingTime;
	}
	
	/**
	 * Retrieves the current ending time of the auction.<br>
	 * The value is returned as a {@code long}.
	 * @return The auction's ending time.
	 */
	public long getEndingTime()
	{
		return _endingTime;
	}
	
	/**
	 * Calculates the time remaining until the auction ends.<br>
	 * It returns the difference between the {@code _endingTime} and the current system time.<br>
	 * If the auction has already ended, it returns {@code 0}.
	 * @return The number of milliseconds remaining before the auction finishes.
	 */
	public long getStartingTimeRemaining()
	{
		return Math.max(_endingTime - System.currentTimeMillis(), 0);
	}
	
	/**
	 * Calculates the time remaining until the auction ends.<br>
	 * It returns the difference between {@code _endingTime} and the current system time.<br>
	 * If the auction has already finished, it returns {@code 0}.
	 * @return The number of milliseconds remaining before the auction finishes.
	 */
	public long getFinishingTimeRemaining()
	{
		return Math.max(_endingTime - System.currentTimeMillis(), 0);
	}
	
	/**
	 * Saves the current auction data to the database.<br>
	 * It inserts a new record or updates the existing state if it already exists.<br>
	 * Any {@code SQLException} is caught and logged as a warning.
	 */
	public void storeMe()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement("INSERT INTO item_auction (auctionId,instanceId,auctionItemId,startingTime,endingTime,auctionStateId) VALUES (?,?,?,?,?,?) ON DUPLICATE KEY UPDATE auctionStateId=?"))
		{
			statement.setInt(1, _auctionId);
			statement.setInt(2, _instanceId);
			final int auctionItemId = _auctionItem.getAuctionItemId();
			statement.setInt(3, auctionItemId);
			statement.setLong(4, _startingTime);
			statement.setLong(5, _endingTime);
			final byte stateId = _auctionState.getStateId();
			statement.setByte(6, stateId);
			statement.setByte(7, stateId);
			statement.execute();
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, "storeMe: ", e);
		}
	}
	
	/**
	 * Retrieves the previous bidder ID and updates it to the new one.<br>
	 * This method performs an atomic-like swap of the {@code _lastBidPlayerObjId}.
	 * @param playerObjId The new object ID of the player who placed the bid.
	 * @return The object ID of the player who placed the previous bid.
	 */
	public int getAndSetLastBidPlayerObjectId(int playerObjId)
	{
		final int lastBid = _lastBidPlayerObjId;
		_lastBidPlayerObjId = playerObjId;
		return lastBid;
	}
	
	/**
	 * Updates the bid information for a specific player.<br>
	 * This method handles both updating existing bids and removing them.
	 * @param bid The {@code ItemAuctionBid} object to be processed.
	 * @param delete Set to {@code true} to remove the bid, or {@code false} to update it.
	 */
	private void updatePlayerBid(ItemAuctionBid bid, boolean delete)
	{
		updatePlayerBidInternal(bid, delete);
	}
	
	/**
	 * Updates the database record for a player's bid in an auction.<br>
	 * This method handles both inserting new bids and deleting existing ones.
	 * @param bid The {@code ItemAuctionBid} object containing the bid details.
	 * @param delete Set to {@code true} to remove the bid from the database, or {@code false} to insert it.
	 */
	private void updatePlayerBidInternal(ItemAuctionBid bid, boolean delete)
	{
		final String query = delete ? DELETE_ITEM_AUCTION_BID : INSERT_ITEM_AUCTION_BID;
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(query))
		{
			ps.setInt(1, _auctionId);
			ps.setInt(2, bid.getPlayerObjId());
			if (!delete)
			{
				final long lastBid = bid.getLastBid();
				ps.setLong(3, lastBid);
				ps.setLong(4, lastBid);
			}
			
			ps.execute();
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, "updatePlayerBidInternal: ", e);
		}
	}
	
	/**
	 * Registers a new bid for the current auction.<br>
	 * Validates the price against minimum and maximum limits.<br>
	 * Deducts the required amount of currency from the {@code Player}.<br>
	 * Updates the player's highest bid in the auction list.
	 * @param player The {@code Player} submitting the bid.
	 * @param newBid The amount of currency offered for the item.
	 */
	public void registerBid(Player player, long newBid)
	{
		if (player == null)
		{
			throw new NullPointerException();
		}
		
		if (newBid < _auctionItem.getAuctionInitBid())
		{
			player.sendPacket(SystemMessageId.YOUR_BID_PRICE_MUST_BE_HIGHER_THAN_THE_MINIMUM_PRICE_CURRENTLY_BEING_BID);
			return;
		}
		
		if (newBid > 100000000000L)
		{
			player.sendPacket(SystemMessageId.THE_HIGHEST_BID_IS_OVER_999_9_BILLION_THEREFORE_YOU_CANNOT_PLACE_A_BID);
			return;
		}
		
		if (getAuctionState() != ItemAuctionState.STARTED)
		{
			return;
		}
		
		final int playerObjId = player.getObjectId();
		
		synchronized (_auctionBids)
		{
			if ((_highestBid != null) && (newBid < _highestBid.getLastBid()))
			{
				player.sendPacket(SystemMessageId.YOUR_BID_MUST_BE_HIGHER_THAN_THE_CURRENT_HIGHEST_BID);
				return;
			}
			
			ItemAuctionBid bid = getBidFor(playerObjId);
			if (bid == null)
			{
				if (!reduceItemCount(player, newBid))
				{
					player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_ENOUGH_ADENA_FOR_THIS_BID);
					return;
				}
				
				bid = new ItemAuctionBid(playerObjId, newBid);
				_auctionBids.add(bid);
			}
			else
			{
				if (!bid.isCanceled())
				{
					final long lastBidValue = bid.getLastBid();
					if (newBid < lastBidValue) // just another check
					{
						player.sendPacket(SystemMessageId.YOUR_BID_MUST_BE_HIGHER_THAN_THE_CURRENT_HIGHEST_BID);
						return;
					}
					
					if (!reduceItemCount(player, newBid - lastBidValue))
					{
						player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_ENOUGH_ADENA_FOR_THIS_BID);
						return;
					}
				}
				else if (!reduceItemCount(player, newBid))
				{
					player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_ENOUGH_ADENA_FOR_THIS_BID);
					return;
				}
				
				bid.setLastBid(newBid);
			}
			
			onPlayerBid(player, bid);
			updatePlayerBid(bid, false);
			
			final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_SUBMITTED_A_BID_FOR_THE_AUCTION_OF_S1);
			sm.addLong(newBid);
			player.sendPacket(sm);
		}
	}
	
	/**
	 * Processes a new bid placed by a player on an auction.<br>
	 * This method updates the highest bid and handles auction time extensions.<br>
	 * It sends notifications to players who have been outbid.
	 * @param player The {@code Player} who is placing the bid.
	 * @param bid The {@code ItemAuctionBid} containing the new bid details.
	 */
	private void onPlayerBid(Player player, ItemAuctionBid bid)
	{
		if (_highestBid == null)
		{
			_highestBid = bid;
		}
		else if (_highestBid.getLastBid() < bid.getLastBid())
		{
			final Player old = _highestBid.getPlayer();
			if (old != null)
			{
				old.sendPacket(new SystemMessage(SystemMessageId.YOU_WERE_OUTBID_THE_NEW_HIGHEST_BID_IS_S1_ADENA).addLong(bid.getLastBid()));
			}
			
			_highestBid = bid;
		}
		
		if ((_endingTime - System.currentTimeMillis()) <= (1000 * 60 * 10)) // 10 minutes.
		{
			switch (_auctionEndingExtendState)
			{
				case INITIAL:
				{
					_auctionEndingExtendState = ItemAuctionExtendState.EXTEND_BY_5_MIN;
					_endingTime += ENDING_TIME_EXTEND_5;
					broadcastToAllBidders(new SystemMessage(SystemMessageId.BIDDER_EXISTS_THE_AUCTION_TIME_HAS_BEEN_EXTENDED_FOR_5_MIN));
					break;
				}
				case EXTEND_BY_5_MIN:
				{
					final int objectId = player.getObjectId();
					if (getAndSetLastBidPlayerObjectId(objectId) != objectId)
					{
						_auctionEndingExtendState = ItemAuctionExtendState.EXTEND_BY_3_MIN;
						_endingTime += ENDING_TIME_EXTEND_3;
						broadcastToAllBidders(new SystemMessage(SystemMessageId.BIDDER_EXISTS_AUCTION_TIME_HAS_BEEN_EXTENDED_FOR_3_MIN));
					}
					break;
				}
				case EXTEND_BY_3_MIN:
				{
					final int objectId = player.getObjectId();
					if ((GeneralConfig.ALT_ITEM_AUCTION_TIME_EXTENDS_ON_BID > 0) && (getAndSetLastBidPlayerObjectId(objectId) != objectId))
					{
						_auctionEndingExtendState = ItemAuctionExtendState.EXTEND_BY_CONFIG_PHASE_A;
						_endingTime += GeneralConfig.ALT_ITEM_AUCTION_TIME_EXTENDS_ON_BID;
					}
					break;
				}
				case EXTEND_BY_CONFIG_PHASE_A:
				{
					final int objectId = player.getObjectId();
					if ((getAndSetLastBidPlayerObjectId(objectId) != objectId) && (_scheduledAuctionEndingExtendState == ItemAuctionExtendState.EXTEND_BY_CONFIG_PHASE_B))
					{
						_auctionEndingExtendState = ItemAuctionExtendState.EXTEND_BY_CONFIG_PHASE_B;
						_endingTime += GeneralConfig.ALT_ITEM_AUCTION_TIME_EXTENDS_ON_BID;
					}
					break;
				}
				case EXTEND_BY_CONFIG_PHASE_B:
				{
					final int objectId = player.getObjectId();
					if ((getAndSetLastBidPlayerObjectId(objectId) != objectId) && (_scheduledAuctionEndingExtendState == ItemAuctionExtendState.EXTEND_BY_CONFIG_PHASE_A))
					{
						_endingTime += GeneralConfig.ALT_ITEM_AUCTION_TIME_EXTENDS_ON_BID;
						_auctionEndingExtendState = ItemAuctionExtendState.EXTEND_BY_CONFIG_PHASE_A;
					}
					break;
				}
			}
		}
	}
	
	/**
	 * Sends a packet to every player who has placed a bid on this auction.<br>
	 * This operation is executed asynchronously using a thread pool.
	 * @param packet The {@code ServerPacket} to be sent to the bidders.
	 */
	public void broadcastToAllBidders(ServerPacket packet)
	{
		ThreadPool.execute(() -> broadcastToAllBiddersInternal(packet));
	}
	
	/**
	 * Sends a specific packet to every player who has placed a bid on this auction.<br>
	 * It iterates through the list of {@code ItemAuctionBid} objects.<br>
	 * If a valid {@code Player} is found, it delivers the {@code ServerPacket}.
	 * @param packet The {@code ServerPacket} to be sent to all bidders.
	 */
	public void broadcastToAllBiddersInternal(ServerPacket packet)
	{
		for (int i = _auctionBids.size(); i-- > 0;)
		{
			final ItemAuctionBid bid = _auctionBids.get(i);
			if (bid != null)
			{
				final Player player = bid.getPlayer();
				if (player != null)
				{
					player.sendPacket(packet);
				}
			}
		}
	}
	
	/**
	 * Cancels an existing bid made by the specified player.<br>
	 * This method returns {@code false} if the auction state is invalid or if no valid bid exists.<br>
	 * It also returns {@code false} if the player currently holds the highest bid but the reserve has not been met.<br>
	 * If successful, it refunds the items to the player and sends a confirmation packet.
	 * @param player The {@code Player} who is attempting to cancel their bid.
	 * @return {@code true} if the bid was successfully canceled, or {@code false} otherwise.
	 */
	public boolean cancelBid(Player player)
	{
		if (player == null)
		{
			throw new NullPointerException();
		}
		
		switch (getAuctionState())
		{
			case CREATED:
			{
				return false;
			}
			case FINISHED:
			{
				if (_startingTime < (System.currentTimeMillis() - TimeUnit.MILLISECONDS.convert(GeneralConfig.ALT_ITEM_AUCTION_EXPIRED_AFTER, TimeUnit.DAYS)))
				{
					return false;
				}
				break;
			}
		}
		
		final int playerObjId = player.getObjectId();
		
		synchronized (_auctionBids)
		{
			if (_highestBid == null)
			{
				return false;
			}
			
			final int bidIndex = getBidIndexFor(playerObjId);
			if (bidIndex == -1)
			{
				return false;
			}
			
			final ItemAuctionBid bid = _auctionBids.get(bidIndex);
			final ItemAuctionState auctionState = getAuctionState();
			if (bid.getPlayerObjId() == _highestBid.getPlayerObjId())
			{
				// Can't return winning bid.
				if (auctionState == ItemAuctionState.FINISHED)
				{
					return false;
				}
				
				player.sendPacket(SystemMessageId.YOU_VE_ALREADY_MADE_A_BID);
				return true;
			}
			
			if (bid.isCanceled())
			{
				return false;
			}
			
			increaseItemCount(player, bid.getLastBid());
			bid.cancelBid();
			
			// Delete bid from database if auction already finished.
			updatePlayerBid(bid, auctionState == ItemAuctionState.FINISHED);
			
			player.sendPacket(SystemMessageId.YOU_HAVE_CANCELLED_YOUR_BID);
		}
		
		return true;
	}
	
	/**
	 * Removes all canceled bids from the auction.<br>
	 * This method updates the status of players who had canceled bids.<br>
	 * It throws an {@code IllegalStateException} if the auction is not in the {@code FINISHED} state.
	 */
	public void clearCanceledBids()
	{
		if (getAuctionState() != ItemAuctionState.FINISHED)
		{
			throw new IllegalStateException("Attempt to clear canceled bids for non-finished auction");
		}
		
		synchronized (_auctionBids)
		{
			for (ItemAuctionBid bid : _auctionBids)
			{
				if ((bid == null) || !bid.isCanceled())
				{
					continue;
				}
				
				updatePlayerBid(bid, true);
			}
		}
	}
	
	/**
	 * Deducts a specific amount of Adena from the player's balance.<br>
	 * This method is used to process auction fees.<br>
	 * It sends an error message if the player has insufficient funds.
	 * @param player The {@code Player} whose currency will be reduced.
	 * @param count The amount of Adena to remove.
	 * @return {@code true} if the reduction was successful, or {@code false} otherwise.
	 */
	private boolean reduceItemCount(Player player, long count)
	{
		if (!player.reduceAdena(ItemProcessType.FEE, count, player, true))
		{
			player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_ENOUGH_ADENA_FOR_THIS_BID);
			return false;
		}
		
		return true;
	}
	
	/**
	 * Adds a specific amount of currency to the {@code Player}.<br>
	 * This method uses the {@code ItemProcessType.REFUND} type.
	 * @param player The {@code Player} receiving the currency.
	 * @param count The amount of currency to add.
	 */
	private void increaseItemCount(Player player, long count)
	{
		player.addAdena(ItemProcessType.REFUND, count, player, true);
	}
	
	/**
	 * Retrieves the last bid amount made by a specific player.<br>
	 * It searches for the most recent bid associated with the {@code Player}.<br>
	 * If no bid is found, it returns {@code -1L}.
	 * @param player The {@code Player} whose last bid needs to be retrieved.
	 * @return The amount of the last bid or {@code -1L} if none exists.
	 */
	public long getLastBid(Player player)
	{
		final ItemAuctionBid bid = getBidFor(player.getObjectId());
		return bid != null ? bid.getLastBid() : -1L;
	}
	
	/**
	 * Retrieves the bid associated with a specific player.<br>
	 * It searches for the bid based on the provided {@code playerObjId}.
	 * @param playerObjId The unique identifier of the player.
	 * @return The {@code ItemAuctionBid} object if found, or {@code null} otherwise.
	 */
	public ItemAuctionBid getBidFor(int playerObjId)
	{
		final int index = getBidIndexFor(playerObjId);
		return index != -1 ? _auctionBids.get(index) : null;
	}
	
	/**
	 * Finds the position of a bid made by a specific player.<br>
	 * It searches through the {@code _auctionBids} list in reverse order.<br>
	 * Returns the index if found, or -1 if no matching bid exists.
	 * @param playerObjId The unique identifier of the player to search for.
	 * @return The zero-based index of the bid in the list, or -1.
	 */
	private int getBidIndexFor(int playerObjId)
	{
		for (int i = _auctionBids.size(); i-- > 0;)
		{
			final ItemAuctionBid bid = _auctionBids.get(i);
			if ((bid != null) && (bid.getPlayerObjId() == playerObjId))
			{
				return i;
			}
		}
		
		return -1;
	}
}
