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
package org.l2jmobius.gameserver.entity.residences;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.data.xml.ClanHallData;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.itemcontainer.Inventory;

/**
 * Manages the auction system specifically for items within a {@code Clan} hall.<br>
 * This class handles listing, viewing, and purchasing items from the residence auction house.
 * @author Sdw, Mobius
 */
public class ClanHallAuction
{
	private static final Logger LOGGER = Logger.getLogger(ClanHallAuction.class.getName());
	
	private final int _clanHallId;
	private static final String LOAD_CLANHALL_BIDDERS = "SELECT * FROM clanhall_auctions_bidders WHERE clanHallId=?";
	private static final String DELETE_CLANHALL_BIDDERS = "DELETE FROM clanhall_auctions_bidders WHERE clanHallId=?";
	private static final String INSERT_CLANHALL_BIDDER = "REPLACE INTO clanhall_auctions_bidders (clanHallId, clanId, bid, bidTime) VALUES (?,?,?,?)";
	private static final String DELETE_CLANHALL_BIDDER = "DELETE FROM clanhall_auctions_bidders WHERE clanId=?";
	
	/**
	 * Creates a new instance of {@code ClanHallAuction}.<br>
	 * This constructor sets the unique identifier for the auction.<br>
	 * It also triggers the loading of existing bidders from the database.
	 * @param clanHallId The unique ID of the clan hall associated with this auction.
	 */
	public ClanHallAuction(int clanHallId)
	{
		_clanHallId = clanHallId;
		loadBidder();
	}
	
	/**
	 * Loads all bidders from the database for a specific clan hall.<br>
	 * It retrieves records using {@code LOAD_CLANHALL_BIDDERS}.<br>
	 * Each bidder is added to the current auction instance.
	 */
	private void loadBidder()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(LOAD_CLANHALL_BIDDERS))
		{
			ps.setInt(1, _clanHallId);
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					final Clan clan = ClanTable.getInstance().getClan(rs.getInt("clanId"));
					addBid(clan, rs.getLong("bid"), rs.getLong("bidTime"));
				}
			}
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, "Failed loading clan hall auctions bidder for clan hall " + _clanHallId + ".", e);
		}
	}
	
	private Map<Integer, Bidder> _bidders;
	
	/**
	 * Retrieves all current bids for the auction.<br>
	 * Returns an empty {@code Map} if no bidders exist.
	 * @return A {@code Map} where the key is the bidder ID and the value is a {@code Bidder} object.
	 */
	public Map<Integer, Bidder> getBids()
	{
		return _bidders == null ? Collections.emptyMap() : _bidders;
	}
	
	/**
	 * Adds a new bid for a specific {@code Clan}.<br>
	 * This method records the amount and current system time.
	 * @param clan The {@code Clan} placing the bid.
	 * @param bid The amount of the bid.
	 */
	public void addBid(Clan clan, long bid)
	{
		addBid(clan, bid, System.currentTimeMillis());
	}
	
	/**
	 * Adds a new bid to the auction for a specific {@code Clan}.<br>
	 * This method saves the bid to the database and updates the local bidder map.
	 * @param clan The {@code Clan} placing the bid.
	 * @param bid The amount of the bid.
	 * @param bidTime The timestamp when the bid was placed.
	 */
	public void addBid(Clan clan, long bid, long bidTime)
	{
		if (_bidders == null)
		{
			synchronized (this)
			{
				if (_bidders == null)
				{
					_bidders = new ConcurrentHashMap<>();
				}
			}
		}
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(INSERT_CLANHALL_BIDDER))
		{
			ps.setInt(1, _clanHallId);
			
			final int clanId = clan.getId();
			ps.setInt(2, clanId);
			ps.setLong(3, bid);
			ps.setLong(4, bidTime);
			ps.execute();
			
			_bidders.put(clanId, new Bidder(clan, bid, bidTime));
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, "Failed insert clan hall auctions bidder " + clan.getName() + " for clan hall " + _clanHallId + ".", e);
		}
	}
	
	/**
	 * Removes a bid associated with a specific {@code Clan}.<br>
	 * This method updates the internal map and deletes the record from the database.
	 * @param clan The {@code Clan} whose bid should be removed.
	 */
	public void removeBid(Clan clan)
	{
		final int clanId = clan.getId();
		getBids().remove(clanId);
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(DELETE_CLANHALL_BIDDER))
		{
			ps.setInt(1, clanId);
			ps.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Failed clearing bidder " + clan.getName() + " for clan hall " + _clanHallId + ": ", e);
		}
	}
	
	/**
	 * Retrieves the highest bid amount for this auction.<br>
	 * It returns the minimum bid if no bids have been placed.
	 * @return The maximum {@code long} value of all current bids.
	 */
	public long getHighestBid()
	{
		final ClanHall clanHall = ClanHallData.getInstance().getClanHallById(_clanHallId);
		return getBids().values().stream().mapToLong(Bidder::getBid).max().orElse(clanHall.getMinBid());
	}
	
	/**
	 * Retrieves the bid amount for a specific clan.<br>
	 * This method looks up the {@code Clan} in the current auction bids.
	 * @param clan The {@code Clan} object to check.
	 * @return The bid value as a {@code long}.
	 */
	public long getClanBid(Clan clan)
	{
		return getBids().get(clan.getId()).getBid();
	}
	
	/**
	 * Retrieves the bidder with the highest bid amount.<br>
	 * This method searches through all current bids.<br>
	 * It returns an {@code Optional} containing the {@code Bidder}.<br>
	 * If no bids exist, it returns {@code Optional.empty()}.
	 * @return An {@code Optional} containing the {@code Bidder} with the highest bid or empty if none found.
	 */
	public Optional<Bidder> getHighestBidder()
	{
		return getBids().values().stream().max(Comparator.comparingLong(Bidder::getBid).thenComparing(Comparator.comparingLong(Bidder::getTime).reversed())); // Equal bids go to the earliest bidder.
	}
	
	/**
	 * Returns the total number of bids placed for this auction.<br>
	 * It calculates the size of the current bid map.
	 * @return The count of all active bids.
	 */
	public int getBidCount()
	{
		return getBids().values().size();
	}
	
	/**
	 * Returns the bid amount to the winner's clan warehouse.<br>
	 * This method processes a refund for the {@code Bidder}.<br>
	 * The adena is added using the {@code ItemProcessType.REFUND} type.
	 * @param bidder The {@code Bidder} who will receive the returned adena.
	 */
	public void returnAdenas(Bidder bidder)
	{
		bidder.getClan().getWarehouse().addItem(ItemProcessType.REFUND, Inventory.ADENA_ID, bidder.getBid(), null, null);
	}
	
	/**
	 * Completes the auction process for a specific clan hall.<br>
	 * This method assigns ownership to the highest bidder and clears all current bids.<br>
	 * It also removes the bidding data from the database.
	 */
	public void finalizeAuctions()
	{
		final Optional<Bidder> potentialHighestBidder = getHighestBidder();
		if (potentialHighestBidder.isPresent())
		{
			final Bidder highestBidder = potentialHighestBidder.get();
			final ClanHall clanHall = ClanHallData.getInstance().getClanHallById(_clanHallId);
			clanHall.setOwner(highestBidder.getClan());
			getBids().clear();
			
			try (Connection con = DatabaseFactory.getConnection();
				PreparedStatement ps = con.prepareStatement(DELETE_CLANHALL_BIDDERS))
			{
				ps.setInt(1, _clanHallId);
				ps.execute();
			}
			catch (Exception e)
			{
				LOGGER.log(Level.SEVERE, "Failed clearing bidder for clan hall " + _clanHallId + ": ", e);
			}
		}
	}
	
	/**
	 * Retrieves the unique identifier for the clan hall.<br>
	 * This ID is used to identify which auction this object belongs to.
	 * @return The {@code int} value of the clan hall ID.
	 */
	public int getClanHallId()
	{
		return _clanHallId;
	}
}
