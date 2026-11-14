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

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.taskmanagers.AttackStanceTaskManager;

/**
 * Handles the admin command to list all currently online players.<br>
 * It retrieves a list of {@code Player} objects from the {@code World}.<br>
 * The results are formatted and sent back to the administrator.
 * @author Mobius
 */
public class AdminOnline implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_online"
	};
	
	/**
	 * Processes the admin command to display a report of all online players.<br>
	 * It calculates statistics like unique IPs and combat status.<br>
	 * The results are sent as system messages to the {@code activeChar}.
	 * @param command The name of the command being executed.
	 * @param activeChar The {@code Player} who is executing the command.
	 * @return {@code true} if the command was handled successfully, otherwise {@code false}.
	 */
	@Override
	public boolean onCommand(String command, Player activeChar)
	{
		if (command.equalsIgnoreCase("admin_online"))
		{
			final List<String> ips = new ArrayList<>();
			int total = 0;
			int online = 0;
			int offline = 0;
			int peace = 0;
			int notPeace = 0;
			int instanced = 0;
			int combat = 0;
			for (Player player : World.getPlayers())
			{
				final String ip = player.getIPAddress();
				if ((ip != null) && !ips.contains(ip))
				{
					ips.add(ip);
				}
				
				total++;
				
				if (player.getOfflineMode().isEnabled())
				{
					offline++;
				}
				else if (player.isOnline())
				{
					online++;
				}
				
				if (player.isInsideZone(ZoneId.PEACE))
				{
					peace++;
				}
				else
				{
					notPeace++;
				}
				
				if (player.getInstanceId() > 0)
				{
					instanced++;
				}
				
				if (AttackStanceTaskManager.getInstance().hasAttackStanceTask(player) || (player.getPvpFlag() > 0) || player.isInsideZone(ZoneId.PVP) || player.isInsideZone(ZoneId.SIEGE))
				{
					combat++;
				}
			}
			
			activeChar.sendSysMessage("Online Player Report");
			activeChar.sendSysMessage("Total count: " + total);
			activeChar.sendSysMessage("Total online: " + online);
			activeChar.sendSysMessage("Total offline: " + offline);
			activeChar.sendSysMessage("Max connected: " + World.MAX_CONNECTED_COUNT);
			activeChar.sendSysMessage("Unique IPs: " + ips.size());
			activeChar.sendSysMessage("In peace zone: " + peace);
			activeChar.sendSysMessage("Not in peace zone: " + notPeace);
			activeChar.sendSysMessage("In instances: " + instanced);
			activeChar.sendSysMessage("In combat: " + combat);
		}
		
		return true;
	}
	
	/**
	 * Retrieves the list of available admin commands.<br>
	 * This method returns all valid command strings for this handler.
	 * @return an array of {@code String} containing the command names.
	 */
	@Override
	public String[] getCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
