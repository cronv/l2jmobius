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
package ai.others;

import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.enums.ChatType;

/**
 * This class defines the artificial intelligence behavior for the NPC {@code Crimson Hatu Otis} in the Kamaloka (33) area.<br>
 * It handles the specific actions and logic for this mob using the {@code Script} system.
 * @author Gladicek
 */
public class CrimsonHatuOtis extends Script
{
	// Npc
	private static final int CRIMSON_HATU_OTIS = 18558;
	
	// Skills
	private static final SkillHolder BOSS_SPINING_SLASH = new SkillHolder(4737, 1);
	private static final SkillHolder BOSS_HASTE = new SkillHolder(4175, 1);
	
	/**
	 * Private constructor for the {@code CrimsonHatuOtis} class.<br>
	 * This constructor initializes the required attack and kill IDs.<br>
	 * It prevents other classes from creating new instances of this script.
	 */
	private CrimsonHatuOtis()
	{
		addAttackId(CRIMSON_HATU_OTIS);
		addKillId(CRIMSON_HATU_OTIS);
	}
	
	/**
	 * Handles specific events for the Crimson Hatu Otis NPC.<br>
	 * It processes skill usage and buff logic based on the event type.
	 * @param event The name of the event to trigger.
	 * @param npc The {@code Npc} instance involved in the event.
	 * @param player The {@code Player} instance involved in the event.
	 * @return A {@code String} result or {@code null}.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "SKILL":
			{
				if (npc.isDead())
				{
					cancelQuestTimer("SKILL", npc, null);
					return null;
				}
				
				npc.setTarget(player);
				npc.doCast(BOSS_SPINING_SLASH.getSkill());
				startQuestTimer("SKILL", 60000, npc, null);
				break;
			}
			case "BUFF":
			{
				if (npc.isScriptValue(2))
				{
					npc.setTarget(npc);
					npc.doCast(BOSS_HASTE.getSkill());
				}
				break;
			}
		}
		
		return super.onEvent(event, npc, player);
	}
	
	/**
	 * Handles the logic when an {@code Npc} is attacked by a {@code Player}.<br>
	 * This method manages specific script values and quest timers based on health.
	 * @param npc The {@code Npc} being attacked.
	 * @param attacker The {@code Player} performing the attack.
	 * @param damage The amount of damage dealt to the {@code Npc}.
	 * @param isSummon Indicates if the attack was performed by a summon.
	 */
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	{
		if (npc.isScriptValue(0))
		{
			npc.setScriptValue(1);
			startQuestTimer("SKILL", 5000, npc, null);
		}
		else if (npc.isScriptValue(1) && (npc.getCurrentHp() < (npc.getMaxHp() * 0.3)))
		{
			npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.I_VE_HAD_IT_UP_TO_HERE_WITH_YOU_I_LL_TAKE_CARE_OF_YOU);
			npc.setScriptValue(2);
			startQuestTimer("BUFF", 1000, npc, null);
		}
	}
	
	/**
	 * Handles the logic when an {@code Npc} is killed by a {@code Player}.<br>
	 * This method cancels specific quest timers for the target.
	 * @param npc The {@code Npc} that was defeated.
	 * @param player The {@code Player} who dealt the killing blow.
	 * @param isSummon Indicates if the kill occurred on a summon.
	 */
	@Override
	public void onKill(Npc npc, Player player, boolean isSummon)
	{
		cancelQuestTimer("SKILL", npc, null);
		cancelQuestTimer("BUFF", npc, null);
	}
	
	/**
	 * This is the entry point for the {@code CrimsonHatuOtis} class.<br>
	 * It initializes a new instance of the script.
	 * @param args Command line arguments passed to the program.
	 */
	public static void main(String[] args)
	{
		new CrimsonHatuOtis();
	}
}
