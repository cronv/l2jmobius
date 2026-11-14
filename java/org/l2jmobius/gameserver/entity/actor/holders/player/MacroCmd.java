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
package org.l2jmobius.gameserver.entity.actor.holders.player;

import org.l2jmobius.gameserver.entity.actor.enums.player.MacroType;

/**
 * Holds the data for a single macro command.
 * @author Zoey76
 */
public class MacroCmd
{
	private final int _entry;
	private final MacroType _type;
	private final int _d1; // skill_id or page for shortcuts
	private final int _d2; // shortcut
	private final String _cmd;
	
	/**
	 * Creates a macro command with the given entry, type and data values.
	 * @param entry The entry id.
	 * @param type The {@code MacroType}.
	 * @param d1 The first data value (skill id or page).
	 * @param d2 The second data value (shortcut index).
	 * @param cmd The command string.
	 */
	public MacroCmd(int entry, MacroType type, int d1, int d2, String cmd)
	{
		_entry = entry;
		_type = type;
		_d1 = d1;
		_d2 = d2;
		_cmd = cmd;
	}
	
	/**
	 * Retrieves the entry id.
	 * @return The entry id as an {@code int}.
	 */
	public int getEntry()
	{
		return _entry;
	}
	
	/**
	 * Retrieves the macro type.
	 * @return The {@code MacroType}.
	 */
	public MacroType getType()
	{
		return _type;
	}
	
	/**
	 * Retrieves the first data value (skill id or page).
	 * @return The value as an {@code int}.
	 */
	public int getD1()
	{
		return _d1;
	}
	
	/**
	 * Retrieves the second data value (shortcut index).
	 * @return The value as an {@code int}.
	 */
	public int getD2()
	{
		return _d2;
	}
	
	/**
	 * Retrieves the command string.
	 * @return The command as a {@code String}.
	 */
	public String getCmd()
	{
		return _cmd;
	}
}
