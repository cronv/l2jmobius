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
 * This class defines the artificial intelligence behavior for the {@code Sodian} NPC.<br>
 * It handles the logic for how this entity interacts with players on Talking Island.
 * @author Gladicek
 */
public class Sodian extends Script
{
	// NPCs
	private static final int SODIAN = 33229;
	
	// Misc
	private static final NpcStringId[] SODIAN_SHOUT =
	{
		NpcStringId.COME_BROWSE_OUR_INVENTORY,
		NpcStringId.INCREDIBLE_WEAPONS_FOR_SALE
	};
	
	/**
	 * Private constructor for the {@code Sodian} class.<br>
	 * This constructor initializes the script and registers its spawn ID.
	 */
	private Sodian()
	{
		addSpawnId(SODIAN);
	}
	
	/**
	 * Handles specific events for the {@code Sodian} NPC.<br>
	 * This method processes logic like broadcasting text messages.
	 * @param event The name of the event to trigger.
	 * @param npc The {@code Npc} instance involved in the event.
	 * @param player The {@code Player} instance involved in the event.
	 * @return The result of the super class method call.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.equals("SPAM_TEXT") && (npc != null))
		{
			npc.broadcastSay(ChatType.NPC_GENERAL, SODIAN_SHOUT[getRandom(2)], 1000);
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
	 * This is the entry point for the {@code Sodian} class.<br>
	 * It initializes a new instance of the script.
	 * @param args Command line arguments passed to the program.
	 */
	public static void main(String[] args)
	{
		new Sodian();
	}
}
