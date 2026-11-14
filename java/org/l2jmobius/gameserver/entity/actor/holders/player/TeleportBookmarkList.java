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
package org.l2jmobius.gameserver.entity.actor.holders.player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.itemcontainer.PlayerInventory;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.mechanics.skill.AbnormalType;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExGetBookMarkInfoPacket;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * Manages the teleport bookmarks of a {@code Player}.<br>
 * It handles adding, modifying, deleting and using the saved locations.<br>
 * This class interacts with the database to persist the bookmark information.
 * @author Mobius
 */
public class TeleportBookmarkList
{
	private static final Logger LOGGER = Logger.getLogger(TeleportBookmarkList.class.getName());
	
	private static final String INSERT_TP_BOOKMARK = "INSERT INTO character_tpbookmark (charId,Id,x,y,z,icon,tag,name) values (?,?,?,?,?,?,?,?)";
	private static final String UPDATE_TP_BOOKMARK = "UPDATE character_tpbookmark SET icon=?,tag=?,name=? where charId=? AND Id=?";
	private static final String RESTORE_TP_BOOKMARK = "SELECT Id,x,y,z,icon,tag,name FROM character_tpbookmark WHERE charId=?";
	private static final String DELETE_TP_BOOKMARK = "DELETE FROM character_tpbookmark WHERE charId=? AND Id=?";
	
	private static final int MY_TELEPORT_SKILL_ID = 2588;
	private static final int TELEPORT_ITEM_ID = 13016;
	
	private final Player _player;
	private final Map<Integer, TeleportBookmark> _bookmarks = new ConcurrentSkipListMap<>();
	private int _pendingTeleportId;
	
	/**
	 * Creates a new {@code TeleportBookmarkList} for a specific player.
	 * @param player The {@code Player} object associated with this bookmark list.
	 */
	public TeleportBookmarkList(Player player)
	{
		_player = player;
	}
	
	/**
	 * Loads the character's teleport bookmarks from the database.<br>
	 * This method populates the {@code _bookmarks} collection.<br>
	 * It handles any SQL errors by logging a severe message.
	 */
	public void restore()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(RESTORE_TP_BOOKMARK))
		{
			statement.setInt(1, _player.getObjectId());
			try (ResultSet rset = statement.executeQuery())
			{
				while (rset.next())
				{
					_bookmarks.put(rset.getInt("Id"), new TeleportBookmark(rset.getInt("Id"), rset.getInt("x"), rset.getInt("y"), rset.getInt("z"), rset.getInt("icon"), rset.getString("tag"), rset.getString("name")));
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Failed restoring character teleport bookmark.", e);
		}
	}
	
	/**
	 * Adds a new teleport bookmark to the player's saved locations.<br>
	 * This method checks for available slots and required items before saving.<br>
	 * The data is stored in the database and sent to the client.
	 * @param x The X coordinate of the location.
	 * @param y The Y coordinate of the location.
	 * @param z The Z coordinate of the location.
	 * @param icon The icon identifier for the bookmark.
	 * @param tag A unique tag associated with the bookmark.
	 * @param name The display name of the bookmark.
	 */
	public void add(int x, int y, int z, int icon, String tag, String name)
	{
		if (!checkCondition(1))
		{
			return;
		}
		
		if (_player.isInTimedHuntingZone())
		{
			return;
		}
		
		final int bookmarkSlot = _player.getBookMarkSlot();
		if (_bookmarks.size() >= bookmarkSlot)
		{
			_player.sendPacket(SystemMessageId.YOU_HAVE_NO_SPACE_TO_SAVE_THE_TELEPORT_LOCATION);
			return;
		}
		
		if (GeneralConfig.BOOKMARK_CONSUME_ITEM_ID > 0)
		{
			final PlayerInventory inventory = _player.getInventory();
			if (inventory.getInventoryItemCount(GeneralConfig.BOOKMARK_CONSUME_ITEM_ID, -1) == 0)
			{
				if (GeneralConfig.BOOKMARK_CONSUME_ITEM_ID == 20033)
				{
					_player.sendPacket(SystemMessageId.YOU_CANNOT_BOOKMARK_THIS_LOCATION_BECAUSE_YOU_DO_NOT_HAVE_A_MY_TELEPORT_FLAG);
				}
				else
				{
					_player.sendPacket(SystemMessageId.INCORRECT_ITEM_COUNT_2);
				}
				return;
			}
			
			_player.destroyItem(ItemProcessType.NONE, inventory.getItemByItemId(GeneralConfig.BOOKMARK_CONSUME_ITEM_ID).getObjectId(), 1, null, true);
		}
		
		int id;
		for (id = 1; id <= bookmarkSlot; ++id)
		{
			if (!_bookmarks.containsKey(id))
			{
				break;
			}
		}
		
		_bookmarks.put(id, new TeleportBookmark(id, x, y, z, icon, tag, name));
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(INSERT_TP_BOOKMARK))
		{
			statement.setInt(1, _player.getObjectId());
			statement.setInt(2, id);
			statement.setInt(3, x);
			statement.setInt(4, y);
			statement.setInt(5, z);
			statement.setInt(6, icon);
			statement.setString(7, tag);
			statement.setString(8, name);
			statement.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Could not insert character teleport bookmark data: " + e.getMessage(), e);
		}
		
		_player.sendPacket(new ExGetBookMarkInfoPacket(_player));
	}
	
	/**
	 * Updates the details of an existing teleport bookmark.<br>
	 * This method modifies the icon, tag, and name in both memory and the database.<br>
	 * It also sends a packet to update the client information.
	 * @param id The unique identifier of the teleport bookmark to modify.
	 * @param icon The new icon ID for the bookmark.
	 * @param tag The new tag string for the bookmark.
	 * @param name The new display name for the bookmark.
	 */
	public void modify(int id, int icon, String tag, String name)
	{
		if (_player.isInTimedHuntingZone())
		{
			return;
		}
		
		final TeleportBookmark bookmark = _bookmarks.get(id);
		if (bookmark != null)
		{
			bookmark.setIcon(icon);
			bookmark.setTag(tag);
			bookmark.setName(name);
			
			try (Connection con = DatabaseFactory.getConnection();
				PreparedStatement statement = con.prepareStatement(UPDATE_TP_BOOKMARK))
			{
				statement.setInt(1, icon);
				statement.setString(2, tag);
				statement.setString(3, name);
				statement.setInt(4, _player.getObjectId());
				statement.setInt(5, id);
				statement.execute();
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, "Could not update character teleport bookmark data: " + e.getMessage(), e);
			}
		}
		
		_player.sendPacket(new ExGetBookMarkInfoPacket(_player));
	}
	
	/**
	 * Removes a specific teleport bookmark from the player.<br>
	 * This method deletes the bookmark from the internal cache and the database.<br>
	 * It then sends an update packet to the client.
	 * @param id The unique identifier of the bookmark to delete.
	 */
	public void delete(int id)
	{
		if (_bookmarks.remove(id) != null)
		{
			try (Connection con = DatabaseFactory.getConnection();
				PreparedStatement statement = con.prepareStatement(DELETE_TP_BOOKMARK))
			{
				statement.setInt(1, _player.getObjectId());
				statement.setInt(2, id);
				statement.execute();
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, "Could not delete character teleport bookmark data: " + e.getMessage(), e);
			}
			
			_player.sendPacket(new ExGetBookMarkInfoPacket(_player));
		}
	}
	
	/**
	 * Starts a teleport to a specific bookmark location.<br>
	 * This method checks if the teleport conditions are met and if the required item is in the inventory.<br>
	 * The player then casts My Teleport (skill 2588), which finishes the teleport in {@link #teleportToPending()}.
	 * @param id The unique identifier of the bookmark to use.
	 */
	public void teleport(int id)
	{
		if (!checkCondition(0) || !_bookmarks.containsKey(id))
		{
			return;
		}
		
		if (_player.getInventory().getInventoryItemCount(TELEPORT_ITEM_ID, 0) == 0)
		{
			_player.sendPacket(SystemMessageId.YOU_CANNOT_TELEPORT_BECAUSE_YOU_DO_NOT_HAVE_A_TELEPORT_ITEM);
			return;
		}
		
		if (_player.isInTimedHuntingZone())
		{
			_player.sendMessage("You cannot teleport at this location.");
			return;
		}
		
		_pendingTeleportId = id;
		_player.useMagic(SkillData.getInstance().getSkill(MY_TELEPORT_SKILL_ID, 1), null, false, false);
	}
	
	/**
	 * Finishes a My Teleport cast.<br>
	 * It checks the conditions again, consumes one teleport item and moves the player to the bookmark chosen before the cast.
	 */
	public void teleportToPending()
	{
		final TeleportBookmark bookmark = _bookmarks.get(_pendingTeleportId);
		_pendingTeleportId = 0;
		if ((bookmark == null) || !checkCondition(0))
		{
			return;
		}
		
		final Item teleportItem = _player.getInventory().getItemByItemId(TELEPORT_ITEM_ID);
		if (teleportItem == null)
		{
			_player.sendPacket(SystemMessageId.YOU_CANNOT_TELEPORT_BECAUSE_YOU_DO_NOT_HAVE_A_TELEPORT_ITEM);
			return;
		}
		
		final SystemMessage sm = new SystemMessage(SystemMessageId.S1_DISAPPEARED);
		sm.addItemName(TELEPORT_ITEM_ID);
		_player.sendPacket(sm);
		_player.destroyItem(null, teleportItem.getObjectId(), 1, null, false);
		_player.teleToLocation(bookmark, false);
		_player.sendPacket(new ExGetBookMarkInfoPacket(_player));
	}
	
	/**
	 * Retrieves all teleport bookmarks for the player.
	 * @return a {@code Collection} of {@code TeleportBookmark} objects.
	 */
	public Collection<TeleportBookmark> getAll()
	{
		return _bookmarks.values();
	}
	
	/**
	 * Checks if the player can use a teleport bookmark based on their current state.<br>
	 * This method validates conditions such as combat, siege, duels, and specific zones.<br>
	 * It sends an appropriate system message to the player if the action is denied.
	 * @param type The type of teleport being attempted.
	 * @return {@code true} if the teleport is allowed, {@code false} otherwise.
	 */
	private boolean checkCondition(int type)
	{
		if (_player.isInCombat())
		{
			_player.sendPacket(SystemMessageId.YOU_CANNOT_USE_MY_TELEPORTS_DURING_A_BATTLE);
			return false;
		}
		else if (_player.isInSiege() || (_player.getSiegeState() != 0))
		{
			_player.sendPacket(SystemMessageId.YOU_CANNOT_USE_MY_TELEPORTS_WHILE_PARTICIPATING_A_LARGE_SCALE_BATTLE_SUCH_AS_A_CASTLE_SIEGE_FORTRESS_SIEGE_OR_CLAN_HALL_SIEGE);
			return false;
		}
		else if (_player.isInDuel() || _player.isStartingDuel())
		{
			_player.sendPacket(SystemMessageId.YOU_CANNOT_USE_MY_TELEPORTS_DURING_A_DUEL);
			return false;
		}
		else if (_player.isFlying())
		{
			_player.sendPacket(SystemMessageId.YOU_CANNOT_USE_MY_TELEPORTS_WHILE_FLYING);
			return false;
		}
		else if (_player.isInOlympiadMode())
		{
			_player.sendPacket(SystemMessageId.YOU_CANNOT_USE_MY_TELEPORTS_WHILE_PARTICIPATING_IN_AN_OLYMPIAD_MATCH);
			return false;
		}
		else if (_player.hasBlockActions() && _player.hasAbnormalType(AbnormalType.PARALYZE))
		{
			_player.sendPacket(SystemMessageId.CANNOT_TELEPORT_WHILE_PETRIFIED_OR_PARALYZED);
			return false;
		}
		else if (_player.isDead())
		{
			_player.sendPacket(SystemMessageId.YOU_CANNOT_USE_TELEPORT_WHILE_YOU_ARE_DEAD);
			return false;
		}
		else if (_player.isInWater())
		{
			_player.sendPacket(SystemMessageId.YOU_CANNOT_USE_TELEPORT_UNDERWATER);
			return false;
		}
		else if ((type == 1) && (_player.isInsideZone(ZoneId.SIEGE) || _player.isInsideZone(ZoneId.CLAN_HALL) || _player.isInsideZone(ZoneId.JAIL) || _player.isInsideZone(ZoneId.CASTLE) || _player.isInsideZone(ZoneId.NO_SUMMON_FRIEND) || _player.isInsideZone(ZoneId.FORT)))
		{
			_player.sendPacket(SystemMessageId.YOU_CANNOT_USE_MY_TELEPORTS_TO_REACH_THIS_AREA);
			return false;
		}
		else if (_player.isInsideZone(ZoneId.NO_BOOKMARK) || _player.isInBoat() || _player.isInAirShip() || _player.isInTimedHuntingZone())
		{
			if (type == 0)
			{
				_player.sendPacket(SystemMessageId.YOU_CANNOT_USE_TELEPORT_IN_THIS_AREA);
			}
			else if (type == 1)
			{
				_player.sendPacket(SystemMessageId.YOU_CANNOT_USE_MY_TELEPORTS_TO_REACH_THIS_AREA);
			}
			
			return false;
		}
		/*
		 * TODO: Instant Zone still not implemented else if (isInsideZone(ZoneId.INSTANT)) { sendPacket(new SystemMessage(2357)); return; }
		 */
		else
		{
			return true;
		}
	}
}
