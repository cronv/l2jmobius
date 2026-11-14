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
package ai.bosses.Trasken.Daichir;

import java.util.List;

import org.l2jmobius.gameserver.config.GrandBossConfig;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.groups.Party;
import org.l2jmobius.gameserver.managers.GrandBossManager;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * This class handles the artificial intelligence for the boss {@code Daichir}.<br>
 * It manages specific behaviors and teleportation logic for this entity.
 * @author Mobius
 */
public class Daichir extends Script
{
	// NPCs
	private static final int DAICHIR = 30537;
	private static final int TRASKEN = 29197;
	
	// Locations
	private static final Location ENTER_LOCATION = new Location(75445, -182112, -9880);
	
	// Status
	private static final int FIGHTING = 1;
	private static final int DEAD = 3;
	
	/**
	 * Initializes the {@code Daichir} script.<br>
	 * Registers the required talk IDs for the NPC.
	 */
	public Daichir()
	{
		addFirstTalkId(DAICHIR);
		addTalkId(DAICHIR);
	}
	
	/**
	 * Handles specific events for the Daichir NPC.<br>
	 * This method processes logic such as teleporting players and checking party requirements.
	 * @param event The name of the event to trigger.
	 * @param npc The {@code Npc} instance involved in the event.
	 * @param player The {@code Player} who triggered the event.
	 * @return A string representing the HTML message to display, or {@code null} if no action is taken.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.equals("enterEarthWyrnCave"))
		{
			final int status = GrandBossManager.getInstance().getStatus(TRASKEN);
			if (player.isGM())
			{
				player.teleToLocation(ENTER_LOCATION, true);
				GrandBossManager.getInstance().setStatus(TRASKEN, FIGHTING);
			}
			else
			{
				if (status == FIGHTING)
				{
					return "30537-1.html";
				}
				
				if (status == DEAD)
				{
					return "30537-2.html";
				}
				
				if (!player.isInParty())
				{
					return "30537-3.html";
				}
				
				final Party party = player.getParty();
				final boolean isInCC = party.isInCommandChannel();
				final boolean isPartyLeader = (isInCC) ? party.getCommandChannel().isLeader(player) : party.isLeader(player);
				if (!isPartyLeader)
				{
					return "30537-3.html";
				}
				
				final List<Player> members = (isInCC) ? party.getCommandChannel().getMembers() : party.getMembers();
				final int memberSize = members.size();
				if ((memberSize < GrandBossConfig.TRASKEN_MIN_PLAYERS) || (memberSize > GrandBossConfig.TRASKEN_MAX_PLAYERS))
				{
					final NpcHtmlMessage packet = new NpcHtmlMessage(npc.getObjectId());
					packet.setHtml(getHtm(player, "30537-4.html"));
					packet.replace("%min%", Integer.toString(GrandBossConfig.TRASKEN_MIN_PLAYERS));
					packet.replace("%max%", Integer.toString(GrandBossConfig.TRASKEN_MAX_PLAYERS));
					player.sendPacket(packet);
					return null;
				}
				
				for (Player member : members)
				{
					if (member.getLevel() < GrandBossConfig.TRASKEN_MIN_PLAYER_LEVEL)
					{
						final NpcHtmlMessage packet = new NpcHtmlMessage(npc.getObjectId());
						packet.setHtml(getHtm(player, "30537-5.html"));
						packet.replace("%minLevel%", Integer.toString(GrandBossConfig.TRASKEN_MIN_PLAYER_LEVEL));
						player.sendPacket(packet);
						return null;
					}
				}
				
				for (Player member : members)
				{
					if (member.isInsideRadius3D(npc, PlayerConfig.ALT_PARTY_RANGE))
					{
						member.teleToLocation(ENTER_LOCATION, true);
						GrandBossManager.getInstance().setStatus(TRASKEN, FIGHTING);
					}
				}
			}
		}
		
		return null;
	}
	
	/**
	 * Handles the initial interaction when a {@code Player} talks to an {@code Npc}.<br>
	 * This method determines which HTML file to display.
	 * @param npc The {@code Npc} that is being interacted with.
	 * @param player The {@code Player} who initiated the conversation.
	 * @return A {@code String} representing the path to the HTML file.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return npc.getId() + ".html";
	}
	
	/**
	 * This is the main entry point for the {@code Daichir} script.<br>
	 * It initializes a new instance of the {@code Daichir} class.
	 * @param args Command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		new Daichir();
	}
}
