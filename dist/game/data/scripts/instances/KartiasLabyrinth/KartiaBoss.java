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
package instances.KartiasLabyrinth;

import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.creature.OnCreatureAttacked;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.creature.OnCreatureDeath;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the artificial intelligence for the {@code KartiaBoss} entity.<br>
 * It defines the behaviors and logic for this boss within the {@code KartiasLabyrinth} instance.
 * @author flanagak
 */
public class KartiaBoss extends Script
{
	// NPCs
	private static final int[] BOSSES =
	{
		19253, // Zellaka (solo 85)
		25882, // Zellaka (group 85)
		19254, // Pelline (solo 90)
		25883, // Pelline (group 90)
		19255, // Kalios (solo 95)
		25884, // Kalios (group 95)
	};
	private static final int FIGHTER_SOLO_85 = 19220;
	private static final int MAGE_SOLO_85 = 19221;
	private static final int FIGHTER_GROUP_85 = 19229;
	private static final int MAGE_GROUP_85 = 19230;
	private static final int FIGHTER_SOLO_90 = 19223;
	private static final int MAGE_SOLO_90 = 19224;
	private static final int FIGHTER_GROUP_90 = 19232;
	private static final int MAGE_GROUP_90 = 19233;
	private static final int FIGHTER_SOLO_95 = 19226;
	private static final int MAGE_SOLO_95 = 19227;
	private static final int FIGHTER_GROUP_95 = 19235;
	private static final int MAGE_GROUP_95 = 19236;
	
	/**
	 * Private constructor for the {@code KartiaBoss} class.<br>
	 * It initializes the boss spawn IDs and registers event listeners.
	 */
	private KartiaBoss()
	{
		addSpawnId(BOSSES);
		setCreatureKillId(this::onCreatureKill, BOSSES);
		setCreatureAttackedId(this::onCreatureAttacked, BOSSES);
	}
	
	/**
	 * This method is called when an {@code Npc} is spawned.<br>
	 * It initializes the repeating timers for the NPC.
	 * @param npc The {@code Npc} instance that was just created.
	 */
	@Override
	public void onSpawn(Npc npc)
	{
		getTimers().addRepeatingTimer("NPC_SAY", 20000, npc, null);
	}
	
	/**
	 * Handles periodic timer events for the boss.<br>
	 * This method checks if specific conditions are met to trigger actions like dialogue.
	 * @param event The name of the timer event to process.
	 * @param params A set of statistics used as parameters for the event.
	 * @param npc The {@code Npc} instance that owns this script.
	 * @param player The {@code Player} instance involved in the interaction.
	 */
	@Override
	public void onTimerEvent(String event, StatSet params, Npc npc, Player player)
	{
		if (event.equals("NPC_SAY") && npc.isTargetable())
		{
			npc.broadcastSay(ChatType.NPC_SHOUT, NpcStringId.YOU_PUNY_INSECTS_DON_T_KNOW_YOUR_PLACE_YOU_CANNOT_STOP_ME);
		}
	}
	
	/**
	 * Handles the logic when a creature is killed.<br>
	 * This method triggers a specific shout and cancels timers for the target NPC.
	 * @param event The {@code OnCreatureDeath} event containing the target information.
	 */
	public void onCreatureKill(OnCreatureDeath event)
	{
		final Npc npc = event.getTarget().asNpc();
		npc.broadcastSay(ChatType.NPC_SHOUT, NpcStringId.NO_HOW_COULD_THIS_BE_I_CAN_T_GO_BACK_TO_NIHIL_LIKE_THIS);
		getTimers().cancelTimersOf(npc);
	}
	
	/**
	 * Handles logic when a creature is attacked.<br>
	 * This method checks the health of the target {@code Npc}.<br>
	 * It spawns minions at specific health thresholds.<br>
	 * It uses script values to ensure minions only spawn once per phase.
	 * @param event The {@code OnCreatureAttacked} event containing the target information.
	 */
	public void onCreatureAttacked(OnCreatureAttacked event)
	{
		final Npc npc = event.getTarget().asNpc();
		if ((npc.getCurrentHpPercent() <= 75) && npc.isScriptValue(0))
		{
			spawnMinions(npc);
			npc.setScriptValue(1);
		}
		else if ((npc.getCurrentHpPercent() <= 50) && npc.isScriptValue(1))
		{
			spawnMinions(npc);
			npc.setScriptValue(2);
		}
		else if ((npc.getCurrentHpPercent() <= 25) && npc.isScriptValue(2))
		{
			spawnMinions(npc);
			npc.setScriptValue(3);
		}
	}
	
	/**
	 * Spawns minions based on the boss level and party type.<br>
	 * It checks the {@code cartia_level} and {@code party_type} parameters.<br>
	 * The method adds three fighters and three mages if both types are valid.
	 * @param npc The {@code Npc} instance used to determine spawn parameters.
	 */
	public void spawnMinions(Npc npc)
	{
		final StatSet param = npc.getParameters();
		final int kartiaLevel = param.getInt("cartia_level", 0);
		final boolean isParty = param.getInt("party_type", 0) == 1;
		int fighter = 0;
		int mage = 0;
		switch (kartiaLevel)
		{
			case 85:
			{
				fighter = isParty ? FIGHTER_GROUP_85 : FIGHTER_SOLO_85;
				mage = isParty ? MAGE_GROUP_85 : MAGE_SOLO_85;
				break;
			}
			case 90:
			{
				fighter = isParty ? FIGHTER_GROUP_90 : FIGHTER_SOLO_90;
				mage = isParty ? MAGE_GROUP_90 : MAGE_SOLO_90;
				break;
			}
			case 95:
			{
				fighter = isParty ? FIGHTER_GROUP_95 : FIGHTER_SOLO_95;
				mage = isParty ? MAGE_GROUP_95 : MAGE_SOLO_95;
				break;
			}
		}
		
		if ((fighter > 0) && (mage > 0))
		{
			final int npcInstanceId = npc.getInstanceId();
			for (int i = 0; i < 3; i++)
			{
				addSpawn(fighter, npc, true, 0, false, npcInstanceId);
				addSpawn(mage, npc, true, 0, false, npcInstanceId);
			}
		}
	}
	
	/**
	 * This is the entry point for the {@code KartiaBoss} class.<br>
	 * It initializes a new instance of the boss script.
	 * @param args Command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		new KartiaBoss();
	}
}
