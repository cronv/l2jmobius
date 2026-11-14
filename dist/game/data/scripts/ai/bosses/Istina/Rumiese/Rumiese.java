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
package ai.bosses.Istina.Rumiese;

import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.script.Script;

/**
 * This class defines the artificial intelligence behavior for the boss {@code Rumiese}.<br>
 * It handles the logic and actions performed by this entity during combat.
 * @author St3eT
 */
public class Rumiese extends Script
{
	// NPC
	private static final int RUMIESE = 33151;
	
	// Item
	private static final int CONTROL_DEVICE = 17608; // Energy Control Device
	
	/**
	 * Initializes the {@code Rumiese} script.<br>
	 * This constructor registers the required NPC and talk IDs.
	 */
	public Rumiese()
	{
		addStartNpc(RUMIESE);
		addTalkId(RUMIESE);
		addFirstTalkId(RUMIESE);
	}
	
	/**
	 * Handles specific events for the {@code Rumiese} script.<br>
	 * It processes interactions and item rewards based on the provided event name.
	 * @param event The unique identifier for the current script event.
	 * @param npc The {@code Npc} instance triggering the event.
	 * @param player The {@code Player} instance interacting with the NPC.
	 * @return A string representing the path to the HTML file to display.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = null;
		switch (event)
		{
			case "33151-01.html":
			{
				htmltext = event;
				break;
			}
			case "giveDevice":
			{
				if (!hasQuestItems(player, CONTROL_DEVICE))
				{
					giveItems(player, CONTROL_DEVICE, 1);
					htmltext = "33151-02.html";
				}
				else
				{
					htmltext = "33151-03.html";
				}
				break;
			}
		}
		
		return htmltext;
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
	 * This is the entry point for the {@code Rumiese} class.<br>
	 * It initializes a new instance of the {@code Rumiese} script.
	 * @param args Command line arguments passed to the program.
	 */
	public static void main(String[] args)
	{
		new Rumiese();
	}
}
