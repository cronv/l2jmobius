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
package ai.areas.TalkingIsland;

import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.instance.Monster;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class defines the artificial intelligence behavior for the Ye Sagira Guards.<br>
 * It handles how these guards interact with players and other entities within their area.
 * @author Mobius
 */
public class YeSagiraGuards extends Script
{
	// NPCs
	private static final int[] GUARDS =
	{
		19152,
		19153
	};
	
	/**
	 * Private constructor for the {@code YeSagiraGuards} class.<br>
	 * This constructor initializes the script and registers the required spawn IDs.
	 */
	private YeSagiraGuards()
	{
		addSpawnId(GUARDS);
	}
	
	/**
	 * Handles periodic timer events for the NPC.<br>
	 * This method checks if the {@code Npc} is alive and not in combat.<br>
	 * It may cause the NPC to attack a nearby monster.<br>
	 * It also manages the recurring aggro timer.
	 * @param event The name of the timer event being triggered.
	 * @param params Additional parameters for the event.
	 * @param npc The {@code Npc} instance that owns the timer.
	 * @param player The {@code Player} associated with this action.
	 */
	@Override
	public void onTimerEvent(String event, StatSet params, Npc npc, Player player)
	{
		if ((npc != null) && !npc.isDead() && npc.isSpawned())
		{
			if (!npc.isInCombat())
			{
				final Monster monster = World.getRandomVisibleObjectInRange(npc, Monster.class, 1000);
				if ((monster != null) && !monster.isDead() && !monster.isInCombat())
				{
					npc.reduceCurrentHp(1, monster, null); // TODO: Find better way for attack
				}
			}
			
			getTimers().addTimer("GUARD_AGGRO_" + npc.getObjectId(), null, 10000, npc, null);
		}
	}
	
	/**
	 * This method is called when an {@code Npc} is first created.<br>
	 * It sets the NPC to be invincible.<br>
	 * It also starts a new timer for the NPC.
	 * @param npc The {@code Npc} instance that was just spawned.
	 */
	@Override
	public void onSpawn(Npc npc)
	{
		npc.setInvul(true);
		getTimers().addTimer("GUARD_AGGRO_" + npc.getObjectId(), null, 5000, npc, null);
	}
	
	/**
	 * This is the main entry point for the {@code YeSagiraGuards} script.<br>
	 * It initializes a new instance of the class to start the AI logic.
	 * @param args Command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		new YeSagiraGuards();
	}
}
