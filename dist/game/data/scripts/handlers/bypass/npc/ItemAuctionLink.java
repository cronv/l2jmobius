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

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.StringTokenizer;
import java.util.logging.Level;

import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.auction.ItemAuction;
import org.l2jmobius.gameserver.entity.item.auction.ItemAuctionInstance;
import org.l2jmobius.gameserver.handler.IBypassHandler;
import org.l2jmobius.gameserver.managers.ItemAuctionManager;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExItemAuctionInfoPacket;

/**
 * Handles the bypass command for creating a link to an item auction.<br>
 * It allows players to generate a clickable link for a specific {@code ItemAuctionInstance}.<br>
 * This handler processes the input and sends the corresponding {@code ExItemAuctionInfoPacket} to the player.
 */
public class ItemAuctionLink implements IBypassHandler
{
	private static final DateTimeFormatter SDF = DateTimeFormatter.ofPattern("HH:mm:ss dd.MM.yyyy");
	
	private static final String[] COMMANDS =
	{
		"ItemAuction"
	};
	
	/**
	 * Processes commands sent to an NPC for the item auction system.<br>
	 * This method handles requests like showing current auctions or canceling bids.
	 * @param command The full command string sent by the player.
	 * @param player The {@code Player} instance who sent the command.
	 * @param target The {@code Creature} NPC being interacted with.
	 * @return {@code true} if the command was processed successfully, otherwise {@code false}.
	 */
	@Override
	public boolean onCommand(String command, Player player, Creature target)
	{
		if (!target.isNpc())
		{
			return false;
		}
		
		if (!GeneralConfig.ALT_ITEM_AUCTION_ENABLED)
		{
			player.sendPacket(SystemMessageId.IT_IS_NOT_AN_AUCTION_PERIOD);
			return true;
		}
		
		final ItemAuctionInstance au = ItemAuctionManager.getInstance().getManagerInstance(target.getId());
		if (au == null)
		{
			return false;
		}
		
		try
		{
			final StringTokenizer st = new StringTokenizer(command);
			st.nextToken(); // bypass "ItemAuction"
			if (!st.hasMoreTokens())
			{
				return false;
			}
			
			final String cmd = st.nextToken();
			if ("show".equalsIgnoreCase(cmd))
			{
				if (!player.getClient().getFloodProtectors().canUseItemAuction())
				{
					return false;
				}
				
				if (player.isItemAuctionPolling())
				{
					return false;
				}
				
				final ItemAuction currentAuction = au.getCurrentAuction();
				final ItemAuction nextAuction = au.getNextAuction();
				if (currentAuction == null)
				{
					player.sendPacket(SystemMessageId.IT_IS_NOT_AN_AUCTION_PERIOD);
					
					if (nextAuction != null)
					{
						player.sendMessage("The next auction will begin on the " + SDF.format(Instant.ofEpochMilli(nextAuction.getStartingTime()).atZone(ZoneId.systemDefault())) + ".");
					}
					
					return true;
				}
				
				player.sendPacket(new ExItemAuctionInfoPacket(false, currentAuction, nextAuction));
			}
			else if ("cancel".equalsIgnoreCase(cmd))
			{
				boolean returned = false;
				for (ItemAuction auction : au.getAuctionsByBidder(player.getObjectId()))
				{
					if (auction.cancelBid(player))
					{
						returned = true;
					}
				}
				
				if (!returned)
				{
					player.sendPacket(SystemMessageId.THERE_ARE_NO_OFFERINGS_I_OWN_OR_I_MADE_A_BID_FOR);
				}
			}
			else
			{
				return false;
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Exception in " + getClass().getSimpleName(), e);
		}
		
		return true;
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
