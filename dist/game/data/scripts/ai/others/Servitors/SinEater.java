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
package ai.others.Servitors;

import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.Summon;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.ListenerRegisterType;
import org.l2jmobius.gameserver.mechanics.events.annotations.Id;
import org.l2jmobius.gameserver.mechanics.events.annotations.RegisterEvent;
import org.l2jmobius.gameserver.mechanics.events.annotations.RegisterType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.creature.OnCreatureAttacked;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.creature.OnCreatureDeath;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.NpcSay;

/**
 * Handles the artificial intelligence for the {@code SinEater} NPC.<br>
 * This script defines the behaviors and reactions of this specific creature.
 * @author St3eT
 */
public class SinEater extends Script
{
	// NPCs
	private static final int SIN_EATER = 12564;
	
	/**
	 * Private constructor for the {@code SinEater} class.<br>
	 * This prevents other classes from creating new instances of this script.
	 */
	private SinEater()
	{
		addSummonSpawnId(SIN_EATER);
		addSummonTalkId(SIN_EATER);
	}
	
	/**
	 * Handles specific events for the {@code SinEater} script.<br>
	 * This method processes logic based on the provided event type.<br>
	 * It handles special dialogue and timers when a player talks to an NPC.
	 * @param event The name of the event being triggered.
	 * @param npc The {@code Npc} instance involved in the event.
	 * @param player The {@code Player} instance involved in the event.
	 * @return The result of the super class {@code onEvent} method.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.equals("TALK") && (player != null) && (player.getPet() != null))
		{
			if (getRandom(100) < 30)
			{
				final int random = getRandom(100);
				final Summon summon = player.getPet();
				if (random < 20)
				{
					broadcastSummonSay(summon, NpcStringId.YAWWWWN_IT_S_SO_BORING_HERE_WE_SHOULD_GO_AND_FIND_SOME_ACTION);
				}
				else if (random < 40)
				{
					broadcastSummonSay(summon, NpcStringId.HEY_IF_YOU_CONTINUE_TO_WASTE_TIME_YOU_WILL_NEVER_FINISH_YOUR_PENANCE);
				}
				else if (random < 60)
				{
					broadcastSummonSay(summon, NpcStringId.I_KNOW_YOU_DON_T_LIKE_ME_THE_FEELING_IS_MUTUAL);
				}
				else if (random < 80)
				{
					broadcastSummonSay(summon, NpcStringId.I_NEED_A_DRINK);
				}
				else
				{
					broadcastSummonSay(summon, NpcStringId.OH_THIS_IS_DRAGGING_ON_TOO_LONG_AT_THIS_RATE_I_WON_T_MAKE_IT_HOME_BEFORE_THE_SEVEN_SEALS_ARE_BROKEN);
				}
			}
			
			startQuestTimer("TALK", 60000, null, player);
		}
		
		return super.onEvent(event, npc, player);
	}
	
	/**
	 * Handles the logic when a creature dies.<br>
	 * This method triggers random dialogue for the {@code Summon}.<br>
	 * It selects one of three possible messages based on a random chance.
	 * @param event The {@code OnCreatureDeath} event containing the target data.
	 */
	@RegisterEvent(EventType.ON_CREATURE_DEATH)
	@RegisterType(ListenerRegisterType.NPC)
	@Id(SIN_EATER)
	public void onCreatureKill(OnCreatureDeath event)
	{
		final int random = getRandom(100);
		final Summon summon = event.getTarget().asSummon();
		if (random < 30)
		{
			broadcastSummonSay(summon, NpcStringId.OH_THIS_IS_JUST_GREAT_WHAT_ARE_YOU_GOING_TO_DO_NOW);
		}
		else if (random < 70)
		{
			broadcastSummonSay(summon, NpcStringId.YOU_INCONSIDERATE_MORON_CAN_T_YOU_EVEN_TAKE_CARE_OF_LITTLE_OLD_ME);
		}
		else
		{
			broadcastSummonSay(summon, NpcStringId.OH_NO_THE_MAN_WHO_EATS_ONE_S_SINS_HAS_DIED_PENITENCE_IS_FURTHER_AWAY);
		}
	}
	
	/**
	 * Handles the logic when a creature is attacked.<br>
	 * This method triggers random dialogue for summons based on an attack event.
	 * @param event The {@code OnCreatureAttacked} event containing target information.
	 */
	@RegisterEvent(EventType.ON_CREATURE_ATTACKED)
	@RegisterType(ListenerRegisterType.NPC)
	@Id(SIN_EATER)
	public void onCreatureAttacked(OnCreatureAttacked event)
	{
		if (getRandom(100) < 30)
		{
			final int random = getRandom(100);
			final Summon summon = event.getTarget().asSummon();
			if (random < 35)
			{
				broadcastSummonSay(summon, NpcStringId.OH_THAT_SMARTS);
			}
			else if (random < 70)
			{
				broadcastSummonSay(summon, NpcStringId.HEY_MASTER_PAY_ATTENTION_I_M_DYING_OVER_HERE);
			}
			else
			{
				broadcastSummonSay(summon, NpcStringId.WHAT_HAVE_I_DONE_TO_DESERVE_THIS);
			}
		}
	}
	
	/**
	 * This method is called when a new {@code Summon} is spawned.<br>
	 * It triggers a random dialogue for the summon.<br>
	 * It also starts a quest timer for the owner of the summon.
	 * @param summon The {@code Summon} instance that was just created.
	 */
	@Override
	public void onSummonSpawn(Summon summon)
	{
		broadcastSummonSay(summon, getRandomBoolean() ? NpcStringId.HEY_IT_SEEMS_LIKE_YOU_NEED_MY_HELP_DOESN_T_IT : NpcStringId.ALMOST_GOT_IT_DAMN_THESE_BLOODY_MANACLES);
		startQuestTimer("TALK", 60000, null, summon.getOwner());
	}
	
	/**
	 * Handles the logic for when a player talks to a {@code Summon}.<br>
	 * This method triggers random dialogue lines based on a small chance.
	 * @param summon The {@code Summon} entity that is being interacted with.
	 */
	@Override
	public void onSummonTalk(Summon summon)
	{
		if (getRandom(100) < 10)
		{
			final int random = getRandom(100);
			if (random < 25)
			{
				broadcastSummonSay(summon, NpcStringId.USING_A_SPECIAL_SKILL_HERE_COULD_TRIGGER_A_BLOODBATH);
			}
			else if (random < 50)
			{
				broadcastSummonSay(summon, NpcStringId.HEY_WHAT_DO_YOU_EXPECT_OF_ME);
			}
			else if (random < 75)
			{
				broadcastSummonSay(summon, NpcStringId.UGGGGGH_PUSH_IT_S_NOT_COMING_OUT);
			}
			else
			{
				broadcastSummonSay(summon, NpcStringId.AH_I_MISSED_THE_MARK);
			}
		}
	}
	
	/**
	 * Sends a general chat message from a {@code Summon} to all nearby players.<br>
	 * This method uses the provided {@code NpcStringId} for the text content.
	 * @param summon The {@code Summon} entity that will speak.
	 * @param npcstringId The unique identifier for the dialogue text.
	 */
	private void broadcastSummonSay(Summon summon, NpcStringId npcstringId)
	{
		summon.broadcastPacket(new NpcSay(summon.getObjectId(), ChatType.NPC_GENERAL, summon.getId(), npcstringId));
	}
	
	/**
	 * This is the main entry point for the {@code SinEater} class.<br>
	 * It initializes a new instance of the {@code SinEater} script.
	 * @param args Command line arguments passed to the program.
	 */
	public static void main(String[] args)
	{
		new SinEater();
	}
}
