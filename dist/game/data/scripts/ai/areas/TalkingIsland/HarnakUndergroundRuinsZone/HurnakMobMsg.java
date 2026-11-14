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
package ai.areas.TalkingIsland.HarnakUndergroundRuinsZone;

import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.enums.ChatType;

/**
 * This class handles the message scripts for monsters in the {@code HarnakUndergroundRuinsZone}.<br>
 * It defines specific dialogue or text outputs triggered by monster AI behaviors.
 * @author Gigi
 * @date 2017-03-11 - [13:50:02]
 */
public class HurnakMobMsg extends Script
{
	// NPCs
	private static final int NOCTUM = 23349;
	private static final int KRAKIA = 22932;
	private static final int SEKNUS = 22938;
	
	// Attack messages
	private static final NpcStringId[] ON_ATTACK_MSG =
	{
		NpcStringId.I_NEED_HELP,
		NpcStringId.I_NEED_HEAL,
		NpcStringId.COME_AT_ME
	};
	private static final NpcStringId[] ON_FAILED_MSG =
	{
		NpcStringId.I_M_GOING_TO_BACK_OFF_FOR_A_BIT,
		NpcStringId.ONLY_DEATH_AWAITS_FOR_THE_WEAK
	};
	
	/**
	 * Private constructor for the {@code HurnakMobMsg} class.<br>
	 * This constructor initializes the script by registering specific IDs.<br>
	 * It sets up which mobs trigger attack and kill messages.
	 */
	private HurnakMobMsg()
	{
		addAttackId(NOCTUM, KRAKIA);
		addKillId(SEKNUS);
	}
	
	/**
	 * Handles specific events for the {@code Npc}.<br>
	 * It checks if the {@code event} is equal to {@code "ATTACK"}.<br>
	 * If it matches, the {@code Npc} broadcasts a random message.
	 * @param event The name of the event to trigger.
	 * @param npc The {@code Npc} instance involved in the event.
	 * @param player The {@code Player} instance involved in the event.
	 * @return The result of the super class {@code onEvent} method.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.equals("ATTACK"))
		{
			npc.broadcastSay(ChatType.NPC_GENERAL, getRandomEntry(ON_ATTACK_MSG));
		}
		
		return super.onEvent(event, npc, player);
	}
	
	/**
	 * Handles the logic when an {@code Npc} is attacked by a {@code Player}.<br>
	 * This method triggers specific behaviors based on the damage dealt.<br>
	 * It checks if the target is alive and matches specific IDs.
	 * @param npc The {@code Npc} being attacked.
	 * @param attacker The {@code Player} performing the attack.
	 * @param damage The amount of damage dealt to the {@code Npc}.
	 * @param isSummon Whether the attack was initiated by a summon.
	 */
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	{
		if ((npc != null) && !npc.isDead() && ((npc.getId() == NOCTUM) || (npc.getId() == KRAKIA)))
		{
			startQuestTimer("ATTACK", (getRandom(7) + 3) * 1000, npc, null);
		}
	}
	
	/**
	 * This method is called when an {@code Npc} is killed.<br>
	 * It triggers a broadcast message for the defeated monster.<br>
	 * The message is selected from a list of failed combat phrases.
	 * @param npc The {@code Npc} that was just killed.
	 * @param attacker The {@code Player} who dealt the killing blow.
	 * @param isSummon Indicates if the kill occurred during a summon action.
	 */
	@Override
	public void onKill(Npc npc, Player attacker, boolean isSummon)
	{
		npc.broadcastSay(ChatType.NPC_GENERAL, getRandomEntry(ON_FAILED_MSG));
		return;
	}
	
	/**
	 * This is the entry point for the {@code HurnakMobMsg} class.<br>
	 * It initializes a new instance of the script.
	 * @param args Command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		new HurnakMobMsg();
	}
}
