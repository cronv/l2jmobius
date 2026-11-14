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
package ai.bosses.Helios;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.SkillCaster;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.mechanics.variables.NpcVariables;
import org.l2jmobius.gameserver.util.MathUtil;

/**
 * This class defines the artificial intelligence behavior for the {@code Helios1} boss.<br>
 * It handles the logic for skills, movements, and interactions during combat.
 * @author NviX
 */
public class Helios1 extends Script
{
	// Npc
	private static final int HELIOS = 29303;
	
	// Skills
	private static final SkillHolder HELIOS_PLASMA = new SkillHolder(16609, 1);
	private static final SkillHolder HELIOS_GRAND_PLASMA = new SkillHolder(16610, 1);
	private static final SkillHolder HELIOS_THRONE_OF_AGONY = new SkillHolder(16611, 1);
	
	/**
	 * This is a private constructor for the {@code Helios1} class.<br>
	 * It initializes the required attack and spell IDs.<br>
	 * This method should not be called directly from outside this class.
	 */
	private Helios1()
	{
		addAttackId(HELIOS);
		addSpellFinishedId(HELIOS);
	}
	
	/**
	 * Handles specific events for the {@code Helios1} script.<br>
	 * It checks if the {@code event} is {@code "MANAGE_SKILLS"} and calls the corresponding logic.<br>
	 * Finally, it returns the result of the superclass method.
	 * @param event The name of the event to trigger.
	 * @param npc The {@code Npc} instance involved in the event.
	 * @param player The {@code Player} instance involved in the event.
	 * @return The result of the superclass {@code onEvent} call.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "MANAGE_SKILLS":
			{
				if (npc != null)
				{
					manageSkills(npc);
				}
				break;
			}
		}
		
		return super.onEvent(event, npc, player);
	}
	
	/**
	 * Handles the logic when an {@code Npc} is attacked by a {@code Player}.<br>
	 * This method updates AI parameters based on current health and damage.<br>
	 * It also triggers skill management for the boss.
	 * @param npc The {@code Npc} being attacked.
	 * @param attacker The {@code Player} performing the attack.
	 * @param damage The amount of damage dealt.
	 * @param isSummon Indicates if the attack comes from a summon.
	 * @param skill The {@code Skill} used for the attack, or {@code null}.
	 */
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon, Skill skill)
	{
		if (npc.getId() == HELIOS)
		{
			if (skill == null)
			{
				refreshAiParams(attacker, npc, (damage * 1000));
			}
			else if (npc.getCurrentHp() < (npc.getMaxHp() * 0.25))
			{
				refreshAiParams(attacker, npc, ((damage / 3) * 100));
			}
			else if (npc.getCurrentHp() < (npc.getMaxHp() * 0.5))
			{
				refreshAiParams(attacker, npc, (damage * 20));
			}
			else if (npc.getCurrentHp() < (npc.getMaxHp() * 0.75))
			{
				refreshAiParams(attacker, npc, (damage * 10));
			}
			else
			{
				refreshAiParams(attacker, npc, ((damage / 3) * 20));
			}
			
			manageSkills(npc);
		}
	}
	
	/**
	 * Updates the AI parameters for an {@code Npc}.<br>
	 * This method is called when a {@code Creature} deals damage.<br>
	 * It recalculates behavior based on the current combat state.
	 * @param attacker The {@code Creature} performing the action.
	 * @param npc The {@code Npc} whose AI is being updated.
	 * @param damage The amount of damage dealt in the last hit.
	 */
	private void refreshAiParams(Creature attacker, Npc npc, int damage)
	{
		refreshAiParams(attacker, npc, damage, damage);
	}
	
	/**
	 * Updates the AI parameters for an {@code Npc} based on combat data.<br>
	 * This method recalculates aggro values and tracks the primary target.
	 * @param attacker The {@code Creature} that dealt damage.
	 * @param npc The {@code Npc} whose AI is being updated.
	 * @param damage The amount of damage dealt by the attacker.
	 * @param aggro The current aggro value of the target.
	 */
	private void refreshAiParams(Creature attacker, Npc npc, int damage, int aggro)
	{
		final int newAggroVal = damage + getRandom(3000);
		final int aggroVal = aggro + 1000;
		final NpcVariables vars = npc.getVariables();
		for (int i = 0; i < 3; i++)
		{
			if (attacker == vars.getObject("c_quest" + i, Creature.class))
			{
				if (vars.getInt("i_quest" + i) < aggroVal)
				{
					vars.set("i_quest" + i, newAggroVal);
				}
				return;
			}
		}
		
		final int index = MathUtil.getIndexOfMinValue(vars.getInt("i_quest0"), vars.getInt("i_quest1"), vars.getInt("i_quest2"));
		vars.set("i_quest" + index, newAggroVal);
		vars.set("c_quest" + index, attacker);
	}
	
	/**
	 * This method is called when a spell cast by an {@code Npc} finishes.<br>
	 * It triggers the skill management timer for the boss.
	 * @param npc The {@code Npc} that finished casting the spell.
	 * @param player The {@code Player} who was targeted or involved in the action.
	 * @param skill The {@code Skill} that has just completed its execution.
	 */
	@Override
	public void onSpellFinished(Npc npc, Player player, Skill skill)
	{
		startQuestTimer("MANAGE_SKILLS", 1000, npc, null);
	}
	
	/**
	 * Handles the logic for selecting and casting skills for an {@code Npc}.<br>
	 * It checks if the {@code Npc} is eligible to cast a skill based on combat status.<br>
	 * The method identifies the nearest valid target from quest variables.<br>
	 * It then selects a random skill and executes it if conditions are met.
	 * @param npc The {@code Npc} instance whose skills will be managed.
	 */
	private void manageSkills(Npc npc)
	{
		if (npc.isCastingNow(SkillCaster::isAnyNormalType) || npc.isCoreAIDisabled() || !npc.isInCombat())
		{
			return;
		}
		
		final NpcVariables vars = npc.getVariables();
		for (int i = 0; i < 3; i++)
		{
			final Creature attacker = vars.getObject("c_quest" + i, Creature.class);
			if ((attacker == null) || ((npc.calculateDistance3D(attacker) > 9000) || attacker.isDead()))
			{
				vars.set("i_quest" + i, 0);
			}
		}
		
		final int index = MathUtil.getIndexOfMaxValue(vars.getInt("i_quest0"), vars.getInt("i_quest1"), vars.getInt("i_quest2"));
		final Creature player = vars.getObject("c_quest" + index, Creature.class);
		final int i2 = vars.getInt("i_quest" + index);
		if ((i2 > 0) && (getRandom(100) < 70))
		{
			vars.set("i_quest" + index, 500);
		}
		
		SkillHolder skillToCast = null;
		if ((player != null) && !player.isDead())
		{
			if (getRandom(100) < 30)
			{
				skillToCast = HELIOS_GRAND_PLASMA;
			}
			else if (getRandom(100) < 50)
			{
				skillToCast = HELIOS_THRONE_OF_AGONY;
			}
			else
			{
				skillToCast = HELIOS_PLASMA;
			}
		}
		
		if ((skillToCast != null) && SkillCaster.checkUseConditions(npc, skillToCast.getSkill()))
		
		{
			npc.setTarget(player);
			npc.doCast(skillToCast.getSkill());
		}
	}
	
	/**
	 * This is the entry point for the {@code Helios1} class.<br>
	 * It initializes a new instance of the {@code Helios1} script.
	 * @param args Command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		new Helios1();
	}
}
