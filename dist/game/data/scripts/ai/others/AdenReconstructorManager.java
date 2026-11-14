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
package ai.others;

import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.enums.ChatType;

/**
 * Manages the artificial intelligence behavior for the {@code AdenReconstructor} entity.<br>
 * This class handles the logic and script execution for this specific NPC type.
 * @author St3eT
 */
public class AdenReconstructorManager extends Script
{
	// NPCs
	private static final int[] NPCS =
	{
		33584, // Moe
		33581, // Eeny
	};
	
	/**
	 * Private constructor for the {@code AdenReconstructorManager} class.<br>
	 * This constructor initializes the manager and registers the required NPC IDs.<br>
	 * It prevents other classes from creating new instances of this manager.
	 */
	private AdenReconstructorManager()
	{
		addSpawnId(NPCS);
	}
	
	/**
	 * Handles specific events for the Aden Reconstructor NPC.<br>
	 * This method triggers dialogue sequences based on the provided {@code event} string.<br>
	 * It manages timed text broadcasts to players.
	 * @param event The name of the event to trigger.
	 * @param npc The {@code Npc} instance performing the action.
	 * @param player The {@code Player} who triggered the interaction.
	 * @return The result of the super class {@code onEvent} method.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (npc != null)
		{
			if (event.equals("SPAM_TEXT"))
			{
				npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.THE_LAND_OF_ADEN_IS_IN_NEED_OF_MATERIALS_TO_REBUILD_FROM_SHILLIEN_S_DESTRUCTION);
			}
		}
		
		return super.onEvent(event, npc, player);
	}
	
	/**
	 * This method is called when an {@code Npc} is first created.<br>
	 * It initializes the quest timer for the NPC.
	 * @param npc The {@code Npc} instance that was just spawned.
	 */
	@Override
	public void onSpawn(Npc npc)
	{
		startQuestTimer("SPAM_TEXT", (5 * 60 * 1000), npc, null, true);
	}
	
	/**
	 * This is the entry point for the {@code AdenReconstructorManager} class.<br>
	 * It initializes a new instance of the manager to start the script.
	 * @param args Command line arguments passed to the program.
	 */
	public static void main(String[] args)
	{
		new AdenReconstructorManager();
	}
}
