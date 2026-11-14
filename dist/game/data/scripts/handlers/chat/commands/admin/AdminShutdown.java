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

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.Shutdown;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.taskmanagers.GameTimeTaskManager;

/**
 * Handles the {@code server_shutdown} admin command.<br>
 * This allows administrators to display a shutdown menu or shut down the server after a specified number of seconds.
 */
public class AdminShutdown implements IAdminCommandHandler
{
	private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("h:mm a");
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_server_shutdown",
		"admin_server_restart",
		"admin_server_abort"
	};
	
	/**
	 * Processes admin commands for shutting down, restarting, or aborting the server.<br>
	 * This method checks the {@code command} string and executes the corresponding logic.
	 * @param command The raw command string sent by the player.
	 * @param activeChar The {@code Player} object who executed the command.
	 * @return Always returns {@code true}.
	 */
	@Override
	public boolean onCommand(String command, Player activeChar)
	{
		if (command.startsWith("admin_server_shutdown"))
		{
			try
			{
				final String val = command.substring(22);
				if (StringUtil.isNumeric(val))
				{
					serverShutdown(activeChar, Integer.parseInt(val), false);
				}
				else
				{
					activeChar.sendSysMessage("Usage: //server_shutdown <seconds>");
					sendHtmlForm(activeChar);
				}
			}
			catch (StringIndexOutOfBoundsException e)
			{
				sendHtmlForm(activeChar);
			}
		}
		else if (command.startsWith("admin_server_restart"))
		{
			try
			{
				final String val = command.substring(21);
				if (StringUtil.isNumeric(val))
				{
					serverShutdown(activeChar, Integer.parseInt(val), true);
				}
				else
				{
					activeChar.sendSysMessage("Usage: //server_restart <seconds>");
					sendHtmlForm(activeChar);
				}
			}
			catch (StringIndexOutOfBoundsException e)
			{
				sendHtmlForm(activeChar);
			}
		}
		else if (command.startsWith("admin_server_abort"))
		{
			serverAbort(activeChar);
		}
		
		return true;
	}
	
	/**
	 * Retrieves the list of available admin commands.<br>
	 * This method returns a {@code String[]} containing all supported command names.
	 * @return An array of {@code String} objects representing the valid commands.
	 */
	@Override
	public String[] getCommandList()
	{
		return ADMIN_COMMANDS;
	}
	
	/**
	 * Sends an HTML message to the player.<br>
	 * This method displays server statistics like player count and memory usage.<br>
	 * It uses the {@code data/html/admin/shutdown.htm} file template.
	 * @param activeChar The {@code Player} who will receive the HTML packet.
	 */
	private void sendHtmlForm(Player activeChar)
	{
		final NpcHtmlMessage adminReply = new NpcHtmlMessage(0, 1);
		final int t = GameTimeTaskManager.getInstance().getGameTime();
		final int h = t / 60;
		final int m = t % 60;
		final LocalTime time = LocalTime.of(h, m);
		adminReply.setFile(activeChar, "data/html/admin/shutdown.htm");
		adminReply.replace("%count%", String.valueOf(World.getPlayers().size()));
		adminReply.replace("%used%", String.valueOf(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()));
		adminReply.replace("%time%", TIME_FORMAT.format(time));
		activeChar.sendPacket(adminReply);
	}
	
	/**
	 * Initiates the shutdown process for the game server.<br>
	 * This method triggers a timed shutdown or restart based on the provided arguments.
	 * @param activeChar The {@code Player} who issued the command.
	 * @param seconds The number of seconds to wait before shutting down.
	 * @param restart A boolean indicating if the server should restart after shutting down. If {@code false}, it will stay off.
	 */
	private void serverShutdown(Player activeChar, int seconds, boolean restart)
	{
		Shutdown.getInstance().startShutdown(activeChar, seconds, restart);
	}
	
	/**
	 * Immediately stops the server shutdown process.<br>
	 * This method triggers an abort action for the specified player.
	 * @param activeChar The {@code Player} who initiated the command.
	 */
	private void serverAbort(Player activeChar)
	{
		Shutdown.getInstance().abort(activeChar);
	}
}
