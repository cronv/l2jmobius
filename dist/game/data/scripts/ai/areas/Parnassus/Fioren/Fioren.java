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
package ai.areas.Parnassus.Fioren;

import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.network.enums.Movie;

/**
 * This class defines the artificial intelligence behavior for the {@code Fioren} NPC.<br>
 * It handles the logic and actions performed by this entity within its designated area.
 * @author St3eT
 */
public class Fioren extends Script
{
	// NPCs
	private static final int FIOREN = 33044;
	
	/**
	 * This is a private constructor for the {@code Fioren} class.<br>
	 * It initializes the NPC IDs for events and dialogue.<br>
	 * This method does not take any parameters.
	 */
	private Fioren()
	{
		addStartNpc(FIOREN);
		addTalkId(FIOREN);
		addFirstTalkId(FIOREN);
	}
	
	/**
	 * Handles specific events triggered by the {@code Fioren} script.<br>
	 * It checks for the {@code startMovie} event to play a cinematic.<br>
	 * If no special logic is met, it calls the parent method.
	 * @param event The name of the event being triggered.
	 * @param npc The {@code Npc} instance involved in the event.
	 * @param player The {@code Player} instance interacting with the NPC.
	 * @return The result of the event processing as a {@code String}.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.equals("startMovie"))
		{
			playMovie(player, Movie.SI_BARLOG_STORY);
		}
		
		return super.onEvent(event, npc, player);
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
	 * This is the entry point for the {@code Fioren} class.<br>
	 * It initializes a new instance of the {@code Fioren} script.
	 * @param args Command line arguments passed to the program.
	 */
	public static void main(String[] args)
	{
		new Fioren();
	}
}
