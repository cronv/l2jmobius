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
package custom.FakePlayers;

import org.l2jmobius.gameserver.config.PvpConfig;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.script.Script;

/**
 * This task handles the logic for stopping {@code PvpFlagging} on a target.<br>
 * It ensures that players or NPCs no longer trigger PvP flags during specific interactions.
 * @author Mobius
 */
public class PvpFlaggingStopTask extends Script
{
	/**
	 * Private constructor for the {@code PvpFlaggingStopTask} class.<br>
	 * This prevents other classes from creating new instances of this task.
	 */
	private PvpFlaggingStopTask()
	{
	}
	
	/**
	 * Handles specific events for the PvpFlaggingStopTask script.<br>
	 * It manages quest timers and script values based on combat status.<br>
	 * This method updates the NPC state when it interacts with players.
	 * @param event The name of the event to process.
	 * @param npc The {@code Npc} instance triggering the event.
	 * @param player The {@code Player} instance involved in the interaction.
	 * @return The result of the super class {@code onEvent} method.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (npc == null)
		{
			return null;
		}
		
		if (npc.isDead())
		{
			cancelQuestTimer("FLAG_CHECK", npc, null);
			cancelQuestTimer("FINISH_FLAG", npc, null);
			cancelQuestTimer("REMOVE_FLAG", npc, null);
			return null;
		}
		
		if (event.equals("FLAG_CHECK"))
		{
			final WorldObject target = npc.getTarget();
			if ((target != null) && (target.isPlayable() || target.isFakePlayer()))
			{
				npc.setScriptValue(1); // in combat
				cancelQuestTimer("FINISH_FLAG", npc, null);
				cancelQuestTimer("REMOVE_FLAG", npc, null);
				startQuestTimer("FINISH_FLAG", PvpConfig.PVP_NORMAL_TIME - 20000, npc, null);
				startQuestTimer("FLAG_CHECK", 5000, npc, null);
			}
		}
		else if (event.equals("FINISH_FLAG"))
		{
			if (npc.isScriptValue(1))
			{
				npc.setScriptValue(2); // blink status
				npc.broadcastInfo(); // update flag status
				startQuestTimer("REMOVE_FLAG", 20000, npc, null);
			}
		}
		else if (event.equals("REMOVE_FLAG"))
		{
			if (npc.isScriptValue(2))
			{
				npc.setScriptValue(0); // not in combat
				npc.broadcastInfo(); // update flag status
			}
		}
		
		return super.onEvent(event, npc, player);
	}
	
	/**
	 * This is the entry point for the {@code PvpFlaggingStopTask} script.<br>
	 * It initializes a new instance of the task to start execution.
	 * @param args Command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		new PvpFlaggingStopTask();
	}
}
