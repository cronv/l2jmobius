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
package ai.areas.DenOfEvil;

import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.itemcontainer.Inventory;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.enums.ChatType;

/**
 * This class defines the artificial intelligence for the {@code FrightenedRagnaOrc} NPC.<br>
 * It handles the specific behaviors and actions of this mob within the {@code DenOfEvil} area.
 * @author Gladicek, malyelfik
 */
public class FrightenedRagnaOrc extends Script
{
	// NPC ID
	private static final int MOB_ID = 18807;
	
	// Chances
	private static final int ADENA = 10000;
	private static final int CHANCE = 1000;
	private static final int ADENA2 = 1000;
	private static final int CHANCE2 = 10;
	
	// Skill
	private static final SkillHolder SKILL = new SkillHolder(6234, 1);
	
	/**
	 * Initializes the {@code FrightenedRagnaOrc} script.<br>
	 * Registers the required attack and kill IDs for the mob.<br>
	 * This constructor sets up the basic behavior for this NPC.
	 */
	private FrightenedRagnaOrc()
	{
		addAttackId(MOB_ID);
		addKillId(MOB_ID);
	}
	
	/**
	 * Handles the logic when an {@code Npc} is attacked by a {@code Player}.<br>
	 * It checks the health of the {@code Npc} to trigger specific quest events.
	 * @param npc The {@code Npc} being attacked.
	 * @param attacker The {@code Player} who performed the attack.
	 * @param damage The amount of damage dealt to the {@code Npc}.
	 * @param isSummon Whether the attack was performed by a summon.
	 */
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	{
		if (npc.isScriptValue(0))
		{
			npc.setScriptValue(1);
			startQuestTimer("say", (getRandom(5) + 3) * 1000, npc, null, true);
		}
		else if ((npc.getCurrentHp() < (npc.getMaxHp() * 0.2)) && npc.isScriptValue(1))
		{
			startQuestTimer("reward", 10000, npc, attacker);
			npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.WAIT_WAIT_STOP_SAVE_ME_AND_I_LL_GIVE_YOU_1000_ADENA);
			npc.setScriptValue(2);
		}
	}
	
	/**
	 * This method is called when an {@code Npc} is killed by a {@code Player}.<br>
	 * It triggers a random dialogue message from the NPC.<br>
	 * It also cancels specific quest timers for the NPC and the player.
	 * @param npc The {@code Npc} that was killed.
	 * @param player The {@code Player} who performed the kill.
	 * @param isSummon Indicates if the action was performed by a summon.
	 */
	@Override
	public void onKill(Npc npc, Player player, boolean isSummon)
	{
		final NpcStringId msg = getRandomBoolean() ? NpcStringId.A_CURSE_UPON_YOU_2 : NpcStringId.I_DON_T_WANT_TO_FIGHT_YOU_2;
		npc.broadcastSay(ChatType.NPC_GENERAL, msg);
		cancelQuestTimer("say", npc, null);
		cancelQuestTimer("reward", npc, player);
	}
	
	/**
	 * Handles specific script events for the frightened Ragna Orc.<br>
	 * Processes logic for saying dialogue, giving rewards, and despawning.
	 * @param event The name of the event to trigger.
	 * @param npc The {@code Npc} instance involved in the event.
	 * @param player The {@code Player} instance interacting with the NPC.
	 * @return A {@code null} value after processing the event.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "say":
			{
				if (npc.isDead() || !npc.isScriptValue(1))
				{
					cancelQuestTimer("say", npc, null);
					return null;
				}
				
				final NpcStringId msg = getRandomBoolean() ? NpcStringId.I_DON_T_WANT_TO_FIGHT_YOU : NpcStringId.IS_THIS_REALLY_NECESSARY;
				npc.broadcastSay(ChatType.NPC_GENERAL, msg);
				break;
			}
			case "reward":
			{
				if (!npc.isDead() && npc.isScriptValue(2))
				{
					if (getRandom(100000) < CHANCE2)
					{
						final NpcStringId msg = getRandomBoolean() ? NpcStringId.TH_THANKS_I_COULD_HAVE_BECOME_GOOD_FRIENDS_WITH_YOU : NpcStringId.I_LL_GIVE_YOU_1000_ADENA_LIKE_I_PROMISED_I_M_AN_ORC_WHO_ALWAYS_KEEPS_HIS_PROMISES;
						npc.broadcastSay(ChatType.NPC_GENERAL, msg);
						npc.setScriptValue(3);
						npc.doCast(SKILL.getSkill());
						for (int i = 0; i < 10; i++)
						{
							npc.dropItem(player, Inventory.ADENA_ID, ADENA2);
						}
					}
					else if (getRandom(100000) < CHANCE)
					{
						final NpcStringId msg = getRandomBoolean() ? NpcStringId.TH_THANKS_I_COULD_HAVE_BECOME_GOOD_FRIENDS_WITH_YOU : NpcStringId.SORRY_BUT_THIS_IS_ALL_I_HAVE_GIVE_ME_A_BREAK;
						npc.broadcastSay(ChatType.NPC_GENERAL, msg);
						npc.setScriptValue(3);
						npc.doCast(SKILL.getSkill());
						for (int i = 0; i < 10; i++)
						{
							npc.dropItem(player, Inventory.ADENA_ID, ADENA);
						}
					}
					else
					{
						final NpcStringId msg = getRandomBoolean() ? NpcStringId.THANKS_BUT_THAT_THING_ABOUT_1000_ADENA_WAS_A_LIE_SEE_YA : NpcStringId.YOU_RE_PRETTY_DUMB_TO_BELIEVE_ME;
						npc.broadcastSay(ChatType.NPC_GENERAL, msg);
					}
					
					startQuestTimer("despawn", 1000, npc, null);
				}
				break;
			}
			case "despawn":
			{
				npc.setRunning();
				npc.getAI().setIntentionMoveTo(new Location((npc.getX() + getRandom(-800, 800)), (npc.getY() + getRandom(-800, 800)), npc.getZ(), npc.getHeading()));
				npc.deleteMe();
				break;
			}
		}
		
		return null;
	}
	
	/**
	 * This is the entry point for the {@code FrightenedRagnaOrc} class.<br>
	 * It initializes a new instance of the script.
	 * @param args Command line arguments passed to the program.
	 */
	public static void main(String[] args)
	{
		new FrightenedRagnaOrc();
	}
}
