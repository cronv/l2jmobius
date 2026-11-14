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
package ai.areas.TalkingIsland;

import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.enums.ChatType;

/**
 * This class defines the artificial intelligence behavior for the {@code Galad} NPC.<br>
 * It handles specific interactions and logic for this character on Talking Island.
 * @author Gladicek
 */
public class Galad extends Script
{
	// NPCs
	private static final int GALAD = 33572;
	
	/**
	 * Private constructor for the {@code Galad} class.<br>
	 * This prevents other classes from creating new instances of this script.
	 */
	private Galad()
	{
		addSpawnId(GALAD);
	}
	
	/**
	 * Handles specific events triggered by the {@code Galad} script.<br>
	 * It checks for a {@code SPAM_TEXT} event to trigger NPC actions.
	 * @param event The name of the event being triggered.
	 * @param npc The {@code Npc} instance involved in the event.
	 * @param player The {@code Player} instance involved in the event.
	 * @return The result of the superclass {@code onEvent} method.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.equals("SPAM_TEXT") && (npc != null))
		{
			npc.broadcastSocialAction(3);
			npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.OCCASIONALLY_RARE_JEWELS_OF_GIANTS_ARE_DISCOVERED_IN_THE_BEACH, 1000);
		}
		
		return super.onEvent(event, npc, player);
	}
	
	/**
	 * This method is called when an {@code Npc} is spawned.<br>
	 * It initializes the quest timer for the NPC.
	 * @param npc The {@code Npc} instance that was just created.
	 */
	@Override
	public void onSpawn(Npc npc)
	{
		startQuestTimer("SPAM_TEXT", 8000, npc, null, true);
	}
	
	/**
	 * This is the entry point for the {@code Galad} class.<br>
	 * It initializes a new instance of the {@code Galad} script.
	 * @param args Command line arguments passed to the program.
	 */
	public static void main(String[] args)
	{
		new Galad();
	}
}
