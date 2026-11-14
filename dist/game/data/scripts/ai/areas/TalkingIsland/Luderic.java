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
 * Defines the artificial intelligence behavior for the {@code Luderic} NPC.<br>
 * This class handles how the character interacts with players on Talking Island.
 * @author Gladicek
 */
public class Luderic extends Script
{
	// NPCs
	private static final int LUDERIC = 33575;
	
	// Misc
	private static final NpcStringId[] LUDERIC_SHOUT =
	{
		NpcStringId.THERE_IS_A_DAY_WHERE_YOU_CAN_SEE_EVEN_THE_ADEN_CONTINENT_IF_THE_WEATHER_IS_GOOD,
		NpcStringId.IF_I_M_HERE_IT_FEELS_LIKE_TIME_HAS_STOPPED
	};
	
	/**
	 * Private constructor for the {@code Luderic} class.<br>
	 * This constructor initializes the NPC spawn ID.<br>
	 * It prevents other classes from creating new instances of this script.
	 */
	private Luderic()
	{
		addSpawnId(LUDERIC);
	}
	
	/**
	 * Handles specific events for the {@code Luderic} NPC.<br>
	 * It processes text spam and social actions.
	 * @param event The name of the event to trigger.
	 * @param npc The {@code Npc} instance involved in the event.
	 * @param player The {@code Player} instance interacting with the NPC.
	 * @return The result of the super class method call.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.equals("SPAM_TEXT") && (npc != null))
		{
			npc.broadcastSay(ChatType.NPC_GENERAL, LUDERIC_SHOUT[getRandom(2)], 1000);
		}
		else if (event.equals("SOCIAL_ACTION") && (npc != null))
		{
			npc.broadcastSocialAction(1);
		}
		
		return super.onEvent(event, npc, player);
	}
	
	/**
	 * This method is called when the {@code Npc} first appears in the world.<br>
	 * It initializes several timers for the NPC to perform actions.
	 * @param npc The {@code Npc} instance that was just spawned.
	 */
	@Override
	public void onSpawn(Npc npc)
	{
		startQuestTimer("SPAM_TEXT", 7000, npc, null, true);
		startQuestTimer("SOCIAL_ACTION", 3000, npc, null, true);
	}
	
	/**
	 * This is the entry point for the {@code Luderic} class.<br>
	 * It initializes a new instance of the {@code Luderic} script.
	 * @param args Command line arguments passed to the program.
	 */
	public static void main(String[] args)
	{
		new Luderic();
	}
}
