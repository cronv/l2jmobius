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
package ai.areas.Gracia.SeedOfAnnihilation.Klemis;

import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.script.Script;

/**
 * This class defines the artificial intelligence behavior for the {@code Klemis} NPC.<br>
 * It handles the logic for how this entity interacts with players and its environment.
 * @author St3eT
 */
public class Klemis extends Script
{
	// NPC
	private static final int KLEMIS = 32734; // Klemis
	
	// Location
	private static final Location LOCATION = new Location(-180218, 185923, -10576);
	
	// Misc
	private static final int MIN_LV = 85;
	
	/**
	 * Private constructor for the {@code Klemis} class.<br>
	 * This prevents other classes from creating new instances of this script.
	 */
	private Klemis()
	{
		addStartNpc(KLEMIS);
		addTalkId(KLEMIS);
		addFirstTalkId(KLEMIS);
	}
	
	/**
	 * Handles specific events triggered by the {@code Klemis} script.<br>
	 * It checks for the {@code portInside} event to teleport players.<br>
	 * If the player level is too low, it returns a specific HTML file.
	 * @param event The name of the event being triggered.
	 * @param npc The {@code Npc} instance involved in the event.
	 * @param player The {@code Player} instance interacting with the NPC.
	 * @return A string representing the result or the next script to run.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.equals("portInside"))
		{
			if (player.getLevel() >= MIN_LV)
			{
				player.teleToLocation(LOCATION);
			}
			else
			{
				return "32734-01.html";
			}
		}
		
		return super.onEvent(event, npc, player);
	}
	
	/**
	 * This method handles the first interaction between a {@code Player} and an {@code Npc}.<br>
	 * It determines which dialogue file to display based on the NPC's ID.
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
	 * This is the entry point for the {@code Klemis} class.<br>
	 * It initializes a new instance of the {@code Klemis} object.
	 * @param args Command line arguments passed to the program.
	 */
	public static void main(String[] args)
	{
		new Klemis();
	}
}
