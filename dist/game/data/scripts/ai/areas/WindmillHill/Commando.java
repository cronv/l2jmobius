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
package ai.areas.WindmillHill;

import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.enums.ChatType;

/**
 * Defines the artificial intelligence behavior for the {@code Commando} NPC.<br>
 * This script handles how the entity interacts with players and other mobs in the Windmill Hill area.
 * @author St3eT
 */
public class Commando extends Script
{
	// NPCs
	private static final int COMMANDO = 33422;
	
	/**
	 * Private constructor for the {@code Commando} class.<br>
	 * It registers the NPC spawn ID during initialization.
	 */
	private Commando()
	{
		addSpawnId(COMMANDO);
	}
	
	/**
	 * Handles specific events for the {@code Commando} script.<br>
	 * It checks if the {@code event} is {@code NPC_SHOUT}.<br>
	 * If it matches, the {@code Npc} broadcasts a message and starts a timer.
	 * @param event The name of the event to trigger.
	 * @param npc The {@code Npc} instance involved in the event.
	 * @param player The {@code Player} instance involved in the event.
	 * @return The result of the super class method call.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.equals("NPC_SHOUT"))
		{
			npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.WE_RE_LOOKING_FOR_RANGERS_FROM_ADEN);
			startQuestTimer("NPC_SHOUT", (10 + getRandom(5)) * 1000, npc, null);
		}
		
		return super.onEvent(event, npc, player);
	}
	
	/**
	 * This method is called when an {@code Npc} is first created.<br>
	 * It initializes the quest timer for the NPC.
	 * @param npc The {@code Npc} instance that just spawned.
	 */
	@Override
	public void onSpawn(Npc npc)
	{
		startQuestTimer("NPC_SHOUT", (10 + getRandom(5)) * 1000, npc, null);
	}
	
	/**
	 * This is the entry point for the {@code Commando} class.<br>
	 * It initializes a new instance of the {@code Commando} script.
	 * @param args Command line arguments passed to the program.
	 */
	public static void main(String[] args)
	{
		new Commando();
	}
}
