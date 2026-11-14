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
package org.l2jmobius.gameserver.data.sql;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.announce.Announcement;
import org.l2jmobius.gameserver.mechanics.announce.AnnouncementType;
import org.l2jmobius.gameserver.mechanics.announce.AutoAnnouncement;
import org.l2jmobius.gameserver.mechanics.announce.IAnnouncement;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;

/**
 * Loads and manages all server announcements from the database.<br>
 * Acts as the central registry for normal, critical, event, and automatic announcements.<br>
 * This class handles loading persisted data during startup and broadcasting messages to players.
 * @author UnAfraid, Mobius
 */
public class AnnouncementsTable
{
	private static final Logger LOGGER = Logger.getLogger(AnnouncementsTable.class.getName());
	
	// Announcement Storage.
	private static final Map<Integer, IAnnouncement> ANNOUNCEMENTS = new ConcurrentSkipListMap<>();
	
	// Announcement types shown to a player on login. AUTO_* are intentionally excluded - they broadcast themselves on a schedule via AutoAnnouncement.
	private static final Set<AnnouncementType> LOGIN_VISIBLE_TYPES = EnumSet.of(AnnouncementType.NORMAL, AnnouncementType.CRITICAL, AnnouncementType.EVENT);
	
	/**
	 * Initializes a new instance of the {@code AnnouncementsTable}.<br>
	 * This constructor triggers the initial loading of data from the database.
	 */
	protected AnnouncementsTable()
	{
		load();
	}
	
	/**
	 * Loads all announcement data from the database into memory.<br>
	 * This method clears existing records and populates the {@code ANNOUNCEMENTS} map.<br>
	 * It handles different types of announcements like {@code NORMAL} and {@code AUTO_NORMAL}.
	 */
	private void load()
	{
		ANNOUNCEMENTS.clear();
		
		try (Connection con = DatabaseFactory.getConnection();
			Statement st = con.createStatement();
			ResultSet rset = st.executeQuery("SELECT * FROM announcements"))
		{
			while (rset.next())
			{
				final AnnouncementType type = AnnouncementType.findById(rset.getInt("type"));
				final Announcement announcement;
				switch (type)
				{
					case NORMAL:
					case CRITICAL:
					{
						announcement = new Announcement(rset);
						break;
					}
					case AUTO_NORMAL:
					case AUTO_CRITICAL:
					{
						announcement = new AutoAnnouncement(rset);
						break;
					}
					default:
					{
						continue;
					}
				}
				
				ANNOUNCEMENTS.put(announcement.getId(), announcement);
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Failed loading announcements from table 'announcements': " + e.getMessage(), e);
		}
	}
	
	/**
	 * Adds a new announcement to the server registry.<br>
	 * This method attempts to store the {@code IAnnouncement} in memory.<br>
	 * If storage fails, a warning is logged and the operation stops.
	 * @param announce The {@code IAnnouncement} object to be added.
	 */
	public void addAnnouncement(IAnnouncement announce)
	{
		if (!announce.storeMe())
		{
			LOGGER.warning(getClass().getSimpleName() + ": Failed storing announcement of type " + announce.getType() + "; not registered in memory.");
			return;
		}
		
		ANNOUNCEMENTS.put(announce.getId(), announce);
	}
	
	/**
	 * Removes an announcement from the registry based on its unique identifier.<br>
	 * This method checks if the announcement exists and is eligible for deletion.
	 * @param id The unique {@code int} ID of the announcement to remove.
	 * @return {@code true} if the announcement was successfully removed, otherwise {@code false}.
	 */
	public boolean deleteAnnouncement(int id)
	{
		final IAnnouncement announcement = ANNOUNCEMENTS.get(id);
		if ((announcement == null) || !announcement.deleteMe())
		{
			return false;
		}
		
		ANNOUNCEMENTS.remove(id);
		return true;
	}
	
	/**
	 * Retrieves a specific announcement from the registry.<br>
	 * It uses the unique identifier provided to find the matching object.
	 * @param id The unique ID of the announcement to retrieve.
	 * @return The {@code IAnnouncement} associated with the given ID, or {@code null} if not found.
	 */
	public IAnnouncement getAnnouncement(int id)
	{
		return ANNOUNCEMENTS.get(id);
	}
	
	/**
	 * Retrieves all announcements currently registered in the system.<br>
	 * This method returns a collection of every {@code IAnnouncement} object.
	 * @return A {@code Collection} containing all {@code IAnnouncement} objects.
	 */
	public Collection<IAnnouncement> getAllAnnouncements()
	{
		return ANNOUNCEMENTS.values();
	}
	
	/**
	 * Displays all valid announcements to a specific player.<br>
	 * This method filters the announcements based on visibility types.<br>
	 * It sends each message as a {@code CreatureSay} packet to the {@code Player}.
	 * @param player The {@code Player} who will receive the messages.
	 */
	public void showAnnouncements(Player player)
	{
		for (IAnnouncement announcement : ANNOUNCEMENTS.values())
		{
			final AnnouncementType type = announcement.getType();
			if (!LOGIN_VISIBLE_TYPES.contains(type) || !announcement.isValid())
			{
				continue;
			}
			
			final ChatType chat = (type == AnnouncementType.CRITICAL) ? ChatType.CRITICAL_ANNOUNCE : ChatType.ANNOUNCEMENT;
			player.sendPacket(new CreatureSay(null, chat, player.getName(), announcement.getContent()));
		}
	}
	
	/**
	 * Provides access to the singleton instance of {@code AnnouncementsTable}.<br>
	 * Use this method to get the main manager for server announcements.
	 * @return The single instance of {@code AnnouncementsTable}.
	 */
	public static AnnouncementsTable getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final AnnouncementsTable INSTANCE = new AnnouncementsTable();
	}
}
