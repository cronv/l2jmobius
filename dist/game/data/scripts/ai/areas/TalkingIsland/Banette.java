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
 * This class defines the artificial intelligence behavior for the {@code Banette} NPC.<br>
 * It handles the specific actions and logic for this entity within the Talking Island area.
 * @author St3eT
 */
public class Banette extends Script
{
	// NPCs
	private static final int BANETTE = 33114;
	
	/**
	 * Private constructor for the {@code Banette} class.<br>
	 * This constructor initializes the spawn ID for the NPC.<br>
	 * It prevents other classes from creating new instances of this script.
	 */
	private Banette()
	{
		addSpawnId(BANETTE);
	}
	
	/**
	 * Handles specific events triggered by the {@code Banette} script.<br>
	 * It processes actions like NPC shouts and manages quest timers.
	 * @param event The name of the event being triggered.
	 * @param npc The {@code Npc} instance involved in the event.
	 * @param player The {@code Player} instance involved in the event.
	 * @return The result of the super class method call.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.equals("NPC_SHOUT"))
		{
			switch (getRandom(4))
			{
				case 0:
				{
					npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.TRAINING_GROUND_IS_LOCATED_STRAIGHT_AHEAD);
					break;
				}
				case 1:
				{
					npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.WHILE_TRAINING_IN_THE_TRAINING_GROUNDS_IT_BECOMES_PROGRESSIVELY_DIFFICULT);
					break;
				}
				case 2:
				{
					npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.TO_ENTER_THE_TRAINING_HALL_FIND_PANTHEON_S_MUSEUM_FIRST);
					break;
				}
			}
			
			startQuestTimer("NPC_SHOUT", (10 + getRandom(5)) * 1000, npc, null);
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
		startQuestTimer("NPC_SHOUT", (10 + getRandom(5)) * 1000, npc, null);
	}
	
	/**
	 * This is the entry point for the {@code Banette} class.<br>
	 * It initializes a new instance of the {@code Banette} script.
	 * @param args Command line arguments passed to the program.
	 */
	public static void main(String[] args)
	{
		new Banette();
	}
}
