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

import org.l2jmobius.gameserver.data.xml.AdminData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.handler.IAdminCommandHandler;

/**
 * This class handles specific administrative chat commands.<br>
 * It processes the {@code GM} command to turn off GM mode for the user.
 */
public class AdminGm implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_gm"
	};
	
	/**
	 * Processes the admin command sent by a player.<br>
	 * This method checks if the {@code command} is valid for the {@code activeChar}.<br>
	 * It handles the logic for deactivating GM status.
	 * @param command The name of the command to execute.
	 * @param activeChar The {@code Player} who sent the command.
	 * @return {@code true} if the command was processed successfully, otherwise {@code false}.
	 */
	@Override
	public boolean onCommand(String command, Player activeChar)
	{
		if (command.equals("admin_gm") && activeChar.isGM())
		{
			AdminData.getInstance().deleteGm(activeChar);
			activeChar.setAccessLevel(0, true, false);
			activeChar.sendSysMessage("You deactivated your GM access for this session, if you login again you will be GM again, in order to remove your access completely please shift yourself and set your accesslevel to 0.");
		}
		
		return true;
	}
	
	/**
	 * Retrieves the list of available admin commands.<br>
	 * This method returns the internal {@code ADMIN_COMMANDS} array.
	 * @return an array of {@code String} containing all supported command names.
	 */
	@Override
	public String[] getCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
