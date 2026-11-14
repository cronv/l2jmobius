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
package ai.areas.GiantsCave;

import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.network.serverpackets.PlaySound;

/**
 * This class handles the artificial intelligence for NPCs located in the {@code GiantsCave} area.<br>
 * It defines the behaviors and scripts used by these entities to interact with players.
 * @author Gigi
 * @date 2017-03-04 - [17:33:25]
 */
public class GiantsCave extends Script
{
	// NPC
	private static final int SUMADRIBA = 34217;
	private static final int KRENAHT = 34237;
	private static final int STHOR = 34219;
	private static final int GIANT_TELEPORT = 34223;
	
	/**
	 * Private constructor for the {@code GiantsCave} class.<br>
	 * This prevents other classes from creating new instances of this script.
	 */
	private GiantsCave()
	{
		addTalkId(SUMADRIBA, KRENAHT, GIANT_TELEPORT, STHOR);
		addFirstTalkId(SUMADRIBA, KRENAHT, GIANT_TELEPORT, STHOR);
	}
	
	/**
	 * Handles specific events for the {@code GiantsCave} script.<br>
	 * It processes logic for attacks and clearing actions.
	 * @param event The name of the event to trigger.
	 * @param npc The {@code Npc} instance involved in the event.
	 * @param player The {@code Player} instance involved in the event.
	 * @return The result of the super class method call.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = null;
		switch (event)
		{
			case "34237.html":
			case "34237-1.html":
			case "34237-2.html":
			case "34237-3.html":
			case "34237-4.html":
			case "34237-5.html":
			case "34237-6.html":
			{
				htmltext = event;
				break;
			}
			case "first_area":
			{
				player.teleToLocation(174501, 52876, -4370);
				break;
			}
			case "second_area":
			{
				player.teleToLocation(178037, 52250, -3993);
				break;
			}
			case "third_area":
			{
				player.teleToLocation(181226, 50418, -4370);
				break;
			}
			case "fourth_area":
			{
				player.teleToLocation(183638, 47541, -4370);
				break;
			}
			case "stronghold":
			{
				player.teleToLocation(187467, 60921, -4983);
				break;
			}
			case "relics_rooom":
			{
				player.teleToLocation(186611, 59968, -7236);
				break;
			}
		}
		
		return htmltext;
	}
	
	/**
	 * Handles the logic when a player talks to an {@code Npc} for the first time.<br>
	 * This method plays specific sounds based on the NPC's ID.<br>
	 * It returns the path to the dialogue file.
	 * @param npc The {@code Npc} entity being interacted with.
	 * @param player The {@code Player} who initiated the conversation.
	 * @return A {@code String} representing the HTML dialogue file name.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		final int npcId = npc.getId();
		switch (npcId)
		{
			case SUMADRIBA:
			{
				player.sendPacket(new PlaySound(3, "Npcdialog1.schmadriba_faction_1", 0, 0, 0, 0, 0));
				break;
			}
			case KRENAHT:
			{
				if (getRandom(10) < 5)
				{
					player.sendPacket(new PlaySound(3, "Npcdialog1.krenat_faction_2", 0, 0, 0, 0, 0));
				}
				else
				{
					player.sendPacket(new PlaySound(3, "Npcdialog1.krenat_faction_1", 0, 0, 0, 0, 0));
				}
				break;
			}
			case STHOR:
			{
				if (getRandom(10) < 4)
				{
					player.sendPacket(new PlaySound(3, "Npcdialog1.stor_faction_2", 0, 0, 0, 0, 0));
				}
				else
				{
					player.sendPacket(new PlaySound(3, "Npcdialog1.stor_faction_1", 0, 0, 0, 0, 0));
				}
				break;
			}
		}
		
		return npcId + ".html";
	}
	
	/**
	 * This is the main entry point for the {@code GiantsCave} script.<br>
	 * It initializes a new instance of the class to start the AI logic.
	 * @param args Command line arguments passed to the program.
	 */
	public static void main(String[] args)
	{
		new GiantsCave();
	}
}
