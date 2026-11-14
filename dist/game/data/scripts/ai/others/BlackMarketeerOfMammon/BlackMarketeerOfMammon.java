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
package ai.others.BlackMarketeerOfMammon;

import java.util.StringTokenizer;

import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.itemcontainer.Inventory;
import org.l2jmobius.gameserver.mechanics.script.Script;

/**
 * This class defines the artificial intelligence behavior for the {@code BlackMarketeerOfMammon} NPC.<br>
 * It handles the logic for how this specific entity interacts with players and the game world.
 * @author St3eT, Mobius
 */
public class BlackMarketeerOfMammon extends Script
{
	// NPC
	private static final int BLACK_MARKETEER = 31092;
	
	/**
	 * Private constructor for the {@code BlackMarketeerOfMammon} class.<br>
	 * This constructor initializes the NPC IDs for the script.<br>
	 * It registers the start, talk, and first talk IDs.
	 */
	private BlackMarketeerOfMammon()
	{
		addStartNpc(BLACK_MARKETEER);
		addTalkId(BLACK_MARKETEER);
		addFirstTalkId(BLACK_MARKETEER);
	}
	
	/**
	 * Handles specific events for the Black Marketeer of Mammon script.<br>
	 * It processes item exchanges and returns the appropriate HTML response.
	 * @param event The name of the current event or HTML file to process.
	 * @param npc The {@code Npc} instance triggering the event.
	 * @param player The {@code Player} instance interacting with the NPC.
	 * @return The name of the HTML file to display next.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;
		if (event.equals("31092-01.html"))
		{
			return event;
		}
		else if (event.startsWith("exchange"))
		{
			final StringTokenizer st = new StringTokenizer(event, " ");
			htmltext = st.nextToken();
			if (!st.hasMoreElements())
			{
				return "31092-02.html";
			}
			
			final String value = st.nextToken();
			if (!StringUtil.isNumeric(value) || (value.length() > 18))
			{
				return "31092-02.html";
			}
			
			final long count = Long.parseLong(value);
			final long AAcount = player.getAncientAdena();
			if (count < 1)
			{
				return "31092-02.html";
			}
			
			if (count > AAcount)
			{
				return "31092-03.html";
			}
			
			takeItems(player, Inventory.ANCIENT_ADENA_ID, count);
			giveAdena(player, count, false);
			return "31092-04.html";
		}
		
		return super.onEvent(htmltext, npc, player);
	}
	
	/**
	 * This method handles the first interaction between a {@code Player} and an {@code Npc}.<br>
	 * It determines which HTML file to display based on the NPC's ID.
	 * @param npc The {@code Npc} entity that is being interacted with.
	 * @param player The {@code Player} entity who initiated the conversation.
	 * @return A {@code String} representing the path to the HTML file.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return npc.getId() + ".html";
	}
	
	/**
	 * This is the entry point for the {@code BlackMarketeerOfMammon} class.<br>
	 * It initializes a new instance of the script.
	 * @param args Command line arguments passed to the program.
	 */
	public static void main(String[] args)
	{
		new BlackMarketeerOfMammon();
	}
}
