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
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;

/**
 * This class handles administrative chat commands for Game Masters.<br>
 * It processes the {@code gmchat} command to send messages to all online GMs.<br>
 * It also processes the {@code gmchat_menu} command to send a message and open the admin panel.
 */
public class AdminGmChat implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_gmchat",
		"admin_snoop",
		"admin_gmchat_menu"
	};
	
	/**
	 * Processes admin commands sent by a {@code Player}.<br>
	 * This method checks the {@code command} string to trigger specific actions.<br>
	 * It handles chat, snoop, and menu commands.
	 * @param command The text of the command entered by the player.
	 * @param activeChar The {@code Player} who sent the command.
	 * @return Always returns {@code true}.
	 */
	@Override
	public boolean onCommand(String command, Player activeChar)
	{
		if (command.startsWith("admin_gmchat"))
		{
			handleGmChat(command, activeChar);
		}
		else if (command.startsWith("admin_snoop"))
		{
			snoop(command, activeChar);
		}
		
		if (command.startsWith("admin_gmchat_menu"))
		{
			AdminHtml.showAdminHtml(activeChar, "gm_menu.htm");
		}
		
		return true;
	}
	
	/**
	 * This method handles the logic for the {@code admin_snoop} command.<br>
	 * It links an administrator to a target player for monitoring purposes.<br>
	 * If no target is specified, it uses the current target of the {@code activeChar}.<br>
	 * The method sends system messages if the target is missing or invalid.
	 * @param command The command string being executed.
	 * @param activeChar The player who is executing the command.
	 */
	private void snoop(String command, Player activeChar)
	{
		WorldObject target = null;
		if (command.length() > 12)
		{
			target = World.getPlayer(command.substring(12));
		}
		
		if (target == null)
		{
			target = activeChar.getTarget();
		}
		
		if (target == null)
		{
			activeChar.sendPacket(SystemMessageId.SELECT_YOUR_TARGET);
			return;
		}
		
		if (!target.isPlayer())
		{
			activeChar.sendPacket(SystemMessageId.INVALID_TARGET);
			return;
		}
		
		final Player player = target.asPlayer();
		player.addSnooper(activeChar);
		activeChar.addSnooped(player);
	}
	
	/**
	 * Retrieves the list of available admin chat commands.<br>
	 * This method returns all valid strings for this handler.
	 * @return an array of {@code String} containing the command names.
	 */
	@Override
	public String[] getCommandList()
	{
		return ADMIN_COMMANDS;
	}
	
	/**
	 * Processes the {@code admin_gmchat} and {@code admin_gmchat_menu} commands.<br>
	 * It extracts the message text from the {@code command} string.<br>
	 * The message is then broadcast to all online GMs using {@code AdminData}.
	 * @param command The full command string sent by the player.
	 * @param activeChar The {@code Player} object who executed the command.
	 */
	private void handleGmChat(String command, Player activeChar)
	{
		try
		{
			int offset = 0;
			String text;
			if (command.startsWith("admin_gmchat_menu"))
			{
				offset = 18;
			}
			else
			{
				offset = 13;
			}
			
			text = command.substring(offset);
			AdminData.getInstance().broadcastToGMs(new CreatureSay(null, ChatType.ALLIANCE, activeChar.getName(), text));
		}
		catch (StringIndexOutOfBoundsException e)
		{
			// Who cares?
		}
	}
}
