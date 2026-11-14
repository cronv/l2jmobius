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
 * This class handles the artificial intelligence for the Boy and Girl NPCs.<br>
 * It defines their behaviors within the Talking Island area.
 * @author St3eT
 */
public class BoyAndGirl extends Script
{
	// NPCs
	private static final int BOY = 33224;
	private static final int GIRL = 33217;
	
	// Items
	private static final int WEAPON = 15304;
	
	/**
	 * Private constructor for the {@code BoyAndGirl} class.<br>
	 * This constructor initializes the required spawn and move finished IDs.<br>
	 * It prevents other classes from creating new instances of this script.
	 */
	private BoyAndGirl()
	{
		addSpawnId(BOY, GIRL);
		addMoveFinishedId(BOY, GIRL);
	}
	
	/**
	 * Handles specific events for the {@code BoyAndGirl} script.<br>
	 * It manages weapon swapping and shouting behaviors based on the event name.
	 * @param event The name of the event to trigger.
	 * @param npc The {@code Npc} instance involved in the event.
	 * @param player The {@code Player} instance interacting with the NPC.
	 * @return The result of the super class {@code onEvent} method.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.equals("NPC_CHANGEWEAP"))
		{
			if (npc.isScriptValue(1))
			{
				npc.setRHandId(0);
				npc.setScriptValue(0);
			}
			else
			{
				npc.setRHandId(WEAPON);
				npc.setScriptValue(1);
			}
			
			startQuestTimer("NPC_CHANGEWEAP", 15000 + (getRandom(5) * 1000), npc, null);
		}
		else if (event.equals("NPC_SHOUT"))
		{
			npc.broadcastSay(ChatType.NPC_GENERAL, npc.getId() == BOY ? NpcStringId.WOW_2 : NpcStringId.BOYS_ARE_SO_ANNOYING);
			startQuestTimer("NPC_SHOUT", 10000 + (getRandom(5) * 1000), npc, null);
		}
		
		return super.onEvent(event, npc, player);
	}
	
	/**
	 * This method is called when an {@code Npc} first appears in the world.<br>
	 * It initializes quest timers and sets the initial movement for the NPC.<br>
	 * The NPC will start moving to a random nearby location.
	 * @param npc The {@code Npc} instance that was just spawned.
	 */
	@Override
	public void onSpawn(Npc npc)
	{
		cancelQuestTimer("NPC_CHANGEWEAP", npc, null);
		startQuestTimer("NPC_CHANGEWEAP", 15000 + (getRandom(5) * 1000), npc, null);
		cancelQuestTimer("NPC_SHOUT", npc, null);
		startQuestTimer("NPC_SHOUT", 10000 + (getRandom(5) * 1000), npc, null);
		npc.setRunning();
		final Location randomLoc = LocationUtil.getRandomLocation(npc.getSpawn().getLocation(), 200, 600);
		addMoveToDesire(npc, GeoEngine.getInstance().getValidLocation(npc.getLocation().getX(), npc.getLocation().getY(), npc.getLocation().getZ(), randomLoc.getX(), randomLoc.getY(), randomLoc.getZ(), npc.getInstanceWorld()), 23);
	}
	
	/**
	 * This method is called when an {@code Npc} finishes its movement.<br>
	 * It calculates a new random destination for the NPC.<br>
	 * It then adds a new move desire to the NPC's behavior tree.
	 * @param npc The {@code Npc} instance that finished moving.
	 */
	@Override
	public void onMoveFinished(Npc npc)
	{
		final Location randomLoc = LocationUtil.getRandomLocation(npc.getSpawn().getLocation(), 200, 600);
		addMoveToDesire(npc, GeoEngine.getInstance().getValidLocation(npc.getLocation().getX(), npc.getLocation().getY(), npc.getLocation().getZ(), randomLoc.getX(), randomLoc.getY(), randomLoc.getZ(), npc.getInstanceWorld()), 23);
		super.onMoveFinished(npc);
	}
	
	/**
	 * This is the entry point for the {@code BoyAndGirl} class.<br>
	 * It initializes a new instance of the script.
	 * @param args Command line arguments passed to the program.
	 */
	public static void main(String[] args)
	{
		new BoyAndGirl();
	}
}
