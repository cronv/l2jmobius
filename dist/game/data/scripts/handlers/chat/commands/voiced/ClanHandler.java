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
package handlers.chat.commands.voiced;

import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.handler.IVoicedCommandHandler;

/**
 * Handles voiced chat commands specifically related to clan functionality.<br>
 * This class processes requests sent by players within the {@code voiced} channel.<br>
 * It ensures that clan-specific actions are executed correctly for members.
 */
public class ClanHandler implements IVoicedCommandHandler
{
	private static final String[] VOICED_COMMANDS =
	{
		"set name",
		"set home",
		"set group"
	};
	
	/**
	 * Processes voiced commands for clan management.<br>
	 * This method handles setting privileges and titles for other players.<br>
	 * It validates the target player and required permissions.
	 * @param command The name of the command to execute.
	 * @param activeChar The {@code Player} who is sending the command.
	 * @param params The additional parameters provided with the command.
	 * @return {@code true} if the command was executed successfully, otherwise {@code false}.
	 */
	@Override
	public boolean onCommand(String command, Player activeChar, String params)
	{
		if (!command.equals("set"))
		{
			return false;
		}
		
		final WorldObject target = activeChar.getTarget();
		if ((target == null) || !target.isPlayer())
		{
			return false;
		}
		
		final Player player = target.asPlayer();
		final boolean isSameClan = (activeChar.getClan() != null) && (player.getClan() != null) && (activeChar.getClan().getId() == player.getClan().getId());
		if (!isSameClan)
		{
			return false;
		}
		
		if (params.startsWith("privileges"))
		{
			final String privilegesValue = params.substring(11);
			if (!StringUtil.isNumeric(privilegesValue))
			{
				return false;
			}
			
			final int privilegesMask = Integer.parseInt(privilegesValue);
			final boolean hasEnoughPrivileges = (activeChar.getClanPrivileges().getMask() > privilegesMask) && activeChar.isClanLeader();
			if (!hasEnoughPrivileges)
			{
				return false;
			}
			
			player.getClanPrivileges().setMask(privilegesMask);
			activeChar.sendMessage("Your clan privileges have been set to " + privilegesMask + " by " + activeChar.getName() + ".");
		}
		else if (params.startsWith("title"))
		{
			if (!player.isClanLeader())
			{
				return false;
			}
			
			final String newTitle = params.substring(11);
			player.setTitle(newTitle);
			player.sendMessage("Your title has been set to " + newTitle + " by " + activeChar.getName() + ".");
		}
		
		return true;
	}
	
	/**
	 * Retrieves the list of available voiced commands.<br>
	 * This method returns all valid command strings for this handler.
	 * @return an array of {@code String} containing the supported commands.
	 */
	@Override
	public String[] getCommandList()
	{
		return VOICED_COMMANDS;
	}
}
