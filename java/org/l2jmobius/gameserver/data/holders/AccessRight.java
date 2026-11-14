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
package org.l2jmobius.gameserver.data.holders;

import org.l2jmobius.gameserver.data.xml.AdminData;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class represents the permissions granted to a specific user or group.<br>
 * It determines whether an entity has the right to perform certain actions within the game server.
 * @author HorridoJoho, Mobius
 */
public class AccessRight
{
	private final String _command;
	private final String _description;
	private final int _level;
	private final boolean _confirmDlg;
	
	/**
	 * Creates a new {@code AccessRight} instance from a configuration set.<br>
	 * This method extracts command details from the provided {@code StatSet}.<br>
	 * It sets the default values for description, confirmation dialogs, and levels.
	 * @param set The {@code StatSet} containing the configuration data.
	 */
	public AccessRight(StatSet set)
	{
		_command = "admin_" + set.getString("command");
		_description = set.getString("description", "");
		_confirmDlg = set.getBoolean("confirmDlg", false);
		_level = set.getInt("accessLevel", 100);
	}
	
	/**
	 * Creates a new {@code AccessRight} object.<br>
	 * This constructor defines the permissions for an admin command.
	 * @param command The name of the command to restrict.
	 * @param description A short explanation of what the command does.
	 * @param confirm Whether the user must see a confirmation dialog before execution.
	 * @param level The minimum required level to use this command.
	 */
	public AccessRight(String command, String description, boolean confirm, int level)
	{
		_command = command;
		_description = description;
		_confirmDlg = confirm;
		_level = level;
	}
	
	/**
	 * Retrieves the command associated with this access right.<br>
	 * This returns the raw string used to trigger the action.
	 * @return The {@code String} representation of the command.
	 */
	public String getCommand()
	{
		return _command;
	}
	
	/**
	 * Retrieves the description of the access right.<br>
	 * This string provides a human-readable explanation for the command.
	 * @return The description as a {@code String}.
	 */
	public String getDescription()
	{
		return _description;
	}
	
	/**
	 * Checks if a confirmation dialog is required for this action.
	 * @return {@code true} if the user must confirm, {@code false} otherwise.
	 */
	public boolean requireConfirm()
	{
		return _confirmDlg;
	}
	
	/**
	 * Checks if a player has sufficient permissions to perform an action.<br>
	 * It compares the required level against the provided {@code AccessLevel}.
	 * @param playerAccessLevel The access level of the player performing the action.
	 * @return {@code true} if the player has permission, otherwise {@code false}.
	 */
	public boolean hasAccess(AccessLevel playerAccessLevel)
	{
		// Check if the required access level is null.
		final AccessLevel accessLevel = AdminData.getInstance().getAccessLevel(_level);
		if (accessLevel == null)
		{
			return false;
		}
		
		// Check if the character's access level matches the required access level.
		if (accessLevel.getLevel() == playerAccessLevel.getLevel())
		{
			return true;
		}
		
		// Check if the character's access level has child access to the required access level.
		if (playerAccessLevel.hasChildAccess(accessLevel))
		{
			return true;
		}
		
		// If none of the above conditions are met, return false.
		return false;
	}
}
