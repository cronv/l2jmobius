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

import org.l2jmobius.gameserver.entity.actor.Attackable;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;

/**
 * This {@code SummonPc} script handles the logic for summoning a player to an NPC during an attack.<br>
 * It triggers when an NPC targets a {@code Player}.
 * @author Zoey76
 */
public class SummonPc extends Script
{
	// NPCs
	private static final int PORTA = 20213;
	private static final int PERUM = 20221;
	
	// Skill
	private static final SkillHolder SUMMON_PC = new SkillHolder(4161, 1);
	
	/**
	 * Private constructor for the {@code SummonPc} class.<br>
	 * This constructor initializes the required attack and spell IDs.
	 */
	private SummonPc()
	{
		addAttackId(PORTA, PERUM);
		addSpellFinishedId(PORTA, PERUM);
	}
	
	/**
	 * Handles the logic when an {@code Npc} is attacked by a {@code Player}.<br>
	 * This method checks if the NPC should cast a summon skill based on distance and chance.
	 * @param npc The {@code Npc} being attacked.
	 * @param attacker The {@code Player} performing the attack.
	 * @param damage The amount of damage dealt to the NPC.
	 * @param isSummon A boolean indicating if the attack is a summon action.
	 */
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	{
		final int chance = getRandom(100);
		final boolean attacked = npc.getVariables().getBoolean("attacked", false);
		if ((npc.calculateDistance3D(attacker) > 300) && !attacked)
		{
			if (chance < 50)
			{
				final double currentMp = npc.getCurrentMp();
				final double currentHp = npc.getCurrentHp();
				final boolean skillDisabled = npc.isSkillDisabled(SUMMON_PC.getSkill());
				if ((SUMMON_PC.getSkill().getMpConsume() < currentMp) && (SUMMON_PC.getSkill().getHpConsume() < currentHp) && !skillDisabled)
				{
					npc.setTarget(attacker);
					npc.doCast(SUMMON_PC.getSkill());
				}
				
				if ((SUMMON_PC.getSkill().getMpConsume() < currentMp) && (SUMMON_PC.getSkill().getHpConsume() < currentHp) && !skillDisabled)
				{
					npc.setTarget(attacker);
					npc.doCast(SUMMON_PC.getSkill());
					npc.getVariables().set("attacked", true);
				}
			}
		}
		else if ((npc.calculateDistance3D(attacker) > 100) && !attacked)
		{
			final Attackable monster = npc.asAttackable();
			if ((monster.getMostHated() != null) && (((monster.getMostHated() == attacker) && (chance < 50)) || (chance < 10)) && (SUMMON_PC.getSkill().getMpConsume() < npc.getCurrentMp()) && (SUMMON_PC.getSkill().getHpConsume() < npc.getCurrentHp()) && !npc.isSkillDisabled(SUMMON_PC.getSkill()))
			{
				npc.setTarget(attacker);
				npc.doCast(SUMMON_PC.getSkill());
				npc.getVariables().set("attacked", true);
			}
		}
	}
	
	/**
	 * Handles the logic after a spell is completed.<br>
	 * This method checks if the {@code SUMMON_PC} skill was used.<br>
	 * If valid, it teleports the {@code Player} to the {@code Npc}.
	 * @param npc The {@code Npc} that cast or received the effect.
	 * @param player The {@code Player} who performed the action.
	 * @param skill The {@code Skill} object used in this event.
	 */
	@Override
	public void onSpellFinished(Npc npc, Player player, Skill skill)
	{
		if ((skill.getId() == SUMMON_PC.getSkillId()) && !npc.isDead() && npc.getVariables().getBoolean("attacked", false))
		{
			player.teleToLocation(npc);
			npc.getVariables().set("attacked", false);
		}
	}
	
	/**
	 * This is the entry point for the {@code SummonPc} class.<br>
	 * It initializes a new instance of the script to start the program.
	 * @param args Command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		new SummonPc();
	}
}
