/*
 * Copyright (c) 2013 L2jMobius
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package ai.areas.GardenOfSpirits;

import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.groups.Party;
import org.l2jmobius.gameserver.mechanics.script.Script;

/**
 * This class defines the artificial intelligence behavior for the {@code FuryKiku} NPC.<br>
 * It handles the specific actions and logic for this entity within the Garden of Spirits area.
 * @author Gigi
 * @date 2018-07-23 - [15:47:15]
 */
public class FuryKiku extends Script
{
	// Monsters
	private static final int FURYKIKU = 23545;
	private static final int[] MONSTERS =
	{
		23544, // Fury Sylph Barrena
		23553, // Fury Sylph Barrena (night)
	};
	
	/**
	 * Initializes the {@code FuryKiku} script.<br>
	 * This constructor registers the required monster IDs.<br>
	 * It sets up the spawn and kill logic for this entity.
	 */
	public FuryKiku()
	{
		addKillId(MONSTERS);
		addSpawnId(FURYKIKU);
	}
	
	/**
	 * Handles specific events for the {@code FuryKiku} script.<br>
	 * It manages spawning logic and aggression behavior based on the event type.
	 * @param event The name of the event to trigger.
	 * @param npc The {@code Npc} instance involved in the event.
	 * @param player The {@code Player} instance involved in the event.
	 * @return The result of the super class method call.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "SPAWN":
			{
				final Party party = player.getParty();
				if (party != null)
				{
					party.getMembers().forEach(p -> addSpawn(FURYKIKU, p, true, 180000, true, 0));
				}
				else
				{
					addSpawn(FURYKIKU, player, true, 180000, true, 0);
				}
				break;
			}
			case "ATTACK":
			{
				npc.setRunning();
				World.forEachVisibleObjectInRange(npc, Player.class, 300, p ->
				{
					if ((p != null) && p.isPlayable() && !p.isDead())
					{
						npc.getAI().notifyActionAggression(p, 1000);
					}
				});
				break;
			}
		}
		
		return super.onEvent(event, npc, player);
	}
	
	/**
	 * This method is called when an {@code Npc} is killed.<br>
	 * It handles logic triggered by the death of a monster.<br>
	 * There is a chance to start a quest timer for the killer.
	 * @param npc The {@code Npc} that was killed.
	 * @param killer The {@code Player} who killed the {@code Npc}.
	 * @param isSummon Indicates if the {@code Npc} was a summon.
	 */
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		if (getRandom(10) < 5)
		{
			startQuestTimer("SPAWN", 2000, npc, killer);
		}
	}
	
	/**
	 * This method is called when an {@code Npc} is spawned.<br>
	 * It initializes the quest timer for the entity.
	 * @param npc The {@code Npc} instance that was just created.
	 */
	@Override
	public void onSpawn(Npc npc)
	{
		startQuestTimer("ATTACK", 1000, npc, null);
	}
	
	/**
	 * This is the entry point for the {@code FuryKiku} class.<br>
	 * It initializes a new instance of the script.
	 * @param args Command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		new FuryKiku();
	}
}
