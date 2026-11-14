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

import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.script.Script;

/**
 * This class defines the artificial intelligence behavior for the {@code DrillSergeant} NPC.<br>
 * It handles the specific logic and interactions for this character on Talking Island.
 * @author St3eT
 */
public class DrillSergeant extends Script
{
	// NPCs
	private static final int SERGANT = 33007; // Drill Sergant
	private static final int GUARD = 33018;
	
	// Misc
	// @formatter:off
	private final int[] SOCIAL_ACTIONS = {9, 10, 11, 1 };
	// @formatter:on
	
	/**
	 * Private constructor for the {@code DrillSergeant} class.<br>
	 * This prevents other classes from creating new instances of this script.<br>
	 * It initializes the spawn ID for the sergeant NPC.
	 */
	private DrillSergeant()
	{
		addSpawnId(SERGANT);
	}
	
	/**
	 * Handles specific events for the drill sergeant script.<br>
	 * It manages social actions and updates nearby guards.
	 * @param event The name of the event to trigger.
	 * @param npc The {@code Npc} instance triggering the event.
	 * @param player The {@code Player} instance involved in the interaction.
	 * @return The result of the super class method call.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.equals("SOCIAL_SHOW"))
		{
			final int socialActionId = getRandomEntry(SOCIAL_ACTIONS);
			npc.broadcastSocialAction(socialActionId);
			
			World.forEachVisibleObjectInRange(npc, Npc.class, 500, chars ->
			{
				if (chars.getId() == GUARD)
				{
					chars.getVariables().set("SOCIAL_ACTION_ID", socialActionId);
					startQuestTimer("SOCIAL_ACTION", getRandom(500, 1500), chars, null);
				}
			});
		}
		else if (event.equals("SOCIAL_ACTION"))
		{
			final int socialActionId = npc.getVariables().getInt("SOCIAL_ACTION_ID", 0);
			if (socialActionId > 0)
			{
				npc.broadcastSocialAction(socialActionId);
			}
		}
		
		return super.onEvent(event, npc, player);
	}
	
	/**
	 * This method is called when an {@code Npc} enters the world.<br>
	 * It checks if the {@code Npc} ID matches the sergeant.<br>
	 * It starts a quest timer for specific actions.<br>
	 * It disables random animations for the {@code Npc}.
	 * @param npc The {@code Npc} instance that just spawned.
	 */
	@Override
	public void onSpawn(Npc npc)
	{
		if (npc.getId() == SERGANT)
		{
			startQuestTimer("SOCIAL_SHOW", 10000, npc, null, true);
		}
		
		npc.setRandomAnimation(false);
	}
	
	/**
	 * This is the entry point for the {@code DrillSergeant} class.<br>
	 * It initializes a new instance of the {@code DrillSergeant} object.
	 * @param args Command line arguments passed to the program.
	 */
	public static void main(String[] args)
	{
		new DrillSergeant();
	}
}
