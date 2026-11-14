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
package ai.areas.BlackbirdCampsite.Belas;

import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.script.Script;

/**
 * This class defines the artificial intelligence for the {@code Belas} teleporter.<br>
 * It handles the behavior and logic for this specific NPC within the Blackbird Campsite area.
 * @author Gigi
 */
public class Belas extends Script
{
	// NPC
	private static final int BELAS = 34056;
	
	// Teleports
	private static final Location EAST = new Location(-41168, 79507, -4000);
	private static final Location WEST = new Location(-59485, 79782, -4104);
	
	// Item
	// private static final int MARK_OF_TRUST_MID_GRADE = 45843;
	// private static final int MARK_OF_TRUST_HIGH_GRADE = 45848;
	
	/**
	 * Private constructor for the {@code Belas} class.<br>
	 * This constructor initializes the required talk IDs.
	 */
	private Belas()
	{
		addFirstTalkId(BELAS);
		addTalkId(BELAS);
	}
	
	/**
	 * Handles specific events for the Belas NPC.<br>
	 * This method processes teleportation requests and returns HTML text.
	 * @param event The name of the event to trigger.
	 * @param npc The {@code Npc} instance involved in the event.
	 * @param player The {@code Player} instance interacting with the NPC.
	 * @return The resulting HTML string or {@code null}.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = null;
		switch (event)
		{
			case "34056-01.html":
			{
				htmltext = event;
				break;
			}
			case "West":
			{
				// if (hasQuestItems(player, MARK_OF_TRUST_MID_GRADE) || hasQuestItems(player, MARK_OF_TRUST_HIGH_GRADE))
				// {
				player.teleToLocation(WEST);
				
				// break;
				// }
				// htmltext = "34056-02.html";
				break;
			}
			case "East":
			{
				// if (hasQuestItems(player, MARK_OF_TRUST_MID_GRADE) || hasQuestItems(player, MARK_OF_TRUST_HIGH_GRADE))
				// {
				player.teleToLocation(EAST);
				
				// break;
				// }
				// htmltext = "34056-02.html";
				break;
			}
		}
		
		return htmltext;
	}
	
	/**
	 * This method handles the first interaction between a {@code Player} and an {@code Npc}.<br>
	 * It returns the name of the HTML file to display.
	 * @param npc The {@code Npc} entity that is being interacted with.
	 * @param player The {@code Player} entity who initiated the conversation.
	 * @return A {@code String} representing the filename of the dialogue to show.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return "34056.html";
	}
	
	/**
	 * This is the entry point for the {@code Belas} class.<br>
	 * It initializes a new instance of the {@code Belas} script.
	 * @param args Command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		new Belas();
	}
}
