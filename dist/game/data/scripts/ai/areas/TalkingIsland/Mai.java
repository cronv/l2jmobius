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
 * This class defines the artificial intelligence behavior for the {@code Mai} NPC.<br>
 * It handles the logic and interactions for this character on Talking Island.
 * @author Gladicek
 */
public class Mai extends Script
{
	// NPCs
	private static final int MAI = 33238;
	
	/**
	 * Private constructor for the {@code Mai} class.<br>
	 * This constructor initializes the script and registers its spawn ID.
	 */
	private Mai()
	{
		addSpawnId(MAI);
	}
	
	/**
	 * Handles specific events triggered by the {@code Mai} script.<br>
	 * It checks for a {@code SPAM_TEXT} event to trigger an NPC broadcast.
	 * @param event The name of the event being triggered.
	 * @param npc The {@code Npc} instance involved in the event.
	 * @param player The {@code Player} instance involved in the event.
	 * @return The result of the super class {@code onEvent} method.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.equals("SPAM_TEXT") && (npc != null))
		{
			npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.EVERY_RACE_BUILT_A_PIECE_OF_THIS_VILLAGE, 1000);
		}
		
		return super.onEvent(event, npc, player);
	}
	
	/**
	 * This method is called when the {@code Npc} first appears in the world.<br>
	 * It initializes the quest timer for the NPC.
	 * @param npc The {@code Npc} instance that was just spawned.
	 */
	@Override
	public void onSpawn(Npc npc)
	{
		startQuestTimer("SPAM_TEXT", 15000, npc, null, true);
	}
	
	/**
	 * This is the entry point for the {@code Mai} class.<br>
	 * It initializes a new instance of the {@code Mai} script.
	 * @param args Command line arguments passed to the program.
	 */
	public static void main(String[] args)
	{
		new Mai();
	}
}
