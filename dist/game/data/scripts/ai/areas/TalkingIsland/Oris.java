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
 * This class defines the artificial intelligence behavior for the {@code Oris} NPC.<br>
 * It handles the logic and interactions for this character on Talking Island.
 * @author Gladicek
 */
public class Oris extends Script
{
	// NPCs
	private static final int ORIS = 33116;
	
	/**
	 * Private constructor for the {@code Oris} class.<br>
	 * This constructor initializes the required NPC IDs.<br>
	 * It registers the spawn, start, and talk IDs.
	 */
	private Oris()
	{
		addSpawnId(ORIS);
		addStartNpc(ORIS);
		addTalkId(ORIS);
	}
	
	/**
	 * Handles specific events triggered by the {@code Oris} NPC.<br>
	 * It processes social actions and text spam messages.
	 * @param event The name of the event to trigger.
	 * @param npc The {@code Npc} instance involved in the event.
	 * @param player The {@code Player} instance interacting with the NPC.
	 * @return The result of the base {@code onEvent} method.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.equals("SOCIAL_ACTION_1"))
		{
			npc.broadcastSocialAction(6);
			startQuestTimer("SOCIAL_ACTION_2", 2500, npc, null);
		}
		else if (event.equals("SOCIAL_ACTION_2"))
		{
			npc.broadcastSocialAction(7);
		}
		else if (event.equals("SPAM_TEXT") && (npc != null))
		{
			npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.I_HAVEN_T_FELT_THIS_GOOD_IN_AGES, 1000);
		}
		
		return super.onEvent(event, npc, player);
	}
	
	/**
	 * This method is called when an {@code Npc} is spawned.<br>
	 * It initializes the animation and quest timers for the NPC.
	 * @param npc The {@code Npc} instance that was just created.
	 */
	@Override
	public void onSpawn(Npc npc)
	{
		npc.setRandomAnimation(false);
		startQuestTimer("SOCIAL_ACTION_1", 6500, npc, null, true);
		startQuestTimer("SPAM_TEXT", 10000, npc, null, true);
	}
	
	/**
	 * This is the entry point for the {@code Oris} class.<br>
	 * It initializes a new instance of the {@code Oris} script.
	 * @param args Command line arguments passed to the program.
	 */
	public static void main(String[] args)
	{
		new Oris();
	}
}
