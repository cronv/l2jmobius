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

import org.l2jmobius.gameserver.config.custom.OfflineTradeConfig;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.PlayerAction;
import org.l2jmobius.gameserver.handler.IVoicedCommandHandler;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.ConfirmDlg;

/**
 * Handles the {@code /offline} voiced command.<br>
 * This class manages the logic for players who wish to go offline while maintaining their current status.<br>
 * It interacts with {@link org.l2jmobius.gameserver.entity.actor.Player} to process the disconnection request.
 * @author Mobius
 */
public class Offline implements IVoicedCommandHandler
{
	private static final String[] VOICED_COMMANDS =
	{
		"offline",
		"offlinetrade",
		"offline_trade"
	};
	
	/**
	 * Handles the execution of voiced commands.<br>
	 * This method checks if the {@code command} is valid for the current player.<br>
	 * It manages specific logic for offline trading and crafting.
	 * @param command The name of the command sent by the player.
	 * @param player The {@code Player} object who executed the command.
	 * @param target The target of the command, if applicable.
	 * @return {@code true} if the command was handled successfully, or {@code false} otherwise.
	 */
	@Override
	public boolean onCommand(String command, Player player, String target)
	{
		if (command.equals("offline") && OfflineTradeConfig.ENABLE_OFFLINE_COMMAND && (OfflineTradeConfig.OFFLINE_TRADE_ENABLE || OfflineTradeConfig.OFFLINE_CRAFT_ENABLE))
		{
			if (!player.isInStoreMode())
			{
				player.sendPacket(SystemMessageId.PRIVATE_STORE_ALREADY_CLOSED);
				player.sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}
			
			if (player.isInInstance() || player.isInVehicle() || !player.canLogout())
			{
				player.sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}
			
			player.addAction(PlayerAction.OFFLINE_TRADE);
			player.sendPacket(new ConfirmDlg("Do you wish to exit and continue trading?"));
		}
		
		return true;
	}
	
	/**
	 * Retrieves the list of supported voice commands.<br>
	 * This method returns all valid command strings for this handler.
	 * @return an array of {@code String} containing the allowed commands.
	 */
	@Override
	public String[] getCommandList()
	{
		return VOICED_COMMANDS;
	}
}
