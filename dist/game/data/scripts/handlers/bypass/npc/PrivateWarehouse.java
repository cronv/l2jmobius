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
package handlers.bypass.npc;

import java.util.logging.Level;

import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.handler.IBypassHandler;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.WareHouseDepositList;
import org.l2jmobius.gameserver.network.serverpackets.WareHouseWithdrawalList;

/**
 * Handles the bypass logic for private warehouse interactions.<br>
 * This class allows players to access and manage their personal storage via an NPC.<br>
 * It processes requests for depositing and withdrawing items from the {@code WareHouse} system.
 */
public class PrivateWarehouse implements IBypassHandler
{
	private static final String[] COMMANDS =
	{
		"withdrawp",
		"depositp"
	};
	
	/**
	 * Handles specific commands for the private warehouse system.<br>
	 * This method checks if the player is interacting with a valid NPC.<br>
	 * It processes {@code withdrawp} and {@code depositp} actions.
	 * @param command The command string sent by the player.
	 * @param player The {@code Player} instance who sent the command.
	 * @param target The {@code Creature} being interacted with.
	 * @return {@code true} if the command was successfully processed, otherwise {@code false}.
	 */
	@Override
	public boolean onCommand(String command, Player player, Creature target)
	{
		if (!GeneralConfig.ALLOW_WAREHOUSE)
		{
			return false;
		}
		
		if (!target.isNpc())
		{
			return false;
		}
		
		if (player.hasItemRequest())
		{
			return false;
		}
		
		try
		{
			final String lowerCommand = command.toLowerCase();
			if (lowerCommand.startsWith(COMMANDS[0])) // WithdrawP
			{
				showWithdrawWindow(player);
				return true;
			}
			else if (lowerCommand.startsWith(COMMANDS[1])) // DepositP
			{
				player.sendPacket(ActionFailed.STATIC_PACKET);
				player.setActiveWarehouse(player.getWarehouse());
				player.setInventoryBlockingStatus(true);
				player.sendPacket(new WareHouseDepositList(1, player, WareHouseDepositList.PRIVATE));
				player.sendPacket(new WareHouseDepositList(2, player, WareHouseDepositList.PRIVATE));
				return true;
			}
			
			return false;
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Exception in " + getClass().getSimpleName(), e);
		}
		
		return false;
	}
	
	/**
	 * Opens the private warehouse withdrawal window for a specific player.<br>
	 * It checks if the player has any items deposited before sending the packet.<br>
	 * If the warehouse is empty, it sends an error message to the player.
	 * @param player The {@code Player} who will view the warehouse contents.
	 */
	private void showWithdrawWindow(Player player)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
		player.setActiveWarehouse(player.getWarehouse());
		
		if (player.getActiveWarehouse().getSize() == 0)
		{
			player.sendPacket(SystemMessageId.YOU_HAVE_NOT_DEPOSITED_ANY_ITEMS_IN_YOUR_WAREHOUSE);
			return;
		}
		
		player.sendPacket(new WareHouseWithdrawalList(1, player, WareHouseWithdrawalList.PRIVATE));
		player.sendPacket(new WareHouseWithdrawalList(2, player, WareHouseWithdrawalList.PRIVATE));
	}
	
	/**
	 * Retrieves the list of available commands.<br>
	 * This method returns all valid strings for this handler.
	 * @return an array of {@code String} containing the command names.
	 */
	@Override
	public String[] getCommandList()
	{
		return COMMANDS;
	}
}
