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

import java.util.Collection;
import java.util.StringTokenizer;

import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.managers.FortManager;
import org.l2jmobius.gameserver.mechanics.siege.Fort;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * Handles administrative commands related to fort sieges.<br>
 * This class allows administrators to manage {@code Fort} mechanics via chat commands.
 */
public class AdminFortSiege implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_fortsiege",
		"admin_add_fortattacker",
		"admin_list_fortsiege_clans",
		"admin_clear_fortsiege_list",
		"admin_spawn_fortdoors",
		"admin_endfortsiege",
		"admin_startfortsiege",
		"admin_setfort",
		"admin_removefort"
	};
	
	/**
	 * Processes administrative commands related to fort sieges.<br>
	 * It parses the command string and identifies the target fort.<br>
	 * If no fort is specified, it opens a selection page for the player.<br>
	 * Otherwise, it executes specific siege actions based on the command provided.
	 * @param commandValue The raw command string entered by the administrator.
	 * @param activeChar The {@code Player} who executed the command.
	 * @return Always returns {@code true}.
	 */
	@Override
	public boolean onCommand(String commandValue, Player activeChar)
	{
		String command = commandValue;
		final StringTokenizer st = new StringTokenizer(command, " ");
		command = st.nextToken(); // Get actual command
		
		// Get fort
		Fort fort = null;
		int fortId = 0;
		if (st.hasMoreTokens())
		{
			fortId = Integer.parseInt(st.nextToken());
			fort = FortManager.getInstance().getFortById(fortId);
		}
		
		// Get fort
		if (((fort == null) || (fortId == 0)))
		{
			// No fort specified
			showFortSelectPage(activeChar);
		}
		else
		{
			final WorldObject target = activeChar.getTarget();
			Player player = null;
			if ((target != null) && target.isPlayer())
			{
				player = target.asPlayer();
			}
			
			if (command.equalsIgnoreCase("admin_add_fortattacker"))
			{
				if (player == null)
				{
					activeChar.sendPacket(SystemMessageId.THAT_IS_AN_INCORRECT_TARGET);
				}
				else if (fort.getSiege().addAttacker(player, false) == 4)
				{
					final SystemMessage sm = new SystemMessage(SystemMessageId.YOUR_CLAN_HAS_BEEN_REGISTERED_TO_S1_S_FORTRESS_BATTLE);
					sm.addCastleId(fort.getResidenceId());
					player.sendPacket(sm);
				}
				else
				{
					player.sendMessage("During registering error occurred!");
				}
			}
			else if (command.equalsIgnoreCase("admin_clear_fortsiege_list"))
			{
				fort.getSiege().clearSiegeClan();
			}
			else if (command.equalsIgnoreCase("admin_endfortsiege"))
			{
				fort.getSiege().endSiege();
			}
			else if (command.equalsIgnoreCase("admin_list_fortsiege_clans"))
			{
				activeChar.sendSysMessage("Not implemented yet.");
			}
			else if (command.equalsIgnoreCase("admin_setfort"))
			{
				if ((player == null) || (player.getClan() == null))
				{
					activeChar.sendPacket(SystemMessageId.THAT_IS_AN_INCORRECT_TARGET);
				}
				else
				{
					fort.endOfSiege(player.getClan());
				}
			}
			else if (command.equalsIgnoreCase("admin_removefort"))
			{
				final Clan clan = fort.getOwnerClan();
				if (clan != null)
				{
					fort.removeOwner(true);
				}
				else
				{
					activeChar.sendSysMessage("Unable to remove fort");
				}
			}
			else if (command.equalsIgnoreCase("admin_spawn_fortdoors"))
			{
				fort.resetDoors();
			}
			else if (command.equalsIgnoreCase("admin_startfortsiege"))
			{
				fort.getSiege().startSiege();
			}
			
			showFortSiegePage(activeChar, fort);
		}
		
		return true;
	}
	
	/**
	 * Displays the fort selection page to an administrator.<br>
	 * This method generates a list of all available {@code Fort} objects.<br>
	 * It sends an {@code NpcHtmlMessage} containing these forts to the player.
	 * @param activeChar The {@code Player} who will view the selection page.
	 */
	private void showFortSelectPage(Player activeChar)
	{
		int i = 0;
		final NpcHtmlMessage adminReply = new NpcHtmlMessage(0, 1);
		adminReply.setFile(activeChar, "data/html/admin/forts.htm");
		
		final Collection<Fort> forts = FortManager.getInstance().getForts();
		final StringBuilder cList = new StringBuilder(forts.size() * 100);
		for (Fort fort : forts)
		{
			if (fort != null)
			{
				final int residenceId = fort.getResidenceId();
				cList.append("<td fixwidth=90><a action=\"bypass -h admin_fortsiege " + residenceId + "\">" + fort.getName() + " id: " + residenceId + "</a></td>");
				i++;
			}
			
			if (i > 2)
			{
				cList.append("</tr><tr>");
				i = 0;
			}
		}
		
		adminReply.replace("%forts%", cList.toString());
		activeChar.sendPacket(adminReply);
	}
	
	/**
	 * Displays the siege information page for a specific fort.<br>
	 * This method sends an {@code NpcHtmlMessage} to the player.<br>
	 * It replaces placeholders with the name and ID of the {@code Fort}.
	 * @param activeChar The {@code Player} who will view the page.
	 * @param fort The {@code Fort} object containing the siege data.
	 */
	private void showFortSiegePage(Player activeChar, Fort fort)
	{
		final NpcHtmlMessage adminReply = new NpcHtmlMessage(0, 1);
		adminReply.setFile(activeChar, "data/html/admin/fort.htm");
		adminReply.replace("%fortName%", fort.getName());
		adminReply.replace("%fortId%", String.valueOf(fort.getResidenceId()));
		activeChar.sendPacket(adminReply);
	}
	
	/**
	 * Retrieves the list of available admin siege commands.<br>
	 * This method returns all valid command strings for this handler.
	 * @return an array of {@code String} containing the command names.
	 */
	@Override
	public String[] getCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
