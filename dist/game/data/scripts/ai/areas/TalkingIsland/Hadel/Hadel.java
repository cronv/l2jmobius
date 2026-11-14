/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package ai.areas.TalkingIsland.Hadel;

import org.l2jmobius.gameserver.data.enums.CategoryType;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.script.Script;

/**
 * This class defines the artificial intelligence behavior for the {@code Hadel} NPC.<br>
 * It handles the specific logic and actions for this entity within its designated area.
 * @author St3eT
 */
public class Hadel extends Script
{
	// NPC
	private static final int HADEL = 33344;
	
	// Locations
	private static final Location GIANTS = new Location(-114562, 227307, -2864);
	private static final Location HARNAK = new Location(-114700, 147909, -7720);
	
	/**
	 * Private constructor for the {@code Hadel} class.<br>
	 * This constructor initializes the NPC IDs and talk IDs.<br>
	 * It prevents other classes from creating new instances of this script.
	 */
	private Hadel()
	{
		addStartNpc(HADEL);
		addTalkId(HADEL);
		addFirstTalkId(HADEL);
	}
	
	/**
	 * Handles specific events triggered by the {@code Hadel} NPC.<br>
	 * This method processes teleport requests and dialogue responses.
	 * @param event The name of the event to trigger.
	 * @param npc The {@code Npc} instance involved in the interaction.
	 * @param player The {@code Player} instance interacting with the NPC.
	 * @return The HTML text to display, or {@code null} if no text is returned.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = null;
		switch (event)
		{
			case "33344.html":
			case "33344-01.html":
			{
				htmltext = event;
				break;
			}
			case "teleportToGiants":
			{
				player.teleToLocation(GIANTS);
				break;
			}
			case "teleportToHarnak":
			{
				if ((!player.isInCategory(CategoryType.SIXTH_CLASS_GROUP)) || (player.getLevel() < 85))
				{
					htmltext = "33344-noClass.html";
					break;
				}
				
				player.teleToLocation(HARNAK);
				break;
			}
		}
		
		return htmltext;
	}
	
	/**
	 * This method handles the first interaction between a {@code Player} and an {@code Npc}.<br>
	 * It determines which dialogue file to display based on the NPC's unique ID.
	 * @param npc The {@code Npc} entity that is being spoken to.
	 * @param player The {@code Player} entity who initiated the conversation.
	 * @return A {@code String} representing the path to the HTML dialogue file.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return npc.getId() + ".html";
	}
	
	/**
	 * This is the entry point for the {@code Hadel} class.<br>
	 * It initializes a new instance of the {@code Hadel} object.
	 * @param args Command line arguments passed to the program.
	 */
	public static void main(String[] args)
	{
		new Hadel();
	}
}
