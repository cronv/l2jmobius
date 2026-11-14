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
 * Defines the artificial intelligence behavior for the {@code Heymond} NPC.<br>
 * This class handles the logic for how this character interacts with players on Talking Island.
 * @author St3eT
 */
public class Heymond extends Script
{
	// NPCs
	private static final int BANETTE = 33114;
	
	/**
	 * Private constructor for the {@code Heymond} class.<br>
	 * This prevents other classes from creating new instances of this script.
	 */
	private Heymond()
	{
		addSpawnId(BANETTE);
	}
	
	/**
	 * Handles specific events triggered by the {@code Heymond} script.<br>
	 * It processes logic for NPC interactions and shouts.
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
			switch (getRandom(4))
			{
				case 0:
				{
					npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.VIEW_OUR_WIDE_VARIETY_OF_ACCESSORIES);
					break;
				}
				case 1:
				{
					npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.THE_BEST_WEAPON_DOESN_T_MAKE_YOU_THE_BEST);
					break;
				}
				case 2:
				{
					npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.WE_BUY_AND_SELL_COME_TAKE_A_LOOK);
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
	 * This is the entry point for the {@code Heymond} application.<br>
	 * It initializes a new instance of the {@code Heymond} class.
	 * @param args Command line arguments passed to the program.
	 */
	public static void main(String[] args)
	{
		new Heymond();
	}
}
