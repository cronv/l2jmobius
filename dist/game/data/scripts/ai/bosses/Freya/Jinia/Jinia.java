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
package ai.bosses.Freya.Jinia;

import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.script.Script;

/**
 * This class handles the artificial intelligence for the {@code Jinia} boss.<br>
 * It defines the behaviors and logic for this specific entity within the game world.
 * @author Adry_85
 */
public class Jinia extends Script
{
	// NPC
	private static final int JINIA = 32781;
	
	// Items
	private static final int FROZEN_CORE = 15469;
	private static final int BLACK_FROZEN_CORE = 15470;
	
	// Misc
	private static final int MIN_LEVEL = 82;
	
	/**
	 * Private constructor for the {@code Jinia} class.<br>
	 * This constructor initializes the NPC IDs and talk scripts.<br>
	 * It prevents other classes from creating new instances of this script.
	 */
	private Jinia()
	{
		addStartNpc(JINIA);
		addFirstTalkId(JINIA);
		addTalkId(JINIA);
	}
	
	/**
	 * Handles specific events for the {@code Jinia} NPC.<br>
	 * This method processes logic based on the provided {@code event} string.<br>
	 * It determines which HTML file to display or gives items to the player.
	 * @param event The name of the event to trigger.
	 * @param npc The {@code Npc} instance involved in the interaction.
	 * @param player The {@code Player} interacting with the NPC.
	 * @return The name of the HTML file to be displayed.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;
		switch (event)
		{
			case "check":
			{
				if (hasAtLeastOneQuestItem(player, FROZEN_CORE, BLACK_FROZEN_CORE))
				{
					htmltext = "32781-03.html";
				}
				else
				{
					// final QuestState qs = player.getQuestState(Q10286_ReunionWithSirra.class.getSimpleName());
					// if ((qs != null) && qs.isCompleted())
					// {
					// giveItems(player, FROZEN_CORE, 1);
					// }
					// else
					// {
					giveItems(player, BLACK_FROZEN_CORE, 1);
					
					// }
					
					htmltext = "32781-04.html";
				}
				break;
			}
		}
		
		return htmltext;
	}
	
	/**
	 * Handles the dialogue for a player's first interaction with this NPC.<br>
	 * It checks the quest progress and the level of the {@code Player}.<br>
	 * The method returns different HTML files based on specific quest conditions.
	 * @param npc The {@code Npc} instance interacting with the player.
	 * @param player The {@code Player} instance initiating the conversation.
	 * @return A {@code String} representing the path to the dialogue HTML file.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		// final QuestState qs = player.getQuestState(Q10286_ReunionWithSirra.class.getSimpleName());
		// if ((qs != null) && (player.getLevel() >= MIN_LEVEL))
		// {
		// if (qs.isCond(5) || qs.isCond(6))
		// {
		// return "32781-09.html";
		// }
		// else if (qs.isCond(7))
		// {
		// return "32781-01.html";
		// }
		// }
		// return "32781-02.html";
		if (player.getLevel() >= MIN_LEVEL)
		{
			return "32781-01.html";
		}
		
		return "32781-02.html";
	}
	
	/**
	 * This is the entry point for the {@code Jinia} class.<br>
	 * It initializes a new instance of the {@code Jinia} script.
	 * @param args Command line arguments passed to the program.
	 */
	public static void main(String[] args)
	{
		new Jinia();
	}
}
