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
package ai.areas.GardenOfSpirits.Eubina;

import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.script.Script;

/**
 * This class defines the AI behavior for the NPC {@code Eubina} in the Garden of Spirits.<br>
 * It handles specific interactions and logic for this character within the game world.
 * @author Mobius
 */
public class Eubina extends Script
{
	// NPC
	private static final int EUBINA = 34238;
	
	// Teleport
	private static final Location BLACKBIRD_CAMPSITE = new Location(-48354, 69435, -3081);
	
	/**
	 * Private constructor for the {@code Eubina} class.<br>
	 * This constructor initializes the script IDs.<br>
	 * It registers both first talk and regular talk IDs.
	 */
	private Eubina()
	{
		addFirstTalkId(EUBINA);
		addTalkId(EUBINA);
	}
	
	/**
	 * Handles specific events triggered by the {@code Eubina} script.<br>
	 * It processes logic based on the provided event name and entity states.
	 * @param event The name of the event to trigger.
	 * @param npc The {@code Npc} instance involved in the event.
	 * @param player The {@code Player} instance interacting with the script.
	 * @return A string containing HTML text or null if no message is returned.
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
				// htmltext = "34238-01.html";
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
	 * Handles the dialogue when a {@code Player} speaks to an {@code Npc} for the first time.<br>
	 * This method returns the name of the HTML file to display.
	 * @param npc The {@code Npc} being interacted with.
	 * @param player The {@code Player} initiating the conversation.
	 * @return The filename of the dialogue script.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return "34238.html";
	}
	
	/**
	 * This is the main entry point for the {@code Eubina} class.<br>
	 * It initializes a new instance of the {@code Eubina} script.
	 * @param args Command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		new Eubina();
	}
}
