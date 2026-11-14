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
package org.l2jmobius.gameserver.entity.residences;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.l2jmobius.gameserver.entity.clan.Clan;

/**
 * Represents a player or entity participating in a residence bidding process.<br>
 * This class tracks the bidding information for specific residence auctions.
 * @author Sdw
 */
public class Bidder
{
	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
	
	private final Clan _clan;
	private final long _bid;
	private final long _time;
	
	/**
	 * Creates a new {@code Bidder} instance.<br>
	 * This constructor initializes the clan, the bid amount, and the timestamp.<br>
	 * If the provided {@code time} is {@code 0}, it defaults to the current system time.
	 * @param clan The {@code Clan} making the bid.
	 * @param bid The amount of the bid.
	 * @param time The timestamp for the bid in milliseconds.
	 */
	public Bidder(Clan clan, long bid, long time)
	{
		_clan = clan;
		_bid = bid;
		_time = time == 0 ? Instant.now().toEpochMilli() : time;
	}
	
	/**
	 * Retrieves the {@code Clan} associated with this bidder.<br>
	 * Returns the {@code Clan} object.
	 * @return The {@code Clan} of the bidder.
	 */
	public Clan getClan()
	{
		return _clan;
	}
	
	/**
	 * Retrieves the name of the clan associated with this bidder.<br>
	 * This method calls {@code getName()} on the internal {@code Clan} object.
	 * @return The name of the clan as a {@code String}.
	 */
	public String getClanName()
	{
		return _clan.getName();
	}
	
	/**
	 * Retrieves the name of the leader for the associated {@code Clan}.<br>
	 * This method calls {@code getLeaderName()} on the internal clan object.
	 * @return The name of the clan leader as a {@code String}.
	 */
	public String getLeaderName()
	{
		return _clan.getLeaderName();
	}
	
	/**
	 * Retrieves the current bid amount.<br>
	 * This value is stored as a {@code long}.
	 * @return The bid amount.
	 */
	public long getBid()
	{
		return _bid;
	}
	
	/**
	 * Retrieves the timestamp associated with this bid.<br>
	 * The value is returned as a {@code long}.
	 * @return The raw time value.
	 */
	public long getTime()
	{
		return _time;
	}
	
	/**
	 * Returns the bid time as a readable string.<br>
	 * It converts the internal {@code _time} value into a formatted date and time.<br>
	 * The format used is {@code dd/MM/yyyy HH:mm}.
	 * @return A {@code String} representing the formatted time.
	 */
	public String getFormattedTime()
	{
		return DATE_FORMAT.format(Instant.ofEpochMilli(_time).atZone(ZoneId.systemDefault()));
	}
}
