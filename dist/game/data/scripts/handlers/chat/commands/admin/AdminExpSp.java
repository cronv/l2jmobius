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

import java.util.StringTokenizer;

import org.l2jmobius.gameserver.data.xml.ClassListData;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * This class handles admin commands for modifying character experience and SP.<br>
 * It supports adding or removing values via the {@code add_exp_sp} and {@code remove_exp_sp} commands.<br>
 * If required parameters are missing, it displays a menu to the administrator.
 */
public class AdminExpSp implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_add_exp_sp_to_character",
		"admin_add_exp_sp",
		"admin_remove_exp_sp"
	};
	
	/**
	 * Processes admin commands for adding or removing experience and SP.<br>
	 * It checks the {@code command} string to determine which action to take.<br>
	 * If parameters are missing, it sends a usage message to the {@code activeChar}.
	 * @param command The full command string entered by the user.
	 * @param activeChar The {@code Player} who executed the command.
	 * @return Always returns {@code true}.
	 */
	@Override
	public boolean onCommand(String command, Player activeChar)
	{
		if (command.startsWith("admin_add_exp_sp"))
		{
			try
			{
				final String val = command.substring(16);
				if (!adminAddExpSp(activeChar, val))
				{
					activeChar.sendSysMessage("Usage: //add_exp_sp exp sp");
				}
			}
			catch (StringIndexOutOfBoundsException e)
			{ // Case of missing parameter
				activeChar.sendSysMessage("Usage: //add_exp_sp exp sp");
			}
		}
		else if (command.startsWith("admin_remove_exp_sp"))
		{
			try
			{
				final String val = command.substring(19);
				if (!adminRemoveExpSP(activeChar, val))
				{
					activeChar.sendSysMessage("Usage: //remove_exp_sp exp sp");
				}
			}
			catch (StringIndexOutOfBoundsException e)
			{ // Case of missing parameter
				activeChar.sendSysMessage("Usage: //remove_exp_sp exp sp");
			}
		}
		
		addExpSp(activeChar);
		return true;
	}
	
	/**
	 * Retrieves the list of available admin commands.<br>
	 * This method returns all registered command strings for this handler.
	 * @return an array of {@code String} containing the command names.
	 */
	@Override
	public String[] getCommandList()
	{
		return ADMIN_COMMANDS;
	}
	
	/**
	 * Displays a menu to add or remove experience and SP from a target player.<br>
	 * This method checks if the current target is a {@code Player}.<br>
	 * It sends an {@code NpcHtmlMessage} containing the target's stats to the admin.
	 * @param activeChar The {@code Player} who is executing the command.
	 */
	private void addExpSp(Player activeChar)
	{
		final WorldObject target = activeChar.getTarget();
		Player player = null;
		if ((target != null) && target.isPlayer())
		{
			player = target.asPlayer();
		}
		else
		{
			activeChar.sendPacket(SystemMessageId.INVALID_TARGET);
			return;
		}
		
		final NpcHtmlMessage adminReply = new NpcHtmlMessage(0, 1);
		adminReply.setFile(activeChar, "data/html/admin/expsp.htm");
		adminReply.replace("%name%", player.getName());
		adminReply.replace("%level%", String.valueOf(player.getLevel()));
		adminReply.replace("%xp%", String.valueOf(player.getExp()));
		adminReply.replace("%sp%", String.valueOf(player.getSp()));
		adminReply.replace("%class%", ClassListData.getInstance().getClass(player.getPlayerClass()).getClassName());
		activeChar.sendPacket(adminReply);
	}
	
	/**
	 * Adds experience points and SP to a target player.<br>
	 * This method validates the target and parses the provided values.<br>
	 * It sends confirmation messages to both the admin and the target.
	 * @param activeChar The {@code Player} executing the command.
	 * @param expSp A string containing two space-separated numbers for EXP and SP.
	 * @return {@code true} if the operation succeeded, otherwise {@code false}.
	 */
	private boolean adminAddExpSp(Player activeChar, String expSp)
	{
		final WorldObject target = activeChar.getTarget();
		Player player = null;
		if ((target != null) && target.isPlayer())
		{
			player = target.asPlayer();
		}
		else
		{
			activeChar.sendPacket(SystemMessageId.INVALID_TARGET);
			return false;
		}
		
		final StringTokenizer st = new StringTokenizer(expSp);
		if (st.countTokens() != 2)
		{
			return false;
		}
		
		final String exp = st.nextToken();
		final String sp = st.nextToken();
		long expval = 0;
		long spval = 0;
		try
		{
			expval = Long.parseLong(exp);
			spval = Long.parseLong(sp);
		}
		catch (Exception e)
		{
			return false;
		}
		
		if ((expval != 0) || (spval != 0))
		{
			// Common character information
			player.sendMessage("Admin is adding you " + expval + " xp and " + spval + " sp.");
			player.addExpAndSp(expval, spval);
			
			// Admin information
			activeChar.sendSysMessage("Added " + expval + " xp and " + spval + " sp to " + player.getName() + ".");
		}
		
		return true;
	}
	
	/**
	 * Removes experience and SP from a target player.<br>
	 * This method validates the target and parses the provided values.<br>
	 * It sends notifications to both the admin and the affected player.
	 * @param activeChar The {@code Player} executing the command.
	 * @param expSp A string containing two tokens for experience and SP.
	 * @return {@code true} if the operation succeeded, otherwise {@code false}.
	 */
	private boolean adminRemoveExpSP(Player activeChar, String expSp)
	{
		final WorldObject target = activeChar.getTarget();
		Player player = null;
		if ((target != null) && target.isPlayer())
		{
			player = target.asPlayer();
		}
		else
		{
			activeChar.sendPacket(SystemMessageId.INVALID_TARGET);
			return false;
		}
		
		final StringTokenizer st = new StringTokenizer(expSp);
		if (st.countTokens() != 2)
		{
			return false;
		}
		
		final String exp = st.nextToken();
		final String sp = st.nextToken();
		long expval = 0;
		int spval = 0;
		try
		{
			expval = Long.parseLong(exp);
			spval = Integer.parseInt(sp);
		}
		catch (Exception e)
		{
			return false;
		}
		
		if ((expval != 0) || (spval != 0))
		{
			// Common character information
			player.sendMessage("Admin is removing you " + expval + " xp and " + spval + " sp.");
			player.removeExpAndSp(expval, spval);
			
			// Admin information
			activeChar.sendSysMessage("Removed " + expval + " xp and " + spval + " sp from " + player.getName() + ".");
		}
		
		return true;
	}
}
