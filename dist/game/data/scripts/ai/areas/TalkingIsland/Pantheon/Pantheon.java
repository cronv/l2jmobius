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
package ai.areas.TalkingIsland.Pantheon;

import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.enums.ChatType;

/**
 * This class handles the artificial intelligence for NPCs located in the {@code Pantheon} area.<br>
 * It manages specific behaviors and interactions for entities within this zone.
 * @author Gladicek
 */
public class Pantheon extends Script
{
	// NPC
	private static final int PANTHEON = 32972;
	
	// Location
	private static final Location MUSEUM = new Location(-114711, 243911, -7968);
	
	// Misc
	private static final int MIN_LEVEL = 20;
	
	/**
	 * Private constructor for the {@code Pantheon} class.<br>
	 * This prevents other classes from creating new instances of this script.<br>
	 * It initializes the required IDs for spawning and talking.
	 */
	private Pantheon()
	{
		addSpawnId(PANTHEON);
		addStartNpc(PANTHEON);
		addFirstTalkId(PANTHEON);
		addTalkId(PANTHEON);
	}
	
	/**
	 * Handles specific events triggered by the {@code Pantheon} script.<br>
	 * It processes teleport requests, level checks, and NPC broadcasts.
	 * @param event The unique identifier for the event to trigger.
	 * @param npc The {@code Npc} instance involved in the interaction.
	 * @param player The {@code Player} instance interacting with the NPC.
	 * @return The name of the HTML file to display, or {@code null}.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = null;
		switch (event)
		{
			case "32972-1.html":
			{
				htmltext = event;
				break;
			}
			case "teleport_museum":
			{
				if (player.getLevel() < MIN_LEVEL)
				{
					htmltext = "32972-noteleport.html";
				}
				else
				{
					player.teleToLocation(MUSEUM);
				}
				break;
			}
			case "TEXT_SPAM":
			{
				npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.IS_IT_BETTER_TO_END_DESTINY_OR_START_DESTINY);
				break;
			}
		}
		
		return htmltext;
	}
	
	/**
	 * This method is called when the {@code Npc} is first created in the world.<br>
	 * It initializes a quest timer for the specific NPC instance.
	 * @param npc The {@code Npc} object that was just spawned.
	 */
	@Override
	public void onSpawn(Npc npc)
	{
		startQuestTimer("TEXT_SPAM", 10000, npc, null, true);
	}
	
	/**
	 * Determines the dialogue file path when a player talks to an {@code Npc} for the first time.<br>
	 * It returns a string based on the unique ID of the {@code Npc}.
	 * @param npc The {@code Npc} that is being interacted with.
	 * @param player The {@code Player} who initiated the conversation.
	 * @return A string representing the path to the HTML dialogue file.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return npc.getId() + ".html";
	}
	
	/**
	 * This is the entry point for the {@code Pantheon} class.<br>
	 * It initializes a new instance of the {@code Pantheon} script.
	 * @param args Command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		new Pantheon();
	}
}
