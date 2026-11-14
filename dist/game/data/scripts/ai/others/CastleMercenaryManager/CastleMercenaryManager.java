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
package ai.others.CastleMercenaryManager;

import java.util.StringTokenizer;

import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.instance.Merchant;
import org.l2jmobius.gameserver.entity.clan.ClanAccess;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.mechanics.siege.Castle;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * Manages the artificial intelligence for castle mercenaries.<br>
 * This class handles the behavior and logic of mercenary units within a {@code Castle}.
 * @author malyelfik, Mobius
 */
public class CastleMercenaryManager extends Script
{
	// NPCs
	private static final int[] NPCS =
	{
		35102, // Greenspan
		35144, // Sanford
		35186, // Arvid
		35228, // Morrison
		35276, // Eldon
		35318, // Solinus
		35365, // Rowell
		35511, // Gompus
		35557, // Kendrew
	};
	
	/**
	 * Private constructor for the {@code CastleMercenaryManager} class.<br>
	 * It initializes the required NPC IDs and talk IDs.<br>
	 * This prevents other classes from creating new instances of this manager.
	 */
	private CastleMercenaryManager()
	{
		addStartNpc(NPCS);
		addTalkId(NPCS);
		addFirstTalkId(NPCS);
	}
	
	/**
	 * Handles specific events triggered by the Castle Mercenary Manager.<br>
	 * It processes commands like limit, buy, and main for NPCs.
	 * @param event The event name or command string to process.
	 * @param npc The {@code Npc} instance triggering the event.
	 * @param player The {@code Player} interacting with the NPC.
	 * @return The HTML text to be sent to the player, or {@code null}.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		// Only the castle owner may use the mercenary manager.
		if (!player.isGM() && ((player.getClanId() != npc.getCastle().getOwnerId()) || !player.hasAccess(ClanAccess.CASTLE_MERCENARIES)))
		{
			return "mercmanager-no.html";
		}
		
		String htmltext = null;
		final StringTokenizer st = new StringTokenizer(event, " ");
		switch (st.nextToken())
		{
			case "limit":
			{
				final Castle castle = npc.getCastle();
				final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
				final String name = castle.getName();
				if (name.equalsIgnoreCase("Aden"))
				{
					html.setHtml(getHtm(player, "mercmanager-aden-limit.html"));
				}
				else if (name.equalsIgnoreCase("Rune"))
				{
					html.setHtml(getHtm(player, "mercmanager-rune-limit.html"));
				}
				else
				{
					html.setHtml(getHtm(player, "mercmanager-limit.html"));
				}
				
				html.replace("%feud_name%", String.valueOf(1001000 + castle.getResidenceId()));
				player.sendPacket(html);
				break;
			}
			case "buy":
			{
				final int listId = Integer.parseInt(npc.getId() + st.nextToken());
				((Merchant) npc).showBuyWindow(player, listId, false); // NOTE: Not affected by Castle Taxes, baseTax is 20% (done in merchant buylists)
				break;
			}
			case "main":
			{
				htmltext = onFirstTalk(npc, player);
				break;
			}
			case "mercmanager-01.html":
			{
				htmltext = event;
				break;
			}
		}
		
		return htmltext;
	}
	
	/**
	 * Determines the correct HTML file to display when a player talks to an NPC for the first time.<br>
	 * It checks if the {@code Player} has the required permissions or is a GM.<br>
	 * If they do not have access, it returns a restricted message.
	 * @param npc The {@code Npc} being interacted with.
	 * @param player The {@code Player} initiating the conversation.
	 * @return The name of the HTML file to be displayed.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		final String htmltext;
		if (player.isGM() || ((player.getClanId() == npc.getCastle().getOwnerId()) && player.hasAccess(ClanAccess.CASTLE_MERCENARIES)))
		{
			htmltext = npc.getCastle().getSiege().isInProgress() ? "mercmanager-siege.html" : "mercmanager.html";
		}
		else
		{
			htmltext = "mercmanager-no.html";
		}
		
		return htmltext;
	}
	
	/**
	 * This is the entry point for the {@code CastleMercenaryManager} class.<br>
	 * It initializes a new instance of the manager to start the script.
	 * @param args Command line arguments passed to the program.
	 */
	public static void main(String[] args)
	{
		new CastleMercenaryManager();
	}
}
