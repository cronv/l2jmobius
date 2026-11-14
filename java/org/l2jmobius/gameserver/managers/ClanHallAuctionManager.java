/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2jmobius.gameserver.managers;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.data.xml.ClanHallData;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.residences.ClanHallAuction;

/**
 * Manages the lifecycle and logic of auctions held within a {@code Clan} hall.<br>
 * It handles the creation, expiration, and processing of {@code ClanHallAuction} objects.
 * @author Sdw
 */
public class ClanHallAuctionManager
{
	private static final Logger LOGGER = Logger.getLogger(ClanHallAuctionManager.class.getName());
	
	private static final Map<Integer, ClanHallAuction> AUCTIONS = new HashMap<>();
	private static ScheduledFuture<?> _endTask;
	
	/**
	 * Initializes the auction manager and schedules periodic tasks.<br>
	 * It sets up the start and end times for weekly auctions.<br>
	 * The manager uses {@code ThreadPool} to handle these scheduled events.
	 */
	protected ClanHallAuctionManager()
	{
		final long currentTime = System.currentTimeMillis();
		
		// Schedule of the start, next Wednesday at 19:00.
		final Calendar start = Calendar.getInstance();
		start.set(Calendar.DAY_OF_WEEK, Calendar.WEDNESDAY);
		start.set(Calendar.HOUR_OF_DAY, 19);
		start.set(Calendar.MINUTE, 0);
		start.set(Calendar.SECOND, 0);
		if (start.getTimeInMillis() < currentTime)
		{
			start.add(Calendar.DAY_OF_YEAR, 1);
			while (start.get(Calendar.DAY_OF_WEEK) != Calendar.WEDNESDAY)
			{
				start.add(Calendar.DAY_OF_YEAR, 1);
			}
		}
		
		final long startDelay = Math.max(0, start.getTimeInMillis() - currentTime);
		ThreadPool.scheduleAtFixedRate(this::onStart, startDelay, 604800000); // 604800000 = 1 week
		if (startDelay > 0)
		{
			onStart();
		}
		
		// Schedule of the end, next Wednesday at 11:00.
		final Calendar end = Calendar.getInstance();
		end.set(Calendar.DAY_OF_WEEK, Calendar.WEDNESDAY);
		end.set(Calendar.HOUR_OF_DAY, 11);
		end.set(Calendar.MINUTE, 0);
		end.set(Calendar.SECOND, 0);
		if (end.getTimeInMillis() < currentTime)
		{
			end.add(Calendar.DAY_OF_YEAR, 1);
			while (end.get(Calendar.DAY_OF_WEEK) != Calendar.WEDNESDAY)
			{
				end.add(Calendar.DAY_OF_YEAR, 1);
			}
		}
		
		final long endDelay = Math.max(0, end.getTimeInMillis() - currentTime);
		_endTask = ThreadPool.scheduleAtFixedRate(this::onEnd, endDelay, 604800000); // 604800000 = 1 week
	}
	
	/**
	 * Initializes the auction system.<br>
	 * Clears all existing auctions from the {@code AUCTIONS} map.<br>
	 * Populates the map with new {@code ClanHallAuction} objects for every free hall.
	 */
	private void onStart()
	{
		LOGGER.info(getClass().getSimpleName() + ": Clan Hall Auction has started!");
		AUCTIONS.clear();
		ClanHallData.getInstance().getFreeAuctionableHall().forEach(c -> AUCTIONS.put(c.getResidenceId(), new ClanHallAuction(c.getResidenceId())));
	}
	
	/**
	 * This method is called when the auction period finishes.<br>
	 * It finalizes all active auctions in the {@code AUCTIONS} map.<br>
	 * It then clears the map and logs a completion message.
	 */
	private void onEnd()
	{
		AUCTIONS.values().forEach(ClanHallAuction::finalizeAuctions);
		AUCTIONS.clear();
		LOGGER.info(getClass().getSimpleName() + ": Clan Hall Auction has ended!");
	}
	
	/**
	 * Retrieves a specific auction based on its unique ID.<br>
	 * This method looks up the {@code ClanHallAuction} in the internal map.<br>
	 * It returns {@code null} if no auction is found for the given ID.
	 * @param clanHallId The unique identifier of the clan hall auction.
	 * @return The {@code ClanHallAuction} object, or {@code null} if not found.
	 */
	public ClanHallAuction getClanHallAuctionById(int clanHallId)
	{
		return AUCTIONS.get(clanHallId);
	}
	
	/**
	 * Retrieves the {@code ClanHallAuction} associated with a specific {@code Clan}.<br>
	 * It searches through all active auctions to find one where the clan has placed a bid.<br>
	 * Returns {@code null} if no such auction is found.
	 * @param clan The {@code Clan} object to check for an active auction.
	 * @return The matching {@code ClanHallAuction} or {@code null}.
	 */
	public ClanHallAuction getClanHallAuctionByClan(Clan clan)
	{
		for (ClanHallAuction auction : AUCTIONS.values())
		{
			if (auction.getBids().containsKey(clan.getId()))
			{
				return auction;
			}
		}
		
		return null;
	}
	
	/**
	 * Checks if a {@code Clan} has placed a bid on any auction other than the specified one.<br>
	 * This method iterates through all active auctions to find conflicting bids.
	 * @param clanHallId The ID of the specific clan hall being checked.
	 * @param clan The {@code Clan} object representing the group placing the bid.
	 * @return {@code true} if the clan has a bid on another auction, {@code false} otherwise.
	 */
	public boolean checkForClanBid(int clanHallId, Clan clan)
	{
		for (Entry<Integer, ClanHallAuction> auction : AUCTIONS.entrySet())
		{
			if ((auction.getKey() != clanHallId) && auction.getValue().getBids().containsKey(clan.getId()))
			{
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Calculates the time left until the current auction ends.<br>
	 * This method retrieves the delay from the scheduled task.
	 * @return The remaining time in milliseconds.
	 */
	public long getRemainingTime()
	{
		return _endTask.getDelay(TimeUnit.MILLISECONDS);
	}
	
	/**
	 * Provides access to the singleton instance of {@code ClanHallAuctionManager}.<br>
	 * Use this method to get the global manager for clan hall auctions.
	 * @return The single instance of {@code ClanHallAuctionManager}.
	 */
	public static ClanHallAuctionManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final ClanHallAuctionManager INSTANCE = new ClanHallAuctionManager();
	}
}
