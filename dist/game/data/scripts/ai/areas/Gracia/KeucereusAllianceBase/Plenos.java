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
 * Defines the artificial intelligence behavior for the {@code Plenos} NPC.<br>
 * This class handles how the entity interacts with players and other actors within its area.
 * @author St3eT
 */
public class Plenos extends Script
{
	// NPCs
	private static final int PLENOS = 32563;
	
	/**
	 * Private constructor for the {@code Plenos} class.<br>
	 * It registers the spawn ID for this NPC.<br>
	 * This prevents other classes from creating new instances of {@code Plenos}.
	 */
	private Plenos()
	{
		addSpawnId(PLENOS);
	}
	
	/**
	 * Handles specific events for the {@code Plenos} NPC.<br>
	 * It checks if the {@code event} is equal to {@code "SPAM_TEXT"}.<br>
	 * If valid, it makes the {@code Npc} broadcast a message.
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
			npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.YOU_CAN_BE_TELEPORTED_TO_EACH_SEED_IF_YOU_VOLUNTEER_WHY_NOT_TRY, 1000);
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
		startQuestTimer("SPAM_TEXT", 10000, npc, null, true);
	}
	
	/**
	 * This is the entry point for the {@code Plenos} class.<br>
	 * It initializes a new instance of the script.
	 * @param args Command line arguments passed to the program.
	 */
	public static void main(String[] args)
	{
		new Plenos();
	}
}
