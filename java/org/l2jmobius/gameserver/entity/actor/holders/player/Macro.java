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

import java.util.List;

/**
 * Represents a sequence of automated actions a player can run as a macro.
 */
public class Macro
{
	private int _id;
	private final int _icon;
	private final String _name;
	private final String _descr;
	private final String _acronym;
	private final List<MacroCmd> _commands;
	
	/**
	 * Creates a macro with the given id, icon, name, description, acronym and commands.
	 * @param id The macro id.
	 * @param icon The icon id.
	 * @param name The macro name.
	 * @param descr The macro description.
	 * @param acronym The macro acronym.
	 * @param list The {@code MacroCmd} objects to execute.
	 */
	public Macro(int id, int icon, String name, String descr, String acronym, List<MacroCmd> list)
	{
		_id = id;
		_icon = icon;
		_name = name;
		_descr = descr;
		_acronym = acronym;
		_commands = list;
	}
	
	/**
	 * Retrieves the macro id.
	 * @return The id as an {@code int}.
	 */
	public int getId()
	{
		return _id;
	}
	
	/**
	 * Sets the macro id.
	 * @param id The new id.
	 */
	public void setId(int id)
	{
		_id = id;
	}
	
	/**
	 * Retrieves the icon id.
	 * @return The icon id as an {@code int}.
	 */
	public int getIcon()
	{
		return _icon;
	}
	
	/**
	 * Retrieves the macro name.
	 * @return The name as a {@code String}.
	 */
	public String getName()
	{
		return _name;
	}
	
	/**
	 * Retrieves the macro description.
	 * @return The description as a {@code String}.
	 */
	public String getDescr()
	{
		return _descr;
	}
	
	/**
	 * Retrieves the macro acronym.
	 * @return The acronym as a {@code String}.
	 */
	public String getAcronym()
	{
		return _acronym;
	}
	
	/**
	 * Retrieves the macro commands.
	 * @return A {@code List} of {@code MacroCmd} objects.
	 */
	public List<MacroCmd> getCommands()
	{
		return _commands;
	}
}
