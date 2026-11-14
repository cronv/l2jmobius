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
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.ShortcutType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.network.serverpackets.ShortcutRegister;

/**
 * Manages a player shortcut bar entries with database persistence.
 */
public class Shortcuts
{
	private static final Logger LOGGER = Logger.getLogger(Shortcuts.class.getName());
	
	public static final int MAX_SHORTCUTS_PER_BAR = 12;
	
	private final Player _owner;
	private final Map<Integer, Shortcut> _shortcuts = new ConcurrentHashMap<>();
	
	/**
	 * Creates a shortcut manager owned by the given player.
	 * @param owner The owning {@code Player}.
	 */
	public Shortcuts(Player owner)
	{
		_owner = owner;
	}
	
	/**
	 * Retrieves all registered shortcuts.
	 * @return A {@code Collection} of all {@code Shortcut} objects.
	 */
	public Collection<Shortcut> getAllShortcuts()
	{
		return _shortcuts.values();
	}
	
	/**
	 * Retrieves the valid shortcut at the given slot and page.
	 * @param slot The slot index.
	 * @param page The page number.
	 * @return The {@code Shortcut} if found and valid, or {@code null}.
	 */
	public Shortcut getShortcut(int slot, int page)
	{
		Shortcut sc = _shortcuts.get(slot + (page * MAX_SHORTCUTS_PER_BAR));
		
		// Verify shortcut.
		if ((sc != null) && (sc.getType() == ShortcutType.ITEM) && (_owner.getInventory().getItemByObjectId(sc.getId()) == null))
		{
			deleteShortcut(sc.getSlot(), sc.getPage());
			sc = null;
		}
		
		return sc;
	}
	
	/**
	 * Registers a shortcut in memory and the database, validating item-type entries against the inventory.
	 * @param shortcut The {@code Shortcut} to register.
	 */
	public void registerShortcut(Shortcut shortcut)
	{
		// Verify shortcut.
		if (shortcut.getType() == ShortcutType.ITEM)
		{
			final Item item = _owner.getInventory().getItemByObjectId(shortcut.getId());
			if (item == null)
			{
				return;
			}
			
			shortcut.setSharedReuseGroup(item.getSharedReuseGroup());
		}
		
		registerShortcutInDb(shortcut, _shortcuts.put(shortcut.getSlot() + (shortcut.getPage() * MAX_SHORTCUTS_PER_BAR), shortcut));
	}
	
	/**
	 * Persists a shortcut to the database, removing any previous entry first.
	 * @param shortcut The new {@code Shortcut} to save.
	 * @param oldShortcut The previous {@code Shortcut} to remove, or {@code null}.
	 */
	private void registerShortcutInDb(Shortcut shortcut, Shortcut oldShortcut)
	{
		if (oldShortcut != null)
		{
			deleteShortcutFromDb(oldShortcut);
		}
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement("REPLACE INTO character_shortcuts (charId,slot,page,type,shortcut_id,level,sub_level,class_index) values(?,?,?,?,?,?,?,?)"))
		{
			statement.setInt(1, _owner.getObjectId());
			statement.setInt(2, shortcut.getSlot());
			statement.setInt(3, shortcut.getPage());
			statement.setInt(4, shortcut.getType().ordinal());
			statement.setInt(5, shortcut.getId());
			statement.setInt(6, shortcut.getLevel());
			statement.setInt(7, shortcut.getSubLevel());
			statement.setInt(8, _owner.getClassIndex());
			statement.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Could not store character shortcut: " + e.getMessage(), e);
		}
	}
	
	/**
	 * Removes the shortcut at the given slot and page from memory and the database.
	 * @param slot The slot index.
	 * @param page The page number.
	 */
	public void deleteShortcut(int slot, int page)
	{
		final Shortcut old = _shortcuts.remove(slot + (page * MAX_SHORTCUTS_PER_BAR));
		if ((old == null) || (_owner == null))
		{
			return;
		}
		
		deleteShortcutFromDb(old);
	}
	
	/**
	 * Removes the item-type shortcut matching the given object id.
	 * @param objectId The object id of the associated item.
	 */
	public void deleteShortcutByObjectId(int objectId)
	{
		for (Shortcut shortcut : _shortcuts.values())
		{
			if ((shortcut.getType() == ShortcutType.ITEM) && (shortcut.getId() == objectId))
			{
				deleteShortcut(shortcut.getSlot(), shortcut.getPage());
				break;
			}
		}
	}
	
	/**
	 * Removes a shortcut record from the database.
	 * @param shortcut The {@code Shortcut} to remove.
	 */
	private void deleteShortcutFromDb(Shortcut shortcut)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement("DELETE FROM character_shortcuts WHERE charId=? AND slot=? AND page=? AND class_index=?"))
		{
			statement.setInt(1, _owner.getObjectId());
			statement.setInt(2, shortcut.getSlot());
			statement.setInt(3, shortcut.getPage());
			statement.setInt(4, _owner.getClassIndex());
			statement.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Could not delete character shortcut: " + e.getMessage(), e);
		}
	}
	
	/**
	 * Reloads the owner shortcuts from the database and validates item entries against the inventory.
	 * @return {@code true} if restoration succeeded, {@code false} on error.
	 */
	public boolean restoreMe()
	{
		_shortcuts.clear();
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT charId, slot, page, type, shortcut_id, level, sub_level FROM character_shortcuts WHERE charId=? AND class_index=?"))
		{
			statement.setInt(1, _owner.getObjectId());
			statement.setInt(2, _owner.getClassIndex());
			
			try (ResultSet rset = statement.executeQuery())
			{
				while (rset.next())
				{
					final int slot = rset.getInt("slot");
					final int page = rset.getInt("page");
					final int type = rset.getInt("type");
					final int id = rset.getInt("shortcut_id");
					final int level = rset.getInt("level");
					final int subLevel = rset.getInt("sub_level");
					_shortcuts.put(slot + (page * MAX_SHORTCUTS_PER_BAR), new Shortcut(slot, page, ShortcutType.values()[type], id, level, subLevel, 1));
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Could not restore character shortcuts: " + e.getMessage(), e);
			return false;
		}
		
		// Verify shortcuts
		for (Shortcut sc : getAllShortcuts())
		{
			if (sc.getType() == ShortcutType.ITEM)
			{
				final Item item = _owner.getInventory().getItemByObjectId(sc.getId());
				if (item == null)
				{
					deleteShortcut(sc.getSlot(), sc.getPage());
				}
				else if (item.isEtcItem())
				{
					sc.setSharedReuseGroup(item.getEtcItem().getSharedReuseGroup());
				}
			}
		}
		
		return true;
	}
	
	/**
	 * Refreshes the level and sub-level of every shortcut bound to the given skill and notifies the player.
	 * @param skillId The skill id to update.
	 * @param skillLevel The new skill level.
	 * @param skillSubLevel The new skill sub-level.
	 */
	public void updateShortcuts(int skillId, int skillLevel, int skillSubLevel)
	{
		// Update all the shortcuts for this skill.
		for (Shortcut sc : _shortcuts.values())
		{
			final int scId = sc.getId();
			if ((scId == skillId) && (sc.getType() == ShortcutType.SKILL))
			{
				final Shortcut newsc = new Shortcut(sc.getSlot(), sc.getPage(), sc.getType(), scId, skillLevel, skillSubLevel, 1);
				newsc.setAutoUse(sc.isAutoUse());
				_owner.sendPacket(new ShortcutRegister(newsc, _owner));
				_owner.registerShortcut(newsc);
			}
		}
	}
}
