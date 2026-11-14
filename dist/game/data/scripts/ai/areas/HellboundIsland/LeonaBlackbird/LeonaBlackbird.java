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
package ai.areas.HellboundIsland.LeonaBlackbird;

import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.network.enums.Movie;

/**
 * This class defines the artificial intelligence behavior for the {@code LeonaBlackbird} NPC.<br>
 * It handles the specific logic and actions for this entity within its designated area.
 * @author St3eT
 */
public class LeonaBlackbird extends Script
{
	// NPCs
	private static final int LEONA = 31595; // Leona Blackbird
	
	/**
	 * Initializes a new instance of the {@code LeonaBlackbird} class.<br>
	 * This constructor registers the necessary IDs for the NPC.<br>
	 * It sets up the starting NPC and talk events.
	 */
	public LeonaBlackbird()
	{
		addFirstTalkId(LEONA);
		addTalkId(LEONA);
		addStartNpc(LEONA);
	}
	
	/**
	 * Handles specific events for the {@code LeonaBlackbird} script.<br>
	 * It processes HTML text requests and movie playback commands.
	 * @param event The name of the event to trigger.
	 * @param npc The {@code Npc} instance involved in the event.
	 * @param player The {@code Player} instance interacting with the NPC.
	 * @return The HTML text to display or null if no text is required.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = null;
		switch (event)
		{
			case "31595.html":
			case "31595-01.html":
			case "31595-02.html":
			case "31595-03.html":
			{
				htmltext = event;
				break;
			}
			case "playMovie":
			{
				playMovie(player, Movie.SC_HELLBOUND);
				break;
			}
		}
		
		return htmltext;
	}
	
	/**
	 * This method handles the first interaction between a {@code Player} and an {@code Npc}.<br>
	 * It determines which dialogue file to display based on the NPC's unique ID.
	 * @param npc The {@code Npc} entity that is being interacted with.
	 * @param player The {@code Player} who initiated the conversation.
	 * @return A {@code String} representing the path to the HTML dialogue file.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return npc.getId() + ".html";
	}
	
	/**
	 * This is the main entry point for the {@code LeonaBlackbird} class.<br>
	 * It initializes a new instance of the script.
	 * @param args Command line arguments passed to the program.
	 */
	public static void main(String[] args)
	{
		new LeonaBlackbird();
	}
}
