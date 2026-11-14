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
package ai.areas.RuinsOfAgony;

import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.enums.ChatType;

/**
 * This class defines the artificial intelligence behavior for guards in the Ruins of Agony area.<br>
 * It handles how these NPCs interact with players and navigate their environment.
 * @author St3eT
 */
public class RoAGuard extends Script
{
	// NPCs
	private static final int GUARD = 33427;
	
	/**
	 * Private constructor for the {@code RoAGuard} class.<br>
	 * This prevents other classes from creating new instances of this script.
	 */
	private RoAGuard()
	{
		addSpawnId(GUARD);
	}
	
	/**
	 * Handles specific events triggered by an {@code Npc}.<br>
	 * This method processes logic like NPC shouts and quest timers.
	 * @param event The name of the event to trigger.
	 * @param npc The {@code Npc} instance involved in the event.
	 * @param player The {@code Player} instance interacting with the NPC.
	 * @return The result of the superclass {@code onEvent} method.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.equals("NPC_SHOUT"))
		{
			npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.TARTI_HAS_PREPARED_THE_TRAINING_GROUNDS_HERE);
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
	 * This is the entry point for the {@code RoAGuard} class.<br>
	 * It initializes a new instance of the script to start the program.
	 * @param args Command line arguments passed during startup.
	 */
	public static void main(String[] args)
	{
		new RoAGuard();
	}
}
