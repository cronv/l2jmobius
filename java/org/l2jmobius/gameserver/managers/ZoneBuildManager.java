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

import java.awt.Color;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.handler.AdminCommandHandler;
import org.l2jmobius.gameserver.network.serverpackets.ExServerPrimitive;

/**
 * Manages the creation and persistence of custom zone builds.<br>
 * This class handles saving, loading, and displaying building data for specific areas.
 * @author Mobius
 */
public class ZoneBuildManager
{
	private static final Logger LOGGER = Logger.getLogger(ZoneBuildManager.class.getName());
	
	private static final String HTML_DELETE_BUTTON = "<button value=\"Delete\" action=\"bypass -h admin_zone_build_delete %d\" width=65 height=21 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">";
	private static final Map<Player, List<Location>> PLAYER_LOCATIONS = new ConcurrentHashMap<>();
	
	/**
	 * Adds a new coordinate to the current player's zone.<br>
	 * This method saves the {@code Location} and updates the display.<br>
	 * It also triggers an admin command for the {@code Player}.
	 * @param player The {@code Player} who owns the zone.
	 * @param location The {@code Location} to be added.
	 */
	public void addPoint(Player player, Location location)
	{
		List<Location> locations = PLAYER_LOCATIONS.get(player);
		if (locations == null)
		{
			locations = new ArrayList<>();
			PLAYER_LOCATIONS.put(player, locations);
		}
		
		locations.add(location);
		player.sendMessage("Point saved " + location);
		displayZones(player);
		
		AdminCommandHandler.getInstance().onCommand(player, "admin_zone_build", false);
	}
	
	/**
	 * Sends the visual representation of a zone to a specific player.<br>
	 * This method retrieves all saved {@code Location} points for the user.<br>
	 * It then constructs and sends an {@code ExServerPrimitive} packet to draw the lines and points.
	 * @param player The {@code Player} who will receive the zone data.
	 */
	public void displayZones(Player player)
	{
		final List<Location> locations = PLAYER_LOCATIONS.get(player);
		if ((locations != null) && !locations.isEmpty())
		{
			int packetCount = 1;
			final Location loc = locations.get(0);
			ExServerPrimitive packet = new ExServerPrimitive("ZoneBuilder" + packetCount, loc.getX(), loc.getY(), 65535 + loc.getZ());
			packet.addPoint("0", Color.RED, true, loc.getX(), loc.getY(), loc.getZ());
			for (int i = 1; i < locations.size(); i++)
			{
				if ((i % 10) == 0)
				{
					packetCount++;
					player.sendPacket(packet);
					final Location prevLoc = locations.get(i - 1);
					packet = new ExServerPrimitive("ZoneBuilder" + packetCount, prevLoc.getX(), prevLoc.getY(), 65535 + prevLoc.getZ());
				}
				
				final Location currentLoc = locations.get(i);
				final Location prevLoc = locations.get(i - 1);
				packet.addPoint(String.valueOf(i), Color.RED, true, currentLoc.getX(), currentLoc.getY(), currentLoc.getZ());
				packet.addLine(Color.GREEN, prevLoc.getX(), prevLoc.getY(), prevLoc.getZ(), currentLoc.getX(), currentLoc.getY(), currentLoc.getZ());
			}
			
			player.sendPacket(packet);
		}
	}
	
	/**
	 * Retrieves the list of locations associated with a specific {@code Player}.<br>
	 * This method looks up the data stored in the internal map.
	 * @param player The {@code Player} whose locations need to be retrieved.
	 * @return A {@code List} of {@code Location} objects, or {@code null} if no locations exist.
	 */
	public List<Location> getLocations(Player player)
	{
		return PLAYER_LOCATIONS.get(player);
	}
	
	/**
	 * Generates an HTML table of locations for a specific player.<br>
	 * This method retrieves all saved points from the {@code PLAYER_LOCATIONS} map.<br>
	 * It formats each location into a row with its coordinates and a delete button.
	 * @param player The {@code Player} whose locations need to be retrieved.
	 * @return A string containing the HTML table or an empty string if no locations exist.
	 */
	public String getPathsForHtml(Player player)
	{
		final List<Location> locations = PLAYER_LOCATIONS.get(player);
		if ((locations != null) && !locations.isEmpty())
		{
			final int locationSize = locations.size();
			final StringBuilder sb = new StringBuilder(locationSize * 50);
			sb.append("<table width=300>");
			for (int i = 0; i < locationSize; i++)
			{
				final Location location = locations.get(i);
				sb.append("<tr>");
				sb.append("<td width=20>" + i + "</td>");
				sb.append("<td width=120>" + "X: " + location.getX() + " Y: " + location.getY() + " Z: " + location.getZ() + "</td>");
				sb.append("<td width=80>" + String.format(HTML_DELETE_BUTTON, i) + "</td>");
				sb.append("</tr>");
			}
			sb.append("</table>");
			return sb.toString();
		}
		
		return "";
	}
	
	/**
	 * Removes all saved locations for a specific player.<br>
	 * This method clears the data from the internal storage.<br>
	 * It also sends a visual update to the {@code Player}.
	 * @param player The {@code Player} whose zone data will be cleared.
	 */
	public void clearZone(Player player)
	{
		final List<Location> locations = PLAYER_LOCATIONS.remove(player);
		if ((locations != null) && !locations.isEmpty())
		{
			int packetCount = 1;
			ExServerPrimitive packet = new ExServerPrimitive("ZoneBuilder" + packetCount, player.getX(), player.getY(), -16000);
			packet.addPoint(Color.GREEN, 0, 0, Short.MIN_VALUE);
			for (int i = 1; i < locations.size(); i++)
			{
				if ((i % 10) == 0)
				{
					packetCount++;
					player.sendPacket(packet);
					packet = new ExServerPrimitive("ZoneBuilder" + packetCount, locations.get(i - 1).getX(), locations.get(i - 1).getY(), -16000);
					packet.addPoint(Color.GREEN, 0, 0, Short.MIN_VALUE);
				}
			}
			
			player.sendPacket(packet);
		}
	}
	
	/**
	 * Saves the current path entries of a player into an XML file.<br>
	 * The file is named using the {@code Player} name and the current system time.<br>
	 * If no locations are found, it sends a message to the {@code Player}.<br>
	 * It also clears the admin zone build command after a successful save.
	 * @param player The {@code Player} whose zone data will be saved.
	 */
	public void buildZone(Player player)
	{
		final List<Location> locations = PLAYER_LOCATIONS.get(player);
		if ((locations == null) || locations.isEmpty())
		{
			player.sendMessage("No path entries to save.");
			return;
		}
		
		final long currentTime = System.currentTimeMillis();
		final String playerName = player.getName();
		final String fileName = "data/zones/" + playerName + "-" + currentTime + ".xml";
		final File spawnFile = new File(fileName);
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(spawnFile)))
		{
			final StringBuilder sb = new StringBuilder();
			sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
			sb.append("<list enabled=\"true\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"../xsd/zones.xsd\">\n");
			sb.append("\t<zone name=\"").append(playerName).append("_Zone_").append(currentTime).append("\" type=\"ScriptZone\" shape=\"NPoly\" minZ=\"").append(locations.get(0).getZ() - 1000).append("\" maxZ=\"").append(locations.get(0).getZ() + 1000).append("\">\n");
			
			for (Location location : locations)
			{
				sb.append("\t\t<node X=\"").append(location.getX()).append("\" Y=\"").append(location.getY()).append("\" />\n");
			}
			
			sb.append("\t</zone>\n");
			sb.append("</list>");
			
			writer.write(sb.toString());
			
			player.sendMessage("Zone saved at " + fileName);
			AdminCommandHandler.getInstance().onCommand(player, "admin_zone_build_clear", false);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Failed to save zone file " + fileName + ": ", e);
			player.sendMessage("Failed to save zone: " + e.getMessage());
		}
	}
	
	/**
	 * Removes a specific location from the zone built by a player.<br>
	 * This method updates the list of locations for the {@code Player}.<br>
	 * If only one location remains, it clears all zones for that player.
	 * @param player The {@code Player} who owns the zone.
	 * @param entry The index of the location to remove from the list.
	 */
	public void deleteEntry(Player player, int entry)
	{
		final List<Location> locations = PLAYER_LOCATIONS.get(player);
		if ((locations == null) || (entry < 0) || (entry >= locations.size()))
		{
			return;
		}
		
		final List<Location> modifiedLocations = new ArrayList<>();
		for (int i = 0; i < locations.size(); i++)
		{
			if (i != entry)
			{
				modifiedLocations.add(locations.get(i));
			}
		}
		
		if (modifiedLocations.size() <= 1)
		{
			clearZone(player);
		}
		else
		{
			PLAYER_LOCATIONS.put(player, modifiedLocations);
			displayZones(player);
		}
	}
	
	/**
	 * Retrieves the singleton instance of {@code ZoneBuildManager}.<br>
	 * Use this method to access the manager from anywhere in the code.
	 * @return The global instance of {@code ZoneBuildManager}.
	 */
	public static ZoneBuildManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final ZoneBuildManager INSTANCE = new ZoneBuildManager();
	}
}
