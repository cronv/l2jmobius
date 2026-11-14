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
package ai.bosses.Lindvior;

import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.taskmanagers.WalkingTaskManager;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class defines the artificial intelligence behavior for the {@code LindviorHerald} boss.<br>
 * It handles the logic for how this NPC interacts with players and other entities during combat.
 * @author Gigi
 * @date 2017-07-23 - [22:54:59]
 */
public class LindviorHerald extends Script
{
	// Npc
	private static final int LIONEL_HUNTER = 33886;
	
	// Misc
	private static final String ROUTE_NAME = "Rune_Lionel";
	
	/**
	 * Initializes a new instance of the {@code LindviorHerald} class.<br>
	 * This constructor registers the spawn ID for the boss.
	 */
	public LindviorHerald()
	{
		addSpawnId(LIONEL_HUNTER);
	}
	
	/**
	 * Handles periodic events triggered by the timer system.<br>
	 * This method checks for specific event types and executes corresponding logic.<br>
	 * It can trigger NPC actions like broadcasting messages.
	 * @param event The name of the timer event to process.
	 * @param params Additional parameters associated with the event.
	 * @param npc The {@code Npc} instance that triggered the event.
	 * @param player The {@code Player} instance involved in the event.
	 */
	@Override
	public void onTimerEvent(String event, StatSet params, Npc npc, Player player)
	{
		if (event.equals("NPC_SHOUT") && (npc != null))
		{
			npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.WE_JUST_LOCATED_LINDVIOR_THOSE_WHO_ARE_WILLING_TO_FIGHT_CAN_DO_SO_AT_ANY_TIME_NOW);
			getTimers().addTimer("NPC_SHOUT", (10 + getRandom(5)) * 1000, npc, null);
		}
	}
	
	/**
	 * This method is called when the {@code Npc} first appears in the world.<br>
	 * It initializes the NPC's timers and starts its movement path.
	 * @param npc The {@code Npc} instance that was just spawned.
	 */
	@Override
	public void onSpawn(Npc npc)
	{
		getTimers().addTimer("NPC_SHOUT", (10 + getRandom(5)) * 1000, npc, null);
		WalkingTaskManager.getInstance().startMoving(npc, ROUTE_NAME);
	}
	
	/**
	 * This is the main entry point for the {@code LindviorHerald} class.<br>
	 * It initializes a new instance of the script.
	 * @param args Command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		new LindviorHerald();
	}
}
