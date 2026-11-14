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
package ai.others;

import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.data.sql.PlayerInfoTable;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.groups.Party;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.itemcontainer.PlayerInventory;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.ListenerRegisterType;
import org.l2jmobius.gameserver.mechanics.events.annotations.RegisterEvent;
import org.l2jmobius.gameserver.mechanics.events.annotations.RegisterType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerBypass;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.network.serverpackets.PartySmallWindowAll;
import org.l2jmobius.gameserver.network.serverpackets.PartySmallWindowDeleteAll;

/**
 * Handles the logic for processing a name change ticket.<br>
 * This script allows players to exchange an item to change their character name.
 * @author Mobius
 */
public class NameChangeTicket extends Script
{
	private static final int NAME_CHANGE_TICKET = 23622;
	
	/**
	 * Private constructor for the {@code NameChangeTicket} class.<br>
	 * This prevents other classes from creating new instances of this script.
	 */
	private NameChangeTicket()
	{
	}
	
	/**
	 * Handles the logic for changing a player's name using a ticket.<br>
	 * This method checks if the command is valid and if the player has the required item.<br>
	 * It validates the new name, consumes the ticket, and updates the character data.
	 * @param event The {@code OnPlayerBypass} event containing the command and player information.
	 */
	@RegisterEvent(EventType.ON_PLAYER_BYPASS)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void onPlayerBypass(OnPlayerBypass event)
	{
		final Player player = event.getPlayer();
		if (player == null)
		{
			return;
		}
		
		final String command = event.getCommand();
		if (!command.contains("_Script NameChangeTicket "))
		{
			return;
		}
		
		// Need to have at least one Name Change Ticket in order to proceed.
		final PlayerInventory inventory = player.getInventory();
		if (inventory.getAllItemsByItemId(NAME_CHANGE_TICKET).isEmpty())
		{
			return;
		}
		
		final String newName = command.split(" ")[2].trim();
		if (!StringUtil.isAlphaNumeric(newName))
		{
			player.sendMessage("Name must only contain alphanumeric characters.");
			return;
		}
		
		if (PlayerInfoTable.getInstance().doesCharNameExist(newName))
		{
			player.sendMessage("Name " + newName + " already exists.");
			return;
		}
		
		// Destroy item.
		player.destroyItemByItemId(ItemProcessType.FEE, NAME_CHANGE_TICKET, 1, player, true);
		
		// Set name and proceed.
		player.setName(newName);
		PlayerInfoTable.getInstance().addName(player);
		player.storeMe();
		
		player.sendMessage("Your name has been changed.");
		player.broadcastUserInfo();
		
		final Party party = player.getParty();
		if (party != null)
		{
			// Delete party window for other party members.
			party.broadcastToPartyMembers(player, PartySmallWindowDeleteAll.STATIC_PACKET);
			for (Player member : party.getMembers())
			{
				// And re-add
				if (member != player)
				{
					member.sendPacket(new PartySmallWindowAll(member, party));
				}
			}
		}
		
		final Clan clan = player.getClan();
		if (clan != null)
		{
			clan.broadcastClanStatus();
		}
	}
	
	/**
	 * This is the entry point for the {@code NameChangeTicket} script.<br>
	 * It initializes a new instance of the class to start execution.
	 * @param args Command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		new NameChangeTicket();
	}
}
