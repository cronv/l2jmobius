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
package ai.areas.AncientCityArcan.Mumu;

import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.network.enums.Movie;

/**
 * Defines the artificial intelligence behavior for the {@code Mumu} NPC.<br>
 * This class handles the logic for how this specific entity interacts with players and its environment.
 * @author St3eT
 */
public class Mumu extends Script
{
	// NPC
	private static final int MUMU = 32900; // Mumu
	
	/**
	 * Creates a new instance of the {@code Mumu} class.<br>
	 * This constructor initializes the NPC IDs for the script.<br>
	 * It registers the starting and talking events for the character.
	 */
	public Mumu()
	{
		addStartNpc(MUMU);
		addFirstTalkId(MUMU);
		addTalkId(MUMU);
	}
	
	/**
	 * Handles specific events triggered by the {@code Mumu} script.<br>
	 * It processes HTML content and movie playback based on the provided event name.
	 * @param event The name of the event to trigger.
	 * @param npc The {@code Npc} instance involved in the event.
	 * @param player The {@code Player} instance interacting with the NPC.
	 * @return The HTML content string if applicable, or {@code null}.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = null;
		switch (event)
		{
			case "32900-1.html":
			{
				htmltext = event;
				break;
			}
			case "playMovie":
			{
				playMovie(player, Movie.SI_ARKAN_ENTER);
				break;
			}
		}
		
		return htmltext;
	}
	
	/**
	 * This method handles the first interaction between a {@code Player} and an {@code Npc}.<br>
	 * It determines which movie file to play based on the NPC's ID.
	 * @param npc The {@code Npc} entity that is being spoken to.
	 * @param player The {@code Player} entity who initiated the conversation.
	 * @return A string representing the path to the HTML movie file.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return npc.getId() + ".html";
	}
	
	/**
	 * This is the entry point for the {@code Mumu} class.<br>
	 * It initializes a new instance of the {@code Mumu} script.
	 * @param args Command line arguments passed to the program.
	 */
	public static void main(String[] args)
	{
		new Mumu();
	}
}
