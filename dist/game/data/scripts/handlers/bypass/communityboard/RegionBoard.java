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
package handlers.bypass.communityboard;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.cache.HtmCache;
import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.residences.ClanHallAuction;
import org.l2jmobius.gameserver.handler.CommunityBoardHandler;
import org.l2jmobius.gameserver.handler.IWriteBoardHandler;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.managers.ClanHallAuctionManager;
import org.l2jmobius.gameserver.mechanics.siege.Castle;
import org.l2jmobius.gameserver.mechanics.siege.CastleTaxType;

/**
 * Handles the logic for region-specific community boards.<br>
 * This class manages how messages are posted and displayed within specific game regions.
 * @author Zoey76, Skache
 */
public class RegionBoard implements IWriteBoardHandler
{
	private static final DateTimeFormatter SIEGE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
	
	private static final String CLAN_HALL_QUERY = "SELECT id, name, ownerId, location FROM clanhall WHERE location = ?";
	
	// Region data
	// @formatter:off
	private static final int[] REGIONS = { 1049, 1052, 1053, 1057, 1060, 1059, 1248, 1247, 1056 };
	// @formatter:on
	private static final String[] COMMANDS =
	{
		"_bbsloc"
	};
	
	private static class ClanHall
	{
		private final String name;
		private final int ownerId;
		
		public ClanHall(String name, int ownerId)
		{
			this.name = name;
			this.ownerId = ownerId;
		}
		
		public String getName()
		{
			return name;
		}
		
		public int getOwnerId()
		{
			return ownerId;
		}
	}
	
	/**
	 * Retrieves the list of available commands.<br>
	 * This method returns all valid command strings for this board.
	 * @return an array of {@code String} containing the commands.
	 */
	@Override
	public String[] getCommandList()
	{
		return COMMANDS;
	}
	
	/**
	 * Processes commands related to region information.<br>
	 * It handles displaying the list of regions and specific details for a single region.
	 * @param command The command string sent by the player.
	 * @param player The {@code Player} who executed the command.
	 * @return {@code true} if the command was handled successfully, otherwise {@code false}.
	 */
	@Override
	public boolean onCommand(String command, Player player)
	{
		if (command.equals("_bbsloc"))
		{
			CommunityBoardHandler.getInstance().addBypass(player, "Region", command);
			
			final String list = HtmCache.getInstance().getHtm(player, "data/html/CommunityBoard/region_list.html");
			final StringBuilder sb = new StringBuilder();
			for (int i = 0; i < REGIONS.length; i++)
			{
				final Castle castle = CastleManager.getInstance().getCastleById(i + 1);
				final Clan clan = ClanTable.getInstance().getClan(castle.getOwnerId());
				
				String link = list.replace("%region_id%", String.valueOf(i)).replace("%region_name%", String.valueOf(REGIONS[i])).replace("%region_owning_clan%", (clan != null ? "<a action=\"bypass _bbsclan_clanhome;" + clan.getId() + "\">" + clan.getName() + "</a>" : "NPC")).replace("%region_owning_clan_alliance%", ((clan != null) && (clan.getAllyName() != null) ? clan.getAllyName() : "None")).replace("%region_tax_rate%", castle.getTaxPercent(CastleTaxType.BUY) + "%");
				sb.append(link);
			}
			
			String html = HtmCache.getInstance().getHtm(player, "data/html/CommunityBoard/region.html");
			html = html.replace("%region_list%", sb.toString());
			CommunityBoardHandler.separateAndSend(html, player);
		}
		else if (command.startsWith("_bbsloc;"))
		{
			CommunityBoardHandler.getInstance().addBypass(player, "Region>", command);
			
			final String id = command.replace("_bbsloc;", "");
			if (!StringUtil.isNumeric(id))
			{
				LOG.warning(RegionBoard.class.getSimpleName() + ": " + player + " sent an invalid region bypass " + command + "!");
				return false;
			}
			
			final int regionId = Integer.parseInt(id);
			final Collection<Castle> castles = CastleManager.getInstance().getCastles();
			if ((regionId < 0) || (regionId >= castles.size()))
			{
				LOG.warning(RegionBoard.class.getSimpleName() + ": " + player + " requested invalid region ID " + regionId + "!");
				return false;
			}
			
			final Castle castle = CastleManager.getInstance().getCastleById(regionId + 1);
			final Clan clan = ClanTable.getInstance().getClan(castle.getOwnerId());
			String html = HtmCache.getInstance().getHtm(player, "data/html/CommunityBoard/region_show.html");
			final String castleName = castle.getName();
			html = html.replace("%regionName%", castleName).replace("%tax%", castle.getTaxPercent(CastleTaxType.BUY) + "%").replace("%lord%", clan != null ? clan.getLeaderName() : "NPC").replace("%clanName%", (clan != null ? "<a action=\"bypass _bbsclan_clanhome;" + clan.getId() + "\">" + clan.getName() + "</a>" : "NPC")).replace("%allyName%", ((clan != null) && (clan.getAllyName() != null) ? clan.getAllyName() : "None")).replace("%siegeDate%", SIEGE_FORMAT.format(Instant.ofEpochMilli(castle.getSiegeDate().getTimeInMillis()).atZone(ZoneId.systemDefault())));
			
			final StringBuilder hallsList = new StringBuilder();
			hallsList.append("<br><br><table width=610 bgcolor=A7A19A><tr><td width=5></td><td width=200>Clan Hall Name</td><td width=200>Owning Clan</td><td width=200>Clan Leader Name</td><td width=5></td></tr></table><br1>");
			final Map<Integer, ClanHall> clanHalls = getClanHallsByLocation(castleName);
			if (clanHalls.isEmpty())
			{
				hallsList.append("<table width=610><tr><td align=\"left\">There are no clan halls in this region.</td></tr></table>");
			}
			else
			{
				for (Entry<Integer, ClanHall> entry : clanHalls.entrySet())
				{
					final ClanHall hall = entry.getValue();
					Clan hallOwner = ClanTable.getInstance().getClan(hall.getOwnerId());
					
					// Halls up for sale show when the weekly auction closes.
					final ClanHallAuction auction = ClanHallAuctionManager.getInstance().getClanHallAuctionById(entry.getKey());
					final String auctionDate = (auction != null) ? SIEGE_FORMAT.format(Instant.ofEpochMilli(System.currentTimeMillis() + ClanHallAuctionManager.getInstance().getRemainingTime()).atZone(ZoneId.systemDefault())) : "None";
					hallsList.append("<table><tr><td width=5></td><td width=150>").append(hall.getName()).append("</td><td width=150>").append(hallOwner != null ? "<a action=\"bypass _bbsclan_clanhome;" + hallOwner.getId() + "\">" + hallOwner.getName() + "</a>" : "None").append("</td><td width=150>").append(hallOwner != null ? hallOwner.getLeaderName() : "None").append("</td><td width=150>").append(auctionDate).append("</td><td width=5></td></tr></table><br1><img src=\"L2UI.Squaregray\" width=605 height=1><br1>");
				}
			}
			
			html = html.replace("%hallsList%", hallsList.toString());
			CommunityBoardHandler.separateAndSend(html, player);
		}
		
		return true;
	}
	
	/**
	 * Retrieves all {@code ClanHall} objects for a specific location.<br>
	 * This method queries the database using the provided {@code location} string.<br>
	 * It returns a map where the key is the hall ID and the value is the {@code ClanHall} object.
	 * @param location The name of the location to search for.
	 * @return A {@code Map} containing the halls found at that location.
	 */
	private Map<Integer, ClanHall> getClanHallsByLocation(String location)
	{
		final Map<Integer, ClanHall> clanHalls = new HashMap<>();
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(CLAN_HALL_QUERY))
		{
			ps.setString(1, location);
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					String name = rs.getString("name");
					int ownerId = rs.getInt("ownerId");
					ClanHall hall = new ClanHall(name, ownerId);
					clanHalls.put(rs.getInt("id"), hall);
				}
			}
		}
		catch (SQLException e)
		{
			LOG.warning(RegionBoard.class.getSimpleName() + ": Could not fetch clan halls data. " + e.getMessage());
		}
		
		return clanHalls;
	}
	
	/**
	 * Processes a command to write information onto the community board.<br>
	 * This method handles the input arguments provided by the {@code Player}.<br>
	 * It currently returns {@code false} as it is not yet implemented.
	 * @param player The {@code Player} who sent the command.
	 * @param arg1 The first argument for the command.
	 * @param arg2 The second argument for the command.
	 * @param arg3 The third argument for the command.
	 * @param arg4 The fourth argument for the command.
	 * @param arg5 The fifth argument for the command.
	 * @return {@code false} because the method is not yet implemented.
	 */
	@Override
	public boolean writeCommunityBoardCommand(Player player, String arg1, String arg2, String arg3, String arg4, String arg5)
	{
		// TODO: Implement.
		return false;
	}
}
