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

import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.util.LocationUtil;

/**
 * This class defines the artificial intelligence behavior for the {@code Karonf} NPC.<br>
 * It handles the logic and interactions for this entity within the Talking Island area.
 * @author St3eT
 */
public class Karonf extends Script
{
	// NPC
	private static final int KARONF = 33242;
	
	/**
	 * Private constructor for the {@code Karonf} class.<br>
	 * This prevents other classes from creating new instances of this script.
	 */
	private Karonf()
	{
		addSpawnId(KARONF);
	}
	
	/**
	 * Handles specific events for the {@code Karonf} script.<br>
	 * It processes movement and shouting logic based on the provided event name.
	 * @param event The name of the event to trigger.
	 * @param npc The {@code Npc} instance involved in the event.
	 * @param player The {@code Player} instance interacting with the NPC.
	 * @return The result of the super class {@code onEvent} method.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.equals("NPC_MOVE"))
		{
			if (getRandomBoolean())
			{
				final Location randomLoc = LocationUtil.getRandomLocation(npc.getSpawn().getLocation(), 0, 500);
				addMoveToDesire(npc, GeoEngine.getInstance().getValidLocation(npc.getLocation().getX(), npc.getLocation().getY(), npc.getLocation().getZ(), randomLoc.getX(), randomLoc.getY(), randomLoc.getZ(), npc.getInstanceWorld()), 23);
			}
			
			startQuestTimer("NPC_MOVE", (10 + getRandom(5)) * 1000, npc, null);
		}
		else if (event.equals("NPC_SHOUT"))
		{
			final int rand = getRandom(3);
			if (rand == 0)
			{
				npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.WHEN_YOU_GO_TO_THE_MUSEUM_SPEAK_TO_PANTHEON);
			}
			else if (rand == 1)
			{
				npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.SOME_FOLKS_DON_T_KNOW_WHAT_THEY_ARE_DOING);
			}
			
			startQuestTimer("NPC_SHOUT", (10 + getRandom(5)) * 1000, npc, null);
		}
		
		return super.onEvent(event, npc, player);
	}
	
	/**
	 * This method is called when an {@code Npc} is first created.<br>
	 * It initializes the timers for specific NPC actions.
	 * @param npc The {@code Npc} instance that was just spawned.
	 */
	@Override
	public void onSpawn(Npc npc)
	{
		startQuestTimer("NPC_MOVE", (10 + getRandom(5)) * 1000, npc, null);
		startQuestTimer("NPC_SHOUT", (10 + getRandom(5)) * 1000, npc, null);
	}
	
	/**
	 * This is the main entry point for the {@code Karonf} class.<br>
	 * It initializes a new instance of the {@code Karonf} script.
	 * @param args Command line arguments passed to the program.
	 */
	public static void main(String[] args)
	{
		new Karonf();
	}
}
