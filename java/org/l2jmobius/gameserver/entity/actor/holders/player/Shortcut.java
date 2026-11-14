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

import org.l2jmobius.gameserver.entity.actor.enums.player.ShortcutType;

/**
 * Represents a player shortcut entry mapping a location or action to a hotkey slot.
 */
public class Shortcut
{
	/** Slot from 0 to 11. */
	private final int _slot;
	/** Page from 0 to 9. */
	private final int _page;
	/** Type: item, skill, action, macro, recipe, bookmark. */
	private final ShortcutType _type;
	/** Shortcut ID. */
	private final int _id;
	/** Shortcut level (skills). */
	private final int _level;
	/** Shortcut level (skills). */
	private final int _subLevel;
	/** Character type: 1 player, 2 summon. */
	private final int _characterType;
	/** Shared reuse group. */
	private int _sharedReuseGroup = -1;
	/** Auto use enabled. */
	private boolean _autoUse = false;
	
	/**
	 * Creates a shortcut with the given slot, page, type, id, levels and character type.
	 * @param slot The slot position (0-11).
	 * @param page The page number (0-9).
	 * @param type The {@code ShortcutType}.
	 * @param id The shortcut id.
	 * @param level The primary level (mainly for skills).
	 * @param subLevel The secondary level (mainly for skills).
	 * @param characterType The character type (1 player, 2 summon).
	 */
	public Shortcut(int slot, int page, ShortcutType type, int id, int level, int subLevel, int characterType)
	{
		_slot = slot;
		_page = page;
		_type = type;
		_id = id;
		_level = level;
		_subLevel = subLevel;
		_characterType = characterType;
	}
	
	/**
	 * Retrieves the shortcut id.
	 * @return The id as an {@code int}.
	 */
	public int getId()
	{
		return _id;
	}
	
	/**
	 * Retrieves the level of this shortcut (mainly for skills).
	 * @return The level as an {@code int}.
	 */
	public int getLevel()
	{
		return _level;
	}
	
	/**
	 * Retrieves the sub-level of this shortcut (mainly for skills).
	 * @return The sub-level as an {@code int}.
	 */
	public int getSubLevel()
	{
		return _subLevel;
	}
	
	/**
	 * Retrieves the page number.
	 * @return The page as an {@code int}.
	 */
	public int getPage()
	{
		return _page;
	}
	
	/**
	 * Retrieves the slot index.
	 * @return The slot as an {@code int}.
	 */
	public int getSlot()
	{
		return _slot;
	}
	
	/**
	 * Retrieves the shortcut type.
	 * @return The {@code ShortcutType}.
	 */
	public ShortcutType getType()
	{
		return _type;
	}
	
	/**
	 * Retrieves the character type (1 player, 2 summon).
	 * @return The character type as an {@code int}.
	 */
	public int getCharacterType()
	{
		return _characterType;
	}
	
	/**
	 * Retrieves the shared reuse group.
	 * @return The shared reuse group as an {@code int}.
	 */
	public int getSharedReuseGroup()
	{
		return _sharedReuseGroup;
	}
	
	/**
	 * Sets the shared reuse group.
	 * @param sharedReuseGroup The shared reuse group to assign.
	 */
	public void setSharedReuseGroup(int sharedReuseGroup)
	{
		_sharedReuseGroup = sharedReuseGroup;
	}
	
	/**
	 * Indicates whether auto-use is enabled.
	 * @return {@code true} if auto-use is active, {@code false} otherwise.
	 */
	public boolean isAutoUse()
	{
		return _autoUse;
	}
	
	/**
	 * Sets whether this shortcut is used automatically.
	 * @param value {@code true} to enable auto-use, {@code false} otherwise.
	 */
	public void setAutoUse(boolean value)
	{
		_autoUse = value;
	}
}
