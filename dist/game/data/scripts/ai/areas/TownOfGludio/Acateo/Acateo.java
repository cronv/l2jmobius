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
package ai.areas.TownOfGludio.Acateo;

import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.script.Script;

/**
 * This class defines the artificial intelligence behavior for the {@code Acateo} NPC.<br>
 * It handles the specific logic and actions for this character within the Town of Gludio area.
 * @author Gladicek
 */
public class Acateo extends Script
{
	// NPC
	private static final int ACATEO = 33905;
	
	// Item
	private static final int ACADEMY_CIRCLET = 8181;
	
	/**
	 * Private constructor for the {@code Acateo} class.<br>
	 * This constructor initializes the NPC IDs and talk events.<br>
	 * It prevents other classes from creating new instances of this script.
	 */
	private Acateo()
	{
		addStartNpc(ACATEO);
		addFirstTalkId(ACATEO);
		addTalkId(ACATEO);
	}
	
	/**
	 * Handles specific events for the {@code Acateo} NPC.<br>
	 * It checks if a {@code Player} can receive an item based on their current inventory.<br>
	 * If the event is {@code give_circlet}, it grants the required item and returns a response.<br>
	 * Otherwise, it passes the event to the parent class.
	 * @param event The name of the event being triggered.
	 * @param npc The {@code Npc} instance involved in the interaction.
	 * @param player The {@code Player} interacting with the NPC.
	 * @return A string representing the HTML response to display to the player.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.equals("give_circlet"))
		{
			if (hasQuestItems(player, ACADEMY_CIRCLET))
			{
				return "33905-3.html";
			}
			
			giveItems(player, ACADEMY_CIRCLET, 1);
			return "33905-2.html";
		}
		
		return super.onEvent(event, npc, player);
	}
	
	/**
	 * This method determines the dialogue to show when a {@code Player} talks to an {@code Npc} for the first time.<br>
	 * It checks if the player is an academy member to choose the correct HTML file.
	 * @param npc The {@code Npc} entity being interacted with.
	 * @param player The {@code Player} entity initiating the conversation.
	 * @return A {@code String} representing the path to the dialogue HTML file.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return player.isAcademyMember() ? "33905-1.html" : "33905.html";
	}
	
	/**
	 * This is the entry point for the {@code Acateo} class.<br>
	 * It initializes a new instance of the {@code Acateo} script.
	 * @param args Command line arguments passed to the program.
	 */
	public static void main(String[] args)
	{
		new Acateo();
	}
}
