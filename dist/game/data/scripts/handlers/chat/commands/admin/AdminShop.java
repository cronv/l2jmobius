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

import java.util.logging.Logger;

import org.l2jmobius.gameserver.data.xml.BuyListData;
import org.l2jmobius.gameserver.data.xml.MultisellData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.mechanics.buylist.ProductList;
import org.l2jmobius.gameserver.mechanics.siege.Castle;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.ExBuySellList;

/**
 * This class handles administrative commands for the game shop.<br>
 * It allows users to view the general shop menu using {@code gmshop}.<br>
 * It also enables viewing specific shops by providing a {@code buy id} command.
 */
public class AdminShop implements IAdminCommandHandler
{
	private static final Logger LOGGER = Logger.getLogger(AdminShop.class.getName());
	
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_buy",
		"admin_gmshop",
		"admin_multisell",
		"admin_exc_multisell"
	};
	
	/**
	 * Processes admin commands related to shop management.<br>
	 * This method checks the {@code command} string and executes the corresponding action for the {@code activeChar}.
	 * @param command The raw command string sent by the player.
	 * @param activeChar The {@code Player} instance who executed the command.
	 * @return Always returns {@code true} to indicate the command was handled.
	 */
	@Override
	public boolean onCommand(String command, Player activeChar)
	{
		if (command.startsWith("admin_buy"))
		{
			try
			{
				handleBuyRequest(activeChar, command.substring(10));
			}
			catch (IndexOutOfBoundsException e)
			{
				activeChar.sendSysMessage("Please specify buylist.");
			}
		}
		else if (command.equals("admin_gmshop"))
		{
			AdminHtml.showAdminHtml(activeChar, "gmshops.htm");
		}
		else if (command.startsWith("admin_multisell"))
		{
			try
			{
				final int listId = Integer.parseInt(command.substring(16).trim());
				MultisellData.getInstance().separateAndSend(listId, activeChar, null, false);
			}
			catch (NumberFormatException | IndexOutOfBoundsException e)
			{
				activeChar.sendSysMessage("Please specify multisell list ID.");
			}
		}
		else if (command.toLowerCase().startsWith("admin_exc_multisell"))
		{
			try
			{
				final int listId = Integer.parseInt(command.substring(20).trim());
				MultisellData.getInstance().separateAndSend(listId, activeChar, null, true);
			}
			catch (NumberFormatException | IndexOutOfBoundsException e)
			{
				activeChar.sendSysMessage("Please specify multisell list ID.");
			}
		}
		
		return true;
	}
	
	/**
	 * Retrieves the list of available admin commands.<br>
	 * This method returns all valid strings for this handler.
	 * @return an array of {@code String} containing the command names.
	 */
	@Override
	public String[] getCommandList()
	{
		return ADMIN_COMMANDS;
	}
	
	/**
	 * Processes a request to open a specific shop for an admin.<br>
	 * It parses the {@code command} string into an integer ID.<br>
	 * If the list exists, it sends the shop data to the {@code activeChar}.<br>
	 * Otherwise, it sends a failure packet and logs a warning.
	 * @param activeChar The {@code Player} who sent the request.
	 * @param command The string ID of the shop to open.
	 */
	private void handleBuyRequest(Player activeChar, String command)
	{
		int val = -1;
		try
		{
			val = Integer.parseInt(command);
		}
		catch (Exception e)
		{
			LOGGER.warning("admin buylist failed:" + command);
		}
		
		final ProductList buyList = BuyListData.getInstance().getBuyList(val);
		if (buyList != null)
		{
			activeChar.sendPacket(new ExBuySellList(buyList, activeChar, 0));
			activeChar.sendPacket(new ExBuySellList(activeChar, false));
			activeChar.sendPacket(new ExBuySellList((Castle) null, false));
		}
		else
		{
			LOGGER.warning("no buylist with id:" + val);
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
		}
	}
}
