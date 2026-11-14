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

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

import org.l2jmobius.gameserver.cache.HtmCache;
import org.l2jmobius.gameserver.config.custom.PremiumSystemConfig;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.managers.PcCafePointsManager;
import org.l2jmobius.gameserver.managers.PremiumManager;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * Handles the admin command for managing player premium status.<br>
 * This class allows administrators to grant or remove premium privileges from a {@code Player}. It interacts with the {@code PremiumManager} and updates the user's status accordingly.
 * @author Mobius
 */
public class AdminPremium implements IAdminCommandHandler
{
	private static final DateTimeFormatter PREMIUM_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
	
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_premium_menu",
		"admin_premium_add1",
		"admin_premium_add2",
		"admin_premium_add3",
		"admin_premium_info",
		"admin_premium_remove"
	};
	
	/**
	 * Processes administrative commands related to premium status.<br>
	 * This method handles adding, removing, and viewing premium information for accounts.<br>
	 * It sends the appropriate HTML menu or feedback to the player.
	 * @param command The string input received from the player.
	 * @param activeChar The {@code Player} object who executed the command.
	 * @return {@code true} if the command was successfully processed, otherwise {@code false}.
	 */
	@Override
	public boolean onCommand(String command, Player activeChar)
	{
		if (command.equals("admin_premium_menu"))
		{
			AdminHtml.showAdminHtml(activeChar, "premium_menu.htm");
		}
		else if (command.startsWith("admin_premium_add1"))
		{
			try
			{
				addPremiumStatus(activeChar, 1, command.substring(19));
			}
			catch (StringIndexOutOfBoundsException e)
			{
				activeChar.sendSysMessage("Please enter a valid account name.");
			}
		}
		else if (command.startsWith("admin_premium_add2"))
		{
			try
			{
				addPremiumStatus(activeChar, 2, command.substring(19));
			}
			catch (StringIndexOutOfBoundsException e)
			{
				activeChar.sendSysMessage("Please enter a valid account name.");
			}
		}
		else if (command.startsWith("admin_premium_add3"))
		{
			try
			{
				addPremiumStatus(activeChar, 3, command.substring(19));
			}
			catch (StringIndexOutOfBoundsException e)
			{
				activeChar.sendSysMessage("Please enter a valid account name.");
			}
		}
		else if (command.startsWith("admin_premium_info"))
		{
			try
			{
				viewPremiumInfo(activeChar, command.substring(19));
			}
			catch (StringIndexOutOfBoundsException e)
			{
				activeChar.sendSysMessage("Please enter a valid account name.");
			}
		}
		else if (command.startsWith("admin_premium_remove"))
		{
			try
			{
				removePremium(activeChar, command.substring(21));
			}
			catch (StringIndexOutOfBoundsException e)
			{
				activeChar.sendSysMessage("Please enter a valid account name.");
			}
		}
		
		final NpcHtmlMessage html = new NpcHtmlMessage(0, 1);
		html.setHtml(HtmCache.getInstance().getHtm(activeChar, "data/html/admin/premium_menu.htm"));
		activeChar.sendPacket(html);
		return true;
	}
	
	/**
	 * Grants premium status to a specific account.<br>
	 * This method updates the expiration time based on the provided months.<br>
	 * It also sends a confirmation message to the {@code admin}.
	 * @param admin The {@code Player} who is executing the command.
	 * @param months The number of months to add to the premium status.
	 * @param accountName The name of the account receiving the premium.
	 */
	private void addPremiumStatus(Player admin, int months, String accountName)
	{
		if (!PremiumSystemConfig.PREMIUM_SYSTEM_ENABLED)
		{
			admin.sendMessage("Premium system is disabled.");
			return;
		}
		
		final String entityType = PremiumSystemConfig.ACCOUNT_WIDE_PREMIUM ? "Account " : "Player ";
		// TODO: Add check if account exists XD
		PremiumManager.getInstance().addPremiumTime(accountName, months * 30, TimeUnit.DAYS);
		admin.sendMessage(entityType + accountName + " will now have premium status until " + PREMIUM_FORMAT.format(Instant.ofEpochMilli(PremiumManager.getInstance().getPremiumExpiration(accountName)).atZone(ZoneId.systemDefault())) + ".");
		if (PremiumSystemConfig.PC_CAFE_RETAIL_LIKE)
		{
			for (Player player : World.getPlayers())
			{
				if (player.getAccountName().matches(accountName))
				{
					PcCafePointsManager.getInstance().run(player);
					break;
				}
			}
		}
	}
	
	/**
	 * Displays the premium status of a specific account to an administrator.<br>
	 * It checks if the premium system is enabled before sending the message.<br>
	 * The output shows the expiration date or states that no premium exists.
	 * @param admin The {@code Player} who is executing the command.
	 * @param accountName The name of the account to check.
	 */
	private void viewPremiumInfo(Player admin, String accountName)
	{
		if (!PremiumSystemConfig.PREMIUM_SYSTEM_ENABLED)
		{
			admin.sendMessage("Premium system is disabled.");
			return;
		}
		
		final String entityType = PremiumSystemConfig.ACCOUNT_WIDE_PREMIUM ? "Account " : "Player ";
		if (PremiumManager.getInstance().getPremiumExpiration(accountName) > 0)
		{
			admin.sendMessage(entityType + accountName + " has premium status until " + PREMIUM_FORMAT.format(Instant.ofEpochMilli(PremiumManager.getInstance().getPremiumExpiration(accountName)).atZone(ZoneId.systemDefault())) + ".");
		}
		else
		{
			admin.sendMessage(entityType + accountName + " has no premium status.");
		}
	}
	
	/**
	 * Removes the premium status from a specific account.<br>
	 * This method checks if the premium system is enabled before proceeding.<br>
	 * It notifies the {@code admin} of the result via a chat message.
	 * @param admin The {@code Player} who is executing the command.
	 * @param accountName The name of the account to remove premium from.
	 */
	private void removePremium(Player admin, String accountName)
	{
		if (!PremiumSystemConfig.PREMIUM_SYSTEM_ENABLED)
		{
			admin.sendMessage("Premium system is disabled.");
			return;
		}
		
		final String entityType = PremiumSystemConfig.ACCOUNT_WIDE_PREMIUM ? "Account " : "Player ";
		if (PremiumManager.getInstance().getPremiumExpiration(accountName) > 0)
		{
			PremiumManager.getInstance().removePremiumStatus(accountName, true);
			admin.sendMessage(entityType + accountName + " has no longer premium status.");
		}
		else
		{
			admin.sendMessage(entityType + accountName + " has no premium status.");
		}
	}
	
	/**
	 * Retrieves the list of available administrative premium commands.<br>
	 * This method returns all valid command strings for this handler.
	 * @return an array of {@code String} containing the command names.
	 */
	@Override
	public String[] getCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
