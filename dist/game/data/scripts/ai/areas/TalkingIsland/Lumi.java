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
 * Defines the artificial intelligence behavior for the {@code Lumi} NPC.<br>
 * This class handles the logic for interactions and actions within the Talking Island area.
 * @author Gladicek
 */
public class Lumi extends Script
{
	// NPCs
	private static final int LUMI = 33025;
	
	// Misc
	private static final NpcStringId[] LUMI_SHOUT =
	{
		NpcStringId.TO_YOUR_RIGHT_THE_ADMINISTRATIVE_DISTRICT_AND_TO_THE_LEFT_IS_THE_MUSEUM,
		NpcStringId.WHEN_YOU_USE_THE_TELEPORTER_YOU_CAN_GO_TO_THE_RUINS_OF_YE_SAGIRA,
		NpcStringId.HAVE_YOU_BEEN_TO_RUINS_OF_YE_SAGIRA_YOU_HAVE_TO_GO_AT_LEAST_ONCE,
	};
	
	/**
	 * Private constructor for the {@code Lumi} class.<br>
	 * It initializes the script by registering the spawn ID.<br>
	 * This prevents other classes from creating new instances.
	 */
	private Lumi()
	{
		addSpawnId(LUMI);
	}
	
	/**
	 * Handles specific events triggered by the {@code Lumi} script.<br>
	 * It checks for a {@code SPAM_TEXT} event to trigger random NPC dialogue.
	 * @param event The name of the event being triggered.
	 * @param npc The {@code Npc} instance involved in the event.
	 * @param player The {@code Player} instance involved in the event.
	 * @return The result of the parent {@code onEvent} method.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.equals("SPAM_TEXT") && (npc != null))
		{
			npc.broadcastSay(ChatType.NPC_GENERAL, LUMI_SHOUT[getRandom(3)], 1000);
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
		startQuestTimer("SPAM_TEXT", 8000, npc, null, true);
	}
	
	/**
	 * This is the entry point for the {@code Lumi} class.<br>
	 * It initializes a new instance of the {@code Lumi} script.
	 * @param args Command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		new Lumi();
	}
}
