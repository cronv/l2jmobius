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

import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;

import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.managers.InstanceManager;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.util.GMAudit;

/**
 * Handles the admin command to teleport a player to a specific instance zone.<br>
 * It validates the target zone and moves the {@code Player} accordingly.<br>
 * This class implements the {@code IAdminCommandHandler} interface.
 */
public class AdminInstanceZone implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_instancezone",
		"admin_instancezone_clear"
	};
	
	/**
	 * Processes administrative commands related to instance zones.<br>
	 * This method handles clearing instances and displaying zone information.
	 * @param command The raw command string entered by the admin.
	 * @param activeChar The {@code Player} object of the administrator executing the command.
	 * @return {@code true} if the command was processed successfully, or {@code false} otherwise.
	 */
	@Override
	public boolean onCommand(String command, Player activeChar)
	{
		final String target = (activeChar.getTarget() != null) ? activeChar.getTarget().getName() : "no-target";
		GMAudit.logAction(activeChar.getName(), command, target, "");
		if (command.startsWith("admin_instancezone_clear"))
		{
			try
			{
				final StringTokenizer st = new StringTokenizer(command, " ");
				st.nextToken();
				final Player player = World.getPlayer(st.nextToken());
				final int instanceId = Integer.parseInt(st.nextToken());
				final String name = InstanceManager.getInstance().getInstanceName(instanceId);
				InstanceManager.getInstance().deleteInstanceTime(player, instanceId);
				activeChar.sendSysMessage("Instance zone " + name + " cleared for player " + player.getName());
				player.sendMessage("Admin cleared instance zone " + name + " for you");
				display(activeChar, activeChar); // for refreshing instance window
				return true;
			}
			catch (Exception e)
			{
				activeChar.sendSysMessage("Failed clearing instance time: " + e.getMessage());
				activeChar.sendSysMessage("Usage: //instancezone_clear <playername> [instanceId]");
				return false;
			}
		}
		else if (command.startsWith("admin_instancezone"))
		{
			final StringTokenizer st = new StringTokenizer(command, " ");
			st.nextToken();
			
			if (st.hasMoreTokens())
			{
				Player player = null;
				final String playername = st.nextToken();
				
				try
				{
					player = World.getPlayer(playername);
				}
				catch (Exception e)
				{
				}
				
				if (player != null)
				{
					display(player, activeChar);
				}
				else
				{
					activeChar.sendSysMessage("The player " + playername + " is not online");
					activeChar.sendSysMessage("Usage: //instancezone [playername]");
					return false;
				}
			}
			else if (activeChar.getTarget() != null)
			{
				final WorldObject activeCharTarget = activeChar.getTarget();
				if (activeCharTarget.isPlayer())
				{
					display(activeCharTarget.asPlayer(), activeChar);
				}
			}
			else
			{
				display(activeChar, activeChar);
			}
		}
		
		return true;
	}
	
	/**
	 * Displays a list of active instances for a specific player.<br>
	 * This method generates an HTML table showing instance names and remaining times.<br>
	 * It sends the formatted message to the target character.
	 * @param player The {@code Player} whose instances will be listed.
	 * @param activeChar The {@code Player} who will receive the display message.
	 */
	private void display(Player player, Player activeChar)
	{
		final Map<Integer, Long> instanceTimes = InstanceManager.getInstance().getAllInstanceTimes(player);
		final StringBuilder html = new StringBuilder(500 + (instanceTimes.size() * 200));
		final String playerName = player.getName();
		html.append("<html><title>Instances</title><center><table width=260><tr><td width=40><button value=\"Main\" action=\"bypass admin_admin\" width=40 height=21 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td><td width=180><center>Character Instances</center></td><td width=40><button value=\"Back\" action=\"bypass -h admin_current_player\" width=40 height=21 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td></tr></table><br><font color=\"LEVEL\"> Instances for " + playerName + "</font><center><br><table><tr><td width=150>Name</td><td width=50>Time</td><td width=70>Action</td></tr>");
		for (Entry<Integer, Long> entry : instanceTimes.entrySet())
		{
			int hours = 0;
			int minutes = 0;
			final int id = entry.getKey();
			final long remainingTime = (entry.getValue() - System.currentTimeMillis()) / 1000;
			if (remainingTime > 0)
			{
				hours = (int) (remainingTime / 3600);
				minutes = (int) ((remainingTime % 3600) / 60);
			}
			
			html.append("<tr><td>" + InstanceManager.getInstance().getInstanceName(id) + "</td><td>" + hours + ":" + minutes + "</td><td><button value=\"Clear\" action=\"bypass -h admin_instancezone_clear " + playerName + " " + id + "\" width=60 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td></tr>");
		}
		
		html.append("</table></html>");
		
		final NpcHtmlMessage ms = new NpcHtmlMessage(0, 1);
		ms.setHtml(html.toString());
		activeChar.sendPacket(ms);
	}
	
	/**
	 * Retrieves the list of available admin instance zone commands.<br>
	 * This method returns all valid command strings for this handler.
	 * @return an array of {@code String} containing the allowed commands.
	 */
	@Override
	public String[] getCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
