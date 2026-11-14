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
package handlers.chat.commands.admin;

import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.handler.IAdminCommandHandler;

/**
 * This class handles administrative commands related to targeting players.<br>
 * It allows an administrator to set a specific player as their target by using their name.
 */
public class AdminTarget implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_target"
	};
	
	/**
	 * Processes an admin command sent by a player.<br>
	 * This method checks if the {@code command} starts with {@code admin_target}.<br>
	 * It delegates the logic to the appropriate handler if matched.
	 * @param command The raw string input from the player.
	 * @param activeChar The {@code Player} who sent the command.
	 * @return {@code true} if the command was processed, otherwise {@code false}.
	 */
	@Override
	public boolean onCommand(String command, Player activeChar)
	{
		if (command.startsWith("admin_target"))
		{
			handleTarget(command, activeChar);
		}
		
		return true;
	}
	
	/**
	 * Retrieves the list of available admin commands.<br>
	 * This method returns all strings that trigger this handler.
	 * @return an array of {@code String} containing the command names.
	 */
	@Override
	public String[] getCommandList()
	{
		return ADMIN_COMMANDS;
	}
	
	/**
	 * Processes the admin command to set a target player.<br>
	 * It extracts the name from the {@code command} string.<br>
	 * If the player exists, it performs an action on them.<br>
	 * Otherwise, it sends an error message to the {@code activeChar}.
	 * @param command The full command string containing the target name.
	 * @param activeChar The {@code Player} who is executing the command.
	 */
	private void handleTarget(String command, Player activeChar)
	{
		try
		{
			final String targetName = command.substring(13);
			final Player player = World.getPlayer(targetName);
			if (player != null)
			{
				player.onAction(activeChar);
			}
			else
			{
				activeChar.sendSysMessage("Player " + targetName + " not found");
			}
		}
		catch (IndexOutOfBoundsException e)
		{
			activeChar.sendSysMessage("Please specify correct name.");
		}
	}
}
