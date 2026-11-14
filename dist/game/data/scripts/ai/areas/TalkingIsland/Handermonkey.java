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

/**
 * Defines the artificial intelligence behavior for the {@code Handermonkey} NPC.<br>
 * This class handles how the entity interacts with players and its environment on Talking Island.
 * @author Gladicek
 */
public class Handermonkey extends Script
{
	// NPC
	private static final int HANDERMONKEY = 33203;
	
	/**
	 * Private constructor for the {@code Handermonkey} class.<br>
	 * This constructor registers the spawn ID for the NPC.<br>
	 * It prevents other classes from creating new instances of this script.
	 */
	private Handermonkey()
	{
		addSpawnId(HANDERMONKEY);
	}
	
	/**
	 * Handles specific events for the {@code Handermonkey} script.<br>
	 * It processes movement logic and social actions based on triggered events.
	 * @param event The name of the event being triggered.
	 * @param npc The {@code Npc} instance involved in the event.
	 * @param player The {@code Player} instance involved in the event.
	 * @return The result of the super class {@code onEvent} method.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.equals("NPC_MOVE") && (npc != null))
		{
			if (getRandom(100) < 70)
			{
				final int x = npc.getSpawn().getX() + (getRandom(-100, 100));
				final int y = npc.getSpawn().getY() + (getRandom(-100, 100));
				final int z = npc.getZ();
				final Location loc = GeoEngine.getInstance().getValidLocation(npc.getX(), npc.getY(), z, x, y, z, npc.getInstanceWorld());
				addMoveToDesire(npc, loc, 0);
			}
			else
			{
				npc.broadcastSocialAction(9);
			}
		}
		
		return super.onEvent(event, npc, player);
	}
	
	/**
	 * This method is called when an {@code Npc} is spawned.<br>
	 * It initializes the NPC state and starts a quest timer.
	 * @param npc The {@code Npc} instance that was just created.
	 */
	@Override
	public void onSpawn(Npc npc)
	{
		npc.setRunning();
		cancelQuestTimer("NPC_MOVE", npc, null);
		startQuestTimer("NPC_MOVE", 5000, npc, null, true);
	}
	
	/**
	 * This is the main entry point for the {@code Handermonkey} class.<br>
	 * It initializes a new instance of the script.
	 * @param args Command line arguments passed to the program.
	 */
	public static void main(String[] args)
	{
		new Handermonkey();
	}
}
