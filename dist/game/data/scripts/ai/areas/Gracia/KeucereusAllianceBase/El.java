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
package ai.areas.Gracia.KeucereusAllianceBase;

import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.enums.ChatType;

/**
 * Defines the artificial intelligence behavior for the {@code El} NPC.<br>
 * This class handles how the entity interacts with players and other actors within its area.
 * @author St3eT
 */
public class El extends Script
{
	// NPCs
	private static final int EL = 32556;
	
	/**
	 * This is a private constructor for the {@code El} class.<br>
	 * It registers the spawn ID {@code 32556} during initialization.
	 */
	private El()
	{
		addSpawnId(EL);
	}
	
	/**
	 * Handles specific events triggered by the {@code El} script.<br>
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
			npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.YOU_CAN_USE_THE_JUMP_BOARD_TO_GET_STRAIGHT_TO_THE_2ND_FLOOR, 1000);
		}
		
		return super.onEvent(event, npc, player);
	}
	
	/**
	 * This method is called when an {@code Npc} spawns.<br>
	 * It initializes the quest timer for the NPC.
	 * @param npc The {@code Npc} instance that was just spawned.
	 */
	@Override
	public void onSpawn(Npc npc)
	{
		startQuestTimer("SPAM_TEXT", 10000, npc, null, true);
	}
	
	/**
	 * This is the entry point for the {@code El} class.<br>
	 * It initializes a new instance of the {@code El} script.
	 * @param args Command line arguments passed to the program.
	 */
	public static void main(String[] args)
	{
		new El();
	}
}
