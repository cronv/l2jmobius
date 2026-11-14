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
package ai.areas.AteliaRefinery.Leona;

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
 * This class defines the AI behavior for the {@code Leona} NPC.<br>
 * It handles specific script logic and interactions within the Atelia Refinery area.
 * @author Liamxroy
 */
public class Leona extends Script
{
	// NPCs
	private static final int LEONA = 34426;
	private static final int ETINA_RAID = 29318;
	
	// Location
	private static final Location ENTER_LOC = new Location(-245778, 181088, 2860);
	private static final Location REFINERY = new Location(-59328, 52624, -8608);
	
	/**
	 * Initializes the {@code Leona} script.<br>
	 * Registers the required talk IDs for the NPC.
	 */
	public Leona()
	{
		addFirstTalkId(LEONA);
		addTalkId(LEONA);
	}
	
	/**
	 * Handles specific events for the {@code Leona} NPC.<br>
	 * This method processes teleport requests and checks party requirements.<br>
	 * It manages transitions to different locations based on game status.
	 * @param event The name of the event being triggered.
	 * @param npc The {@code Npc} instance triggering the event.
	 * @param player The {@code Player} instance interacting with the NPC.
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
					return "34426-1.html";
				}
				
				if (status == 2)
				{
					return "34426-2.html";
				}
				
				if (!player.isInParty())
				{
					return "34426-3.html";
				}
				
				final Party party = player.getParty();
				final boolean isInCC = party.isInCommandChannel();
				final boolean isPartyLeader = (isInCC) ? party.getCommandChannel().isLeader(player) : party.isLeader(player);
				if (!isPartyLeader)
				{
					return "34426-3.html";
				}
				
				final List<Player> members = (isInCC) ? party.getCommandChannel().getMembers() : party.getMembers();
				final int memberSize = members.size();
				if ((memberSize < GrandBossConfig.ETINA_MIN_PLAYERS) || (memberSize > GrandBossConfig.ETINA_MAX_PLAYERS))
				{
					final NpcHtmlMessage packet = new NpcHtmlMessage(npc.getObjectId());
					packet.setHtml(getHtm(player, "34426-4.html"));
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
						packet.setHtml(getHtm(player, "34426-5.html"));
						packet.replace("%minlvl%", Integer.toString(GrandBossConfig.ETINA_MIN_PLAYER_LEVEL));
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
			player.teleToLocation(REFINERY, true);
		}
		
		return super.onEvent(event, npc, player);
	}
	
	/**
	 * This method determines the correct HTML file to display when a player talks to an {@code Npc} for the first time.<br>
	 * It checks if the player has completed the {@code Q10890_SaviorsPathHallOfEtina} quest.<br>
	 * If the quest is finished, it returns a specific completion message.<br>
	 * Otherwise, it returns the default dialogue text.
	 * @param npc The {@code Npc} entity being interacted with.
	 * @param player The {@code Player} who initiated the conversation.
	 * @return A {@code String} representing the name of the HTML file to be displayed.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		// final QuestState qs = player.getQuestState(Q10890_SaviorsPathHallOfEtina.class.getSimpleName());
		String htmltext = null;
		
		// if (((qs != null) && qs.isCompleted()))
		// {
		// htmltext = "34426-0.html";
		// }
		// else
		// {
		htmltext = "34426.html";
		
		// }
		
		return htmltext;
	}
	
	/**
	 * This is the entry point for the {@code Leona} script.<br>
	 * It initializes a new instance of the {@code Leona} class.
	 * @param args Command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		new Leona();
	}
}
