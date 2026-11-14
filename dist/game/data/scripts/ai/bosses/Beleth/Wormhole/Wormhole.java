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
package ai.bosses.Beleth.Wormhole;

import java.util.List;

import org.l2jmobius.gameserver.config.GrandBossConfig;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.groups.Party;
import org.l2jmobius.gameserver.managers.GrandBossManager;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * Handles the artificial intelligence for the {@code Wormhole} NPC with ID {@code 33901}.<br>
 * This class manages the specific behaviors and logic for this boss entity.
 * @author Gigi
 */
public class Wormhole extends Script
{
	// NPCs
	private static final int WORMHOLE = 33901;
	private static final int BELETH = 29118;
	
	// Location
	private static final Location BELETH_LOCATION = new Location(16327, 209228, -9357);
	
	// TODO: New location
	// private static final Location BELETH_LOCATION = new Location(-17551, 245949, -832);
	
	/**
	 * Initializes the {@code Wormhole} script.<br>
	 * Registers the required talk IDs for the NPC.
	 */
	public Wormhole()
	{
		addFirstTalkId(WORMHOLE);
		addTalkId(WORMHOLE);
	}
	
	/**
	 * Handles specific events for the Wormhole NPC.<br>
	 * This method processes logic such as teleporting players based on boss status and party requirements.
	 * @param event The name of the event to trigger.
	 * @param npc The {@code Npc} instance involved in the event.
	 * @param player The {@code Player} who triggered the event.
	 * @return A string representing the HTML message to display, or {@code null} if no message is sent.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.equals("teleport"))
		{
			final int status = GrandBossManager.getInstance().getStatus(BELETH);
			if (status == 1)
			{
				return "33901-4.html";
			}
			
			if (status == 2)
			{
				return "33901-5.html";
			}
			
			if (!player.isInParty())
			{
				final NpcHtmlMessage packet = new NpcHtmlMessage(npc.getObjectId());
				packet.setHtml(getHtm(player, "33901-2.html"));
				packet.replace("%min%", Integer.toString(GrandBossConfig.BELETH_MIN_PLAYERS));
				player.sendPacket(packet);
				return null;
			}
			
			final Party party = player.getParty();
			final boolean isInCC = party.isInCommandChannel();
			final boolean isPartyLeader = (isInCC) ? party.getCommandChannel().isLeader(player) : party.isLeader(player);
			if (!isPartyLeader)
			{
				return "33901-3.html";
			}
			
			final List<Player> members = (isInCC) ? party.getCommandChannel().getMembers() : party.getMembers();
			final int memberSize = members.size();
			if ((memberSize < GrandBossConfig.BELETH_MIN_PLAYERS) || (memberSize > GrandBossConfig.BELETH_MAX_PLAYERS))
			{
				final NpcHtmlMessage packet = new NpcHtmlMessage(npc.getObjectId());
				packet.setHtml(getHtm(player, "33901-2.html"));
				packet.replace("%min%", Integer.toString(GrandBossConfig.BELETH_MIN_PLAYERS));
				player.sendPacket(packet);
			}
			else
			{
				for (Player member : members)
				{
					if (member.isInsideRadius3D(npc, 1000))
					{
						member.teleToLocation(BELETH_LOCATION, true);
					}
				}
			}
		}
		
		return null;
	}
	
	/**
	 * This method handles the first interaction between a {@code Player} and an {@code Npc}.<br>
	 * It sends a specific HTML message to the player.<br>
	 * The message includes minimum and maximum player requirements from {@code GrandBossConfig}.
	 * @param npc The {@code Npc} entity being interacted with.
	 * @param player The {@code Player} who initiated the interaction.
	 * @return This method returns {@code null} after sending the packet.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		final NpcHtmlMessage packet = new NpcHtmlMessage(npc.getObjectId());
		packet.setHtml(getHtm(player, "33901-1.html"));
		packet.replace("%min%", Integer.toString(GrandBossConfig.BELETH_MIN_PLAYERS));
		packet.replace("%max%", Integer.toString(GrandBossConfig.BELETH_MAX_PLAYERS));
		player.sendPacket(packet);
		return null;
	}
	
	/**
	 * This is the entry point for the {@code Wormhole} script.<br>
	 * It initializes a new instance of the {@code Wormhole} class.
	 * @param args Command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		new Wormhole();
	}
}
