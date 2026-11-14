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
 * This class defines the artificial intelligence behavior for the {@code Stiller} NPC.<br>
 * It handles the specific actions and logic for this entity within the Talking Island area.
 * @author Gladicek
 */
public class Stiller extends Script
{
	// NPCs
	private static final int STILLER = 33125;
	
	// Misc
	private static final NpcStringId[] STILLER_SHOUT =
	{
		NpcStringId.HEY_DID_YOU_SPEAK_WITH_PANTHEON,
		NpcStringId.EVERYONE_NEEDS_TO_MEET_PANTHEON_FIRST_BEFORE_HUNTING
	};
	
	/**
	 * Private constructor for the {@code Stiller} class.<br>
	 * This prevents other classes from creating new instances of this script.
	 */
	private Stiller()
	{
		addSpawnId(STILLER);
	}
	
	/**
	 * Handles specific events triggered by the {@code Stiller} script.<br>
	 * It checks for a {@code SPAM_TEXT} event to trigger random NPC dialogue.
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
			npc.broadcastSay(ChatType.NPC_GENERAL, STILLER_SHOUT[getRandom(2)], 1000);
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
		startQuestTimer("SPAM_TEXT", 10000, npc, null, true);
	}
	
	/**
	 * This is the entry point for the {@code Stiller} class.<br>
	 * It initializes a new instance of the {@code Stiller} script.
	 * @param args Command line arguments passed to the program.
	 */
	public static void main(String[] args)
	{
		new Stiller();
	}
}
