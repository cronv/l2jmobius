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
package ai.areas.Gracia.KeucereusAllianceBase.Erda;

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
 * This class handles the AI behavior for the {@code Erda} NPC.<br>
 * It manages interactions and logic specific to this entity within its designated area.
 * @author NviX
 */
public class Erda extends Script
{
	// NPCs
	private static final int ERDA = 34319;
	private static final int ETINA_RAID = 29318;
	
	// Location
	private static final Location ENTER_LOC = new Location(-245778, 181088, 2860);
	private static final Location CAMILLE_LOC = new Location(-245758, 149020, 11838);
	
	/**
	 * Initializes the {@code Erda} script.<br>
	 * Registers the required talk IDs for the NPC.
	 */
	public Erda()
	{
		addFirstTalkId(ERDA);
		addTalkId(ERDA);
	}
	
	/**
	 * Handles specific events for the Erda NPC.<br>
	 * This method processes teleport requests and checks party requirements.<br>
	 * It manages raid status updates and location teleports.
	 * @param event The name of the event to trigger.
	 * @param npc The {@code Npc} object involved in the interaction.
	 * @param player The {@code Player} who triggered the event.
	 * @return A string representing the HTML message to display or {@code null}.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.equals("teleport"))
		{
			final int status = GrandBossManager.getInstance().getStatus(ETINA_RAID);
			if (player.isGM())
			{
				player.teleToLocation(ENTER_LOC, true);
				GrandBossManager.getInstance().setStatus(ETINA_RAID, 1);
			}
			else
			{
				if (status == 1)
				{
					return "34319-1.html";
				}
				
				if (status == 2)
				{
					return "34319-2.html";
				}
				
				if (!player.isInParty())
				{
					return "34319-3.html";
				}
				
				final Party party = player.getParty();
				final boolean isInCC = party.isInCommandChannel();
				final boolean isPartyLeader = (isInCC) ? party.getCommandChannel().isLeader(player) : party.isLeader(player);
				if (!isPartyLeader)
				{
					return "34319-3.html";
				}
				
				final List<Player> members = (isInCC) ? party.getCommandChannel().getMembers() : party.getMembers();
				final int memberSize = members.size();
				if ((memberSize < GrandBossConfig.ETINA_MIN_PLAYERS) || (memberSize > GrandBossConfig.ETINA_MAX_PLAYERS))
				{
					final NpcHtmlMessage packet = new NpcHtmlMessage(npc.getObjectId());
					packet.setHtml(getHtm(player, "34319-4.html"));
					packet.replace("%min%", Integer.toString(GrandBossConfig.ETINA_MIN_PLAYERS));
					packet.replace("%max%", Integer.toString(GrandBossConfig.ETINA_MAX_PLAYERS));
					player.sendPacket(packet);
					return null;
				}
				
				for (Player member : members)
				{
					if (member.getLevel() < GrandBossConfig.ETINA_MIN_PLAYER_LEVEL)
					{
						final NpcHtmlMessage packet = new NpcHtmlMessage(npc.getObjectId());
						packet.setHtml(getHtm(player, "34319-5.html"));
						packet.replace("%minLevel%", Integer.toString(GrandBossConfig.ETINA_MIN_PLAYER_LEVEL));
						player.sendPacket(packet);
						return null;
					}
				}
				
				for (Player member : members)
				{
					if (member.isInsideRadius3D(npc, PlayerConfig.ALT_PARTY_RANGE))
					{
						member.teleToLocation(ENTER_LOC, false);
						GrandBossManager.getInstance().setStatus(ETINA_RAID, 1);
					}
				}
			}
		}
		else if (event.equals("tp_inner"))
		{
			player.teleToLocation(CAMILLE_LOC, true);
		}
		
		return super.onEvent(event, npc, player);
	}
	
	/**
	 * Handles the interaction when a {@code Player} talks to an {@code Npc} for the first time.<br>
	 * This method returns the name of the HTML file to display.
	 * @param npc The {@code Npc} being interacted with.
	 * @param player The {@code Player} who initiated the conversation.
	 * @return A {@code String} representing the path to the HTML message.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return "34319.html";
	}
	
	/**
	 * This is the main entry point for the {@code Erda} script.<br>
	 * It initializes a new instance of the {@code Erda} class.
	 * @param args Command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		new Erda();
	}
}
