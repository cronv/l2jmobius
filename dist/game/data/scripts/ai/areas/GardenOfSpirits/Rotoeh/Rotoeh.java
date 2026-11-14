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
package ai.areas.GardenOfSpirits.Rotoeh;

import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.script.Script;

/**
 * This class defines the AI behavior for the {@code Rotoeh} NPC.<br>
 * It handles specific interactions and logic within the Garden of Spirits area.
 * @author Mobius
 */
public class Rotoeh extends Script
{
	// NPC
	private static final int ROTOEH = 34239;
	
	// Teleport
	private static final Location BLACKBIRD_CAMPSITE = new Location(-48354, 69435, -3081);
	
	/**
	 * Private constructor for the {@code Rotoeh} class.<br>
	 * This constructor initializes the script IDs.
	 */
	private Rotoeh()
	{
		addFirstTalkId(ROTOEH);
		addTalkId(ROTOEH);
	}
	
	/**
	 * Handles specific events triggered by the {@code Rotoeh} script.<br>
	 * It processes logic based on the provided {@code event} string.<br>
	 * This method manages teleportation requirements and returns HTML content.
	 * @param event The name of the event to trigger.
	 * @param npc The {@code Npc} instance involved in the interaction.
	 * @param player The {@code Player} instance interacting with the NPC.
	 * @return A string containing HTML text or {@code null}.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = null;
		switch (event)
		{
			case "teleport":
			{
				// if (player.getFactionLevel(Faction.UNWORLDLY_VISITORS) < 4)
				// {
				// htmltext = "34239-01.html";
				// }
				// else
				// {
				player.teleToLocation(BLACKBIRD_CAMPSITE);
				// }
				break;
			}
		}
		
		return htmltext;
	}
	
	/**
	 * This method handles the first interaction between a {@code Player} and an {@code Npc}.<br>
	 * It returns the name of the dialogue file to be displayed.
	 * @param npc The {@code Npc} entity that is being spoken to.
	 * @param player The {@code Player} entity who initiated the conversation.
	 * @return A {@code String} representing the path to the HTML dialogue file.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return "34239.html";
	}
	
	/**
	 * This is the entry point for the {@code Rotoeh} class.<br>
	 * It initializes a new instance of the script.
	 * @param args Command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		new Rotoeh();
	}
}
