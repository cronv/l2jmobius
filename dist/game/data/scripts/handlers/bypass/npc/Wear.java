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

import java.util.StringTokenizer;
import java.util.logging.Level;

import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.data.xml.BuyListData;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.handler.IBypassHandler;
import org.l2jmobius.gameserver.mechanics.buylist.ProductList;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.ShopPreviewList;

/**
 * Handles the bypass command for NPCs that allow players to try on equipment.<br>
 * It manages the logic for displaying items from a {@code ProductList} and updating the player's preview.
 */
public class Wear implements IBypassHandler
{
	private static final String[] COMMANDS =
	{
		"Wear"
	};
	
	/**
	 * Handles the {@code Wear} command for NPCs.<br>
	 * This method opens a wear window if the target is an NPC and the config allows it.<br>
	 * It parses an integer value from the command string to determine the wear amount.
	 * @param command The full command string sent by the player.
	 * @param player The {@code Player} who executed the command.
	 * @param target The {@code Creature} being targeted by the command.
	 * @return {@code true} if the command was processed successfully, otherwise {@code false}.
	 */
	@Override
	public boolean onCommand(String command, Player player, Creature target)
	{
		if (!target.isNpc())
		{
			return false;
		}
		
		if (!GeneralConfig.ALLOW_WEAR)
		{
			return false;
		}
		
		try
		{
			final StringTokenizer st = new StringTokenizer(command, " ");
			st.nextToken();
			
			if (st.countTokens() < 1)
			{
				return false;
			}
			
			showWearWindow(player, Integer.parseInt(st.nextToken()));
			return true;
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Exception in " + getClass().getSimpleName(), e);
		}
		
		return false;
	}
	
	/**
	 * Displays the wear window to a specific {@code Player}.<br>
	 * This method fetches the product list based on the provided {@code value}.<br>
	 * It also blocks the inventory while showing the preview.
	 * @param player The {@code Player} who will see the window.
	 * @param value The unique identifier for the buy list to display.
	 */
	private void showWearWindow(Player player, int value)
	{
		final ProductList buyList = BuyListData.getInstance().getBuyList(value);
		if (buyList == null)
		{
			LOGGER.warning("BuyList not found! BuyListId:" + value);
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		player.setInventoryBlockingStatus(true);
		
		player.sendPacket(new ShopPreviewList(buyList, player.getAdena()));
	}
	
	/**
	 * Retrieves the list of valid commands for this handler.<br>
	 * This method returns all available command strings.
	 * @return an array of {@code String} containing the allowed commands.
	 */
	@Override
	public String[] getCommandList()
	{
		return COMMANDS;
	}
}
