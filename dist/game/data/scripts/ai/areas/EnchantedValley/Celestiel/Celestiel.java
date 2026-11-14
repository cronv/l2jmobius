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
package ai.areas.EnchantedValley.Celestiel;

import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.script.Faction;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.network.serverpackets.PlaySound;

/**
 * This class defines the artificial intelligence behavior for {@code Celestiel} NPCs.<br>
 * It handles the logic and actions performed by these entities within their specific area.
 * @author Gigi
 * @date 2017-06-13 - [20:09:34]
 */
public class Celestiel extends Script
{
	// NPC
	private static final int CELESTIEL = 34234;
	
	// Teleports
	private static final Location SOUTH_LOCATION = new Location(110815, 59655, -3720);
	private static final Location NORTH_LOCATION = new Location(124040, 43970, -3720);
	
	private static final String[] CELESTIEL_VOICE =
	{
		"Npcdialog1.selestiel_faction_1",
		"Npcdialog1.selestiel_faction_2"
	};
	
	/**
	 * Private constructor for the {@code Celestiel} class.<br>
	 * This prevents other classes from creating new instances of this script.
	 */
	private Celestiel()
	{
		addTalkId(CELESTIEL);
		addFirstTalkId(CELESTIEL);
	}
	
	/**
	 * Handles specific events for the Celestiel NPC.<br>
	 * Processes teleport requests and faction checks based on the event name.
	 * @param event The unique identifier for the event to trigger.
	 * @param npc The {@code Npc} instance involved in the interaction.
	 * @param player The {@code Player} instance interacting with the NPC.
	 * @return The name of the HTML file to display or {@code null}.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = null;
		switch (event)
		{
			case "34234-1.html":
			case "34234-2.html":
			case "34234-3.html":
			case "34234-4.html":
			{
				htmltext = event;
				break;
			}
			case "south":
			{
				if (player.getFactionLevel(Faction.MOTHER_TREE_GUARDIANS) < 2)
				{
					htmltext = "34234-5.html";
				}
				else
				{
					player.teleToLocation(SOUTH_LOCATION);
				}
				break;
			}
			case "north":
			{
				if (player.getFactionLevel(Faction.MOTHER_TREE_GUARDIANS) < 2)
				{
					htmltext = "34234-5.html";
				}
				else
				{
					player.teleToLocation(NORTH_LOCATION);
				}
				break;
			}
		}
		
		return htmltext;
	}
	
	/**
	 * Handles the interaction when a player talks to an {@code Npc} for the first time.<br>
	 * This method plays a random voice sound and returns the dialog file path.
	 * @param npc The {@code Npc} entity being interacted with.
	 * @param player The {@code Player} who initiated the conversation.
	 * @return A {@code String} representing the name of the HTML dialog file to display.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		player.sendPacket(new PlaySound(3, CELESTIEL_VOICE[getRandom(2)], 0, 0, 0, 0, 0));
		return "34234.html";
	}
	
	/**
	 * This is the main entry point for the {@code Celestiel} class.<br>
	 * It initializes a new instance of the {@code Celestiel} object.
	 * @param args Command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		new Celestiel();
	}
}
