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
package instances.KartiasLabyrinth;

import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.instance.Monster;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the artificial intelligence for the {@code KartiaSupportTroop} NPC.<br>
 * It defines the behaviors and logic for these units within the {@code KartiasLabyrinth} instance.
 * @author St3eT
 */
public class KartiaSupportTroop extends Script
{
	// NPCs
	private static final int[] SUPPORT_TROOPS =
	{
		33642, // Support Troop (Kartia 85)
		33644, // Support Troop (Kartia 90)
		33646, // Support Troop (Kartia 95)
	};
	
	/**
	 * Private constructor for the {@code KartiaSupportTroop} class.<br>
	 * It initializes the script by registering the required spawn IDs.
	 */
	private KartiaSupportTroop()
	{
		addSpawnId(SUPPORT_TROOPS);
	}
	
	/**
	 * Handles periodic events for the {@code KartiaSupportTroop}.<br>
	 * It manages NPC dialogue and target selection logic.
	 * @param event The name of the timer event to process.
	 * @param params A set of statistics used as parameters for the event.
	 * @param npc The {@code Npc} instance that triggered the event.
	 * @param player The {@code Player} associated with this action, if any.
	 */
	@Override
	public void onTimerEvent(String event, StatSet params, Npc npc, Player player)
	{
		if (event.equals("NPC_SAY") && !npc.isDead())
		{
			npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.DEFEAT_ALL_THE_MONSTERS);
			getTimers().addTimer("NPC_SAY", 20000, npc, null);
		}
		else if (event.equals("CHECK_TARGET") && (!npc.isInCombat() || !npc.isAttackingNow() || (npc.getTarget() == null)))
		{
			final Monster monster = World.getRandomVisibleObject(npc, Monster.class);
			if (monster != null)
			{
				if (monster.isTargetable() && GeoEngine.getInstance().canSeeTarget(npc, monster))
				{
					addAttackDesire(npc, monster);
				}
			}
		}
	}
	
	/**
	 * This method is called when an {@code Npc} is spawned.<br>
	 * It checks if the {@code Npc} belongs to a world.<br>
	 * If it does, it broadcasts a message and starts several timers.
	 * @param npc The {@code Npc} instance that was just created.
	 */
	@Override
	public void onSpawn(Npc npc)
	{
		if (npc.getInstanceWorld() != null)
		{
			npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.VANGUARD_OF_ADEN_WE_HAVE_RETURNED);
			getTimers().addTimer("NPC_SAY", 20000, npc, null);
			getTimers().addRepeatingTimer("CHECK_TARGET", 1000, npc, null);
		}
	}
	
	/**
	 * This is the entry point for the {@code KartiaSupportTroop} class.<br>
	 * It initializes a new instance of the script to start the AI logic.
	 * @param args Command line arguments passed to the program.
	 */
	public static void main(String[] args)
	{
		new KartiaSupportTroop();
	}
}
